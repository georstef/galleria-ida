package com.galleriaida.ui

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File

/**
 * Reads and writes per-language translation caches.
 * Each language is stored as translations_<language>.json in the app's files dir.
 * The file is a flat JSON object: { "homeGreeting": "...", "homeSubtitle": "...", ... }
 *
 * IMPORTANT: whenever a new key is added to UiStrings, it must also be added to
 * both [buildUiStrings] and [defaultsMap] below, otherwise it will never be
 * translated or applied.
 *
 * CACHE VERSION: bump [CACHE_VERSION] whenever English default strings change value
 * (not just when new keys are added). On version mismatch all translation cache
 * files are wiped so they get rebuilt fresh — player data and images are untouched.
 */
object UiStringsCache {

    /**
     * Bump this number whenever an existing English string changes its value.
     * New keys don't require a bump — they're detected automatically via missingKeys().
     */
    private const val CACHE_VERSION = 2

    private fun translationsDir(context: Context) =
        File(context.filesDir, "translations").also { it.mkdirs() }

    private fun versionFile(context: Context) =
        File(translationsDir(context), "cache_version.txt")

    /**
     * Call once at app startup (e.g. from AppViewModel.init).
     * If the stored version doesn't match CACHE_VERSION, all translation
     * cache files are deleted so they get retranslated fresh.
     */
    fun invalidateIfVersionChanged(context: Context) {
        val dir     = translationsDir(context)
        val vFile   = versionFile(context)
        val stored  = try { vFile.readText().trim().toInt() } catch (e: Exception) { -1 }
        if (stored != CACHE_VERSION) {
            Log.d("UiStringsCache", "Cache version changed ($stored → $CACHE_VERSION), wiping translation files")
            dir.listFiles()
                ?.filter { it.name.startsWith("translations_") && it.name.endsWith(".json") }
                ?.forEach { it.delete() }
            vFile.writeText(CACHE_VERSION.toString())
        }
    }

    private fun cacheFile(context: Context, language: String): File {
        return File(translationsDir(context), "translations_${language.lowercase().replace(" ", "_")}.json")
    }

    /** Load the cached JSON for [language]. Returns empty JSONObject if none exists. */
    fun load(context: Context, language: String): JSONObject {
        return try {
            val file = cacheFile(context, language)
            if (file.exists()) JSONObject(file.readText()) else JSONObject()
        } catch (e: Exception) {
            Log.e("UiStringsCache", "load error: ${e.message}")
            JSONObject()
        }
    }

    /** Merge [newEntries] into the existing cache for [language] and save. */
    fun save(context: Context, language: String, newEntries: Map<String, String>) {
        try {
            val existing = load(context, language)
            newEntries.forEach { (k, v) -> existing.put(k, v) }
            cacheFile(context, language).writeText(existing.toString())
            Log.d("UiStringsCache", "Saved ${newEntries.size} entries for $language")
        } catch (e: Exception) {
            Log.e("UiStringsCache", "save error: ${e.message}")
        }
    }

    /**
     * Given the English defaults, returns a map of key → englishDefault
     * for every key that is missing or still has its English value in the cache.
     */
    fun missingKeys(
        context: Context,
        language: String,
        defaults: UiStrings
    ): Map<String, String> {
        val cache  = load(context, language)
        val allKeys = defaultsMap(defaults)
        return allKeys.filter { (key, englishValue) ->
            !cache.has(key) || cache.getString(key) == englishValue
        }
    }

