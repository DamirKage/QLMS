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
        val manager = SmsManager.getDefault()
        contacts.filter { it.notifyOnSos && it.phoneNumber.isNotBlank() }.forEach { contact ->
            runCatching {
                val parts = manager.divideMessage(message)
                manager.sendMultipartTextMessage(contact.phoneNumber, null, parts, null, null)
            }
        }
    }
}
