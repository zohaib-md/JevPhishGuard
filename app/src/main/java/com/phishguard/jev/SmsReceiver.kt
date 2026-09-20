package com.phishguard.jev

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * We never call abortBroadcast() — this receiver only observes. The real
 * SMS app still receives and displays the message normally; we just also
 * get a look at it, in parallel.
 *
 * NOTE ON CORRECTNESS vs. SCOPE: BroadcastReceivers are short-lived and
 * can't safely host a long-running coroutine on their own — goAsync() plus
 * a receiver-scoped launch (below) is the minimum needed to make a
 * suspend network call survive past onReceive() returning, and it's
 * adequate for a weekend build. If you want this to survive process death
 * (e.g. the OS kills the app between the broadcast and the network
 * response), move classifyAndStore() into a WorkManager one-off job
 * instead — same function, just enqueued rather than launched directly.
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val sender = messages[0].originatingAddress ?: "Unknown sender"
        val body = messages.joinToString(separator = "") { it.messageBody ?: "" }

        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                PhishingClassifier.classifyAndStore(appContext, sender, body)
            } catch (e: Exception) {
                // Fail open, always. A broken API call should never crash the
                // receiver or block the real SMS from reaching the user.
                android.util.Log.e("SmsReceiver", "Classification failed for $sender", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
