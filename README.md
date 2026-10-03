<div align="center">
  <img src="app/src/main/res/drawable/fourlink_logo.png" alt="FourLink Logo" width="320" />

# FourLink

### Connect. Compete. Get four in a row.

A colorful **two-player Connect Four game for Android**, built with Kotlin as a **Mobile Development project**.

[![Kotlin](https://img.shields.io/badge/Kotlin-Android-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![Project](https://img.shields.io/badge/Project-Mobile%20Development-2563EB)](#project-purpose)
[![Version](https://img.shields.io/badge/Version-1.0-F59E0B)](#)

[![Download APK](https://img.shields.io/badge/Download-FourLink.apk-16A34A?style=for-the-badge&logo=android&logoColor=white)](./FourLink.apk)

</div>

---

## 🎮 About the Game

**FourLink** is an Android adaptation of the classic **Connect Four** strategy game. Two players take turns dropping colored discs into a vertical **7-column × 6-row board**. The goal is simple: be the first player to connect **four discs of the same color** in a straight line.

The game is designed for **local two-player play** on one Android device, with **Yellow** and **Red** alternating turns.

---

## 🕹️ How to Play

1. Launch **FourLink** and start a match from the main menu.
2. **Yellow goes first.**
3. Tap a column to drop your disc.
4. The disc falls into the **lowest available space** in that column.
5. Players alternate between **Yellow** and **Red** after every valid move.
6. Keep building your position while blocking your opponent from completing a line of four.
7. The match ends when a player wins, the board fills completely, or a player chooses to surrender.

> **Tip:** A good move can attack and defend at the same time. Watch for horizontal, vertical, and diagonal threats.

---

## 🏆 Winning Condition

A player wins by connecting **four or more of their discs in one continuous straight line**.

| Direction | Example |
| --- | --- |
| **Horizontal** | 🟡 🟡 🟡 🟡 |
| **Vertical** | 🟡<br>🟡<br>🟡<br>🟡 |
| **Diagonal ↘** | 🟡 · · ·<br>· 🟡 · ·<br>· · 🟡 ·<br>· · · 🟡 |
| **Diagonal ↗** | · · · 🔴<br>· · 🔴 ·<br>· 🔴 · ·<br>🔴 · · · |

FourLink checks all four possible winning axes after each move:

- Horizontal
- Vertical
- Diagonal down-right / up-left
- Diagonal up-right / down-left

When a winning line is found, the winning discs are highlighted before the result is shown. If every cell is occupied without a winning line, the game ends in a **draw**.

---

## ✨ Game Features

- 🟡 **Yellow vs. Red local multiplayer**
- 🎯 Classic **6 × 7 Connect Four board**
- 💧 Animated disc-drop movement
- 🏆 Automatic win detection in every direction
- ✨ Winning-disc highlight/blink animation
- 🤝 Draw detection when the board is full
- 🚩 Surrender option during a match
- 🔁 Play Again and Main Menu options after a game
- 📖 Built-in tutorial / How to Play screen
- 📱 Adaptive layouts for different screen sizes and orientations
- 🌙 UI resources for light and dark appearances
- 💾 Match state restoration across Android activity recreation

---

## 🎓 Project Purpose

FourLink was developed as a **Mobile Development Project** and serves as the final project for **CSIT284**.

The project applies core Android development concepts through a complete playable application, including:

- Android activities and screen navigation
- User interaction and event handling
- Game-state and turn management
- Two-dimensional board logic
- Win and draw algorithms
- Android lifecycle-aware state restoration
- Dialogs and user confirmations
- UI animation and visual feedback
- Responsive layouts for different device configurations
- Unit and instrumentation testing

Rather than being only a visual recreation of Connect Four, the project focuses on building the complete game loop—from accepting a move to validating the board, detecting a winner, handling surrender, and starting a new match.

---

## 🛠️ Built With

| Technology | Purpose |
| --- | --- |
| **Kotlin** | Main application and game logic |
| **Android SDK** | Native Android application development |
| **XML Layouts** | Game screens and interface layouts |
| **Gradle** | Project build and dependency management |
| **JUnit** | Game-logic testing |
| **Espresso / AndroidX Test** | Android UI and instrumentation testing |

The current application configuration targets **Android API 34**, compiles with **API 35**, and supports devices from **Android 7.0 (API 24)** onward.

---

## 📲 Run the App

### Option 1 — Install the APK

A ready-to-install APK is included in the repository:

**[`FourLink.apk`](./FourLink.apk)**

Download the APK onto an Android device and install it. Android may ask you to allow installation from your browser or file manager.

### Option 2 — Run from Android Studio

```bash
git clone https://github.com/arkeyyy/FourLink.git
cd FourLink
```

Then:

1. Open the project in **Android Studio**.
2. Allow Gradle to sync the project.
3. Connect an Android device or start an emulator.
4. Run the `app` configuration.

---

## 🧠 Game Logic Overview

FourLink stores the board as **6 rows × 7 columns**. Whenever a player selects a column, the game searches upward from the bottom of that column for the first empty cell and places the disc there.

After every successful move, the game checks outward from the newly placed disc in both directions across each axis. If the continuous line contains at least four matching discs, that player wins. Otherwise, the turn switches to the opposing player.

```text
Yellow turn → Drop disc → Check for four connected discs
                             │
                  ┌──────────┴──────────┐
                  │                     │
                WIN                   No win
                  │                     │
          Show winning line      Is board full?
                                        │
                              ┌─────────┴─────────┐
                              │                   │
                            DRAW              Switch turn
```

---

<div align="center">

### 🔴 🟡 FourLink 🟡 🔴

**Think ahead. Block your opponent. Connect four.**

Made for a **Mobile Development Project**.

</div>
