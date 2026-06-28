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
    val galleryImageMetadata: String = "Image Metadata",
    val galleryFullscreenClose: String = "Close",
    val gallerySell: String = "Sell 💰",
    val gallerySellConfirmTitle: String = "Sell this image?",
    val gallerySellConfirmMessage: String = "You'll get back %d ⭐.\n\nThis permanently deletes the image and cannot be undone.",  // %d = refund
    val gallerySellConfirmYes: String = "Sell",
    val gallerySellConfirmNo: String = "Cancel",

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

    // ── QuizSummaryScreen ─────────────────────────────────────────────────────
    val summaryTitle: String = "Quiz Complete! 🎉",
    val summaryStarsEarned: String = "+%d ⭐",                  // %d = stars earned
    val summaryScore: String = "%d / %d correct",               // correct / total
    val summaryBonus: String = "Bonus +%d ⭐",                    // %d = bonus stars
    val summaryQuestionNumber: String = "Q%d · %s",             // number · subject
    val summaryCorrect: String = "✓ Correct",
    val summaryWrong: String = "✗ Wrong",
    val summaryYourAnswer: String = "Your answer:",
    val summaryCorrectAnswer: String = "Correct answer:",
    val summaryNoAnswer: String = "No answer given",
    val summaryOptions: String = "Options:",
    val summaryClose: String = "Close 🏠",

    // ── QuizHistoryScreen ─────────────────────────────────────────────────────
    val historyTitle: String = "Quiz History 📖",
    val historyEmpty: String = "No quizzes completed yet! Start your first quiz and come back here to see your results.",
    val historyColumnDate: String = "Date",
    val historyColumnTime: String = "Time",
    val historyColumnDuration: String = "Duration",
    val historyColumnScore: String = "Score",
    val historyColumnStars: String = "Stars",

    // ── ImageCreationScreen ───────────────────────────────────────────────────
    val imageTitle: String = "Create your image!",
    val imageSubtitle: String = "Pick one from each group",
    val imagePreparingWords: String = "Preparing words…",
    val imageBackupEngine: String = "Creating image with %s…",    // %s = model name
    val imageCreatingMsg: String = "Creating your image... 🎨\nThis may take a moment",
    val imageCategoryCharacter: String = "🦸 Character",
    val imageCategoryAction: String = "⚡ Action",
    val imageCategoryPlace: String = "🗺️ Place",
    val imageNeedStars: String = "You need 100 ⭐ to create an image (you have %d)",  // %d = stars
    val imageButtonNeedStars: String = "Need 100 ⭐",
    val imageButtonPickAll: String = "Pick one from each group",
    val imageButtonCreate: String = "Create Image! 🎨 (100 ⭐)",

    // ── MiniGamesScreen ───────────────────────────────────────────────────────
    val miniGamesTitle: String = "🎮 Mini Games",
    val miniGamesLockedHint: String = "Unlocks after %d images",
    val miniGamesCostHint: String = "10 ⭐ to play",
    val miniGamesCostHint5: String = "5 ⭐ to play",
    val miniGamesFreeHint: String = "Free to play",
    val miniGamesPuzzleName: String = "🧩 Puzzle",
    val miniGamesPuzzleDesc: String = "Reassemble a picture from your gallery",
    val miniGamesFillBlankName: String = "🔡 Fill the Blank",
    val miniGamesFillBlankDesc: String = "Guess the title to reveal hidden pictures",
    val miniGamesOnePieceName: String = "🔲 One Piece",
    val miniGamesOnePieceDesc: String = "Guess the full picture from a tiny piece",

    // ── PuzzleScreen ──────────────────────────────────────────────────────────
    val puzzleTitle: String = "Puzzle 🧩",
    val puzzleSelectSize: String = "Choose difficulty",
    val puzzleEasy: String = "Easy",
    val puzzleMedium: String = "Medium",
    val puzzleHard: String = "Hard",
    val puzzleReroll: String = "Try another image 🔀",
    val puzzleNoImages: String = "No images yet",
    val puzzlePlay: String = "Play!  (10 ⭐)",
    val puzzleNotEnoughStars: String = "Not enough stars",
    val puzzleSolved: String = "Puzzle solved! 🎉",
    val puzzleClose: String = "Back",
    val puzzleShowImage: String = "👁 Show Image",
    val puzzleCloseImage: String = "Close",
    val puzzleSolvedPopupBody: String = "You completed the picture!",
    val puzzleSolvedPopupClose: String = "Close puzzle",
    val puzzleExitTitle: String = "Leave the puzzle?",
    val puzzleExitBody: String = "Your 10 ⭐ have already been spent and won't be refunded if you leave now. Are you sure you want to exit?",
    val puzzleExitConfirm: String = "Yes, exit",
    val puzzleExitCancel: String = "Keep playing",

    // ── FillTheBlankScreen ──────────────────────────────────────────────────
    val fillBlankTitle: String = "Fill the Blank 🔡",
    val fillBlankSelectDifficulty: String = "Choose difficulty",
    val fillBlankEasy: String = "Easy",
    val fillBlankMedium: String = "Medium",
    val fillBlankHard: String = "Hard",
    val fillBlankEasyHint: String = "30% missing",
    val fillBlankMediumHint: String = "50% missing",
    val fillBlankHardHint: String = "70% missing",
    val fillBlankPlay: String = "Play!  (10 ⭐)",
    val fillBlankNotEnoughStars: String = "Not enough stars",
    val fillBlankNotEnoughImages: String = "Not enough images in your gallery yet",
    val fillBlankRoundCounter: String = "Round %d / %d",          // current / total
    val fillBlankExplainer: String = "%d rounds — guess the title to reveal each hidden picture!",  // %d = total rounds
    val fillBlankCorrect: String = "Correct! 🎉",
    val fillBlankWrong: String = "Not quite, try again!",
    val fillBlankNext: String = "Next round →",
    val fillBlankSeeResults: String = "See Results 🎉",
    val fillBlankUndo: String = "⌫ Undo",
    val fillBlankSummaryTitle: String = "All done! 🎉",
    val fillBlankSummaryScore: String = "%d / %d revealed",        // correct / total
    val fillBlankSummaryClose: String = "Close",
    val fillBlankExitTitle: String = "Leave Fill the Blank?",
    val fillBlankExitBody: String = "Your 10 ⭐ have already been spent and won't be refunded if you leave now. Are you sure you want to exit?",
    val fillBlankExitConfirm: String = "Yes, exit",
    val fillBlankExitCancel: String = "Keep playing",

    // ── MemoryMatchScreen ────────────────────────────────────────────────────
    val memoryMatchName: String = "🧠 Memory Match",
    val memoryMatchDesc: String = "Find all matching pairs in your gallery",
    val memoryMatchTitle: String = "Memory Match 🧠",
    val memoryMatchSelectDifficulty: String = "Choose difficulty",
    val memoryMatchEasy: String = "Easy",
    val memoryMatchMedium: String = "Medium",
    val memoryMatchHard: String = "Hard",
    val memoryMatchPlay: String = "Play!  (10 ⭐)",
    val memoryMatchNotEnoughStars: String = "Not enough stars",
    val memoryMatchWonTitle: String = "Well done! 🎉",
    val memoryMatchWonBody: String = "You matched all the pairs!",
    val memoryMatchWonClose: String = "Back to Mini Games",
    val memoryMatchExitTitle: String = "Leave Memory Match?",
    val memoryMatchExitBody: String = "Your 10 ⭐ have already been spent and won't be refunded if you leave now. Are you sure you want to exit?",
    val memoryMatchExitConfirm: String = "Yes, exit",
    val memoryMatchExitCancel: String = "Keep playing",

    // ── ImageTournamentScreen ────────────────────────────────────────────────
    val tournamentName: String = "🏆 Image Tournament",
    val tournamentDesc: String = "Vote your favourite images to the top!",
    val tournamentTitle: String = "Image Tournament 🏆",
    val tournamentRound: String = "Round %d",
    val tournamentRoundComplete: String = "Round %d complete! Here are the winners:",
    val tournamentStart: String = "Start Tournament",
    val tournamentContinue: String = "Continue →",
    val tournamentR16: String = "R16",
    val tournamentQF: String = "QF",
    val tournamentSF: String = "SF",
    val tournamentFinal: String = "Final",
    val tournamentChampion: String = "🏆",
    val tournamentVs: String = "VS",
    val tournamentVote: String = "Pick ✨",
    val tournamentMatchProgress: String = "Match %d / %d",
    val tournamentWonTitle: String = "We have a champion! 🏆",
    val tournamentWonBody: String = "This image won the tournament!",
    val tournamentWonClose: String = "Back to Mini Games",
    val tournamentExitTitle: String = "Leave Tournament?",
    val tournamentExitBody: String = "Your tournament progress will be lost. Are you sure?",
    val tournamentExitConfirm: String = "Yes, exit",
    val tournamentExitCancel: String = "Keep playing",

    // ── OnePieceScreen ──────────────────────────────────────────────────────
    val onePieceTitle: String = "One Piece 🔲",
    val onePieceSelectDifficulty: String = "Choose difficulty",
    val onePieceEasy: String = "Easy",
    val onePieceMedium: String = "Medium",
    val onePieceHard: String = "Hard",
    val onePiecePlay: String = "Play!  (5 ⭐)",
    val onePieceNotEnoughStars: String = "Not enough stars",
    val onePieceNotEnoughImages: String = "Not enough images in your gallery yet",
    val onePieceExplainer: String = "%d rounds — guess the picture from one tiny piece!",  // %d = total rounds
    val onePieceRoundCounter: String = "Round %d / %d",            // current / total
    val onePieceQuestion: String = "Which picture is this from?",
    val onePieceCorrect: String = "Correct! 🎉",
    val onePieceWrong: String = "Not quite!",
    val onePieceNext: String = "Next round →",
    val onePieceSummaryTitle: String = "All done! 🎉",
    val onePieceSummaryScore: String = "%d / %d correct",          // correct / total
    val onePieceSummaryClose: String = "Close",
    val onePieceExitTitle: String = "Leave One Piece?",
    val onePieceExitBody: String = "Your 10 ⭐ have already been spent and won't be refunded if you leave now. Are you sure you want to exit?",
    val onePieceExitConfirm: String = "Yes, exit",
    val onePieceExitCancel: String = "Keep playing",
)