# Death by Burpees Timer ⏱️

<p align="center">
  <img src="https://github.com/reimen-cpu/Death-by-burpees-timer/blob/main/images/Screenshot_20260120_212326.png" alt="App Screenshot" width="300"/>
</p>

<p align="center">
  <strong>A minimalist Android timer with audio feedback for interval training</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green.svg" alt="Platform">
  <img src="https://img.shields.io/badge/Language-Kotlin-blue.svg" alt="Language">
  <img src="https://img.shields.io/badge/Min%20SDK-24-orange.svg" alt="Min SDK">
  <img src="https://img.shields.io/badge/License-MIT-yellow.svg" alt="License">
</p>

---

## 📋 Overview

**Death by Burpees Timer** is a minimalist Android application designed for interval training workouts. The app provides precise audio feedback without requiring you to look at your phone, making it perfect for exercises like burpees, HIIT, EMOM, or any minute-based workout routine.

### ✨ Key Features

- ⏰ **Customizable duration** - Set your workout from 1 to 999 minutes
- 🔊 **Smart audio feedback** - Get warned 5 seconds before each minute ends
- 📱 **Minimalist UI** - Clean, distraction-free interface
- 🌙 **Background operation** - Works even when screen is off
- 💾 **Saves your settings** - Remembers your last configuration
- 🎯 **Precise timing** - Accurate to the second

---

## 🎵 Audio Pattern

The timer follows a specific audio pattern for each minute:

### Warning Beeps (5 seconds before completing the minute)
- Plays at **:55, :54, :53, :52, :51** seconds
- **5 short beeps** (0.2 seconds each)
- Medium tone (600-700 Hz)

### Completion Beep (at the exact minute mark)
- Plays at **:00** seconds
- **1 long beep** (1 second)
- High tone (800-1000 Hz)

### Final Beep (when timer reaches 00:00)
- **1 extra long beep** (2 seconds)
- Signals workout completion

### Example for a 3-minute timer:

```text
03:00 - Timer starts
02:55 - short beep
02:54 - short beep
02:53 - short beep
02:52 - short beep
02:51 - short beep
02:00 - LONG BEEP (1 sec) ← Minute 1 completed
01:55 - short beep
01:54 - short beep
01:53 - short beep
01:52 - short beep
01:51 - short beep
01:00 - LONG BEEP (1 sec) ← Minute 2 completed
00:55 - short beep
00:54 - short beep
00:53 - short beep
00:52 - short beep
00:51 - short beep
00:00 - FINAL LONG BEEP (2 sec) ← Workout finished!
```

---

## 🚀 Getting Started

### Prerequisites

- Android Studio (latest version recommended)
- Android SDK 24 or higher
- Kotlin plugin

### Installation

1. **Clone the repository**

   ```bash
   git clone https://github.com/reimen-cpu/Death-by-burpees-timer.git
   cd Death-by-burpees-timer
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Select "Open an existing project"
   - Navigate to the cloned directory

3. **Build the project**
   - Wait for Gradle sync to complete
   - Click on "Build > Make Project"

4. **Run the app**
   - Connect your Android device or start an emulator
   - Click the "Run" button or press Shift + F10

### Direct APK Installation

You can also download the latest APK from the [Releases](https://github.com/reimen-cpu/Death-by-burpees-timer/releases) page and install it directly on your Android device.

---

## 📱 How to Use

1. **Set your workout duration**
   - Enter the number of minutes in the input field
   - Valid range: 1-999 minutes

2. **Start the timer**
   - Tap the "Start" button
   - The countdown begins immediately

3. **Focus on your workout**
   - Listen for the warning beeps (5 seconds before each minute)
   - The long beep signals the completion of each minute

4. **Stop or pause** (optional)
   - Tap the "Stop" button to pause
   - Tap "Start" again to resume

5. **Workout complete**
   - A final 2-second beep signals the end
   - The timer resets to your configured duration

---

## 🏗️ Technical Architecture

### Technologies Used

- **Language**: Kotlin
- **Min SDK**: 24 (Android 7.0 Nougat)
- **Architecture**: MVVM (Model-View-ViewModel)
- **UI**: Jetpack Compose / XML Layouts
- **Audio**: ToneGenerator API
- **Background Service**: Foreground Service with notification
- **Data Persistence**: SharedPreferences

### Key Components

```text
app/
├── ui/
│   ├── MainActivity.kt          # Main screen UI
│   └── TimerViewModel.kt        # Business logic & state
├── service/
│   └── TimerService.kt          # Foreground service for background operation
├── utils/
│   ├── SoundManager.kt          # Audio beep generation
│   └── TimerManager.kt          # Countdown logic
└── data/
    └── PreferencesManager.kt    # Settings persistence
