package kz.qlms.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.SmsManager
import kz.qlms.app.data.model.EmergencyContact

object PhoneUtils {

    /** Directly places the call — requires CALL_PHONE, which is only ever requested right before SOS is armed. */
    fun callEmergencyNumber(context: Context, number: String) {
        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /** Falls back to the dialer (no permission needed) when CALL_PHONE was denied. */
    fun dialEmergencyNumber(context: Context, number: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun notifyContactsBySms(contacts: List<EmergencyContact>, message: String) {
        contacts.filter { it.notifyOnSos && it.phoneNumber.isNotBlank() }
            .forEach { sendSms(it.phoneNumber, message) }
    }

    fun sendSms(number: String, message: String) {
        runCatching {
            val manager = SmsManager.getDefault()
            val parts = manager.divideMessage(message)
            manager.sendMultipartTextMessage(number, null, parts, null, null)
        }
    }

    /**
     * Advanced Mobile Location (ETSI TS 103 625) has every Android/iOS phone
     * silently text a precise GPS fix to the emergency call center the moment
     * you dial an emergency number — no app, no action from the caller. A
     * citizen app can't reach into carrier/OS-level call handling to add that,
     * so this reproduces the same idea at the app layer: on SOS, in addition to
     * the 112 call, text a precise fix straight to a configured gateway number.
     * Returns null (nothing to send) unless the feature is on AND a real
     * number is configured — guards against silently no-op'ing to a blank
     * number, or firing when the user never opted in.
     */
    fun amlGatewayNumberOrNull(enabled: Boolean, gatewayNumber: String): String? =
        gatewayNumber.trim().takeIf { enabled && it.isNotBlank() }

    /** Fills [template]'s single `%1$s` placeholder with the maps link for (latitude, longitude). */
    fun buildAmlLocationSms(template: String, latitude: Double, longitude: Double): String =
        String.format(template, LocationUtils.googleMapsLink(latitude, longitude))

    /**
     * Whether an SOS should place a real phone call — the text-to-911
     * equivalent: false for a report the user (or the trigger path itself,
     * e.g. shake/crash — see [kz.qlms.app.data.model.Incident.isTextOnly])
     * marked text-only, because calling back a phone that can't safely
     * answer would put the person in more danger, not less.
     */
    fun shouldPlaceEmergencyCall(isTextOnly: Boolean): Boolean = !isTextOnly
}
