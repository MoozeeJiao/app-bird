package com.attentionpet.service

class SessionTracker(
    private val targetPackage: String,
    private val graceMillis: Long = 30_000L
) {
    enum class Status { IDLE, ACTIVE, GRACE, CLOSED }

    data class State(
        val status: Status,
        val activeStartMillis: Long?,
        val foregroundDurationMillis: Long,
        val closedAtMillis: Long?
    )

    private var activeStartMillis: Long? = null
    private var lastForegroundMillis: Long? = null
    private var foregroundDurationMillis: Long = 0L
    private var lastClosedState: State? = null

    fun onForegroundSample(packageName: String?, nowMillis: Long): State {
        return if (packageName == targetPackage) {
            onTargetSample(nowMillis)
        } else {
            onNonTargetSample(nowMillis)
        }
    }

    private fun onTargetSample(nowMillis: Long): State {
        lastClosedState = null

        val activeStart = activeStartMillis
        if (activeStart == null) {
            activeStartMillis = nowMillis
            lastForegroundMillis = nowMillis
            foregroundDurationMillis = 0L
            return state(Status.ACTIVE)
        }

        val last = lastForegroundMillis ?: activeStart
        foregroundDurationMillis += (nowMillis - last).coerceAtLeast(0L)
        if (nowMillis >= last) {
            lastForegroundMillis = nowMillis
        }

        return state(Status.ACTIVE)
    }

    private fun onNonTargetSample(nowMillis: Long): State {
        val activeStart = activeStartMillis
        val last = lastForegroundMillis
        if (activeStart == null || last == null) {
            return lastClosedState ?: State(Status.IDLE, null, 0L, null)
        }

        val elapsedSinceForeground = (nowMillis - last).coerceAtLeast(0L)
        if (elapsedSinceForeground <= graceMillis) {
            return state(Status.GRACE)
        }

        return closeSession(closedAtMillis = last)
    }

    private fun closeSession(closedAtMillis: Long): State {
        val closedState = State(
            status = Status.CLOSED,
            activeStartMillis = null,
            foregroundDurationMillis = foregroundDurationMillis,
            closedAtMillis = closedAtMillis
        )
        activeStartMillis = null
        lastForegroundMillis = null
        foregroundDurationMillis = 0L
        lastClosedState = closedState
        return closedState
    }

    private fun state(status: Status): State {
        return State(
            status = status,
            activeStartMillis = activeStartMillis,
            foregroundDurationMillis = foregroundDurationMillis,
            closedAtMillis = null
        )
    }
}