```

### Permissions

```xml
```

---

## 🎯 Use Cases

This timer is perfect for:

- **Death by Burpees** - Classic CrossFit workout
- **EMOM** (Every Minute On the Minute) - Any exercise repeated each minute
- **HIIT Training** - High-Intensity Interval Training
- **Tabata Workouts** - Customized intervals
- **Boxing Rounds** - Minute-based round training
- **Meditation/Breathing** - Timed breathing exercises
- **Study Sessions** - Pomodoro-style time blocking

---

## 🛠️ Configuration

### Customizing Audio Settings

You can modify the beep tones and durations in `SoundManager.kt`:

```kotlin
// Short beep configuration
private const val SHORT_BEEP_FREQUENCY = 650 // Hz
private const val SHORT_BEEP_DURATION = 200 // ms

// Long beep configuration
private const val LONG_BEEP_FREQUENCY = 900 // Hz
private const val LONG_BEEP_DURATION = 1000 // ms
```

### Changing Default Duration

Edit the default value in `PreferencesManager.kt`:

```kotlin
private const val DEFAULT_DURATION_MINUTES = 20
```

---

## 🐛 Troubleshooting

### Beeps not playing in background
- Ensure the app has notification permissions enabled
- Check that "Do Not Disturb" mode allows alarms
- Verify alarm volume is not muted

### Timer stops when screen locks
- The app uses a Foreground Service to prevent this
- Check battery optimization settings and whitelist the app
- Some manufacturers (Xiaomi, Huawei) may require additional permissions

### Timing is slightly off
- The app compensates for system delays
- Ensure your device is not under heavy CPU load
- Close battery-intensive apps running in background

---

## 🤝 Contributing

Contributions are welcome! Here's how you can help:

1. **Fork the repository**
2. **Create a feature branch** (`git checkout -b feature/AmazingFeature`)
3. **Commit your changes** (`git commit -m 'Add some AmazingFeature'`)
4. **Push to the branch** (`git push origin feature/AmazingFeature`)
5. **Open a Pull Request**

### Contribution Ideas

- [ ] Add vibration feedback option
- [ ] Implement custom beep tones
- [ ] Create workout presets (5, 10, 15, 20 min)
- [ ] Add dark/light theme toggle
- [ ] Support for interval patterns (e.g., 40s work / 20s rest)
- [ ] Workout history tracking
- [ ] Widget for home screen

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 👤 Author

**Reimen**

- GitHub: [@reimen-cpu](https://github.com/reimen-cpu)
- Project Link: [https://github.com/reimen-cpu/Death-by-burpees-timer](https://github.com/reimen-cpu/Death-by-burpees-timer)

---

## 🙏 Acknowledgments

- Inspired by CrossFit "Death by Burpees" workout
- Built for athletes who need reliable interval timing
- Thanks to the Android development community

---

## 📊 Project Status

This project is currently in **active development**.

**Current Version**: 1.0.0
**Last Updated**: January 2026

### Roadmap

- [x] Basic timer functionality
- [x] Audio feedback system
- [x] Background operation
- [ ] Multi-language support
- [ ] Apple Watch companion app
- [ ] Workout statistics
- [ ] Social sharing features

---

<p align="center">
  Made with 💪 for athletes everywhere
</p>

<p align="center">
  <strong>Don't forget to ⭐ this repo if you found it useful!</strong>
</p>
