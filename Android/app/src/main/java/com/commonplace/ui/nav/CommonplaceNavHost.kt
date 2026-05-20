package com.commonplace.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.commonplace.CommonplaceApp
import com.commonplace.ui.about.AboutScreen
import com.commonplace.ui.fragments.FragmentDetailScreen
import com.commonplace.ui.fragments.FragmentsListScreen
import com.commonplace.ui.ledger.LedgerScreen
import com.commonplace.ui.letters.LettersScreen
import com.commonplace.ui.log.LogScreen
import com.commonplace.ui.settings.SettingsScreen
import com.commonplace.ui.welcome.WelcomeScreen
import com.commonplace.ui.theme.Ink
import com.commonplace.ui.theme.MetaMono
import com.commonplace.ui.theme.Muted
import com.commonplace.ui.theme.Paper
import com.commonplace.ui.theme.Rule

object Routes {
    const val Welcome = "welcome"
    const val Fragments = "fragments"
    const val FragmentDetail = "fragments/{id}"
    fun fragmentDetail(id: String) = "fragments/$id"
    const val Log = "log"
    const val Ledger = "ledger"
    const val Letters = "letters"
    const val Settings = "settings"
    const val About = "about"
}

private data class TopLevelDestination(val route: String, val label: String)

private val TopLevel = listOf(
    TopLevelDestination(Routes.Fragments, "fragments"),
    TopLevelDestination(Routes.Log, "log"),
    TopLevelDestination(Routes.Ledger, "ledger"),
    TopLevelDestination(Routes.Letters, "letters"),
    TopLevelDestination(Routes.Settings, "settings"),
)

@Composable
fun CommonplaceNavHost(app: CommonplaceApp, startWithWelcome: Boolean = false) {
    val nav = rememberNavController()
    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showAppChrome = currentRoute != Routes.Welcome && currentRoute != null

    Scaffold(
        containerColor = Paper,
        contentColor = Ink,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = { if (showAppChrome) TopHeader() },
        bottomBar = { if (showAppChrome) BottomNav(nav) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Paper)
                .padding(padding)
                .consumeWindowInsets(padding),
        ) {
            NavHost(
                navController = nav,
                startDestination = if (startWithWelcome) Routes.Welcome else Routes.Fragments,
            ) {
                composable(Routes.Welcome) {
                    WelcomeScreen(
                        app = app,
                        onFinishToCapture = {
                            nav.navigate(Routes.Fragments) {
                                popUpTo(Routes.Welcome) { inclusive = true }
                            }
                        },
                        onFinishToSettings = {
                            nav.navigate(Routes.Settings) {
                                popUpTo(Routes.Welcome) { inclusive = true }
                            }
                        },
                    )
                }
                composable(Routes.Fragments) {
                    FragmentsListScreen(
                        app = app,
                        onOpen = { id -> nav.navigate(Routes.fragmentDetail(id)) },
                    )
                }
                composable(Routes.FragmentDetail) { entry ->
                    val id = entry.arguments?.getString("id") ?: return@composable
                    FragmentDetailScreen(
                        app = app,
                        fragmentId = id,
                        onBack = { nav.popBackStack() },
                    )
                }
                composable(Routes.Log) { LogScreen() }
                composable(Routes.Ledger) { LedgerScreen(app) }
                composable(Routes.Letters) { LettersScreen(app) }
                composable(Routes.Settings) {
                    SettingsScreen(
                        app = app,
                        onOpenAbout = { nav.navigate(Routes.About) },
                    )
                }
                composable(Routes.About) {
                    AboutScreen(onBack = { nav.popBackStack() })
                }
            }
        }
    }
}

@Composable
private fun TopHeader() {
    Surface(
        color = Paper,
        contentColor = Ink,
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Commonplace",
                style = androidx.compose.ui.text.TextStyle(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                    fontSize = 22.sp,
                    color = Ink,
                ),
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 12.dp),
            )
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Rule),
            )
        }
    }
}

@Composable
private fun BottomNav(nav: NavHostController) {
    val backStackEntry by nav.currentBackStackEntryAsState()
    val current = backStackEntry?.destination?.route

    Surface(
        color = Paper,
        contentColor = Ink,
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Rule),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (item in TopLevel) {
                    val active = current == item.route ||
                        (item.route == Routes.Fragments && current == Routes.FragmentDetail) ||
                        (item.route == Routes.Settings && current == Routes.About)
                    NavLabel(
                        label = item.label,
                        active = active,
                        onClick = {
                            if (current != item.route) {
                                nav.navigate(item.route) {
                                    popUpTo(nav.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun NavLabel(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        style = MetaMono.copy(color = if (active) Ink else Muted),
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 6.dp),
    )
}
