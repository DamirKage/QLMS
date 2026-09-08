package kz.qlms.app.data.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafetyAlertTest {

    // Center roughly on Almaty; 1 degree of latitude is ~111km.
    private val alert = SafetyAlert(
        title = "Flood warning",
        latitude = 43.238949,
        longitude = 76.889709,
        radiusKm = 5.0,
    )

    @Test
    fun `a point at the exact same location is relevant`() {
        assertTrue(alert.isRelevantTo(43.238949, 76.889709))
    }

    @Test
    fun `a point well inside the radius is relevant`() {
        // ~1km north.
        assertTrue(alert.isRelevantTo(43.247949, 76.889709))
    }

    @Test
    fun `a point well outside the radius is not relevant`() {
        // ~50km north — 1 degree of latitude is ~111km, radius is 5km.
        assertFalse(alert.isRelevantTo(43.688949, 76.889709))
    }

    @Test
    fun `server-side topic fan-out is coarser than the alert's real circular radius`() {
        // A point diagonally ~6km away can fall inside the ~20km topic cell the
        // server fanned out to, but must still be filtered out client-side since
        // it's outside this alert's actual 5km radius.
        assertFalse(alert.isRelevantTo(43.238949 + 0.045, 76.889709 + 0.045))
    }

    @Test
    fun `severity falls back to INFO for an unrecognized stored value`() {
        val corrupted = alert.copy(severityName = "NOT_A_REAL_SEVERITY")
        assertTrue(corrupted.severity == AlertSeverity.INFO)
    }

    @Test
    fun `severity round-trips through its stored name`() {
        val critical = alert.copy(severityName = AlertSeverity.CRITICAL.name)
        assertTrue(critical.severity == AlertSeverity.CRITICAL)
    }
}
