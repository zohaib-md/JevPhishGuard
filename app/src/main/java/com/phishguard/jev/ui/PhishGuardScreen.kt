package com.phishguard.jev.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phishguard.jev.R

/* ------------------------------------------------------------------ */
/*  Tokens                                                            */
/* ------------------------------------------------------------------ */

object Jev {
    val Page = Color(0xFFEEF0F3)
    val Card = Color(0xFFFFFFFF)
    val Ink = Color(0xFF1A1C20)
    val Body = Color(0xFF2E3238)
    val Muted = Color(0xFF6C7280)
    val Faint = Color(0xFF9AA1AB)
    val Line = Color(0xFFE3E6EB)

    val BandCalm = Color(0xFF102A22)
    val BandAlert = Color(0xFFB02E24)
    val OnBand = Color(0xFFFFFFFF)
    val OnBandSoft = Color(0xB3FFFFFF)
    val OnBandFaint = Color(0x8CFFFFFF)
    val BandHairline = Color(0x1FFFFFFF)

    val PulseCalm = Color(0xFF5DD6A8)
    val PulseAlert = Color(0xFFFFC9C3)

    val ClearFg = Color(0xFF0E6F55)
    val ClearBg = Color(0xFFE2F1EB)
    val CautionFg = Color(0xFF9A5B08)
    val CautionBg = Color(0xFFFBEFDC)
    val PhishFg = Color(0xFFB02E24)
    val PhishBg = Color(0xFFFAE7E4)
}

/**
 * The screen is set in one family across the whole hierarchy — weight and
 * tracking carry the hierarchy, not a second typeface.
 */
private val googleFontsProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val JevFont: FontFamily = FontFamily(
    Font(GoogleFont("Archivo"), googleFontsProvider, FontWeight.Normal),
    Font(GoogleFont("Archivo"), googleFontsProvider, FontWeight.Medium),
    Font(GoogleFont("Archivo"), googleFontsProvider, FontWeight.SemiBold),
    Font(GoogleFont("Archivo"), googleFontsProvider, FontWeight.Bold)
)

private val Tabular = "tnum"

/* ------------------------------------------------------------------ */
/*  Model                                                             */
/* ------------------------------------------------------------------ */

enum class Verdict(val label: String, val fg: Color, val bg: Color) {
    CLEAR("Clear", Jev.ClearFg, Jev.ClearBg),
    CAUTION("Worth a look", Jev.CautionFg, Jev.CautionBg),
    PHISHING("Phishing", Jev.PhishFg, Jev.PhishBg)
}

data class ScannedMessage(
    val id: String,
    val sender: String,
    val text: String,
    val receivedAt: String,
    val verdict: Verdict,
    val latencyMs: Int
)

data class GuardState(
    val scanned: Int = 0,
    val medianMs: Int? = null,
    val spentUsd: Double = 0.0,
    val messages: List<ScannedMessage> = emptyList()
) {
    val flagged: Int get() = messages.count { it.verdict == Verdict.PHISHING }
    val topThreatSender: String?
        get() = messages.firstOrNull { it.verdict == Verdict.PHISHING }?.sender
}

private val SampleCatch = ScannedMessage(
    id = "sample",
    sender = "VM-HDFCBK",
    text = "Dear customer, your account is suspended. Complete KYC now at " +
        "hdfc-verify.in/kyc to restore access.",
    receivedAt = "Sample",
    verdict = Verdict.PHISHING,
    latencyMs = 190
)

/* ------------------------------------------------------------------ */
/*  Screen                                                            */
/* ------------------------------------------------------------------ */

@Composable
fun PhishGuardScreen(
    state: GuardState,
    onOpenSettings: () -> Unit,
    onSendTest: () -> Unit,
    onSimulate: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Jev.Page)
    ) {
        item { StatusBand(state, onOpenSettings) }

        if (state.messages.isEmpty()) {
            item { EmptyState(onSendTest, onSimulate) }
        } else {
            item {
                SectionHeader(
                    title = "Today",
                    trailing = "${state.messages.size} messages"
                )
            }
            items(state.messages, key = { it.id }) { message ->
                MessageCard(
                    message = message,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)
                )
            }
            item { Spacer(Modifier.height(28.dp)) }
        }
    }
}

/* ------------------------------------------------------------------ */
/*  Status band — the one loud element on the screen                  */
/* ------------------------------------------------------------------ */

@Composable
private fun StatusBand(state: GuardState, onOpenSettings: () -> Unit) {
    val alert = state.flagged > 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (alert) Jev.BandAlert else Jev.BandCalm)
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 22.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Jev Phish Guard",
                style = TextStyle(
                    fontFamily = JevFont,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Jev.OnBandSoft
                )
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Jev.BandHairline, RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpenSettings),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = Jev.OnBand,
                    modifier = Modifier.size(19.dp)
                )
            }
        }

        Spacer(Modifier.height(26.dp))
        LiveIndicator(alert)
        Spacer(Modifier.height(10.dp))

        Text(
            text = when {
                alert && state.topThreatSender != null ->
                    "One message is pretending to be ${state.topThreatSender}."
                alert -> "One message is not what it says it is."
                else -> "Nothing has slipped through yet."
            },
            style = TextStyle(
                fontFamily = JevFont,
                fontSize = 29.sp,
                lineHeight = 33.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.6).sp,
                color = Jev.OnBand
            )
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = if (alert)
                "Don't tap anything in it. Everything else today looked fine."
            else
                "Every text that arrives gets read and judged before you open it.",
            style = TextStyle(
                fontFamily = JevFont,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = Jev.OnBandSoft
            ),
            modifier = Modifier.fillMaxWidth(0.92f)
        )

        Spacer(Modifier.height(24.dp))
        StatRow(state)
    }
}

