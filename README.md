# MAYA Android Agent

Native Kotlin + Jetpack Compose foundation for a MAYA/Jarvis-style Android agent.

## Current foundation
- Kotlin + Jetpack Compose, MVVM-friendly structure
- Min SDK 26 / Target SDK 34
- AccessibilityService: node discovery, text/id/description click, typing, coordinate gestures, global actions
- NotificationListenerService: WhatsApp/Telegram/Messages notification extraction and RemoteInput reply support
- Foreground service foundation for long-running agent work
- Draggable floating overlay orb
- Voice STT/TTS guardian foundation
- Room macro persistence model
- Tool schema + fail-closed native ToolOrchestrator
- EncryptedSharedPreferences for locally stored provider credentials
- GitHub Actions Android build workflow

## Important Android limitations
Android does not grant arbitrary silent control of every application. Accessibility actions depend on what the target app exposes, Android version, OEM restrictions, and user-enabled Accessibility access. Background microphone, notification access, SMS, calls, and overlays are also permission- and policy-controlled.

The implementation therefore fails closed instead of pretending unsupported operations are reliable.

## API keys
**Never commit an API key.** Do not put OpenAI/Gemini/Groq/OpenRouter secrets in this repository, APK resources, BuildConfig, or source code. Store user-entered provider keys in the encrypted local store or use a server-side proxy with environment/secret-manager credentials.

## Build
Open the project in Android Studio with JDK 17 and sync Gradle. The CI workflow provisions Gradle 8.11.1 and runs `gradle :app:assembleDebug`.

## Roadmap
1. Gemini/OpenAI provider adapters with structured tool calls
2. MediaProjection screenshot pipeline with explicit user consent
3. Macro recorder/replayer with checkpoints and rollback semantics
4. Wake-word engine and owner verification using an on-device model
5. SOS workflow with explicit confirmation and Android-version-safe communication APIs
6. Settings UI for permissions, personas, providers, automation safety, and audit logs
