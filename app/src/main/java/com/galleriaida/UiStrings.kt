package com.galleriaida.ui

/**
 * All translatable UI strings used across the player-facing screens
 * (PlayerHomeScreen, PlayerProfileScreen, GalleryScreen, GameScreen,
 * QuizSummaryScreen, ImageCreationScreen).
 *
 * Defaults are English. When a player is selected the ViewModel fetches
 * translated versions via the translation AI model and exposes a
 * StateFlow<UiStrings> that every screen collects.
 */
data class UiStrings(
    // ── PlayerHomeScreen ─────────────────────────────────────────────────────
    val homeGreeting: String = "Hi, %s! 👋",                   // %s = player name
    val homeSubtitle: String = "What do you want to do?",
    val homeQuizzes: String = "📝  Quizzes",
    val homeMyGallery: String = "🖼️  My Gallery",

    // ── QuizzesScreen ─────────────────────────────────────────────────────────
    val quizzesTitle: String = "Quizzes 📝",
    val quizzesStartQuiz: String = "Start Quiz 🚀",
    val quizzesHistory: String = "History 📖",

    // ── PlayerProfileScreen ───────────────────────────────────────────────────
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

    // ── GalleryScreen ─────────────────────────────────────────────────────────
    val galleryTitle: String = "My Gallery 🖼️",
    val galleryEmpty: String = "No images yet!\nEarn 100 ⭐ to create your first image.",
    val galleryNeedStars: String = "Need 100 ⭐ to create an image (you have %d)",  // %d = stars
    val galleryCreateButton: String = "Create Image 🎨 (100 ⭐)",

    // ── GameScreen ────────────────────────────────────────────────────────────
    val gameQuestionCounter: String = "Question %d / %d",      // current / total
    val gameLoadingQuestions: String = "Loading questions... 🤔",
    val gameGoToSettings: String = "Go to Settings",
    val gameWorth: String = "Worth %s",                         // %s = star emoji(s)
    val gameYourAnswer: String = "Your answer:",
    val gameTrue: String = "True",
    val gameFalse: String = "False",
    val gamePrevious: String = "Previous",
    val gameNext: String = "Next",
    val gameSubmit: String = "Submit ✅",

    // Error states
    val gameServerBusy: String = "The server is busy right now. Please try again in a moment! 🙏",
    val gameTryAgain: String = "Try Again 🔄",
    val gameApiKeyMissing: String = "API key not set. Please go to Settings.",

    // Abandon dialog
    val gameAbandonTitle: String = "Exit Quiz? 🚪",
    val gameAbandonMessage: String = "If you exit now, no stars will be awarded and your quiz will be discarded.",
    val gameAbandonConfirm: String = "Exit",
    val gameAbandonCancel: String = "Keep Going",

    // Unanswered questions dialog
    val gameUnansweredTitle: String = "Unanswered Questions ✏️",
    val gameUnansweredMessage: String = "Please answer all questions before submitting:",
    val gameUnansweredQuestion: String = "Question %d",         // %d = question number
    val gameUnansweredClose: String = "Got it!",

    // Kept for backwards compatibility (no longer shown in the new flow)
    val gameLessonDone: String = "Lesson done! 🎉",
    val gameEarnedStars: String = "You earned %d ⭐ stars!",
    val gameOkButton: String = "OK!",
    val gameCorrect: String = "✅ Correct!",
    val gameWrong: String = "❌ The answer was %d",
    val gameCheckAnswer: String = "Check Answer ✔️",
    val gameEarnedSoFar: String = "Earned so far: %d ⭐",

    // ── QuizSummaryScreen ─────────────────────────────────────────────────────
    val summaryTitle: String = "Quiz Complete! 🎉",
    val summaryStarsEarned: String = "+%d ⭐",                  // %d = stars earned
    val summaryScore: String = "%d / %d correct",               // correct / total
    val summaryQuestionNumber: String = "Q%d · %s",             // number · subject
    val summaryCorrect: String = "✓ Correct",
    val summaryWrong: String = "✗ Wrong",
    val summaryYourAnswer: String = "Your answer:",
    val summaryCorrectAnswer: String = "Correct answer:",
    val summaryNoAnswer: String = "No answer given",
    val summaryOptions: String = "Options:",
    val summaryClose: String = "Close 🏠",

    // ── ImageCreationScreen ───────────────────────────────────────────────────
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