@Composable
private fun LiveIndicator(alert: Boolean) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val alpha by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "pulseAlpha"
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .alpha(alpha)
                .background(if (alert) Jev.PulseAlert else Jev.PulseCalm, CircleShape)
        )
        Spacer(Modifier.width(7.dp))
        Text(
            "Watching your inbox",
            style = TextStyle(
                fontFamily = JevFont,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Jev.OnBandSoft
            )
        )
    }
}

@Composable
private fun StatRow(state: GuardState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 0.dp)
    ) {
        StatCell("Checked", state.scanned.toString(), Modifier.weight(1f))
        VerticalHairline()
        StatCell(
            "Typical speed",
            state.medianMs?.let { "$it ms" } ?: "—",
            Modifier.weight(1f).padding(start = 16.dp)
        )
        VerticalHairline()
        StatCell(
            "Spent",
            if (state.spentUsd == 0.0) "$0.00" else "$" + String.format("%.4f", state.spentUsd),
            Modifier.weight(1f).padding(start = 16.dp)
        )
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(top = 14.dp, bottom = 18.dp)) {
        Text(
            label,
            style = TextStyle(
                fontFamily = JevFont,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Jev.OnBandFaint
            )
        )
        Spacer(Modifier.height(5.dp))
        Text(
            value,
            style = TextStyle(
                fontFamily = JevFont,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.2).sp,
                fontFeatureSettings = Tabular,
                color = Jev.OnBand
            )
        )
    }
}

@Composable
private fun VerticalHairline() {
    Box(
        Modifier
            .width(1.dp)
            .height(58.dp)
            .background(Jev.BandHairline)
    )
}

/* ------------------------------------------------------------------ */
/*  Body                                                              */
/* ------------------------------------------------------------------ */

@Composable
private fun SectionHeader(title: String, trailing: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            title,
            style = TextStyle(
                fontFamily = JevFont,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.2).sp,
                color = Jev.Ink
            )
        )
        Text(
            trailing,
            style = TextStyle(fontFamily = JevFont, fontSize = 13.sp, color = Jev.Muted)
        )
    }
}

@Composable
private fun EmptyState(onSendTest: () -> Unit, onSimulate: () -> Unit) {
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 34.dp)) {
        Text(
            "Nothing to review yet",
            style = TextStyle(
                fontFamily = JevFont,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp,
                color = Jev.Ink
            )
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Here's what a caught message looks like, so you know what to expect.",
            style = TextStyle(
                fontFamily = JevFont,
                fontSize = 15.sp,
                lineHeight = 23.sp,
                color = Jev.Muted
            ),
            modifier = Modifier.fillMaxWidth(0.94f)
        )
        Spacer(Modifier.height(18.dp))

        MessageCard(message = SampleCatch, isSample = true)

        Button(
            onClick = onSendTest,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp)
                .height(56.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Jev.Ink,
                contentColor = Color.White
            )
        ) {
            Text(
                "Send a test message",
                style = TextStyle(
                    fontFamily = JevFont,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }

        Text(
            "Or simulate one from the emulator",
            style = TextStyle(
                fontFamily = JevFont,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Jev.Muted
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onSimulate)
                .padding(vertical = 16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun MessageCard(
    message: ScannedMessage,
    modifier: Modifier = Modifier,
    isSample: Boolean = false
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(if (isSample) Color(0x9EFFFFFF) else Jev.Card, shape)
            .then(if (isSample) Modifier.border(1.dp, Jev.Line, shape) else Modifier)
            .padding(horizontal = 17.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                message.sender,
                style = TextStyle(
                    fontFamily = JevFont,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Jev.Ink
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                message.receivedAt,
                style = TextStyle(
                    fontFamily = JevFont,
                    fontSize = 13.sp,
                    fontFeatureSettings = Tabular,
                    color = Jev.Faint
                ),
                maxLines = 1
            )
        }

        Spacer(Modifier.height(7.dp))

        Text(
            message.text,
            style = TextStyle(
                fontFamily = JevFont,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = Jev.Body
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(13.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            VerdictPill(message.verdict)
            Spacer(Modifier.width(10.dp))
            Text(
                "Judged in ${message.latencyMs} ms",
                style = TextStyle(
                    fontFamily = JevFont,
                    fontSize = 12.5.sp,
                    fontFeatureSettings = Tabular,
                    color = Jev.Faint
                )
            )
        }
    }
}

@Composable
private fun VerdictPill(verdict: Verdict) {
    Text(
        verdict.label,
        style = TextStyle(
            fontFamily = JevFont,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = verdict.fg
        ),
        modifier = Modifier
            .background(verdict.bg, CircleShape)
            .padding(horizontal = 11.dp, vertical = 5.dp)
    )
}
