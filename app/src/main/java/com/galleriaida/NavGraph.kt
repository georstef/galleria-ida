package com.galleriaida.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.galleriaida.ui.screens.GameScreen
import com.galleriaida.ui.screens.GalleryScreen
import com.galleriaida.ui.screens.ImageCreationScreen
import com.galleriaida.ui.screens.QuizzesScreen
import com.galleriaida.ui.screens.QuizHistoryScreen
import com.galleriaida.ui.screens.QuizSummaryScreen
import com.galleriaida.ui.screens.PlayerSelectionScreen
import com.galleriaida.ui.screens.PlayerBasicSetupScreen
import com.galleriaida.ui.screens.PlayerProfileScreen
import com.galleriaida.ui.screens.PlayerHomeScreen
import com.galleriaida.ui.screens.PlayerLoadingScreen
import com.galleriaida.ui.screens.SettingsScreen
import com.galleriaida.ui.screens.MiniGamesScreen
import com.galleriaida.ui.screens.PuzzleScreen
import com.galleriaida.ui.screens.FillTheBlankScreen
import com.galleriaida.ui.screens.OnePieceScreen
import com.galleriaida.viewmodel.AppViewModel

object Routes {
    const val PLAYER_SELECTION   = "player_selection"
    const val PLAYER_BASIC_SETUP = "player_basic_setup"
    const val PLAYER_PROFILE     = "player_profile/{isNewPlayer}"
    const val PLAYER_LOADING     = "player_loading"
    const val PLAYER_HOME        = "player_home"
    const val QUIZZES            = "quizzes"
    const val GAME               = "game"
    const val QUIZ_HISTORY       = "quiz_history"
    // fromHistory: true when reached from history, false when reached after submitting a quiz
    const val QUIZ_SUMMARY       = "quiz_summary/{fromHistory}"
    const val GALLERY            = "gallery"
    const val IMAGE_CREATION     = "image_creation"
    const val SETTINGS           = "settings"
    const val MINI_GAMES         = "mini_games"
    const val PUZZLE             = "puzzle"
    const val FILL_THE_BLANK     = "fill_the_blank"
    const val ONE_PIECE          = "one_piece"

    fun quizSummary(fromHistory: Boolean) = "quiz_summary/$fromHistory"
    fun playerProfile(isNewPlayer: Boolean = false) = "player_profile/$isNewPlayer"
}

