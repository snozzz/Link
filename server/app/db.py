from __future__ import annotations

import sqlite3
from contextlib import contextmanager
from pathlib import Path
from typing import Iterator

DB_PATH = Path(__file__).resolve().parent.parent / "link.db"


def _ensure_column(connection: sqlite3.Connection, table: str, column: str, definition: str) -> None:
    columns = {row[1] for row in connection.execute(f"PRAGMA table_info({table})").fetchall()}
    if column not in columns:
        connection.execute(f"ALTER TABLE {table} ADD COLUMN {definition}")


def init_db() -> None:
    DB_PATH.parent.mkdir(parents=True, exist_ok=True)
    with sqlite3.connect(DB_PATH) as connection:
        connection.execute("PRAGMA journal_mode=WAL")
        connection.executescript(
            """
            CREATE TABLE IF NOT EXISTS invites (
                invite_key TEXT PRIMARY KEY,
                pair_code TEXT,
                expires_at_epoch_millis INTEGER NOT NULL,
                created_at_epoch_millis INTEGER NOT NULL,
                max_uses INTEGER NOT NULL DEFAULT 2,
                use_count INTEGER NOT NULL DEFAULT 0,
                last_used_at_epoch_millis INTEGER,
                last_used_by_session_token TEXT,
                used_at_epoch_millis INTEGER,
                used_by_session_token TEXT
            );

            CREATE TABLE IF NOT EXISTS pairs (
                pair_id TEXT PRIMARY KEY,
                pair_code TEXT NOT NULL UNIQUE,
                usage_sharing_enabled INTEGER NOT NULL DEFAULT 1,
                created_at_epoch_millis INTEGER NOT NULL
            );

            CREATE TABLE IF NOT EXISTS pair_members (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                pair_id TEXT NOT NULL,
                nickname TEXT NOT NULL,
                session_token TEXT NOT NULL,
                joined_at_epoch_millis INTEGER NOT NULL,
                UNIQUE(pair_id, session_token)
            );

            CREATE TABLE IF NOT EXISTS sessions (
                session_token TEXT PRIMARY KEY,
                pair_id TEXT NOT NULL,
                nickname TEXT NOT NULL,
                device_public_key TEXT NOT NULL,
                created_at_epoch_millis INTEGER NOT NULL
            );

            CREATE TABLE IF NOT EXISTS messages (
                id TEXT PRIMARY KEY,
                pair_id TEXT NOT NULL,
                session_token TEXT NOT NULL,
                local_id TEXT NOT NULL,
                body TEXT NOT NULL,
                sent_at_epoch_millis INTEGER NOT NULL,
                created_at_epoch_millis INTEGER NOT NULL,
                UNIQUE(pair_id, session_token, local_id)
            );

            CREATE TABLE IF NOT EXISTS usage_snapshots (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                pair_id TEXT NOT NULL,
                session_token TEXT NOT NULL,
                owner_nickname TEXT NOT NULL,
                captured_at_epoch_millis INTEGER NOT NULL,
                created_at_epoch_millis INTEGER NOT NULL
            );

            CREATE TABLE IF NOT EXISTS usage_events (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                snapshot_id INTEGER NOT NULL,
                app_name TEXT NOT NULL,
                package_name TEXT NOT NULL,
                time_label TEXT NOT NULL,
                duration_label TEXT
            );
            """
        )
        _ensure_column(connection, 'invites', 'pair_code', 'pair_code TEXT')
        _ensure_column(connection, 'invites', 'max_uses', 'max_uses INTEGER NOT NULL DEFAULT 2')
        _ensure_column(connection, 'invites', 'use_count', 'use_count INTEGER NOT NULL DEFAULT 0')
        _ensure_column(connection, 'invites', 'last_used_at_epoch_millis', 'last_used_at_epoch_millis INTEGER')
        _ensure_column(connection, 'invites', 'last_used_by_session_token', 'last_used_by_session_token TEXT')
        connection.execute(
            """
            UPDATE invites
            SET max_uses = COALESCE(max_uses, 2),
                use_count = COALESCE(use_count, CASE WHEN used_at_epoch_millis IS NULL THEN 0 ELSE 1 END)
            """
        )
        connection.executescript(
            """
            CREATE INDEX IF NOT EXISTS idx_invites_pair_code ON invites(pair_code);
            CREATE INDEX IF NOT EXISTS idx_pair_members_pair_id ON pair_members(pair_id);
            CREATE INDEX IF NOT EXISTS idx_sessions_pair_id ON sessions(pair_id);
            CREATE INDEX IF NOT EXISTS idx_messages_pair_id_sent_at ON messages(pair_id, sent_at_epoch_millis, created_at_epoch_millis);
            CREATE INDEX IF NOT EXISTS idx_usage_snapshots_pair_id_created ON usage_snapshots(pair_id, captured_at_epoch_millis, created_at_epoch_millis);
            CREATE INDEX IF NOT EXISTS idx_usage_events_snapshot_id ON usage_events(snapshot_id);
            """
        )


@contextmanager
def get_connection() -> Iterator[sqlite3.Connection]:
    connection = sqlite3.connect(DB_PATH)
    connection.row_factory = sqlite3.Row
    try:
        yield connection
        connection.commit()
    except Exception:
        connection.rollback()
        raise
    finally:
        connection.close()
