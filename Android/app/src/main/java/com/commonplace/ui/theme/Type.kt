package com.commonplace.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Typography. The web app's CSS stack is Iowan Old Style → Source Serif → Charter → Georgia → serif.
 * On Android, FontFamily.Serif resolves to Noto Serif on most devices, which
 * is a respectable fallback for a body face. If a future port wants to bundle
 * Source Serif 4 (OFL) as a TTF asset, swap [BodySerif] here and the rest of
 * the typography ripples through.
 */

val BodySerif: FontFamily = FontFamily.Serif
val Mono: FontFamily = FontFamily.Monospace

private val tightLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

// One concrete TextStyle per role, mirroring the web's Tailwind utilities.
// Sizes are tuned for a phone — the web uses 18px body; on a phone we go a
// touch smaller so a fragment fits without the page feeling cramped.

val FragmentBody = TextStyle(
    fontFamily = BodySerif,
    fontWeight = FontWeight.Normal,
    fontSize = 18.sp,
    lineHeight = 28.sp,
    color = Ink,
    lineHeightStyle = tightLineHeight,
)

val FragmentBodyLarge = FragmentBody.copy(
    fontSize = 20.sp,
    lineHeight = 31.sp,
)

val LedgerBody = FragmentBody.copy(
    fontSize = 16.sp,
    lineHeight = 26.sp,
)

val LetterBody = FragmentBody.copy(
    fontSize = 16.sp,
    lineHeight = 26.sp,
)

val LogBody = FragmentBody.copy(
    fontSize = 16.sp,
    lineHeight = 26.sp,
)

val Marginal = TextStyle(
    fontFamily = BodySerif,
    fontWeight = FontWeight.Normal,
    fontStyle = FontStyle.Italic,
    fontSize = 16.sp,
    lineHeight = 26.sp,
    color = Accent,
    lineHeightStyle = tightLineHeight,
)

val MetaMono = TextStyle(
    fontFamily = Mono,
    fontWeight = FontWeight.Normal,
    fontSize = 11.sp,
    letterSpacing = 0.12f.em, // tracking-widest
    color = Muted,
)

val PlaceholderItalic = TextStyle(
    fontFamily = BodySerif,
    fontWeight = FontWeight.Normal,
    fontStyle = FontStyle.Italic,
    fontSize = 16.sp,
    lineHeight = 26.sp,
    color = Muted,
)

val ErrorItalic = PlaceholderItalic.copy(fontSize = 14.sp)

val CommonplaceTypography = Typography(
    bodyLarge = FragmentBody,
    bodyMedium = LedgerBody,
    bodySmall = MetaMono,
    titleLarge = TextStyle(
        fontFamily = BodySerif,
        fontWeight = FontWeight.Normal,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        color = Ink,
    ),
    labelMedium = MetaMono,
    labelSmall = MetaMono,
)
