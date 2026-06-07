package com.gelleriaida.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.gelleriaida.ui.screens.GameScreen
import com.gelleriaida.ui.screens.GalleryScreen
import com.gelleriaida.ui.screens.PlayerSelectionScreen
import com.gelleriaida.ui.screens.PlayerBasicSetupScreen
import com.gelleriaida.ui.screens.PlayerProfileScreen
import com.gelleriaida.ui.screens.PlayerHomeScreen
import com.gelleriaida.ui.screens.SettingsScreen
import com.gelleriaida.viewmodel.AppViewModel

object Routes {
    const val PLAYER_SELECTION = "player_selection"
    const val PLAYER_BASIC_SETUP = "player_basic_setup"
    const val PLAYER_PROFILE = "player_profile"
    const val PLAYER_HOME = "player_home"
    const val GAME = "game"
    const val GALLERY = "gallery"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavGraph(navController: NavHostController, viewModel: AppViewModel) {
    NavHost(navController = navController, startDestination = Routes.PLAYER_SELECTION) {
        composable(Routes.PLAYER_SELECTION) {
            PlayerSelectionScreen(
                viewModel = viewModel,
                onPlayerSelected = { navController.navigate(Routes.PLAYER_HOME) },
                onNewPlayer = { navController.navigate(Routes.PLAYER_BASIC_SETUP) }
            )
        }
        composable(Routes.PLAYER_BASIC_SETUP) {
            PlayerBasicSetupScreen(
                viewModel = viewModel,
                onContinue = {
                    navController.navigate(Routes.PLAYER_PROFILE) {
                        popUpTo(Routes.PLAYER_SELECTION)
                    }
                }
            )
        }
        composable(Routes.PLAYER_PROFILE) {
            PlayerProfileScreen(
                viewModel = viewModel,
                onDone = { navController.navigate(Routes.PLAYER_HOME) { popUpTo(Routes.PLAYER_SELECTION) } },
                onBack = { navController.popBackStack() },
                onSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.PLAYER_HOME) {
            PlayerHomeScreen(
                viewModel = viewModel,
                onStartLesson = { navController.navigate(Routes.GAME) },
                onGallery = { navController.navigate(Routes.GALLERY) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
                onEditProfile = { navController.navigate(Routes.PLAYER_PROFILE) },
                onBack = { navController.navigate(Routes.PLAYER_SELECTION) { popUpTo(0) } }
            )
        }
        composable(Routes.GAME) {
            GameScreen(
                viewModel = viewModel,
                onFinished = { navController.popBackStack() },
                onSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.GALLERY) {
            GalleryScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                onBack = {
                    val currentPlayer = viewModel.currentPlayer.value
                    val players = viewModel.players.value
                    if (currentPlayer == null || players.none { it.id == currentPlayer.id }) {
                        navController.navigate(Routes.PLAYER_SELECTION) { popUpTo(0) }
                    } else {
                        navController.popBackStack()
                    }
                }
            )
        }
    }
}
