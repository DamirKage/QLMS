package kz.qlms.app.data.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IncidentPrivateDetailsTest {

    @Test
    fun `default details are empty`() {
        assertTrue(IncidentPrivateDetails().isEmpty)
    }

    @Test
    fun `an impact force alone (no medical info, no contacts) makes it non-empty`() {
        // A crash-detected incident may have nothing else to report, but the
        // impact telemetry is itself reason enough to sync the private doc.
        val details = IncidentPrivateDetails(impactForceG = 4.2)
        assertFalse(details.isEmpty)
    }

    @Test
    fun `a dispatcher note alone still makes it non-empty`() {
        assertFalse(IncidentPrivateDetails(dispatcherNote = "Reporter is deaf, use chat only").isEmpty)
    }
}
