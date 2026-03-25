# API Contract Draft

This is the first backend-facing contract draft for the private pair app.

## Auth

### POST /v1/auth/invite/unlock

Request body:

- `inviteKey`
- `nickname`
- `pairCode`
- `devicePublicKey`

Response body:

- `sessionToken`
- `pairId`
- `displayName`

## Pair state

### GET /v1/pair/status

Response body:

- `pairId`
- `partnerNickname`
- `usageSharingEnabled`

## Messages

### POST /v1/messages/sync

Request body:

- `pairId`
- `outgoingMessages[]`
  - `localId`
  - `body`
  - `sentAtEpochMillis`

Response body:

- `acknowledgedMessageIds[]`

## Usage timeline

### POST /v1/usage/upload

Request body should later include:

- `pairId`
- `capturedAtEpochMillis`
- `topApps[]`
- `events[]`

## Notes

- Invite verification stays server-side.
- Device public key binding happens during auth unlock.
- Chat remains local-first until sync APIs are implemented.
- End-to-end encryption can later wrap message payloads without changing endpoint shape.
