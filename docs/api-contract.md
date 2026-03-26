# API Contract Draft

这是当前 `Link` 的后端契约草案，已经和服务端最小实现对齐。

## Auth

### POST /v1/internal/invites/create

Response body:

- `invite_key`
- `pair_code`
- `expires_in_minutes`
- `remaining_slots`

说明：

- 当前由系统生成一组 `invite_key + pair_code`
- 同一组信息最多允许两个人加入同一对

### POST /v1/auth/invite/unlock

Request body:

- `invite_key`
- `pair_code`
- `nickname`
- `device_public_key`

Response body:

- `session_token`
- `pair_id`
- `pair_code`
- `display_name`
- `member_count`

## Pair state

### GET /v1/pair/status/{pair_id}

Headers:

- `Authorization: Bearer <session_token>`

Response body:

- `pair_id`
- `pair_code`
- `partner_nickname`
- `usage_sharing_enabled`
- `member_count`

## Messages

### POST /v1/messages/sync

Headers:

- `Authorization: Bearer <session_token>`

Request body:

- `pair_id`
- `outgoing_messages[]`
  - `local_id`
  - `body`
  - `sent_at_epoch_millis`

Response body:

- `acknowledged_message_ids[]`
- `messages[]`
  - `id`
  - `body`
  - `sent_at_epoch_millis`
  - `author_nickname`
  - `from_me`

## Usage timeline

### POST /v1/usage/upload

Headers:

- `Authorization: Bearer <session_token>`

Request body:

- `pair_id`
- `captured_at_epoch_millis`
- `events[]`
  - `app_name`
  - `package_name`
  - `time_label`
  - `duration_label`

### GET /v1/usage/latest/{pair_id}

Headers:

- `Authorization: Bearer <session_token>`

Response body:

- `pair_id`
- `owner_nickname`
- `captured_at_epoch_millis`
- `events[]`
  - `app_name`
  - `package_name`
  - `time_label`
  - `duration_label`

## Notes

- 当前所有持久化都落在 `server/link.db`
- 当前默认只做原型阶段的最小鉴权，不含 HTTPS、refresh token、设备吊销
- 下一步安卓端需要补：
  - 登录时真实调用 `unlock`
  - 本地消息仓库对接 `messages/sync`
  - `Moments` 把本地时间线上传到 `usage/upload`
  - 拉取 `usage/latest` 展示对方动态
