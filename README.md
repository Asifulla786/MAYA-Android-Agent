# MAYA Android Agent

Native Kotlin + Jetpack Compose Android agent for user-authorized phone automation.

## Current build

- Kotlin 2.0 / Jetpack Compose / Coroutines
- Min SDK 26 / Target SDK 34 / Java 17
- MVVM-friendly agent core
- OpenAI Responses API provider with normalized native function tools
- Encrypted on-device API-key storage using AndroidX Security
- Accessibility UI automation: text, resource ID, description, typing, tap, swipe, Home, Back, Recents, Notifications
- Installed-app launcher tool
- Foreground service foundation
- Notification listener + RemoteInput reply foundation
- Floating draggable MAYA orb
- Voice STT/TTS with echo-safe lifecycle
- Room macro storage + deterministic macro executor
- GitHub Actions debug APK artifact

## First-time setup

1. Install the APK from the successful GitHub Actions `MAYA-debug-apk` artifact.
2. Open MAYA.
3. Tap **Accessibility** and enable MAYA.
4. Tap **Notifications** and grant notification access if you want notification intelligence/replies.
5. Grant microphone and notification permissions when Android asks.
6. Grant **Overlay** permission for the floating orb.
7. In MAYA, paste your OpenAI API key into **AI Provider → OpenAI API key** and tap **Save key**.
8. The key is stored locally in encrypted preferences. It is not committed to GitHub.
9. Tap **Start Agent** if you want the foreground agent service.

## Security model

The LLM does not receive unrestricted shell/root access. It can only request allow-listed native tools. High-impact capabilities should require an explicit confirmation layer before sending messages, making calls, deleting data, purchases, or SOS actions.

## APK

Every successful push to `main` builds `app-debug.apk` and uploads it as the `MAYA-debug-apk` workflow artifact for 14 days.

## Android limitations

Accessibility, notification access, microphone background execution, SMS/call control, and screen capture are subject to Android version, OEM, permission, and Google Play policy restrictions. MAYA must report unavailable capabilities rather than pretending they succeeded.
