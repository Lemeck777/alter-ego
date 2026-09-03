package com.alterego.app.core.data

import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Injectable clock so every time-based rule (chapters, planning, anniversaries) is testable. */
@Singleton
open class AppClock @Inject constructor() {
    open fun now(): Instant = Instant.now()
}