@Composable
fun AppNavGraph(navController: NavHostController, viewModel: AppViewModel) {
    NavHost(navController = navController, startDestination = Routes.PLAYER_SELECTION) {

        composable(Routes.PLAYER_SELECTION) {
            PlayerSelectionScreen(
                viewModel        = viewModel,
                onPlayerSelected = { navController.navigate(Routes.PLAYER_LOADING) },
                onNewPlayer      = { navController.navigate(Routes.PLAYER_BASIC_SETUP) }
            )
        }

        composable(Routes.PLAYER_BASIC_SETUP) {
            PlayerBasicSetupScreen(
                viewModel  = viewModel,
                onBack     = { navController.popBackStack() },
                onContinue = {
                    navController.navigate(Routes.playerProfile(isNewPlayer = true)) {
                        popUpTo(Routes.PLAYER_SELECTION)
                    }
                }
            )
        }

        composable(
            route     = Routes.PLAYER_PROFILE,
            arguments = listOf(navArgument("isNewPlayer") { type = NavType.BoolType; defaultValue = false })
        ) { backStackEntry ->
            val isNewPlayer = backStackEntry.arguments?.getBoolean("isNewPlayer") ?: false
            PlayerProfileScreen(
                viewModel  = viewModel,
                onDone     = {
                    if (isNewPlayer) {
                        navController.navigate(Routes.PLAYER_LOADING) {
                            popUpTo(Routes.PLAYER_SELECTION)
                        }
                    } else {
                        navController.popBackStack()
                    }
                },
                onBack     = { navController.popBackStack() },
                onSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.PLAYER_LOADING) {
            PlayerLoadingScreen(
                viewModel = viewModel,
                onReady   = {
                    navController.navigate(Routes.PLAYER_HOME) {
                        popUpTo(Routes.PLAYER_LOADING) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.PLAYER_HOME) {
            PlayerHomeScreen(
                viewModel     = viewModel,
                onQuizzes     = { navController.navigate(Routes.QUIZZES) },
                onGallery     = { navController.navigate(Routes.GALLERY) },
                onMiniGames   = { navController.navigate(Routes.MINI_GAMES) },
                onSettings    = { navController.navigate(Routes.SETTINGS) },
                onEditProfile = { navController.navigate(Routes.playerProfile()) },
                onBack        = { navController.navigate(Routes.PLAYER_SELECTION) { popUpTo(0) } }
            )
        }

        composable(Routes.QUIZZES) {
            QuizzesScreen(
                viewModel     = viewModel,
                onBack        = { navController.popBackStack() },
                onStartQuiz   = { navController.navigate(Routes.GAME) },
                onHistory     = { navController.navigate(Routes.QUIZ_HISTORY) },
                onSettings    = { navController.navigate(Routes.SETTINGS) },
                onEditProfile = { navController.navigate(Routes.playerProfile()) }
            )
        }

        composable(Routes.GAME) {
            GameScreen(
                viewModel   = viewModel,
                onAbandoned = {
                    navController.navigate(Routes.QUIZZES) {
                        popUpTo(Routes.QUIZZES) { inclusive = true }
                    }
                },
                onSubmitted = {
                    // Stars already awarded — go to summary (not from history), clear game from stack
                    navController.navigate(Routes.quizSummary(fromHistory = false)) {
                        popUpTo(Routes.GAME) { inclusive = true }
                    }
                },
                onEditProfile = { navController.navigate(Routes.playerProfile()) }
            )
        }

        composable(Routes.QUIZ_HISTORY) {
            QuizHistoryScreen(
                viewModel = viewModel,
                onBack    = { navController.popBackStack() },
                onQuizSelected = { quiz ->
                    viewModel.selectHistoryQuiz(quiz)
                    navController.navigate(Routes.quizSummary(fromHistory = true))
                }
            )
        }

        composable(
            route     = Routes.QUIZ_SUMMARY,
            arguments = listOf(navArgument("fromHistory") { type = NavType.BoolType })
        ) { backStackEntry ->
            val fromHistory = backStackEntry.arguments?.getBoolean("fromHistory") ?: false
            QuizSummaryScreen(
                viewModel   = viewModel,
                fromHistory = fromHistory,
                onClose     = {
                    viewModel.clearLastCompletedQuiz()
                    if (fromHistory) {
                        // Return to history list
                        navController.popBackStack()
                    } else {
                        // Return to player home, clear full back stack
                        navController.navigate(Routes.QUIZZES) {
                            popUpTo(Routes.PLAYER_HOME) { inclusive = false }
                        }
                    }
                }
            )
        }

        composable(Routes.GALLERY) {
            GalleryScreen(
                viewModel     = viewModel,
                onBack        = { navController.popBackStack() },
                onSettings    = { navController.navigate(Routes.SETTINGS) },
                onCreateImage = { navController.navigate(Routes.IMAGE_CREATION) },
                onEditProfile = { navController.navigate(Routes.playerProfile()) }
            )
        }

        composable(Routes.IMAGE_CREATION) {
            ImageCreationScreen(
                viewModel      = viewModel,
                onBack         = { navController.popBackStack() },
                onImageCreated = {
                    navController.navigate(Routes.GALLERY) {
                        popUpTo(Routes.GALLERY) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                onBack    = {
                    val currentPlayer = viewModel.currentPlayer.value
                    val players       = viewModel.players.value
                    if (currentPlayer == null || players.none { it.id == currentPlayer.id }) {
                        navController.navigate(Routes.PLAYER_SELECTION) { popUpTo(0) }
                    } else {
                        navController.popBackStack()
                    }
                }
            )
        }

        composable(Routes.MINI_GAMES) {
            MiniGamesScreen(
                viewModel      = viewModel,
                onBack         = { navController.popBackStack() },
                onPuzzle       = { navController.navigate(Routes.PUZZLE) },
                onFillTheBlank = { navController.navigate(Routes.FILL_THE_BLANK) },
                onOnePiece     = { navController.navigate(Routes.ONE_PIECE) },
                onEditProfile  = { navController.navigate(Routes.playerProfile()) }
            )
        }

        composable(Routes.PUZZLE) {
            PuzzleScreen(
                viewModel = viewModel,
                onBack    = { navController.popBackStack() }
            )
        }

        composable(Routes.FILL_THE_BLANK) {
            FillTheBlankScreen(
                viewModel = viewModel,
                onBack    = { navController.popBackStack() }
            )
        }

        composable(Routes.ONE_PIECE) {
            OnePieceScreen(
                viewModel = viewModel,
                onBack    = { navController.popBackStack() }
            )
        }
    }
}