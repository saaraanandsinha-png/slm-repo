# aloof

> *stay aloof, yet informed.*

Aloof is an Android app that locally extracts reminders from your WhatsApp messages — no noise, no cloud, no distractions.

---

## how it works
- Listens to WhatsApp notifications in the background
- Runs an on-device SLM (FunctionGemma 270M) to classify messages and extract dates, times, and categories
- Stores reminders locally in a Room database — nothing leaves your device
- Handles reschedules, cancellations, and duplicates automatically

## views
| Today | Week | Calendar | Chat |
|---|---|---|---|
| What's due now | 7-day overview | Full month | Talk to the model directly |

## stack
- Kotlin · Jetpack Compose · Material3
- MVVM · Room · DataStore
- MediaPipe Tasks GenAI · LlamaTik (Llama.cpp)
- Min SDK 26 · Target SDK 36

## setup
```bash
git clone https://github.com/saaraanandsinha-png/slm-repo.git
```
1. Open in Android Studio and let Gradle sync
2. Place `functiongemma-270m-it-Q4_K_M.gguf` in `app/src/main/assets/`
3. Run on a device with API 26+
4. Grant notification access — **Settings → Apps → Special app access → Notification access**

## contributing
See [AGENTS.md](./AGENTS.md) for commit conventions and project structure.
