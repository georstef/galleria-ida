# Developer Guide

This document covers architecture, build setup, and contribution notes for GalleriaIDA. It is intended for human developers and AI coding assistants alike — if you are an AI assistant helping with this codebase, read this document first to understand the conventions before making changes.

---

## Table of Contents

1. [Tech stack](#tech-stack)
2. [Project structure](#project-structure)
3. [Architecture overview](#architecture-overview)
4. [Quiz system](#quiz-system)
5. [Image generation pipeline](#image-generation-pipeline)
6. [Translation system](#translation-system)
7. [Star economy implementation](#star-economy-implementation)
8. [Build instructions](#build-instructions)
9. [Developer mode](#developer-mode)

---

## Tech stack

- **Language** — Kotlin
- **UI** — Jetpack Compose
- **Architecture** — MVVM (`AppViewModel` as single source of truth)
- **AI — Questions & Translation** — Google Gemini API (text models)
- **AI — Image generation** — Google Gemini Imagen + Pollinations (fallback chain)
- **Local persistence** — JSON files on internal storage
- **Min SDK** — 28 (Android 9)
- **Target SDK** — 36

---

## Project structure

```
app/src/main/
├── assets/
│   └── quiz_prompt.txt          # Gemini prompt template for quiz generation
├── java/com/galleriaida/
│   ├── data/                    # Data models (Player, Quiz, GalleryItem, …)
│   ├── navigation/              # NavGraph
│   ├── services/                # GeminiService, PollinationsService
│   ├── ui/
│   │   ├── screens/             # All Composable screens
│   │   ├── theme/               # Colors, typography, shapes
│   │   ├── UiStrings.kt         # All player-facing strings with English defaults
│   │   └── UiStringsCache.kt    # Translation cache (read/write JSON per player)
│   └── viewmodel/
│       └── AppViewModel.kt      # Single shared ViewModel
└── res/
    ├── drawable/                # logo.png and other drawables
    └── mipmap-*/                # Launcher icons
```

---

## Architecture overview

The app uses a single `AppViewModel` shared across all screens via the navigation graph. Screens collect `StateFlow`s from the ViewModel and call ViewModel functions for all side effects — no screen owns business logic.

Key flows:

```
PlayerSelectionScreen
  ├─ onSettings  → SettingsScreen
  └─ selectPlayer()
       ├─ loads translations (UiStringsCache → Gemini if miss)
       └─ navigates to PlayerLoadingScreen → HomeScreen

HomeScreen
  ├─ navigates to GameScreen (quiz)
  ├─ navigates to ImageCreationScreen
  ├─ navigates to GalleryScreen
  ├─ navigates to MiniGamesScreen
  └─ onSettings  → SettingsScreen
```

---

## Quiz system

Quizzes are generated at runtime by `GeminiService.generateQuiz()`. The prompt template lives in `assets/quiz_prompt.txt` and uses three placeholders:

| Placeholder | Value |
|---|---|
| `{{player_language}}` | Player's language (e.g. `Greek`) |
| `{{player_class}}` | Grade (e.g. `3`) |
| `{{school_year_position}}` | `1-beginning`, `2-middle`, or `3-end` |
| `{{selection}}` | Number of questions to generate |

Gemini returns a JSON array. Each question is parsed into a `QuizQuestion` object with fields: `id`, `subject`, `category`, `level` (1–3), `type` (`multiple_choice` / `true_false` / `text`), `instruction`, `question`, `options`, `answer`.

**Important — true/false questions:** The prompt instructs Gemini to always include an `options` array for `true_false` questions (the two localized button labels) and to set `answer` to exactly one of those strings character-for-character. Answer checking in `AppViewModel` uses plain string equality for all question types — no normalization.

Every submitted quiz is persisted locally as a `Quiz` object containing all questions and answers, and is viewable in the Quiz History screen.

---

## Image generation pipeline

When a player creates an image, the following happens in sequence:

1. `AppViewModel.createImage()` calls Gemini (Image Prompt model) to generate a rich English prompt from the three chosen words.
2. The prompt is sent to **Gemini Imagen** (primary). If that fails:
3. **Pollinations** is tried across up to three configurable model slots (`kontext` → `nova-canvas` → `flux` by default). Each fallback updates `_fallbackModelMessage` (displayed on screen as `"Creating image with <model>…"`).
4. The resulting bitmap is saved to internal storage.
5. A `GalleryItem` is created storing: image path, English title, player-language title, all three word choices in both languages, and creation timestamp.

The Pollinations API key is optional — without it the free public tier is used with rate limits.

---

## Translation system

All player-facing strings live in `UiStrings.kt` as a Kotlin data class with English defaults. When a player is selected, `AppViewModel` checks for a cached translation file. On a cache miss it calls Gemini (Translation model) once, passing all English strings and requesting a JSON translation.

**Cache format:** one JSON file per player stored at `<filesDir>/translations/<playerId>.json`, containing a flat `Map<String, String>`. The cache is versioned — bump `CACHE_VERSION` in `UiStringsCache.kt` in either of these cases:
- An existing English default string **changes value**
- A new key is added whose default text is meaningful and should be translated (not just a technical placeholder)

Failing to bump the version means existing players will keep the stale translation and never see the updated string.

**⚠️ Difficulty label convention:** difficulty button labels (e.g. `puzzleEasy`, `memoryMatchEasy`) must contain **only the difficulty word** — never grid dimensions or other dynamic data. The screen appends those separately. If the label includes dimensions, Gemini will translate them as part of the string and they will be shown twice in the UI.

**Adding a new string:**
1. Add the property with an English default to `UiStrings.kt`
2. Add the corresponding `s("key", defaults.key)` line to the `UiStringsCache` constructor
3. Add `"key" to d.key` to the string map in `UiStringsCache`
4. Use `uiStrings.<propertyName>` in the Composable
5. Bump `CACHE_VERSION` if the default text should be translated

Screens collect `viewModel.uiStrings` as a `StateFlow<UiStrings>`. The Settings screen and all admin/debug dialogs intentionally use hardcoded English.

---

## Star economy implementation

Stars are stored on the `Player` object and persisted locally. Deductions happen in `AppViewModel` before the relevant screen is entered (mini-games) or on image creation confirmation. Additions happen in `AppViewModel.submitQuiz()`:

```
baseStars  = correctAnswers × 3
bonusStars = 10  if all correct
           =  3  if exactly one wrong
           =  0  otherwise
starsEarned = baseStars + bonusStars
```

The developer bypass player (see [Developer mode](#developer-mode)) skips all star-cost checks without affecting the stars balance display.

---

## Build instructions

### Prerequisites

- Android Studio Quail or newer
- Android SDK 36
- JDK 17+

### Clone and build

```bash
git clone <repo-url>
cd GalleriaIDA
./gradlew assembleDebug
```

Install on a connected device or emulator:

```bash
./gradlew installDebug
```

### First-run configuration

The app requires a Gemini API key entered at runtime in the Settings screen — there are no hardcoded keys or `local.properties` entries needed.

---

## Developer mode

A player whose name matches `AppConstants.DEV_PLAYER_NAME` bypasses all star-cost checks. This player can create images and play all games without spending stars.

To change the bypass name, edit `AppConstants.kt`:

```kotlin
object AppConstants {
    const val DEV_PLAYER_NAME = "your chosen name here"
}
```
