# Link

Link is a private Android app for two consenting users to share daily app-usage summaries, lightweight status updates, and messages.

## Planned modules

1. Invite-only authentication and pair binding
2. Usage stats collection and daily timeline
3. Private chat
4. Backend sync and encrypted session handling

## Current status

The repository currently contains:

- a Jetpack Compose Android scaffold
- a pastel design system and app shell
- an invite-gated entry screen
- encrypted local session persistence based on Android Keystore
- a local Usage Access permission flow and today timeline reader
- a stateful local chat module with backend contract scaffolding
- security and API contract notes for the later server implementation

## Local build note

This workspace now includes a working Gradle wrapper. The Android SDK is currently wired through a temporary WSL path during local builds.
