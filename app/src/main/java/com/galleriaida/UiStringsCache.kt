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
 */
object UiStringsCache {

    private fun cacheFile(context: Context, language: String): File {
        val dir = File(context.filesDir, "translations").also { it.mkdirs() }
        return File(dir, "translations_${language.lowercase().replace(" ", "_")}.json")
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

            // Kept for backwards compatibility
            gameLessonDone              = s("gameLessonDone",              defaults.gameLessonDone),
            gameEarnedStars             = s("gameEarnedStars",             defaults.gameEarnedStars),
            gameOkButton                = s("gameOkButton",                defaults.gameOkButton),
            gameCorrect                 = s("gameCorrect",                 defaults.gameCorrect),
            gameWrong                   = s("gameWrong",                   defaults.gameWrong),
            gameCheckAnswer             = s("gameCheckAnswer",             defaults.gameCheckAnswer),
            gameEarnedSoFar             = s("gameEarnedSoFar",             defaults.gameEarnedSoFar),

            // ── QuizSummaryScreen ─────────────────────────────────────────────
            summaryTitle                = s("summaryTitle",                defaults.summaryTitle),
            summaryStarsEarned          = s("summaryStarsEarned",          defaults.summaryStarsEarned),
            summaryScore                = s("summaryScore",                defaults.summaryScore),
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

        // Kept for backwards compatibility
        "gameLessonDone"              to d.gameLessonDone,
        "gameEarnedStars"             to d.gameEarnedStars,
        "gameOkButton"                to d.gameOkButton,
        "gameCorrect"                 to d.gameCorrect,
        "gameWrong"                   to d.gameWrong,
        "gameCheckAnswer"             to d.gameCheckAnswer,
        "gameEarnedSoFar"             to d.gameEarnedSoFar,

        // ── QuizSummaryScreen ─────────────────────────────────────────────────
        "summaryTitle"                to d.summaryTitle,
        "summaryStarsEarned"          to d.summaryStarsEarned,
        "summaryScore"                to d.summaryScore,
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
    )
}