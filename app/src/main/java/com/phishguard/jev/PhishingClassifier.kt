package com.phishguard.jev

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * One batched Jev request, 5 questions, evaluated in parallel against the
 * same state: 1 Choice for the overall verdict, 3 Nouls for the individual
 * signals (shown in the UI so a flag is explainable, not just a black-box
 * verdict), and 1 Score for an at-a-glance severity level.
 */
object PhishingClassifier {

    // High-confidence phishing triggers a heads-up notification.
    // Below that, it's still logged, just quietly.
    private const val ALERT_CONFIDENCE_THRESHOLD = 0.7
    private const val SIGNAL_PROBABILITY_THRESHOLD = 0.5

    private val questionSet: JSONObject by lazy {
        JSONObject().apply {
            put("verdict", JSONObject().apply {
                put("type", "choice")
                put("instructions", "How should this message be classified?")
                put("criteria", JSONObject().apply {
                    put(
                        "legitimate",
                        "A normal message from a real service, bank, delivery courier, " +
                            "or known contact, with no signs of fraud."
                    )
                    put("promotional", "Marketing or promotional content, not attempting fraud.")
                    put(
                        "likely_phishing",
                        "Attempts to trick the recipient into clicking a link, sharing " +
                            "credentials or codes, verifying account details, or making a " +
                            "payment by impersonating a trusted sender."
                    )
                    put("uncertain", "Not enough information to classify confidently.")
                })
            })
            put("urgency_pressure", JSONObject().apply {
                put("type", "noul")
                put(
                    "instructions",
                    "Does this message pressure the recipient to act immediately, such as " +
                        "with an account suspension threat, a countdown, or an urgent " +
                        "verification deadline?"
                )
            })
            put("impersonation", JSONObject().apply {
                put("type", "noul")
                put(
                    "instructions",
                    "Does this message claim to be from a bank, government agency, courier " +
                        "or delivery service, or other trusted organization?"
                )
            })
            put("requests_action", JSONObject().apply {
                put("type", "noul")
                put(
                    "instructions",
                    "Does this message ask the recipient to click a link, enter personal " +
                        "or account details, or make a payment?"
                )
            })
            put("risk", JSONObject().apply {
                put("type", "score")
                put("instructions", "How risky is this message overall, from safe to dangerous?")
                put(
                    "criteria",
                    JSONArray(
                        listOf(
                            "Safe - ordinary message",
                            "Low risk - minor concern",
                            "Medium risk - some suspicious signals",
                            "High risk - strong signs of fraud",
                            "Severe - almost certainly a scam"
                        )
                    )
                )
            })
        }
    }

    suspend fun classifyAndStore(context: Context, sender: String, rawBody: String) {
        val apiKey = SettingsStore.getApiKey(context)
        if (apiKey == null) {
            android.util.Log.w("PhishingClassifier", "No API key set — skipping scan for $sender")
            return
        }

        val safeBody = PrivacyFilter.maskOtp(rawBody)
        val state = "SMS from: $sender\nMessage: $safeBody"

        val result = JevClient(apiKey).evaluate(state, questionSet)

        fun probabilityOf(key: String): Double = result.answers[key]?.probability ?: 0.0

        val signals = buildList {
            if (probabilityOf("urgency_pressure") >= SIGNAL_PROBABILITY_THRESHOLD) add("urgency")
            if (probabilityOf("impersonation") >= SIGNAL_PROBABILITY_THRESHOLD) add("impersonation")
            if (probabilityOf("requests_action") >= SIGNAL_PROBABILITY_THRESHOLD) add("asks for action")
        }

        val verdictAnswer = result.answers["verdict"]
        val scanned = ScannedMessage(
            sender = sender,
            snippet = safeBody.take(160),
            verdict = verdictAnswer?.choice ?: "uncertain",
            confidence = verdictAnswer?.confidence ?: 0.0,
            riskLevel = result.answers["risk"]?.choice ?: "unknown",
            signals = signals,
            inputTokens = result.inputTokens,
            latencyMs = result.latencyMs,
            timestamp = System.currentTimeMillis()
        )

        MessageStore.add(scanned)

        val isHighConfidencePhishing =
            scanned.verdict == "likely_phishing" && scanned.confidence >= ALERT_CONFIDENCE_THRESHOLD

        if (isHighConfidencePhishing) {
            NotificationHelper.showPhishingAlert(context, sender, scanned.signals)
        }
    }
}
