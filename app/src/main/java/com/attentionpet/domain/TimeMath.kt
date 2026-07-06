package com.attentionpet.domain

import kotlin.math.max
import kotlin.math.min

fun overlapMillis(
    sessionStart: Long,
    sessionEndOrNow: Long,
    windowStart: Long,
    windowEnd: Long
): Long {
    return max(0L, min(sessionEndOrNow, windowEnd) - max(sessionStart, windowStart))
}
