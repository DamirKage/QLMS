package kz.qlms.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CrashHeuristicTest {

    private val heuristic = CrashHeuristic(
        impactThresholdG = 3.5,
        stillnessWindowMs = 8_000L,
        stillnessThresholdG = 0.35f,
    )

    @Test
    fun `normal handling never reports a crash`() {
        var t = 0L
        // Gentle everyday jostling: never spikes past the impact threshold.
        repeat(50) {
            val reading = 1.0f + (if (it % 2 == 0) 0.2f else -0.2f)
            assertNull(heuristic.onReading(reading, t))
            t += 100
        }
    }

    @Test
    fun `impact followed by full stillness reports a crash with the peak g-force`() {
        var t = 0L
        assertNull(heuristic.onReading(1.0f, t)) // resting

        t += 100
        assertNull(heuristic.onReading(6.2f, t)) // the impact itself

        // Stay essentially at rest (1g, only tiny noise) for the whole watch window.
        var event: CrashHeuristic.CrashEvent? = null
        while (t < 100 + 8_000L) {
            t += 500
            val result = heuristic.onReading(1.02f, t)
            if (result != null) event = result
        }

        assertEquals(6.2f, event?.peakImpactG)
    }

    @Test
    fun `impact followed by continued movement is not a crash`() {
        var t = 0L
        assertNull(heuristic.onReading(1.0f, t))

        t += 100
        assertNull(heuristic.onReading(6.0f, t)) // impact

        // Person is still moving around afterward (e.g. picked the phone back up) —
        // deviation from rest stays above the stillness threshold throughout.
        var event: CrashHeuristic.CrashEvent? = null
        while (t < 100 + 8_000L) {
            t += 500
            val result = heuristic.onReading(1.8f, t)
            if (result != null) event = result
        }

        assertNull(event)
    }

    @Test
    fun `resets to idle after each completed sequence so a second crash can still be detected`() {
        var t = 0L
        heuristic.onReading(1.0f, t)
        t += 100
        heuristic.onReading(5.0f, t) // first impact
        t += 8_100
        val first = heuristic.onReading(1.0f, t) // stillness window elapses -> reports, resets to IDLE

        t += 100
        heuristic.onReading(4.0f, t) // second, independent impact
        t += 8_100
        val second = heuristic.onReading(1.0f, t)

        assertEquals(5.0f, first?.peakImpactG)
        assertEquals(4.0f, second?.peakImpactG)
    }

    @Test
    fun `a reading right at the impact threshold counts as an impact`() {
        val t0 = 0L
        assertNull(heuristic.onReading(3.4f, t0)) // just under threshold, ignored
        val result = heuristic.onReading(3.5f, t0 + 100) // exactly at threshold
        // Confirmed by driving it to a clean stillness completion.
        var event: CrashHeuristic.CrashEvent? = null
        var t = t0 + 100
        while (t < t0 + 100 + 8_000L) {
            t += 500
            heuristic.onReading(1.0f, t)?.let { event = it }
        }
        assertNull(result) // onReading only returns non-null once the stillness window completes
        assertEquals(3.5f, event?.peakImpactG)
    }
}
