package com.galleriaida.ui

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File

/**
 * Reads and writes per-language translation caches.
 * Each language is stored as translations_<language>.json in the app's files dir.
 * The file is a flat JSON object: { "homeGreeting": "...", "homeSubtitle": "...", ... }
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
        val cache = load(context, language)
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
            homeGreeting = s("homeGreeting", defaults.homeGreeting),
            homeSubtitle = s("homeSubtitle", defaults.homeSubtitle),
            homeStartLesson = s("homeStartLesson", defaults.homeStartLesson),
            homeMyGallery = s("homeMyGallery", defaults.homeMyGallery),
            profileTitle = s("profileTitle", defaults.profileTitle),
            profileStarsCollected = s("profileStarsCollected", defaults.profileStarsCollected),
            profileLabelName = s("profileLabelName", defaults.profileLabelName),
            profileLabelLanguage = s("profileLabelLanguage", defaults.profileLabelLanguage),
            profileLabelSchoolClass = s("profileLabelSchoolClass", defaults.profileLabelSchoolClass),
            profileLabelSchoolYear = s("profileLabelSchoolYear", defaults.profileLabelSchoolYear),
            profileYearBeginning = s("profileYearBeginning", defaults.profileYearBeginning),
            profileYearMiddle = s("profileYearMiddle", defaults.profileYearMiddle),
            profileYearEnd = s("profileYearEnd", defaults.profileYearEnd),
            profileSaveButton = s("profileSaveButton", defaults.profileSaveButton),
            profileErrorNameBlank = s("profileErrorNameBlank", defaults.profileErrorNameBlank),
            profileErrorNameTaken = s("profileErrorNameTaken", defaults.profileErrorNameTaken),
            galleryTitle = s("galleryTitle", defaults.galleryTitle),
            galleryEmpty = s("galleryEmpty", defaults.galleryEmpty),
            galleryNeedStars = s("galleryNeedStars", defaults.galleryNeedStars),
            galleryCreateButton = s("galleryCreateButton", defaults.galleryCreateButton),
            gameQuestionCounter = s("gameQuestionCounter", defaults.gameQuestionCounter),
            gameLoadingQuestions = s("gameLoadingQuestions", defaults.gameLoadingQuestions),
            gameLessonDone = s("gameLessonDone", defaults.gameLessonDone),
            gameEarnedStars = s("gameEarnedStars", defaults.gameEarnedStars),
            gameOkButton = s("gameOkButton", defaults.gameOkButton),
            gameCorrect = s("gameCorrect", defaults.gameCorrect),
            gameWrong = s("gameWrong", defaults.gameWrong),
            gameWorth = s("gameWorth", defaults.gameWorth),
            gameYourAnswer = s("gameYourAnswer", defaults.gameYourAnswer),
            gameCheckAnswer = s("gameCheckAnswer", defaults.gameCheckAnswer),
            gameNext = s("gameNext", defaults.gameNext),
            gameEarnedSoFar = s("gameEarnedSoFar", defaults.gameEarnedSoFar),
            gameGoToSettings = s("gameGoToSettings", defaults.gameGoToSettings),
            imageTitle = s("imageTitle", defaults.imageTitle),
            imageSubtitle = s("imageSubtitle", defaults.imageSubtitle),
            imageCreatingMsg = s("imageCreatingMsg", defaults.imageCreatingMsg),
            imageCategoryCharacter = s("imageCategoryCharacter", defaults.imageCategoryCharacter),
            imageCategoryAction = s("imageCategoryAction", defaults.imageCategoryAction),
            imageCategoryPlace = s("imageCategoryPlace", defaults.imageCategoryPlace),
            imageNeedStars = s("imageNeedStars", defaults.imageNeedStars),
            imageButtonNeedStars = s("imageButtonNeedStars", defaults.imageButtonNeedStars),
            imageButtonPickAll = s("imageButtonPickAll", defaults.imageButtonPickAll),
            imageButtonCreate = s("imageButtonCreate", defaults.imageButtonCreate),
        )
    }

    /** All keys with their English default values. */
    fun defaultsMap(d: UiStrings): Map<String, String> = mapOf(
        "homeGreeting" to d.homeGreeting,
        "homeSubtitle" to d.homeSubtitle,
        "homeStartLesson" to d.homeStartLesson,
        "homeMyGallery" to d.homeMyGallery,
        "profileTitle" to d.profileTitle,
        "profileStarsCollected" to d.profileStarsCollected,
        "profileLabelName" to d.profileLabelName,
        "profileLabelLanguage" to d.profileLabelLanguage,
        "profileLabelSchoolClass" to d.profileLabelSchoolClass,
        "profileLabelSchoolYear" to d.profileLabelSchoolYear,
        "profileYearBeginning" to d.profileYearBeginning,
        "profileYearMiddle" to d.profileYearMiddle,
        "profileYearEnd" to d.profileYearEnd,
        "profileSaveButton" to d.profileSaveButton,
        "profileErrorNameBlank" to d.profileErrorNameBlank,
        "profileErrorNameTaken" to d.profileErrorNameTaken,
        "galleryTitle" to d.galleryTitle,
        "galleryEmpty" to d.galleryEmpty,
        "galleryNeedStars" to d.galleryNeedStars,
        "galleryCreateButton" to d.galleryCreateButton,
        "gameQuestionCounter" to d.gameQuestionCounter,
        "gameLoadingQuestions" to d.gameLoadingQuestions,
        "gameLessonDone" to d.gameLessonDone,
        "gameEarnedStars" to d.gameEarnedStars,
        "gameOkButton" to d.gameOkButton,
        "gameCorrect" to d.gameCorrect,
        "gameWrong" to d.gameWrong,
        "gameWorth" to d.gameWorth,
        "gameYourAnswer" to d.gameYourAnswer,
        "gameCheckAnswer" to d.gameCheckAnswer,
        "gameNext" to d.gameNext,
        "gameEarnedSoFar" to d.gameEarnedSoFar,
        "gameGoToSettings" to d.gameGoToSettings,
        "imageTitle" to d.imageTitle,
        "imageSubtitle" to d.imageSubtitle,
        "imageCreatingMsg" to d.imageCreatingMsg,
        "imageCategoryCharacter" to d.imageCategoryCharacter,
        "imageCategoryAction" to d.imageCategoryAction,
        "imageCategoryPlace" to d.imageCategoryPlace,
        "imageNeedStars" to d.imageNeedStars,
        "imageButtonNeedStars" to d.imageButtonNeedStars,
        "imageButtonPickAll" to d.imageButtonPickAll,
        "imageButtonCreate" to d.imageButtonCreate,
    )
}
