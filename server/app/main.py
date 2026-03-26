from __future__ import annotations

import mimetypes
import secrets
import time
from pathlib import Path
from typing import Dict, List, Optional

try:
    from typing import Annotated
except ImportError:
    from typing_extensions import Annotated
from uuid import uuid4

from fastapi import Depends, FastAPI, File, Form, Header, HTTPException, UploadFile, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse

from .db import DB_PATH, get_connection, init_db
from .models import (
    MessageSyncRequest,
    MessageSyncResponse,
    PairCodeCreateResponse,
    PairCodeUnlockRequest,
    PairCodeUnlockResponse,
    PairStatusResponse,
    PhotoBackupItemResponse,
    PhotoBackupListResponse,
    PhotoBackupSummaryResponse,
    PhotoUploadResponse,
    SyncedMessagePayload,
    UsageEventPayload,
    UsageSnapshotResponse,
    UsageUploadRequest,
)

PAIR_CODE_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
PAIR_CODE_LENGTH = 4
PHOTO_UPLOADS_DIR = Path(__file__).resolve().parent.parent / "uploads" / "photos"

app = FastAPI(title="Link Server", version="0.4.1")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=False,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.on_event("startup")
def startup() -> None:
    init_db()
    PHOTO_UPLOADS_DIR.mkdir(parents=True, exist_ok=True)


def now_millis() -> int:
    return int(time.time() * 1000)


def normalize_pair_code(pair_code: str) -> str:
    return pair_code.strip().upper()


def build_pair_id(pair_code: str) -> str:
    return f"pair_{normalize_pair_code(pair_code).lower()}"


def create_pair_code(connection) -> str:
    for _ in range(32):
        candidate = "".join(secrets.choice(PAIR_CODE_ALPHABET) for _ in range(PAIR_CODE_LENGTH))
        row = connection.execute("SELECT 1 FROM pairs WHERE pair_code = ?", (candidate,)).fetchone()
        if row is None:
            return candidate
    raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail="pair_code_generation_failed")


def bearer_token(
    authorization: Annotated[Optional[str], Header()] = None,
) -> str:
    if not authorization:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="missing_authorization")
    scheme, _, token = authorization.partition(" ")
    if scheme.lower() != "bearer" or not token:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="invalid_authorization")
    return token


def get_session(token: str) -> Dict[str, str]:
    with get_connection() as connection:
        row = connection.execute(
            """
            SELECT session_token, pair_id, nickname
            FROM sessions
            WHERE session_token = ?
            """,
            (token,),
        ).fetchone()
    if row is None:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="session_not_found")
    return {
        "session_token": row["session_token"],
        "pair_id": row["pair_id"],
        "nickname": row["nickname"],
    }


def require_pair_access(pair_id: str, token: str) -> Dict[str, str]:
    session = get_session(token)
    if session["pair_id"] != pair_id:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="pair_access_denied")
    return session


def safe_photo_suffix(display_name: str, mime_type: Optional[str]) -> str:
    suffix = Path(display_name).suffix
    if suffix:
        return suffix
    guessed = mimetypes.guess_extension(mime_type or "")
    return guessed or ".bin"


@app.get("/health")
def health() -> Dict[str, str]:
    return {"status": "ok", "db_path": str(DB_PATH), "version": app.version}


@app.post("/v1/internal/pair-codes/create", response_model=PairCodeCreateResponse)
@app.post("/v1/internal/invites/create", response_model=PairCodeCreateResponse)
def create_pair_code_entry() -> PairCodeCreateResponse:
    created_at = now_millis()
    expires_in_minutes = 60
    expires_at = created_at + expires_in_minutes * 60 * 1000
    remaining_slots = 2

    with get_connection() as connection:
        pair_code = create_pair_code(connection)
        pair_id = build_pair_id(pair_code)
        connection.execute(
            """
            INSERT INTO pairs(pair_id, pair_code, created_at_epoch_millis)
            VALUES(?, ?, ?)
            """,
            (pair_id, pair_code, created_at),
        )
        connection.execute(
            """
            INSERT INTO invites(invite_key, pair_code, expires_at_epoch_millis, created_at_epoch_millis, max_uses, use_count)
            VALUES(?, ?, ?, ?, ?, 0)
            """,
            (pair_code, pair_code, expires_at, created_at, remaining_slots),
        )

    return PairCodeCreateResponse(
        pair_code=pair_code,
        expires_in_minutes=expires_in_minutes,
        remaining_slots=remaining_slots,
    )


