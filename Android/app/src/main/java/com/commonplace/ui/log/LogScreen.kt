package com.commonplace.ui.log

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.commonplace.ui.common.ClaudeLogContent
import com.commonplace.ui.theme.MetaMono
import com.commonplace.ui.theme.Paper
import com.commonplace.ui.theme.PlaceholderItalic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val LOG_ASSET = "CLAUDE_LOG.md"

/**
 * Mirrors src/app/log/page.tsx. The log file ships as a read-only asset
 * inside the APK; the on-device view is exactly that — a viewer, in the
 * app's own typography. Updating the log requires shipping a new APK,
 * which is correct: the log entries belong to the build process, not the
 * runtime.
 */
@Composable
fun LogScreen() {
    val context = LocalContext.current
    var content by remember { mutableStateOf<LoadResult>(LoadResult.Loading) }

    LaunchedEffect(Unit) {
        content = withContext(Dispatchers.IO) { loadLog(context) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        when (val c = content) {
            LoadResult.Loading -> Text(
                text = "loading the log…",
                style = PlaceholderItalic,
            )
            is LoadResult.Error -> Column {
                Text(
                    text = "CLAUDE_LOG.md is not where it should be.",
                    style = PlaceholderItalic,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "looked for asset: $LOG_ASSET — ${c.message}",
                    style = MetaMono,
                )
            }
            is LoadResult.Loaded -> Column {
                Text(
                    text = "a document maintained across Claude instances working on Commonplace",
                    style = MetaMono,
                    modifier = Modifier.padding(bottom = 32.dp),
                )
                ClaudeLogContent(c.text)
            }
        }
    }
}

private sealed interface LoadResult {
    data object Loading : LoadResult
    data class Loaded(val text: String) : LoadResult
    data class Error(val message: String) : LoadResult
}

private fun loadLog(context: Context): LoadResult = try {
    val text = context.assets.open(LOG_ASSET).bufferedReader().use { it.readText() }
    LoadResult.Loaded(text)
} catch (e: Throwable) {
    LoadResult.Error(e.message ?: "unknown error.")
}
