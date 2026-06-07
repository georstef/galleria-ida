package com.galleriaida.ui

/**
 * All translatable UI strings used across the player-facing screens
 * (PlayerHomeScreen, PlayerProfileScreen, GalleryScreen, GameScreen, ImageCreationScreen).
 *
 * Defaults are English. When a player is selected the ViewModel fetches
 * translated versions via the translation AI model and exposes a
 * StateFlow<UiStrings> that every screen collects.
 */
data class UiStrings(
    // ── PlayerHomeScreen ────────────────────────────────────────────────────
    val homeGreeting: String = "Hi, %s! 👋",          // %s = player name
    val homeSubtitle: String = "What do you want to do?",
    val homeStartLesson: String = "📚  Start Lesson",
    val homeMyGallery: String = "🖼️  My Gallery",

    // ── PlayerProfileScreen ──────────────────────────────────────────────
    val profileTitle: String = "My Profile",
    val profileStarsCollected: String = "%d stars collected",   // %d = stars
    val profileLabelName: String = "Name",
    val profileLabelLanguage: String = "Language",
    val profileLabelSchoolClass: String = "School class",
    val profileLabelSchoolYear: String = "Where are you in the school year?",
    val profileYearBeginning: String = "Beginning of the year",
    val profileYearMiddle: String = "Middle of the year",
    val profileYearEnd: String = "End of the year",
    val profileSaveButton: String = "Save & Go! 🚀",
    val profileErrorNameBlank: String = "Please enter a name",
    val profileErrorNameTaken: String = "This name is already taken",

    // ── GalleryScreen ────────────────────────────────────────────────────
    val galleryTitle: String = "My Gallery 🖼️",
    val galleryEmpty: String = "No images yet!\nEarn 100 ⭐ to create your first image.",
    val galleryNeedStars: String = "Need 100 ⭐ to create an image (you have %d)",  // %d = stars
    val galleryCreateButton: String = "Create Image 🎨 (100 ⭐)",

    // ── GameScreen ───────────────────────────────────────────────────────
    val gameQuestionCounter: String = "Question %d / %d",    // %d / %d = current / total
    val gameLoadingQuestions: String = "Loading questions... 🤔",
    val gameLessonDone: String = "Lesson done! 🎉",
    val gameEarnedStars: String = "You earned %d ⭐ stars!",  // %d = earned
    val gameOkButton: String = "OK!",
    val gameCorrect: String = "✅ Correct!",
    val gameWrong: String = "❌ The answer was %d",          // %d = correct answer
    val gameWorth: String = "Worth %s",                      // %s = star emoji(s)
    val gameYourAnswer: String = "Your answer",
    val gameCheckAnswer: String = "Check Answer ✔️",
    val gameNext: String = "Next ➡️",
    val gameEarnedSoFar: String = "Earned so far: %d ⭐",    // %d = earned
    val gameGoToSettings: String = "Go to Settings",

    // ── ImageCreationScreen ──────────────────────────────────────────────
    val imageTitle: String = "Create your image! 🎨",
    val imageSubtitle: String = "Pick one from each group",
    val imageCreatingMsg: String = "Creating your image... 🎨\nThis may take a moment",
    val imageCategoryCharacter: String = "🦸 Character",
    val imageCategoryAction: String = "⚡ Action",
    val imageCategoryPlace: String = "🗺️ Place",
    val imageNeedStars: String = "You need 100 ⭐ to create an image (you have %d)",  // %d = stars
    val imageButtonNeedStars: String = "Need 100 ⭐",
    val imageButtonPickAll: String = "Pick one from each group",
    val imageButtonCreate: String = "Create Image! 🎨 (100 ⭐)",
)
