from __future__ import annotations

from typing import List, Optional

from pydantic import BaseModel, Field


class PairCodeCreateResponse(BaseModel):
    pair_code: str
    expires_in_minutes: int = 60
    remaining_slots: int = 2


class PairCodeUnlockRequest(BaseModel):
    nickname: str = Field(min_length=2, max_length=24)
    pair_code: str = Field(min_length=4, max_length=12)
    device_public_key: str = Field(min_length=16)


class PairCodeUnlockResponse(BaseModel):
    session_token: str
    pair_id: str
    pair_code: str
    display_name: str
    member_count: int


class PairStatusResponse(BaseModel):
    pair_id: str
    pair_code: str
    partner_nickname: str
    usage_sharing_enabled: bool
    member_count: int


class OutgoingMessagePayload(BaseModel):
    local_id: str
    body: str = Field(min_length=1, max_length=4000)
    sent_at_epoch_millis: int


class SyncedMessagePayload(BaseModel):
    id: str
    body: str
    sent_at_epoch_millis: int
    author_nickname: str
    from_me: bool


class MessageSyncRequest(BaseModel):
    pair_id: str
    outgoing_messages: List[OutgoingMessagePayload] = Field(default_factory=list)


class MessageSyncResponse(BaseModel):
    acknowledged_message_ids: List[str]
    messages: List[SyncedMessagePayload]


class UsageEventPayload(BaseModel):
    app_name: str
    package_name: str
    time_label: str
    duration_label: Optional[str] = None


class UsageUploadRequest(BaseModel):
    pair_id: str
    captured_at_epoch_millis: int
    events: List[UsageEventPayload]


class UsageSnapshotResponse(BaseModel):
    pair_id: str
    owner_nickname: str
    captured_at_epoch_millis: int
    events: List[UsageEventPayload]


class PhotoUploadResponse(BaseModel):
    photo_id: str
    stored: bool


class PhotoBackupSummaryResponse(BaseModel):
    pair_id: str
    total_photos: int
    my_photo_count: int
    latest_uploaded_at_epoch_millis: Optional[int] = None
    latest_owner_nickname: Optional[str] = None
    latest_display_name: Optional[str] = None
