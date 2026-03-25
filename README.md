# Link

Link is a private Android app for two consenting users to share daily app-usage summaries, lightweight status updates, and messages.

## Planned modules

1. Invite-only authentication and pair binding
2. Usage stats collection and daily timeline
3. Private chat
4. Backend sync and encrypted session handling

## Current status

The repository currently contains the initial Android app scaffold built with Jetpack Compose:

- project Gradle files
- a pastel design system
- bottom navigation shell
- placeholder screens for home, moments, and messages

## Local build note

This workspace currently has Java 17 available, but not a local `gradle` installation or Gradle wrapper yet. If you want me to run builds from WSL, I will need either:

1. Android Studio / Gradle wrapper files generated in the repo
2. an installed Gradle binary
