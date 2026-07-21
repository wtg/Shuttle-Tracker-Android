package edu.rpi.shuttletracker.app.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import edu.rpi.shuttletracker.feature.map.MapsScreen
import edu.rpi.shuttletracker.feature.settings.SettingsScreen
import edu.rpi.shuttletracker.feature.settings.about.AboutScreen
import edu.rpi.shuttletracker.feature.settings.about.LibrariesScreen
import edu.rpi.shuttletracker.feature.settings.developerMenu.DevMenuScreen
import edu.rpi.shuttletracker.feature.setup.SetupScreen
import kotlinx.serialization.Serializable

@Serializable
private data object MapsRoute : NavKey

@Serializable
private data object SetupRoute : NavKey

@Serializable
private data object SettingsRoute : NavKey

@Serializable
private data object AboutRoute : NavKey

@Serializable
private data object LibrariesRoute : NavKey

@Serializable
private data object DeveloperOptionsRoute : NavKey

/**
 * Owns the app's complete navigation state and maps route keys to feature screens.
 *
 * Feature screens receive callbacks instead of a navigation object, keeping them easy to preview,
 * test, and reuse.
 */
@Composable
fun AppNavigation(setupCompleted: Boolean) {
    val startRoute: NavKey = if (setupCompleted) MapsRoute else SetupRoute
    val backStack = rememberNavBackStack(startRoute)

    fun navigateTo(route: NavKey) {
        if (backStack.lastOrNull() != route) {
            backStack.add(route)
        }
    }

    fun navigateBack() {
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
        }
    }

    fun resetTo(route: NavKey) {
        backStack.clear()
        backStack.add(route)
    }

    NavDisplay(
        backStack = backStack,
        onBack = ::navigateBack,
        entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
        entryProvider =
            entryProvider {
                entry<MapsRoute> {
                    MapsScreen(
                        onOpenSettings = { navigateTo(SettingsRoute) },
                    )
                }
                entry<SetupRoute> {
                    SetupScreen(
                        onSetupComplete = { resetTo(MapsRoute) },
                    )
                }
                entry<SettingsRoute> {
                    SettingsScreen(
                        onBack = ::navigateBack,
                        onRedoSetup = { resetTo(SetupRoute) },
                        onOpenAbout = { navigateTo(AboutRoute) },
                        onOpenDeveloperOptions = { navigateTo(DeveloperOptionsRoute) },
                    )
                }
                entry<AboutRoute> {
                    AboutScreen(
                        onBack = ::navigateBack,
                        onOpenLibraries = { navigateTo(LibrariesRoute) },
                    )
                }
                entry<LibrariesRoute> {
                    LibrariesScreen(onOpened = ::navigateBack)
                }
                entry<DeveloperOptionsRoute> {
                    DevMenuScreen(onBack = ::navigateBack)
                }
            },
    )
}
