package kz.qlms.app.util

import kotlin.math.abs

/**
 * Pure state machine behind [CrashDetector], factored out so the impact +
 * stillness heuristic can be unit-tested on the JVM without a real
 * SensorManager/SensorEvent. Feed it every accelerometer reading (already
 * converted to g) with its timestamp; it reports a crash exactly once per
 * impact+stillness sequence, together with the peak g-force at the moment of
 * impact — eCall-style telemetry a dispatcher can act on immediately ("this
 * was a 4.8g hit") instead of an unqualified "possible crash detected".
 */
class CrashHeuristic(
    private val impactThresholdG: Double = IMPACT_THRESHOLD_G,
    private val stillnessWindowMs: Long = STILLNESS_WINDOW_MS,
    private val stillnessThresholdG: Float = STILLNESS_THRESHOLD_G,
) {
    private enum class State { IDLE, WATCHING_STILLNESS }

    private var state = State.IDLE
    private var watchStartMs = 0L
    private var maxDeviationSinceImpact = 0f
    private var peakImpactG = 0f

    data class CrashEvent(val peakImpactG: Float)

    /** Call on every reading; returns a [CrashEvent] the instant a full impact+stillness sequence completes. */
    fun onReading(gForce: Float, nowMs: Long): CrashEvent? = when (state) {
        State.IDLE -> {
            if (gForce >= impactThresholdG) {
                state = State.WATCHING_STILLNESS
                watchStartMs = nowMs
                maxDeviationSinceImpact = 0f
                peakImpactG = gForce
            }
            null
        }
        State.WATCHING_STILLNESS -> {
            if (gForce > peakImpactG) peakImpactG = gForce
            val deviationFromRest = abs(gForce - 1f)
            if (deviationFromRest > maxDeviationSinceImpact) maxDeviationSinceImpact = deviationFromRest

            if (nowMs - watchStartMs >= stillnessWindowMs) {
                val wasStillTheWholeTime = maxDeviationSinceImpact < stillnessThresholdG
                val result = if (wasStillTheWholeTime) CrashEvent(peakImpactG) else null
                state = State.IDLE
                result
            } else {
                null
            }
        }
    }

    fun reset() {
        state = State.IDLE
    }

    companion object {
        const val IMPACT_THRESHOLD_G = 3.5
        const val STILLNESS_WINDOW_MS = 8_000L
        const val STILLNESS_THRESHOLD_G = 0.35f
    }
}