    /**
     * Build a [UiStrings] by overlaying the cache on top of English defaults.
     * Any key not yet in the cache keeps its English default.
     */
    fun buildUiStrings(context: Context, language: String, defaults: UiStrings): UiStrings {
        val cache = load(context, language)
        fun s(key: String, default: String) =
            if (cache.has(key)) cache.getString(key) else default
        return UiStrings(
            // ── PlayerHomeScreen ─────────────────────────────────────────────
            homeGreeting                = s("homeGreeting",                defaults.homeGreeting),
            homeSubtitle                = s("homeSubtitle",                defaults.homeSubtitle),
            homeQuizzes                 = s("homeQuizzes",                 defaults.homeQuizzes),
            homeMyGallery               = s("homeMyGallery",               defaults.homeMyGallery),

            // ── QuizzesScreen ─────────────────────────────────────────────────
            quizzesTitle                = s("quizzesTitle",                defaults.quizzesTitle),
            quizzesStartQuiz            = s("quizzesStartQuiz",            defaults.quizzesStartQuiz),
            quizzesHistory              = s("quizzesHistory",              defaults.quizzesHistory),

            // ── PlayerProfileScreen ───────────────────────────────────────────
            profileTitle                = s("profileTitle",                defaults.profileTitle),
            profileStarsCollected       = s("profileStarsCollected",       defaults.profileStarsCollected),
            profileLabelName            = s("profileLabelName",            defaults.profileLabelName),
            profileLabelLanguage        = s("profileLabelLanguage",        defaults.profileLabelLanguage),
            profileLabelSchoolClass     = s("profileLabelSchoolClass",     defaults.profileLabelSchoolClass),
            profileLabelSchoolYear      = s("profileLabelSchoolYear",      defaults.profileLabelSchoolYear),
            profileYearBeginning        = s("profileYearBeginning",        defaults.profileYearBeginning),
            profileYearMiddle           = s("profileYearMiddle",           defaults.profileYearMiddle),
            profileYearEnd              = s("profileYearEnd",              defaults.profileYearEnd),
            profileSaveButton           = s("profileSaveButton",           defaults.profileSaveButton),
            profileErrorNameBlank       = s("profileErrorNameBlank",       defaults.profileErrorNameBlank),
            profileErrorNameTaken       = s("profileErrorNameTaken",       defaults.profileErrorNameTaken),

            // ── GalleryScreen ─────────────────────────────────────────────────
            galleryTitle                = s("galleryTitle",                defaults.galleryTitle),
            galleryEmpty                = s("galleryEmpty",                defaults.galleryEmpty),
            galleryNeedStars            = s("galleryNeedStars",            defaults.galleryNeedStars),
            galleryCreateButton         = s("galleryCreateButton",         defaults.galleryCreateButton),

            // ── GameScreen ────────────────────────────────────────────────────
            gameQuestionCounter         = s("gameQuestionCounter",         defaults.gameQuestionCounter),
            gameLoadingQuestions        = s("gameLoadingQuestions",        defaults.gameLoadingQuestions),
            gameGoToSettings            = s("gameGoToSettings",            defaults.gameGoToSettings),
            gameWorth                   = s("gameWorth",                   defaults.gameWorth),
            gameYourAnswer              = s("gameYourAnswer",              defaults.gameYourAnswer),
            gameTrue                    = s("gameTrue",                    defaults.gameTrue),
            gameFalse                   = s("gameFalse",                   defaults.gameFalse),
            gamePrevious                = s("gamePrevious",                defaults.gamePrevious),
            gameNext                    = s("gameNext",                    defaults.gameNext),
            gameSubmit                  = s("gameSubmit",                  defaults.gameSubmit),
            gameServerBusy              = s("gameServerBusy",              defaults.gameServerBusy),
            gameTryAgain                = s("gameTryAgain",                defaults.gameTryAgain),
            gameApiKeyMissing           = s("gameApiKeyMissing",           defaults.gameApiKeyMissing),
            gameAbandonTitle            = s("gameAbandonTitle",            defaults.gameAbandonTitle),
            gameAbandonMessage          = s("gameAbandonMessage",          defaults.gameAbandonMessage),
            gameAbandonConfirm          = s("gameAbandonConfirm",          defaults.gameAbandonConfirm),
            gameAbandonCancel           = s("gameAbandonCancel",           defaults.gameAbandonCancel),
            gameUnansweredTitle         = s("gameUnansweredTitle",         defaults.gameUnansweredTitle),
            gameUnansweredMessage       = s("gameUnansweredMessage",       defaults.gameUnansweredMessage),
            gameUnansweredQuestion      = s("gameUnansweredQuestion",      defaults.gameUnansweredQuestion),
            gameUnansweredClose         = s("gameUnansweredClose",         defaults.gameUnansweredClose),

            // ── QuizSummaryScreen ─────────────────────────────────────────────
            summaryTitle                = s("summaryTitle",                defaults.summaryTitle),
            summaryStarsEarned          = s("summaryStarsEarned",          defaults.summaryStarsEarned),
            summaryScore                = s("summaryScore",                defaults.summaryScore),
            summaryBonus                = s("summaryBonus",                defaults.summaryBonus),
            summaryQuestionNumber       = s("summaryQuestionNumber",       defaults.summaryQuestionNumber),
            summaryCorrect              = s("summaryCorrect",              defaults.summaryCorrect),
            summaryWrong                = s("summaryWrong",                defaults.summaryWrong),
            summaryYourAnswer           = s("summaryYourAnswer",           defaults.summaryYourAnswer),
            summaryCorrectAnswer        = s("summaryCorrectAnswer",        defaults.summaryCorrectAnswer),
            summaryNoAnswer             = s("summaryNoAnswer",             defaults.summaryNoAnswer),
            summaryOptions              = s("summaryOptions",              defaults.summaryOptions),
            summaryClose                = s("summaryClose",                defaults.summaryClose),

            // ── QuizHistoryScreen ─────────────────────────────────────────────
            historyTitle                = s("historyTitle",                defaults.historyTitle),
            historyEmpty                = s("historyEmpty",                defaults.historyEmpty),
            historyColumnDate           = s("historyColumnDate",           defaults.historyColumnDate),
            historyColumnTime           = s("historyColumnTime",           defaults.historyColumnTime),
            historyColumnDuration       = s("historyColumnDuration",       defaults.historyColumnDuration),
            historyColumnScore          = s("historyColumnScore",          defaults.historyColumnScore),
            historyColumnStars          = s("historyColumnStars",          defaults.historyColumnStars),

            // ── ImageCreationScreen ───────────────────────────────────────────
            imageTitle                  = s("imageTitle",                  defaults.imageTitle),
            imageSubtitle               = s("imageSubtitle",               defaults.imageSubtitle),
            imageCreatingMsg            = s("imageCreatingMsg",            defaults.imageCreatingMsg),
            imageCategoryCharacter      = s("imageCategoryCharacter",      defaults.imageCategoryCharacter),
            imageCategoryAction         = s("imageCategoryAction",         defaults.imageCategoryAction),
            imageCategoryPlace          = s("imageCategoryPlace",          defaults.imageCategoryPlace),
            imageNeedStars              = s("imageNeedStars",              defaults.imageNeedStars),
            imageButtonNeedStars        = s("imageButtonNeedStars",        defaults.imageButtonNeedStars),
            imageButtonPickAll          = s("imageButtonPickAll",          defaults.imageButtonPickAll),
            imageButtonCreate           = s("imageButtonCreate",           defaults.imageButtonCreate),

            // ── MiniGamesScreen ───────────────────────────────────────────────
            miniGamesTitle              = s("miniGamesTitle",              defaults.miniGamesTitle),
            miniGamesLockedHint         = s("miniGamesLockedHint",         defaults.miniGamesLockedHint),
            miniGamesCostHint           = s("miniGamesCostHint",           defaults.miniGamesCostHint),
            miniGamesPuzzleName         = s("miniGamesPuzzleName",         defaults.miniGamesPuzzleName),
            miniGamesPuzzleDesc         = s("miniGamesPuzzleDesc",         defaults.miniGamesPuzzleDesc),

            // ── PuzzleScreen ──────────────────────────────────────────────────
            puzzleTitle                 = s("puzzleTitle",                 defaults.puzzleTitle),
            puzzleSelectSize            = s("puzzleSelectSize",            defaults.puzzleSelectSize),
            puzzleEasy                  = s("puzzleEasy",                  defaults.puzzleEasy),
            puzzleMedium                = s("puzzleMedium",                defaults.puzzleMedium),
            puzzleHard                  = s("puzzleHard",                  defaults.puzzleHard),
            puzzleReroll                = s("puzzleReroll",                defaults.puzzleReroll),
            puzzlePlay                  = s("puzzlePlay",                  defaults.puzzlePlay),
            puzzleNotEnoughStars        = s("puzzleNotEnoughStars",        defaults.puzzleNotEnoughStars),
            puzzleSolved                = s("puzzleSolved",                defaults.puzzleSolved),
            puzzleClose                 = s("puzzleClose",                 defaults.puzzleClose),
            puzzleShowImage             = s("puzzleShowImage",             defaults.puzzleShowImage),
            puzzleTapToReturn           = s("puzzleTapToReturn",           defaults.puzzleTapToReturn),
        )
    }