@app.post("/v1/auth/pair-code/unlock", response_model=PairCodeUnlockResponse)
@app.post("/v1/auth/invite/unlock", response_model=PairCodeUnlockResponse)
def unlock_pair_code(payload: PairCodeUnlockRequest) -> PairCodeUnlockResponse:
    now = now_millis()
    pair_code = normalize_pair_code(payload.pair_code)
    pair_id = build_pair_id(pair_code)
    nickname = payload.nickname.strip()

    with get_connection() as connection:
        pair_entry = connection.execute(
            """
            SELECT pair_code, expires_at_epoch_millis, use_count, max_uses
            FROM invites
            WHERE pair_code = ?
            ORDER BY created_at_epoch_millis DESC
            LIMIT 1
            """,
            (pair_code,),
        ).fetchone()
        if pair_entry is None:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="pair_code_not_found")

        existing_session = connection.execute(
            """
            SELECT session_token, nickname
            FROM sessions
            WHERE pair_id = ? AND device_public_key = ?
            ORDER BY created_at_epoch_millis DESC
            LIMIT 1
            """,
            (pair_id, payload.device_public_key),
        ).fetchone()
        member_count = connection.execute(
            "SELECT COUNT(*) AS count FROM pair_members WHERE pair_id = ?",
            (pair_id,),
        ).fetchone()["count"]
        if existing_session is not None:
            return PairCodeUnlockResponse(
                session_token=existing_session["session_token"],
                pair_id=pair_id,
                pair_code=pair_code,
                display_name=existing_session["nickname"],
                member_count=member_count,
            )

        if pair_entry["expires_at_epoch_millis"] < now:
            raise HTTPException(status_code=status.HTTP_410_GONE, detail="pair_code_expired")
        if pair_entry["use_count"] >= pair_entry["max_uses"] or member_count >= 2:
            raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="pair_code_exhausted")

        session_token = secrets.token_urlsafe(24)
        connection.execute(
            """
            INSERT INTO sessions(session_token, pair_id, nickname, device_public_key, created_at_epoch_millis)
            VALUES(?, ?, ?, ?, ?)
            """,
            (session_token, pair_id, nickname, payload.device_public_key, now),
        )
        connection.execute(
            """
            INSERT INTO pair_members(pair_id, nickname, session_token, joined_at_epoch_millis)
            VALUES(?, ?, ?, ?)
            """,
            (pair_id, nickname, session_token, now),
        )
        connection.execute(
            """
            UPDATE invites
            SET use_count = use_count + 1,
                last_used_at_epoch_millis = ?,
                last_used_by_session_token = ?,
                used_at_epoch_millis = ?,
                used_by_session_token = ?
            WHERE pair_code = ?
            """,
            (now, session_token, now, session_token, pair_code),
        )
        member_count += 1

    return PairCodeUnlockResponse(
        session_token=session_token,
        pair_id=pair_id,
        pair_code=pair_code,
        display_name=nickname,
        member_count=member_count,
    )


