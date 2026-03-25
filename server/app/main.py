from __future__ import annotations

import secrets
from collections import defaultdict
from typing import Any
from uuid import uuid4

from fastapi import FastAPI, HTTPException

from .models import (
    InviteCreateResponse,
    InviteUnlockRequest,
    InviteUnlockResponse,
    MessageSyncRequest,
    MessageSyncResponse,
    PairStatusResponse,
    UsageUploadRequest,
)

app = FastAPI(title="Link Server", version="0.1.0")

invite_store: dict[str, dict[str, Any]] = {}
pair_store: dict[str, dict[str, Any]] = {}
message_store: dict[str, list[dict[str, Any]]] = defaultdict(list)
usage_store: dict[str, list[dict[str, Any]]] = defaultdict(list)


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/v1/internal/invites/create", response_model=InviteCreateResponse)
def create_invite() -> InviteCreateResponse:
    invite_key = secrets.token_urlsafe(12)
    invite_store[invite_key] = {"used": False}
    return InviteCreateResponse(invite_key=invite_key)


@app.post("/v1/auth/invite/unlock", response_model=InviteUnlockResponse)
def unlock_invite(payload: InviteUnlockRequest) -> InviteUnlockResponse:
    invite = invite_store.get(payload.invite_key)
    if invite is None:
        raise HTTPException(status_code=404, detail="invite_not_found")
    if invite["used"]:
        raise HTTPException(status_code=409, detail="invite_already_used")

    pair_id = f"pair_{payload.pair_code.lower()}"
    pair_store.setdefault(
        pair_id,
        {
            "pair_id": pair_id,
            "members": [],
            "usage_sharing_enabled": True,
        },
    )
    pair_store[pair_id]["members"].append(payload.nickname)
    invite["used"] = True

    return InviteUnlockResponse(
        session_token=secrets.token_urlsafe(24),
        pair_id=pair_id,
        display_name=payload.nickname,
    )


@app.get("/v1/pair/status/{pair_id}", response_model=PairStatusResponse)
def pair_status(pair_id: str) -> PairStatusResponse:
    pair = pair_store.get(pair_id)
    if pair is None:
        raise HTTPException(status_code=404, detail="pair_not_found")
    members = pair.get("members", [])
    partner_nickname = members[-1] if members else "等待配对"
    return PairStatusResponse(
        pair_id=pair_id,
        partner_nickname=partner_nickname,
        usage_sharing_enabled=pair.get("usage_sharing_enabled", True),
    )


@app.post("/v1/messages/sync", response_model=MessageSyncResponse)
def sync_messages(payload: MessageSyncRequest) -> MessageSyncResponse:
    pair = pair_store.get(payload.pair_id)
    if pair is None:
        raise HTTPException(status_code=404, detail="pair_not_found")

    acknowledged_ids: list[str] = []
    for message in payload.outgoing_messages:
        message_store[payload.pair_id].append(
            {
                "id": str(uuid4()),
                "local_id": message.local_id,
                "body": message.body,
                "sent_at_epoch_millis": message.sent_at_epoch_millis,
            }
        )
        acknowledged_ids.append(message.local_id)

    return MessageSyncResponse(acknowledged_message_ids=acknowledged_ids)


@app.post("/v1/usage/upload")
def upload_usage(payload: UsageUploadRequest) -> dict[str, int]:
    pair = pair_store.get(payload.pair_id)
    if pair is None:
        raise HTTPException(status_code=404, detail="pair_not_found")

    usage_store[payload.pair_id].append(payload.model_dump())
    return {"stored_events": len(payload.events)}