    /** All keys with their English default values. Used by [missingKeys]. */
    fun defaultsMap(d: UiStrings): Map<String, String> = mapOf(
        // ── PlayerHomeScreen ─────────────────────────────────────────────────
        "homeGreeting"                to d.homeGreeting,
        "homeSubtitle"                to d.homeSubtitle,
        "homeQuizzes"                 to d.homeQuizzes,
        "homeMyGallery"               to d.homeMyGallery,

        // ── QuizHistoryScreen ─────────────────────────────────────────────────
        "historyTitle"                to d.historyTitle,
        "historyEmpty"                to d.historyEmpty,
        "historyColumnDate"           to d.historyColumnDate,
        "historyColumnTime"           to d.historyColumnTime,
        "historyColumnDuration"       to d.historyColumnDuration,
        "historyColumnScore"          to d.historyColumnScore,
        "historyColumnStars"          to d.historyColumnStars,

        // ── QuizzesScreen ─────────────────────────────────────────────────────
        "quizzesTitle"                to d.quizzesTitle,
        "quizzesStartQuiz"            to d.quizzesStartQuiz,
        "quizzesHistory"              to d.quizzesHistory,

        // ── PlayerProfileScreen ───────────────────────────────────────────────
        "profileTitle"                to d.profileTitle,
        "profileStarsCollected"       to d.profileStarsCollected,
        "profileLabelName"            to d.profileLabelName,
        "profileLabelLanguage"        to d.profileLabelLanguage,
        "profileLabelSchoolClass"     to d.profileLabelSchoolClass,
        "profileLabelSchoolYear"      to d.profileLabelSchoolYear,
        "profileYearBeginning"        to d.profileYearBeginning,
        "profileYearMiddle"           to d.profileYearMiddle,
        "profileYearEnd"              to d.profileYearEnd,
        "profileSaveButton"           to d.profileSaveButton,
        "profileErrorNameBlank"       to d.profileErrorNameBlank,
        "profileErrorNameTaken"       to d.profileErrorNameTaken,

        // ── GalleryScreen ─────────────────────────────────────────────────────
        "galleryTitle"                to d.galleryTitle,
        "galleryEmpty"                to d.galleryEmpty,
        "galleryNeedStars"            to d.galleryNeedStars,
        "galleryCreateButton"         to d.galleryCreateButton,

        // ── GameScreen ────────────────────────────────────────────────────────
        "gameQuestionCounter"         to d.gameQuestionCounter,
        "gameLoadingQuestions"        to d.gameLoadingQuestions,
        "gameGoToSettings"            to d.gameGoToSettings,
        "gameWorth"                   to d.gameWorth,
        "gameYourAnswer"              to d.gameYourAnswer,
        "gameTrue"                    to d.gameTrue,
        "gameFalse"                   to d.gameFalse,
        "gamePrevious"                to d.gamePrevious,
        "gameNext"                    to d.gameNext,
        "gameSubmit"                  to d.gameSubmit,
        "gameServerBusy"              to d.gameServerBusy,
        "gameTryAgain"                to d.gameTryAgain,
        "gameApiKeyMissing"           to d.gameApiKeyMissing,
        "gameAbandonTitle"            to d.gameAbandonTitle,
        "gameAbandonMessage"          to d.gameAbandonMessage,
        "gameAbandonConfirm"          to d.gameAbandonConfirm,
        "gameAbandonCancel"           to d.gameAbandonCancel,
        "gameUnansweredTitle"         to d.gameUnansweredTitle,
        "gameUnansweredMessage"       to d.gameUnansweredMessage,
        "gameUnansweredQuestion"      to d.gameUnansweredQuestion,
        "gameUnansweredClose"         to d.gameUnansweredClose,

        // ── QuizSummaryScreen ─────────────────────────────────────────────────
        "summaryTitle"                to d.summaryTitle,
        "summaryStarsEarned"          to d.summaryStarsEarned,
        "summaryScore"                to d.summaryScore,
        "summaryBonus"                to d.summaryBonus,
        "summaryQuestionNumber"       to d.summaryQuestionNumber,
        "summaryCorrect"              to d.summaryCorrect,
        "summaryWrong"                to d.summaryWrong,
        "summaryYourAnswer"           to d.summaryYourAnswer,
        "summaryCorrectAnswer"        to d.summaryCorrectAnswer,
        "summaryNoAnswer"             to d.summaryNoAnswer,
        "summaryOptions"              to d.summaryOptions,
        "summaryClose"                to d.summaryClose,

        // ── QuizHistoryScreen ─────────────────────────────────────────────────
        "historyTitle"                to d.historyTitle,
        "historyEmpty"                to d.historyEmpty,
        "historyColumnDate"           to d.historyColumnDate,
        "historyColumnTime"           to d.historyColumnTime,
        "historyColumnDuration"       to d.historyColumnDuration,
        "historyColumnScore"          to d.historyColumnScore,
        "historyColumnStars"          to d.historyColumnStars,

        // ── ImageCreationScreen ───────────────────────────────────────────────
        "imageTitle"                  to d.imageTitle,
        "imageSubtitle"               to d.imageSubtitle,
        "imageCreatingMsg"            to d.imageCreatingMsg,
        "imageCategoryCharacter"      to d.imageCategoryCharacter,
        "imageCategoryAction"         to d.imageCategoryAction,
        "imageCategoryPlace"          to d.imageCategoryPlace,
        "imageNeedStars"              to d.imageNeedStars,
        "imageButtonNeedStars"        to d.imageButtonNeedStars,
        "imageButtonPickAll"          to d.imageButtonPickAll,
        "imageButtonCreate"           to d.imageButtonCreate,

        // ── MiniGamesScreen ───────────────────────────────────────────────────
        "miniGamesTitle"              to d.miniGamesTitle,
        "miniGamesLockedHint"         to d.miniGamesLockedHint,
        "miniGamesCostHint"           to d.miniGamesCostHint,
        "miniGamesPuzzleName"         to d.miniGamesPuzzleName,
        "miniGamesPuzzleDesc"         to d.miniGamesPuzzleDesc,

        // ── PuzzleScreen ──────────────────────────────────────────────────────
        "puzzleTitle"                 to d.puzzleTitle,
        "puzzleSelectSize"            to d.puzzleSelectSize,
        "puzzleEasy"                  to d.puzzleEasy,
        "puzzleMedium"                to d.puzzleMedium,
        "puzzleHard"                  to d.puzzleHard,
        "puzzleReroll"                to d.puzzleReroll,
        "puzzlePlay"                  to d.puzzlePlay,
        "puzzleNotEnoughStars"        to d.puzzleNotEnoughStars,
        "puzzleSolved"                to d.puzzleSolved,
        "puzzleClose"                 to d.puzzleClose,
        "puzzleShowImage"             to d.puzzleShowImage,
        "puzzleTapToReturn"           to d.puzzleTapToReturn,
    )
}