@app.get("/v1/pair/status/{pair_id}", response_model=PairStatusResponse)
def pair_status(pair_id: str, token: Annotated[str, Depends(bearer_token)]) -> PairStatusResponse:
    session = require_pair_access(pair_id, token)
    with get_connection() as connection:
        pair = connection.execute(
            "SELECT pair_id, pair_code, usage_sharing_enabled FROM pairs WHERE pair_id = ?",
            (pair_id,),
        ).fetchone()
        if pair is None:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="pair_not_found")
        partner = connection.execute(
            """
            SELECT nickname
            FROM pair_members
            WHERE pair_id = ? AND session_token != ?
            ORDER BY joined_at_epoch_millis ASC
            LIMIT 1
            """,
            (pair_id, session["session_token"]),
        ).fetchone()
        member_count = connection.execute(
            "SELECT COUNT(*) AS count FROM pair_members WHERE pair_id = ?",
            (pair_id,),
        ).fetchone()["count"]

    return PairStatusResponse(
        pair_id=pair_id,
        pair_code=pair["pair_code"],
        partner_nickname=partner["nickname"] if partner else "等待配对",
        usage_sharing_enabled=bool(pair["usage_sharing_enabled"]),
        member_count=member_count,
    )


def load_messages(connection, pair_id: str, session_token: str) -> List[SyncedMessagePayload]:
    rows = connection.execute(
        """
        SELECT m.id, m.body, m.sent_at_epoch_millis, s.nickname AS author_nickname, m.session_token
        FROM messages AS m
        JOIN sessions AS s ON s.session_token = m.session_token
        WHERE m.pair_id = ?
        ORDER BY m.sent_at_epoch_millis ASC, m.created_at_epoch_millis ASC
        """,
        (pair_id,),
    ).fetchall()
    return [
        SyncedMessagePayload(
            id=row["id"],
            body=row["body"],
            sent_at_epoch_millis=row["sent_at_epoch_millis"],
            author_nickname=row["author_nickname"],
            from_me=row["session_token"] == session_token,
        )
        for row in rows
    ]


@app.post("/v1/messages/sync", response_model=MessageSyncResponse)
def sync_messages(
    payload: MessageSyncRequest,
    token: Annotated[str, Depends(bearer_token)],
) -> MessageSyncResponse:
    session = require_pair_access(payload.pair_id, token)
    acknowledged_ids: List[str] = []
    now = now_millis()

    with get_connection() as connection:
        for message in payload.outgoing_messages:
            existing = connection.execute(
                """
                SELECT local_id
                FROM messages
                WHERE pair_id = ? AND session_token = ? AND local_id = ?
                """,
                (payload.pair_id, session["session_token"], message.local_id),
            ).fetchone()
            if existing is None:
                connection.execute(
                    """
                    INSERT INTO messages(id, pair_id, session_token, local_id, body, sent_at_epoch_millis, created_at_epoch_millis)
                    VALUES(?, ?, ?, ?, ?, ?, ?)
                    """,
                    (
                        str(uuid4()),
                        payload.pair_id,
                        session["session_token"],
                        message.local_id,
                        message.body,
                        message.sent_at_epoch_millis,
                        now,
                    ),
                )
            acknowledged_ids.append(message.local_id)

        messages = load_messages(connection, payload.pair_id, session["session_token"])

    return MessageSyncResponse(
        acknowledged_message_ids=acknowledged_ids,
        messages=messages,
    )


@app.post("/v1/usage/upload")
def upload_usage(
    payload: UsageUploadRequest,
    token: Annotated[str, Depends(bearer_token)],
) -> Dict[str, int]:
    session = require_pair_access(payload.pair_id, token)
    now = now_millis()

    with get_connection() as connection:
        snapshot_id = connection.execute(
            """
            INSERT INTO usage_snapshots(pair_id, session_token, owner_nickname, captured_at_epoch_millis, created_at_epoch_millis)
            VALUES(?, ?, ?, ?, ?)
            """,
            (
                payload.pair_id,
                session["session_token"],
                session["nickname"],
                payload.captured_at_epoch_millis,
                now,
            ),
        ).lastrowid
        connection.executemany(
            """
            INSERT INTO usage_events(snapshot_id, app_name, package_name, time_label, duration_label)
            VALUES(?, ?, ?, ?, ?)
            """,
            [
                (
                    snapshot_id,
                    event.app_name,
                    event.package_name,
                    event.time_label,
                    event.duration_label,
                )
                for event in payload.events
            ],
        )

    return {"stored_events": len(payload.events)}


