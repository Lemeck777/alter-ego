package com.alterego.app.feature.root

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.alterego.app.feature.alterego.AlterEgoPickerScreen
import com.alterego.app.feature.alterego.CustomAlterEgoScreen
import com.alterego.app.feature.journey.AnnualReviewScreen
import com.alterego.app.feature.journey.JourneyScreen
import com.alterego.app.feature.journey.SavedMomentsScreen
import com.alterego.app.feature.me.CommitmentsScreen
import com.alterego.app.feature.me.FutureMeScreen
import com.alterego.app.feature.me.MeScreen
import com.alterego.app.feature.me.NewCommitmentScreen
import com.alterego.app.feature.me.PrivacyScreen
import com.alterego.app.feature.me.QuotesScreen
import com.alterego.app.feature.me.RemindersScreen
import com.alterego.app.feature.me.SettingsScreen
import com.alterego.app.feature.moment.MomentRoute
import com.alterego.app.feature.onboarding.OnboardingScreen
import com.alterego.app.feature.premium.PremiumScreen
import com.alterego.app.feature.reset.ResetScreen
import com.alterego.app.feature.science.BiologyScreen
import com.alterego.app.feature.science.LessonScreen
import com.alterego.app.feature.science.ScienceScreen
import com.alterego.app.feature.today.TodayScreen
import com.alterego.app.feature.urge.UrgeScreen

/**
 * The whole app graph. Three tabs plus the flows that open on top of them.
 * Everything the user can reach is listed here, so the surface stays small on purpose.
 */
@Composable
fun AlterEgoNavHost(
    navController: NavHostController,
    startDestination: String,
    rootState: RootState,
    onAnniversaryAcknowledged: (Int) -> Unit,
) {
    val back: () -> Unit = { navController.popBackStack() }
    val go: (String) -> Unit = { route -> navController.navigate(route) }

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Destinations.ONBOARDING) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Destinations.TODAY) {
                        popUpTo(Destinations.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }

        composable(Destinations.TODAY) {
            TodayScreen(
                onOpenMoment = { go(Destinations.MOMENT) },
                onOpenUrge = { go(Destinations.URGE) },
                onOpenBiology = { go(Destinations.BIOLOGY) },
                onOpenPersona = { go(Destinations.ALTER_EGO_PICKER) },
                onOpenReset = { go(Destinations.RESET) },
            )
        }

        composable(Destinations.JOURNEY) {
            JourneyScreen(
                onOpenAnnualReview = { go(Destinations.ANNUAL_REVIEW) },
                onOpenSaved = { go(Destinations.SAVED) },
            )
        }

        composable(Destinations.ME) { MeScreen(onNavigate = go) }

        composable(Destinations.MOMENT) { MomentRoute(onClose = back) }
        composable(Destinations.URGE) { UrgeScreen(onClose = back) }
        composable(Destinations.RESET) { ResetScreen(onClose = back) }

        composable(Destinations.SCIENCE) {
            ScienceScreen(
                onBack = back,
                onOpenLesson = { lessonId -> go(Destinations.lesson(lessonId)) },
                onOpenBiology = { go(Destinations.BIOLOGY) },
            )
        }
        composable(
            route = Destinations.LESSON,
            arguments = listOf(navArgument("lessonId") { type = NavType.StringType }),
        ) { entry ->
            LessonScreen(lessonId = entry.arguments?.getString("lessonId").orEmpty(), onBack = back)
        }
        composable(Destinations.BIOLOGY) { BiologyScreen(onBack = back) }

        composable(Destinations.ALTER_EGO_PICKER) {
            AlterEgoPickerScreen(
                onBack = back,
                onCreateCustom = { go(Destinations.CUSTOM_ALTER_EGO) },
                onUpgrade = { go(Destinations.PREMIUM) },
            )
        }
        composable(Destinations.CUSTOM_ALTER_EGO) { CustomAlterEgoScreen(onDone = back) }

        composable(Destinations.COMMITMENTS) {
            CommitmentsScreen(onBack = back, onNewCommitment = { go(Destinations.NEW_COMMITMENT) }, onNavigate = go)
        }
        composable(Destinations.NEW_COMMITMENT) { NewCommitmentScreen(onDone = back) }
        composable(Destinations.QUOTES) { QuotesScreen(onBack = back, onNavigate = go) }
        composable(Destinations.FUTURE_ME) { FutureMeScreen(onBack = back) }
        composable(Destinations.REMINDERS) { RemindersScreen(onBack = back) }
        composable(Destinations.SETTINGS) { SettingsScreen(onBack = back) }
        composable(Destinations.PRIVACY) { PrivacyScreen(onBack = back) }
        composable(Destinations.PREMIUM) { PremiumScreen(onBack = back) }
        composable(Destinations.SAVED) { SavedMomentsScreen(onBack = back) }

        composable(Destinations.ANNUAL_REVIEW) {
            AnnualReviewScreen(
                onClose = {
                    rootState.anniversaryYear?.let(onAnniversaryAcknowledged)
                    back()
                },
            )
        }
    }
}
