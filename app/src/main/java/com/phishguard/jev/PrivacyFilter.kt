package com.phishguard.jev

/**
 * Strips OTP-shaped codes out of a message BEFORE it's sent to Jev.
 *
 * The surrounding context (sender claim, urgency language, links) is what the
 * classifier actually needs to judge phishing intent — the numeric code
 * itself adds nothing to that judgment, so there's no reason to let it leave
 * the device. This is a narrow, deliberately conservative filter: it only
 * masks digit groups when the message also contains an OTP-ish keyword, so
 * it won't accidentally eat phone numbers or amounts in unrelated texts.
 */
object PrivacyFilter {

    private val otpKeywords = listOf(
        "otp", "one time password", "one-time password", "one time passcode",
        "verification code", "security code", "auth code", "authentication code",
        "your pin", "pin is", "code is"
    )

    private val digitGroup = Regex("""\b\d{4,8}\b""")

    fun maskOtp(text: String): String {
        val lower = text.lowercase()
        val looksLikeOtp = otpKeywords.any { lower.contains(it) }
        return if (looksLikeOtp) digitGroup.replace(text, "[OTP]") else text
    }
}