@app.get("/v1/usage/latest/{pair_id}", response_model=UsageSnapshotResponse)
def latest_usage(
    pair_id: str,
    token: Annotated[str, Depends(bearer_token)],
) -> UsageSnapshotResponse:
    session = require_pair_access(pair_id, token)
    with get_connection() as connection:
        snapshot = connection.execute(
            """
            SELECT id, owner_nickname, captured_at_epoch_millis
            FROM usage_snapshots
            WHERE pair_id = ? AND session_token != ?
            ORDER BY captured_at_epoch_millis DESC, created_at_epoch_millis DESC
            LIMIT 1
            """,
            (pair_id, session["session_token"]),
        ).fetchone()
        if snapshot is None:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="usage_not_found")
        event_rows = connection.execute(
            """
            SELECT app_name, package_name, time_label, duration_label
            FROM usage_events
            WHERE snapshot_id = ?
            ORDER BY id ASC
            """,
            (snapshot["id"],),
        ).fetchall()

    return UsageSnapshotResponse(
        pair_id=pair_id,
        owner_nickname=snapshot["owner_nickname"],
        captured_at_epoch_millis=snapshot["captured_at_epoch_millis"],
        events=[
            UsageEventPayload(
                app_name=row["app_name"],
                package_name=row["package_name"],
                time_label=row["time_label"],
                duration_label=row["duration_label"],
            )
            for row in event_rows
        ],
    )


