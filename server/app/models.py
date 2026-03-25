from __future__ import annotations

from pydantic import BaseModel, Field


class InviteCreateResponse(BaseModel):
    invite_key: str
    expires_in_minutes: int = 60


class InviteUnlockRequest(BaseModel):
    invite_key: str = Field(min_length=8)
    nickname: str = Field(min_length=2)
    pair_code: str = Field(min_length=4, max_length=12)
    device_public_key: str = Field(min_length=16)


class InviteUnlockResponse(BaseModel):
    session_token: str
    pair_id: str
    display_name: str


class PairStatusResponse(BaseModel):
    pair_id: str
    partner_nickname: str
    usage_sharing_enabled: bool


class OutgoingMessagePayload(BaseModel):
    local_id: str
    body: str
    sent_at_epoch_millis: int


class MessageSyncRequest(BaseModel):
    pair_id: str
    outgoing_messages: list[OutgoingMessagePayload]


class MessageSyncResponse(BaseModel):
    acknowledged_message_ids: list[str]


class UsageEventPayload(BaseModel):
    app_name: str
    package_name: str
    time_label: str
    duration_label: str | None = None


class UsageUploadRequest(BaseModel):
    pair_id: str
    captured_at_epoch_millis: int
    events: list[UsageEventPayload]
