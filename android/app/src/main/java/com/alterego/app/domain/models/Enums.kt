package com.alterego.app.domain.models

/** Age bands protect privacy: we never need an exact date of birth for education. */
enum class AgeBand(val id: String, val label: String, val minAge: Int, val maxAge: Int?) {
    A18_24("18-24", "18-24", 18, 24),
    A25_29("25-29", "25-29", 25, 29),
    A30_34("30-34", "30-34", 30, 34),
    A35_39("35-39", "35-39", 35, 39),
    A40_44("40-44", "40-44", 40, 44),
    A45_49("45-49", "45-49", 45, 49),
    A50_59("50-59", "50-59", 50, 59),
    A60_PLUS("60+", "60+", 60, null);

    companion object {
        fun fromId(id: String?): AgeBand? = entries.firstOrNull { it.id == id }
        fun fromAge(age: Int): AgeBand = entries.firstOrNull { age >= it.minAge && (it.maxAge == null || age <= it.maxAge) } ?: A60_PLUS
    }
}

enum class Goal(val id: String, val label: String, val description: String) {
    DISCIPLINE("discipline", "Discipline", "Do what you said you would do."),
    RETENTION("retention", "Semen retention", "Track a commitment about ejaculation, precisely and privately."),
    PORN_AVOIDANCE("porn_avoidance", "Porn avoidance", "Stay away from pornography."),
    FAITH("faith", "Faith", "Prayer, Scripture and coming back to God."),
    FOCUS("focus", "Focus", "Deep work, study, fewer distractions."),
    CONFIDENCE("confidence", "Self-confidence", "Become someone you trust."),
    FITNESS("fitness", "Fitness", "Move your body regularly."),
    CALM("calm", "Calm", "Less overthinking, more breathing."),
    SCREEN_TIME("screen_time", "Screen time", "Less scrolling, more living."),
    CUSTOM("custom", "Create my own", "Your own rule, your own words."),
    GENERAL("general", "General", "Everyday wellbeing.");

    companion object {
        fun fromId(id: String): Goal = entries.firstOrNull { it.id == id } ?: GENERAL
        val selectable: List<Goal> get() = entries.filter { it != GENERAL }
    }
}

/** The precise rule behind a retention commitment. These are not the same thing. */
enum class CommitmentRule(val id: String, val label: String, val description: String) {
    NO_PORN("no_porn", "No pornography", "Sex and masturbation are your own business; porn is out."),
    NO_MASTURBATION("no_masturbation", "No masturbation", "Sex with a partner does not end a chapter."),
    NO_EJACULATION("no_ejaculation", "No ejaculation", "Complete ejaculatory abstinence."),
    TRACK_FREQUENCY("track_frequency", "Just track frequency", "Log events without a rule. Nothing ends a chapter."),
    CUSTOM("custom", "My own rule", "Describe it in your own words.");

    companion object { fun fromId(id: String?): CommitmentRule = entries.firstOrNull { it.id == id } ?: NO_EJACULATION }
}

enum class ReminderIntensity(val id: String, val label: String, val subtitle: String, val momentsPerDay: IntRange) {
    GENTLE("gentle", "Gentle", "1-2 a day", 1..2),
    BALANCED("balanced", "Balanced", "3-4 a day", 3..4),
    STRONG("strong", "Strong accountability", "5-6 a day", 5..6),
    CUSTOM("custom", "Custom", "You choose", 0..12);

    companion object { fun fromId(id: String?): ReminderIntensity = entries.firstOrNull { it.id == id } ?: BALANCED }
}

/** How much a lock-screen notification reveals. Default is PRIVATE. */
enum class NotificationPrivacy(val id: String, val label: String, val example: String) {
    PRIVATE("private", "Private", "\"Sage wants a word.\""),
    NORMAL("normal", "Normal", "\"Your accountability check-in is ready.\""),
    EXPLICIT("explicit", "Explicit", "\"Day 17 retention check-in.\"");

    companion object { fun fromId(id: String?): NotificationPrivacy = entries.firstOrNull { it.id == id } ?: PRIVATE }
}

