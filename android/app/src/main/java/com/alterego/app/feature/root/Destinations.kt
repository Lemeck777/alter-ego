package com.alterego.app.feature.root

/** Every screen in the app. Three tabs, plus the flows that open on top of them. */
object Destinations {
    const val ONBOARDING = "onboarding"
    const val TODAY = "today"
    const val JOURNEY = "journey"
    const val ME = "me"
    const val MOMENT = "moment"
    const val URGE = "urge"
    const val RESET = "reset"
    const val SCIENCE = "science"
    const val LESSON = "lesson/{lessonId}"
    const val BIOLOGY = "biology"
    const val ALTER_EGO_PICKER = "alter_ego"
    const val CUSTOM_ALTER_EGO = "custom_alter_ego"
    const val COMMITMENTS = "commitments"
    const val NEW_COMMITMENT = "new_commitment"
    const val QUOTES = "quotes"
    const val FUTURE_ME = "future_me"
    const val REMINDERS = "reminders"
    const val SETTINGS = "settings"
    const val PRIVACY = "privacy"
    const val PREMIUM = "premium"
    const val SAVED = "saved"
    const val ANNUAL_REVIEW = "annual_review"

    fun lesson(lessonId: String) = "lesson/$lessonId"
}
