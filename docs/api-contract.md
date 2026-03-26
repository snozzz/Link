# API Contract Draft

这是当前 `Link` 的后端契约草案，已经和服务端最小实现对齐。

## Auth

### POST /v1/internal/pair-codes/create

Response body:

- `pair_code`
- `expires_in_minutes`
- `remaining_slots`

说明：

- 当前由系统生成唯一 `pair_code`
- 同一组 `pair_code` 最多允许两台设备加入同一对
- 不是服务器生成的配对码不能进入

### POST /v1/auth/pair-code/unlock

Request body:

- `pair_code`
- `nickname`
- `device_public_key`

Response body:

- `session_token`
- `pair_id`
- `pair_code`
- `display_name`
- `member_count`

错误码语义：

- `pair_code_not_found`
- `pair_code_expired`
- `pair_code_exhausted`

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
- 安卓端当前已接入真实登录、消息同步、Moments 上传与对方动态读取
