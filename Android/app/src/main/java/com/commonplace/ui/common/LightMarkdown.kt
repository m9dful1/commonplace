package com.commonplace.ui.common

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * Mirrors src/lib/lightMarkdown.ts. Three rules:
 *   *italic* / _italic_   → italic
 *   **bold** / __bold__   → bold
 *   --                    → em-dash
 *
 * Anything fancier and the marginalia is misbehaving and the prompt is the
 * place to fix it, not the renderer.
 */
fun renderLightMarkdown(input: String): AnnotatedString {
    if (input.isEmpty()) return AnnotatedString("")
    val dashed = input.replace("--", "—")
    return buildAnnotatedString {
        emit(this, dashed, ParseDepth.Strong)
    }
}

private enum class ParseDepth { Strong, Em, Plain }

private val STRONG_RE = Regex("\\*\\*([^*\\n]+?)\\*\\*|__([^_\\n]+?)__")
private val EM_RE = Regex("\\*([^*\\n]+?)\\*|_([^_\\n]+?)_")

private fun emit(builder: androidx.compose.ui.text.AnnotatedString.Builder, text: String, depth: ParseDepth) {
    if (text.isEmpty()) return
    when (depth) {
        ParseDepth.Strong -> consume(builder, text, STRONG_RE, ParseDepth.Em) { inner ->
            builder.withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                emit(builder, inner, ParseDepth.Em)
            }
        }
        ParseDepth.Em -> consume(builder, text, EM_RE, ParseDepth.Plain) { inner ->
            builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                emit(builder, inner, ParseDepth.Plain)
            }
        }
        ParseDepth.Plain -> builder.append(text)
    }
}

private inline fun consume(
    builder: androidx.compose.ui.text.AnnotatedString.Builder,
    text: String,
    regex: Regex,
    next: ParseDepth,
    crossinline onMatch: (String) -> Unit,
) {
    var cursor = 0
    while (cursor < text.length) {
        val m = regex.find(text, cursor) ?: break
        if (m.range.first > cursor) {
            emit(builder, text.substring(cursor, m.range.first), next)
        }
        val inner = m.groupValues[1].ifEmpty { m.groupValues[2] }
        onMatch(inner)
        cursor = m.range.last + 1
    }
    if (cursor < text.length) {
        emit(builder, text.substring(cursor), next)
    }
}
