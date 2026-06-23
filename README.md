# saara app

An Android app that reads WhatsApp notifications and uses an on-device Small Language Model (FunctionGemma 270M via MediaPipe) to automatically extract and manage reminders.

## Features
- 📲 Reads WhatsApp messages in the background via a notification listener
- 🤖 On-device AI classifies messages and extracts dates, times, and categories
- 📅 View reminders by Today, Week, and Calendar views
- 💬 Chat directly with the on-device model
- 🔁 Handles rescheduled and cancelled reminders automatically
- 🧹 Deduplicates similar reminders intelligently

## Setup

### Prerequisites
- Android Studio Meerkat or later
- JDK 11+
- Android device or emulator running API 26 (Android 8.0) or higher

### 1. Clone the repo
```bash
git clone https://github.com/saaraanandsinha-png/slm-repo.git
cd slm-repo
```

### 2. Open in Android Studio
- Open the project folder in Android Studio
- Wait for Gradle sync to complete

### 3. Add the model file
The app uses `functiongemma-270m-it-Q4_K_M.gguf` bundled in assets.
Place the model file at:
```
app/src/main/assets/functiongemma-270m-it-Q4_K_M.gguf
```
> The model is loaded from assets on first launch and copied to internal storage automatically.

### 4. Build & run
```bash
./gradlew installDebug
```
Or use **Run > Run 'app'** in Android Studio.

### 5. Grant notification access
The app needs permission to read notifications:
1. Open **Settings** on your device
2. Go to **Apps > Special app access > Notification access**
3. Enable access for **saara app**

> Without this, the app cannot read WhatsApp messages.

## Tech Stack
- **Language:** Kotlin
- **UI:** Jetpack Compose + Material3
- **Architecture:** MVVM
- **Database:** Room
- **On-device AI:** MediaPipe Tasks GenAI + LlamaTik (Llama.cpp)
- **Min SDK:** 26 | **Target SDK:** 36

## Contributing
See [AGENTS.md](./AGENTS.md) for commit conventions, Git strategy, and project structure.
