package com.commonplace.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.commonplace.ui.theme.Accent
import com.commonplace.ui.theme.AccentBorder
import com.commonplace.ui.theme.Ink
import com.commonplace.ui.theme.LogBody
import com.commonplace.ui.theme.MetaMono
import com.commonplace.ui.theme.Muted
import com.commonplace.ui.theme.Rule

/**
 * Mirrors src/lib/markdownToReact.tsx for CLAUDE_LOG.md. We don't pull in
 * a Markdown parser library — the document only uses a small subset and
 * a hand-rolled block parser is more controllable for the typography we
 * want.
 *
 * Supported blocks: H1 (skipped — preamble), H2, H3 (with date-role split),
 * paragraphs, blockquotes, unordered list items, hr, and code fences.
 *
 * Inline supports `**bold**`, `*italic*`/`_italic_`, `` `code` ``, and
 * `[link](url)` — though we don't make the link clickable; we just style it.
 */

private val H1_RE = Regex("^# .*")
private val H2_RE = Regex("^## (.+)$")
private val H3_RE = Regex("^### (.+)$")
private val HR_RE = Regex("^---\\s*$")
private val LIST_RE = Regex("^[-*]\\s+(.+)$")
private val NUM_LIST_RE = Regex("^(\\d+)[.)]\\s+(.+)$")
private val FENCE_RE = Regex("^```.*$")
private val DATE_HEADING_RE = Regex("^(\\d{4}-\\d{2}-\\d{2})\\s+—\\s+(.+)$")

@Composable
fun ClaudeLogContent(source: String) {
    val blocks = parseBlocks(source)
    Column(modifier = Modifier.fillMaxWidth()) {
        for (b in blocks) {
            RenderBlock(b)
        }
        Spacer(modifier = Modifier.height(64.dp))
    }
}

private sealed interface Block {
    data class Heading(val depth: Int, val text: String) : Block
    data class Paragraph(val text: String) : Block
    data class Blockquote(val lines: List<String>) : Block
    data class UnorderedList(val items: List<String>) : Block
    data class OrderedList(val items: List<String>) : Block
    data class Code(val text: String) : Block
    data object Rule : Block
    data object BlankBlock : Block
}

private fun parseBlocks(source: String): List<Block> {
    // Skip the file's preamble before the first --- line, mirroring the web
    // implementation. The page chrome supplies its own tagline.
    val parts = source.split(Regex("(?m)^---\\s*$"), limit = 2)
    val body = if (parts.size > 1) parts.drop(1).joinToString("---") else source

    val lines = body.lines()
    val out = mutableListOf<Block>()
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        when {
            line.isBlank() -> {
                i++
            }
            FENCE_RE.matches(line) -> {
                val buf = StringBuilder()
                i++
                while (i < lines.size && !FENCE_RE.matches(lines[i])) {
                    if (buf.isNotEmpty()) buf.append('\n')
                    buf.append(lines[i])
                    i++
                }
                if (i < lines.size) i++ // consume closing fence
                out += Block.Code(buf.toString())
            }
            HR_RE.matches(line) -> {
                out += Block.Rule
                i++
            }
            H3_RE.matches(line) -> {
                out += Block.Heading(3, H3_RE.matchEntire(line)!!.groupValues[1])
                i++
            }
            H2_RE.matches(line) -> {
                out += Block.Heading(2, H2_RE.matchEntire(line)!!.groupValues[1])
                i++
            }
            H1_RE.matches(line) -> {
                // Skipped (file's own H1 title — page chrome owns the page title).
                i++
            }
            line.startsWith("> ") || line == ">" -> {
                val buf = mutableListOf<String>()
                while (i < lines.size && (lines[i].startsWith("> ") || lines[i] == ">")) {
                    buf += lines[i].removePrefix(">").trimStart()
                    i++
                }
                out += Block.Blockquote(buf)
            }
            LIST_RE.containsMatchIn(line) -> {
                val items = mutableListOf<String>()
                while (i < lines.size && LIST_RE.containsMatchIn(lines[i])) {
                    items += LIST_RE.matchEntire(lines[i])!!.groupValues[1]
                    i++
                }
                out += Block.UnorderedList(items)
            }
            NUM_LIST_RE.containsMatchIn(line) -> {
                val items = mutableListOf<String>()
                while (i < lines.size && NUM_LIST_RE.containsMatchIn(lines[i])) {
                    items += NUM_LIST_RE.matchEntire(lines[i])!!.groupValues[2]
                    i++
                }
                out += Block.OrderedList(items)
            }
            else -> {
                val buf = StringBuilder(line)
                i++
                while (
                    i < lines.size &&
                    lines[i].isNotBlank() &&
                    !H1_RE.matches(lines[i]) &&
                    !H2_RE.matches(lines[i]) &&
                    !H3_RE.matches(lines[i]) &&
                    !HR_RE.matches(lines[i]) &&
                    !FENCE_RE.matches(lines[i]) &&
                    !lines[i].startsWith("> ") &&
                    !LIST_RE.containsMatchIn(lines[i]) &&
                    !NUM_LIST_RE.containsMatchIn(lines[i])
                ) {
                    buf.append(' ')
                    buf.append(lines[i].trim())
                    i++
                }
                out += Block.Paragraph(buf.toString())
            }
        }
    }
    return out
}

