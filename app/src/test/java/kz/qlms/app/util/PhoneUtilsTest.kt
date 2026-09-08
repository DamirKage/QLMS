package kz.qlms.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneUtilsTest {

    // ---------- amlGatewayNumberOrNull ----------

    @Test
    fun `amlGatewayNumberOrNull is null when the feature is disabled, even with a number configured`() {
        assertNull(PhoneUtils.amlGatewayNumberOrNull(enabled = false, gatewayNumber = "+77001234567"))
    }

    @Test
    fun `amlGatewayNumberOrNull is null when enabled but no number was ever configured`() {
        assertNull(PhoneUtils.amlGatewayNumberOrNull(enabled = true, gatewayNumber = ""))
    }

    @Test
    fun `amlGatewayNumberOrNull is null for a whitespace-only number`() {
        assertNull(PhoneUtils.amlGatewayNumberOrNull(enabled = true, gatewayNumber = "   "))
    }

    @Test
    fun `amlGatewayNumberOrNull returns the trimmed number when enabled and set`() {
        assertEquals("+77001234567", PhoneUtils.amlGatewayNumberOrNull(enabled = true, gatewayNumber = "  +77001234567  "))
    }

    // ---------- buildAmlLocationSms ----------

    @Test
    fun `buildAmlLocationSms interpolates the maps link for the given coordinates`() {
        val message = PhoneUtils.buildAmlLocationSms(
            template = "Location: %1\$s",
            latitude = 43.238949,
            longitude = 76.889709,
        )
        assertEquals("Location: ${LocationUtils.googleMapsLink(43.238949, 76.889709)}", message)
        assertTrue(message.contains("43.238949"))
        assertTrue(message.contains("76.889709"))
    }

    // ---------- shouldPlaceEmergencyCall ----------

    @Test
    fun `a normal SOS places the emergency call`() {
        assertTrue(PhoneUtils.shouldPlaceEmergencyCall(isTextOnly = false))
    }

    @Test
    fun `a text-only SOS never places the emergency call`() {
        assertFalse(PhoneUtils.shouldPlaceEmergencyCall(isTextOnly = true))
    }
}
