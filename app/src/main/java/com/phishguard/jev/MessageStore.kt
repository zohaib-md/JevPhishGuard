package com.phishguard.jev

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class ScannedMessage(
    val sender: String,
    val snippet: String,
    val verdict: String,       // "legitimate" | "promotional" | "likely_phishing" | "uncertain"
    val confidence: Double,    // 0.0-1.0, confidence in `verdict`
    val riskLevel: String,     // the Score answer's chosen label
    val signals: List<String>, // which Noul questions fired (>= 0.5)
    val inputTokens: Int,
    val latencyMs: Long,
    val timestamp: Long
)

/**
 * In-memory only — resets on process death. That's a fine trade for a
 * one-day build. If you want the log to survive, swap this for a Room table;
 * the shape above maps to a row 1:1.
 */
object MessageStore {
    private val _messages = MutableStateFlow<List<ScannedMessage>>(emptyList())
    val messages: StateFlow<List<ScannedMessage>> = _messages

    // Jev pricing: $0.042 per 1,000,000 input tokens. Output tokens are free.
    private const val COST_PER_INPUT_TOKEN = 0.042 / 1_000_000.0

    fun add(message: ScannedMessage) {
        _messages.update { current -> listOf(message) + current }
    }

    data class Stats(val count: Int, val totalCostUsd: Double, val medianLatencyMs: Long)

    fun stats(): Stats {
        val list = _messages.value
        if (list.isEmpty()) return Stats(0, 0.0, 0L)
        val totalCost = list.sumOf { it.inputTokens * COST_PER_INPUT_TOKEN }
        val sortedLatencies = list.map { it.latencyMs }.sorted()
        val median = sortedLatencies[sortedLatencies.size / 2]
        return Stats(list.size, totalCost, median)
    }
}
