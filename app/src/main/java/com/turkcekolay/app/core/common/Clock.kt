package com.turkcekolay.app.core.common

import java.time.LocalDate
import java.time.ZoneId

/** Injected time source so scheduling logic stays deterministic in tests. */
interface Clock {
    fun now(): Long
    fun today(): LocalDate
}

class SystemClock : Clock {
    override fun now(): Long = System.currentTimeMillis()
    override fun today(): LocalDate = LocalDate.now(ZoneId.systemDefault())
}
