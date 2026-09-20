package com.phishguard.jev

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * One answer to one typed question.
 *
 * NOTE ON FIELD NAMES: Choice answers carry `choice`, `confidence`, and a
 * `probabilities` map. Noul answers carry their single probability under
 * `noul` (confirmed against a live API response on 2026-09-21 — the earlier
 * guesses of `probability`/`value`/`probabilities[choice]` were all wrong).
 */
data class JevAnswer(
    val choice: String? = null,
    val confidence: Double = 0.0,
    val probability: Double? = null
)

data class JevResult(
    val answers: Map<String, JevAnswer>,
    val model: String,
    val inputTokens: Int,
    val latencyMs: Long
)

class JevApiException(val code: Int, val body: String) :
    Exception("Jev API error $code: $body")

class JevClient(
    private val apiKey: String,
    private val baseUrl: String = "https://api.typesafe.ai/v1/systemone"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * Sends [state] plus a batch of typed [questions] in one request. All
     * questions are evaluated in parallel against the same state — this is
     * why the classifier asks 5 questions in a single call instead of 5
     * separate ones.
     *
     * [questions] must already be in the shape TypeSafe expects, e.g.:
     * {
     *   "verdict": {"type": "choice", "instructions": "...", "criteria": {...}},
     *   "urgent":  {"type": "noul", "instructions": "..."}
     * }
     */
    suspend fun evaluate(
        state: String,
        questions: JSONObject,
        model: String = "jev-latest"
    ): JevResult = withContext(Dispatchers.IO) {
        val requestBody = JSONObject().apply {
            put("model", model)
            put("state", state)
            put("questions", questions)
        }

        val request = Request.Builder()
            .url(baseUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val start = System.currentTimeMillis()
        client.newCall(request).execute().use { response ->
            val elapsed = System.currentTimeMillis() - start
            val bodyStr = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                throw JevApiException(response.code, bodyStr)
            }

            android.util.Log.d("JevClient", "raw response: $bodyStr")

            val json = JSONObject(bodyStr)
            val answersJson = json.optJSONObject("answers") ?: JSONObject()
            val answers = mutableMapOf<String, JevAnswer>()

            answersJson.keys().forEach { key ->
                answers[key] = parseAnswer(answersJson.getJSONObject(key))
            }

            JevResult(
                answers = answers,
                model = json.optString("model", model),
                inputTokens = json.optJSONObject("usage")?.optInt("input_tokens", 0) ?: 0,
                latencyMs = elapsed
            )
        }
    }

    private fun parseAnswer(a: JSONObject): JevAnswer {
        val choice = if (a.has("choice") && !a.isNull("choice")) a.optString("choice") else null
        val confidence = a.optDouble("confidence", 0.0)

        val probability: Double? = when {
            a.has("noul") -> a.optDouble("noul").takeIf { !it.isNaN() }
            a.has("probabilities") && choice != null ->
                a.optJSONObject("probabilities")?.optDouble(choice)?.takeIf { !it.isNaN() }
            else -> null
        }

        return JevAnswer(choice = choice, confidence = confidence, probability = probability)
    }
}