@Composable
private fun RenderBlock(block: Block) {
    when (block) {
        Block.Rule -> Spacer(modifier = Modifier.height(24.dp))
        Block.BlankBlock -> Spacer(modifier = Modifier.height(8.dp))
        is Block.Heading -> {
            when (block.depth) {
                3 -> {
                    val m = DATE_HEADING_RE.matchEntire(block.text)
                    if (m != null) {
                        DateHeading(date = m.groupValues[1], role = m.groupValues[2])
                    } else {
                        Text(
                            text = renderInline(block.text, baseColor = Ink),
                            style = LogBody.copy(fontSize = 16.sp, fontWeight = FontWeight.Normal),
                            modifier = Modifier.padding(top = 36.dp, bottom = 12.dp),
                        )
                    }
                }
                2 -> Text(
                    text = renderInline(block.text, baseColor = Ink),
                    style = LogBody.copy(fontSize = 18.sp),
                    modifier = Modifier.padding(top = 32.dp, bottom = 12.dp),
                )
                else -> Text(
                    text = renderInline(block.text, baseColor = Ink),
                    style = LogBody.copy(fontSize = 18.sp),
                    modifier = Modifier.padding(top = 24.dp, bottom = 12.dp),
                )
            }
        }
        is Block.Paragraph -> Text(
            text = renderInline(block.text, baseColor = Ink),
            style = LogBody,
            modifier = Modifier.padding(vertical = 6.dp),
        )
        is Block.Blockquote -> Row(
            modifier = Modifier.padding(vertical = 8.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .background(AccentBorder),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                for (line in block.lines) {
                    Text(
                        text = renderInline(line, baseColor = Accent),
                        style = LogBody.copy(
                            color = Accent,
                            fontStyle = FontStyle.Italic,
                        ),
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                }
            }
        }
        is Block.UnorderedList -> Column(
            modifier = Modifier.padding(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            for (item in block.items) {
                Row {
                    Text(text = "•", style = LogBody.copy(color = Muted))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = renderInline(item, baseColor = Ink),
                        style = LogBody,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        is Block.OrderedList -> Column(
            modifier = Modifier.padding(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            block.items.forEachIndexed { idx, item ->
                Row {
                    Text(
                        text = "${idx + 1}.",
                        style = MetaMono.copy(fontSize = 12.sp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = renderInline(item, baseColor = Ink),
                        style = LogBody,
                    )
                }
            }
        }
        is Block.Code -> Text(
            text = AnnotatedString(block.text),
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = Ink,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .background(Rule.copy(alpha = 0.4f))
                .padding(12.dp),
        )
    }
}

@Composable
private fun DateHeading(date: String, role: String) {
    Row(
        modifier = Modifier.padding(top = 56.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(text = date, style = MetaMono)
        Spacer(modifier = Modifier.size(width = 12.dp, height = 1.dp))
        Text(
            text = role,
            style = LogBody.copy(color = Accent, fontSize = 16.sp),
        )
    }
}

// -----------------------------------------------------------------------
// Inline parser for the log: bold, italic, codespan, links, em-dashes.
// -----------------------------------------------------------------------

private val INLINE_PATTERN = Regex(
    """(\*\*([^*\n]+?)\*\*)|(__([^_\n]+?)__)|(\*([^*\n]+?)\*)|(_([^_\n]+?)_)|(`([^`\n]+?)`)|(\[([^\]\n]+?)\]\(([^)\n]+?)\))"""
)

private fun renderInline(input: String, baseColor: androidx.compose.ui.graphics.Color): AnnotatedString {
    val text = input.replace("--", "—")
    return buildAnnotatedString {
        var cursor = 0
        for (m in INLINE_PATTERN.findAll(text)) {
            if (m.range.first > cursor) {
                append(text.substring(cursor, m.range.first))
            }
            val groups = m.groupValues
            when {
                groups[1].isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.Medium, color = Ink)) {
                    append(groups[2])
                }
                groups[3].isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.Medium, color = Ink)) {
                    append(groups[4])
                }
                groups[5].isNotEmpty() -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(groups[6])
                }
                groups[7].isNotEmpty() -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(groups[8])
                }
                groups[9].isNotEmpty() -> withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        background = Rule.copy(alpha = 0.4f),
                    )
                ) {
                    append(groups[10])
                }
                groups[11].isNotEmpty() -> withStyle(SpanStyle(color = Accent)) {
                    append(groups[12])
                }
            }
            cursor = m.range.last + 1
        }
        if (cursor < text.length) {
            append(text.substring(cursor))
        }
    }
}