enum class AppLockMode(val id: String) { NONE("none"), PIN("pin"), BIOMETRIC("biometric");
    companion object { fun fromId(id: String?): AppLockMode = entries.firstOrNull { it.id == id } ?: NONE }
}

enum class MomentCategory(val id: String) {
    ACCOUNTABILITY("accountability"), HUMOR("humor"), PERSPECTIVE("perspective"), HEALTH("health"), FAITH("faith"),
    WELLBEING("wellbeing"), RANDOM("random"), URGE_MANAGEMENT("urge_management"), WELCOME_BACK("welcome_back"),
    LATE_NIGHT("late_night"), RESET_RECOVERY("reset_recovery"), MORNING("morning"), EVENING("evening"), ANNIVERSARY("anniversary");

    companion object { fun fromId(id: String): MomentCategory = entries.firstOrNull { it.id == id } ?: RANDOM }
}

enum class MomentTrigger(val id: String) {
    RANDOM("random"), SCHEDULED("scheduled"), ACCOUNTABILITY("accountability"), SMART("smart"), URGE("urge"),
    RESET("reset"), WELCOME_BACK("welcome_back"), ANNIVERSARY("anniversary"), HIGH_RISK_WINDOW("high_risk_window");

    companion object { fun fromId(id: String): MomentTrigger = entries.firstOrNull { it.id == id } ?: RANDOM }
}

enum class TimeContext(val id: String) {
    ANY("any"), MORNING("morning"), MIDDAY("midday"), AFTERNOON("afternoon"), EVENING("evening"), LATE_NIGHT("late_night");

    companion object {
        fun fromId(id: String): TimeContext = entries.firstOrNull { it.id == id } ?: ANY
        fun forHour(hour: Int): TimeContext = when (hour) {
            in 5..10 -> MORNING
            in 11..13 -> MIDDAY
            in 14..17 -> AFTERNOON
            in 18..21 -> EVENING
            else -> LATE_NIGHT
        }
    }
}

enum class EvidenceLevel(val id: String, val label: String, val emoji: String) {
    STRONG("strong", "Strong evidence", "🟢"),
    MODERATE("moderate", "Moderate evidence", "🟡"),
    LIMITED("limited", "Limited / mixed evidence", "🟠"),
    TRADITION("tradition", "Tradition / philosophy", "⚪");

    companion object { fun fromId(id: String): EvidenceLevel = entries.firstOrNull { it.id == id } ?: LIMITED }
}

enum class CharacterState(val id: String) {
    IDLE("idle"), ENTER("enter"), LOOK("look"), SMILE("smile"), LAUGH("laugh"), THINK("think"), ENCOURAGE("encourage"),
    SERIOUS("serious"), BREATHE("breathe"), PRAY("pray"), CELEBRATE("celebrate"), WAVE("wave"), POINT("point"), NOD("nod"), EXIT("exit");

    companion object { fun fromId(id: String): CharacterState = entries.firstOrNull { it.id == id } ?: LOOK }
}

enum class HapticPattern(val id: String) { NONE("none"), TAP("tap"), DOUBLE_TAP("double_tap"), HEARTBEAT("heartbeat"), SOFT("soft");
    companion object { fun fromId(id: String): HapticPattern = entries.firstOrNull { it.id == id } ?: SOFT }
}

enum class ResetContext(val id: String, val label: String) {
    BORED("bored", "Bored"), STRESSED("stressed", "Stressed"), LONELY("lonely", "Lonely"),
    STRONG_URGE("strong_urge", "Strong urge"), PORN("porn", "Porn"), SOCIAL_MEDIA("social_media", "Social media trigger"),
    COULDNT_SLEEP("couldnt_sleep", "Couldn't sleep"), RELATIONSHIP("relationship", "Relationship / sex"), OTHER("other", "Other");

    companion object { fun fromId(id: String?): ResetContext? = entries.firstOrNull { it.id == id } }
}

enum class UrgeLevel(val id: String, val label: String) { LOW("low", "Low"), MEDIUM("medium", "Medium"), HIGH("high", "High");
    companion object { fun fromId(id: String?): UrgeLevel = entries.firstOrNull { it.id == id } ?: MEDIUM }
}