@app.post("/v1/photos/upload", response_model=PhotoUploadResponse)
async def upload_photo(
    pair_id: Annotated[str, Form()],
    source_photo_id: Annotated[str, Form()],
    captured_at_epoch_millis: Annotated[int, Form()],
    display_name: Annotated[str, Form()],
    mime_type: Annotated[Optional[str], Form()] = None,
    size_bytes: Annotated[int, Form()] = 0,
    photo_file: UploadFile = File(...),
    token: Annotated[str, Depends(bearer_token)] = "",
) -> PhotoUploadResponse:
    session = require_pair_access(pair_id, token)
    now = now_millis()

    with get_connection() as connection:
        existing = connection.execute(
            """
            SELECT id
            FROM photo_backups
            WHERE pair_id = ? AND session_token = ? AND source_photo_id = ?
            LIMIT 1
            """,
            (pair_id, session["session_token"], source_photo_id),
        ).fetchone()
        if existing is not None:
            await photo_file.close()
            return PhotoUploadResponse(photo_id=existing["id"], stored=False)

        photo_id = str(uuid4())
        suffix = safe_photo_suffix(display_name, mime_type)
        storage_dir = PHOTO_UPLOADS_DIR / pair_id / session["session_token"][:12]
        storage_dir.mkdir(parents=True, exist_ok=True)
        stored_name = f"{photo_id}{suffix}"
        stored_path = storage_dir / stored_name

        written_size = 0
        with stored_path.open("wb") as output:
            while True:
                chunk = await photo_file.read(1024 * 1024)
                if not chunk:
                    break
                output.write(chunk)
                written_size += len(chunk)
        await photo_file.close()

        connection.execute(
            """
            INSERT INTO photo_backups(
                id, pair_id, session_token, owner_nickname, source_photo_id, display_name,
                mime_type, size_bytes, captured_at_epoch_millis, stored_path, uploaded_at_epoch_millis
            )
            VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            (
                photo_id,
                pair_id,
                session["session_token"],
                session["nickname"],
                source_photo_id,
                display_name,
                mime_type,
                size_bytes if size_bytes > 0 else written_size,
                captured_at_epoch_millis,
                str(stored_path.relative_to(DB_PATH.parent)),
                now,
            ),
        )

    return PhotoUploadResponse(photo_id=photo_id, stored=True)


@app.get("/v1/photos/summary/{pair_id}", response_model=PhotoBackupSummaryResponse)
def photo_summary(
    pair_id: str,
    token: Annotated[str, Depends(bearer_token)],
) -> PhotoBackupSummaryResponse:
    session = require_pair_access(pair_id, token)
    with get_connection() as connection:
        total_photos = connection.execute(
            "SELECT COUNT(*) AS count FROM photo_backups WHERE pair_id = ?",
            (pair_id,),
        ).fetchone()["count"]
        my_photo_count = connection.execute(
            "SELECT COUNT(*) AS count FROM photo_backups WHERE pair_id = ? AND session_token = ?",
            (pair_id, session["session_token"]),
        ).fetchone()["count"]
        latest = connection.execute(
            """
            SELECT owner_nickname, display_name, uploaded_at_epoch_millis
            FROM photo_backups
            WHERE pair_id = ?
            ORDER BY uploaded_at_epoch_millis DESC
            LIMIT 1
            """,
            (pair_id,),
        ).fetchone()

    return PhotoBackupSummaryResponse(
        pair_id=pair_id,
        total_photos=total_photos,
        my_photo_count=my_photo_count,
        latest_uploaded_at_epoch_millis=latest["uploaded_at_epoch_millis"] if latest else None,
        latest_owner_nickname=latest["owner_nickname"] if latest else None,
        latest_display_name=latest["display_name"] if latest else None,
    )


@app.get("/v1/photos/list/{pair_id}", response_model=PhotoBackupListResponse)
def photo_list(
    pair_id: str,
    limit: int = 60,
    offset: int = 0,
    token: Annotated[str, Depends(bearer_token)] = "",
) -> PhotoBackupListResponse:
    require_pair_access(pair_id, token)
    safe_limit = max(1, min(limit, 200))
    safe_offset = max(0, offset)
    with get_connection() as connection:
        total_count = connection.execute(
            "SELECT COUNT(*) AS count FROM photo_backups WHERE pair_id = ?",
            (pair_id,),
        ).fetchone()["count"]
        rows = connection.execute(
            """
            SELECT id, owner_nickname, display_name, mime_type, size_bytes,
                   captured_at_epoch_millis, uploaded_at_epoch_millis
            FROM photo_backups
            WHERE pair_id = ?
            ORDER BY uploaded_at_epoch_millis DESC, id DESC
            LIMIT ? OFFSET ?
            """,
            (pair_id, safe_limit, safe_offset),
        ).fetchall()

    return PhotoBackupListResponse(
        pair_id=pair_id,
        total_count=total_count,
        items=[
            PhotoBackupItemResponse(
                photo_id=row["id"],
                owner_nickname=row["owner_nickname"],
                display_name=row["display_name"],
                mime_type=row["mime_type"],
                size_bytes=row["size_bytes"],
                captured_at_epoch_millis=row["captured_at_epoch_millis"],
                uploaded_at_epoch_millis=row["uploaded_at_epoch_millis"],
            )
            for row in rows
        ],
    )


@app.get("/v1/photos/file/{photo_id}")
def photo_file(
    photo_id: str,
    token: Annotated[str, Depends(bearer_token)],
):
    session = get_session(token)
    with get_connection() as connection:
        row = connection.execute(
            """
            SELECT pair_id, stored_path, mime_type, display_name
            FROM photo_backups
            WHERE id = ?
            LIMIT 1
            """,
            (photo_id,),
        ).fetchone()
    if row is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="photo_not_found")
    if row["pair_id"] != session["pair_id"]:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="pair_access_denied")
    file_path = DB_PATH.parent / row["stored_path"]
    if not file_path.exists():
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="photo_file_missing")
    return FileResponse(
        path=file_path,
        media_type=row["mime_type"] or "application/octet-stream",
        filename=row["display_name"],
    )
