package com.turkcekolay.app.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.turkcekolay.app.domain.model.LessonInfo
import com.turkcekolay.app.domain.model.LessonType
import com.turkcekolay.app.features.aitutor.AiTutorScreen
import com.turkcekolay.app.features.alphabet.AlphabetScreen
import com.turkcekolay.app.features.conversations.ConversationListScreen
import com.turkcekolay.app.features.conversations.ConversationScreen
import com.turkcekolay.app.features.conversations.ConversationViewModel
import com.turkcekolay.app.features.home.HomeScreen
import com.turkcekolay.app.features.lessons.LessonScreen
import com.turkcekolay.app.features.onboarding.OnboardingScreen
import com.turkcekolay.app.features.progress.ProgressScreen
import com.turkcekolay.app.features.settings.SettingsScreen
import com.turkcekolay.app.features.smartreview.SmartReviewScreen
import com.turkcekolay.app.features.vocabulary.CategoriesScreen
import com.turkcekolay.app.features.vocabulary.WordsScreen
import com.turkcekolay.app.features.vocabulary.WordsViewModel

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val REVIEW = "review"
    const val PROGRESS = "progress"
    const val TUTOR = "tutor"
    const val SETTINGS = "settings"
    const val LESSON = "lesson/{lessonId}"
    const val ALPHABET = "alphabet"
    const val CATEGORIES = "categories"
    const val WORDS = "words/{categoryId}"
    const val CONVERSATIONS = "conversations"
    const val CONVERSATION = "conversation/{conversationId}/{lessonId}"

    fun lesson(id: String) = "lesson/$id"
    fun words(categoryId: String) = "words/$categoryId"
    fun conversation(id: String, lessonId: String?) = "conversation/$id/${lessonId ?: ConversationViewModel.NO_LESSON}"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "الرئيسية", Icons.Rounded.Home),
    Tab(Routes.REVIEW, "مراجعتي", Icons.Rounded.Replay),
    Tab(Routes.PROGRESS, "تقدّمي", Icons.Rounded.BarChart),
    Tab(Routes.TUTOR, "المعلّم", Icons.Rounded.Psychology),
    Tab(Routes.SETTINGS, "الإعدادات", Icons.Rounded.Settings),
)

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val entry by navController.currentBackStackEntryAsState()
    val destination = entry?.destination
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        tabs.forEach { tab ->
            val selected = destination?.hierarchy?.any { it.route == tab.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(tab.route) {
                        popUpTo(Routes.HOME) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(tab.label, maxLines = 1) },
                colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer),
            )
        }
    }
}

@Composable
fun AppNavHost(onboardingCompleted: Boolean) {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val currentRoute = entry?.destination?.route
    val showBar = tabs.any { it.route == currentRoute }

    val openLesson: (LessonInfo) -> Unit = { lesson ->
        if (lesson.type == LessonType.CONVERSATION && lesson.conversationId != null) {
            navController.navigate(Routes.conversation(lesson.conversationId, lesson.id))
        } else {
            navController.navigate(Routes.lesson(lesson.id))
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { if (showBar) BottomNavigationBar(navController) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            NavHost(navController, startDestination = if (onboardingCompleted) Routes.HOME else Routes.ONBOARDING) {
                composable(Routes.ONBOARDING) {
                    OnboardingScreen(onFinished = { startLesson ->
                        navController.navigate(Routes.HOME) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                        if (startLesson) navController.navigate(Routes.lesson("alpha_1"))
                    })
                }
                composable(Routes.HOME) {
                    HomeScreen(
                        onOpenLesson = openLesson,
                        onOpenAlphabet = { navController.navigate(Routes.ALPHABET) },
                        onOpenVocabulary = { navController.navigate(Routes.CATEGORIES) },
                        onOpenConversations = { navController.navigate(Routes.CONVERSATIONS) },
                        onOpenFavorites = { navController.navigate(Routes.words(WordsViewModel.FAVORITES)) },
                        onOpenReview = {
                            navController.navigate(Routes.REVIEW) {
                                popUpTo(Routes.HOME) { saveState = true }
                                launchSingleTop = true
                            }
                        },
                    )
                }
                composable(Routes.REVIEW) { SmartReviewScreen() }
                composable(Routes.PROGRESS) { ProgressScreen() }
                composable(Routes.TUTOR) { AiTutorScreen() }
                composable(Routes.SETTINGS) { SettingsScreen() }
                composable(Routes.LESSON, arguments = listOf(navArgument("lessonId") { type = NavType.StringType })) {
                    LessonScreen(onClose = { navController.popBackStack() })
                }
                composable(Routes.ALPHABET) {
                    AlphabetScreen(
                        onBack = { navController.popBackStack() },
                        onStartLesson = { navController.navigate(Routes.lesson(it)) },
                    )
                }
                composable(Routes.CATEGORIES) {
                    CategoriesScreen(onBack = { navController.popBackStack() }, onOpenCategory = { navController.navigate(Routes.words(it)) })
                }
                composable(Routes.WORDS, arguments = listOf(navArgument("categoryId") { type = NavType.StringType })) {
                    WordsScreen(onBack = { navController.popBackStack() })
                }
                composable(Routes.CONVERSATIONS) {
                    ConversationListScreen(
                        onBack = { navController.popBackStack() },
                        onOpen = { navController.navigate(Routes.conversation(it, null)) },
                    )
                }
                composable(
                    Routes.CONVERSATION,
                    arguments = listOf(
                        navArgument("conversationId") { type = NavType.StringType },
                        navArgument("lessonId") { type = NavType.StringType },
                    ),
                ) {
                    ConversationScreen(onClose = { navController.popBackStack() })
                }
            }
        }
    }
}
