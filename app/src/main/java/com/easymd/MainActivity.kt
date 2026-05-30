package com.easymd

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.easymd.ui.screens.EditNoteScreen
import com.easymd.ui.screens.NoteListScreen
import com.easymd.ui.screens.SettingsScreen
import com.easymd.ui.screens.StorageSettingsScreen
import com.easymd.ui.theme.EasyMDTheme
import com.easymd.ui.viewmodel.ThemeMode
import com.easymd.ui.viewmodel.ThemeViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeViewModel: ThemeViewModel = viewModel()
            val themeMode by themeViewModel.themeMode.collectAsState()
            val systemDarkMode = isSystemInDarkTheme()

            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> systemDarkMode
            }

            EasyMDTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = "noteList"
                    ) {
                        composable("noteList") {
                            NoteListScreen(
                                navController = navController,
                                themeViewModel = themeViewModel
                            )
                        }
                        composable("editNote/{noteId}") { backStackEntry ->
                            val noteId = backStackEntry.arguments?.getString("noteId")
                            EditNoteScreen(navController, noteId)
                        }
                        composable("newNote") {
                            EditNoteScreen(navController, null)
                        }
                        composable("settings") {
                            SettingsScreen(
                                navController = navController,
                                themeMode = themeMode,
                                onThemeModeChange = { themeViewModel.setThemeMode(it) }
                            )
                        }
                        composable("storageSettings/{libraryId}") { backStackEntry ->
                            val libraryId = backStackEntry.arguments?.getString("libraryId")
                            StorageSettingsScreen(navController, libraryId)
                        }
                        composable("storageSettings") {
                            StorageSettingsScreen(navController, null)
                        }
                    }
                }
            }
        }
    }
}
