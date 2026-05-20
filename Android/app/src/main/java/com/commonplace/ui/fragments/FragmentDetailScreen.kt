package com.commonplace.ui.fragments

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import com.commonplace.CommonplaceApp
import com.commonplace.ui.common.PulsingDots
import com.commonplace.ui.common.formatTimestamp
import com.commonplace.ui.common.renderLightMarkdown
import com.commonplace.ui.common.rememberViewModelWithKey
import com.commonplace.ui.theme.Accent
import com.commonplace.ui.theme.AccentBorder
import com.commonplace.ui.theme.ErrorItalic
import com.commonplace.ui.theme.FragmentBodyLarge
import com.commonplace.ui.theme.Marginal
import com.commonplace.ui.theme.MetaMono
import com.commonplace.ui.theme.Paper
import com.commonplace.ui.theme.PlaceholderItalic
import com.commonplace.ui.theme.Rule

@Composable
fun FragmentDetailScreen(
    app: CommonplaceApp,
    fragmentId: String,
    onBack: () -> Unit,
) {
    val vm: FragmentDetailViewModel = rememberViewModelWithKey(fragmentId) {
        FragmentDetailViewModel(
            fragments = app.fragments,
            marginaliaRepo = app.marginalia,
            anthropic = app.anthropic,
            settings = app.settings,
            fragmentId = fragmentId,
        )
    }
    val fragment by vm.fragment.collectAsState()
    val marginalia by vm.marginalia.collectAsState()
    val streamState by vm.stream.collectAsState()
    val hasApiKey by vm.hasApiKey.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(Paper)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 24.dp,
                vertical = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item("back") {
                Text(
                    text = "← all fragments",
                    style = MetaMono,
                    modifier = Modifier
                        .clickable(onClick = onBack)
                        .padding(vertical = 6.dp),
                )
            }

            val f = fragment
            if (f == null) {
                item("missing") {
                    Text(text = "fragment not found.", style = PlaceholderItalic)
                }
            } else {
                item("body") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = f.body, style = FragmentBodyLarge)
                        val meta = buildAnnotatedString {
                            append(formatTimestamp(f.createdAt))
                            if (!f.source.isNullOrBlank()) {
                                append("  ·  ")
                                append(f.source)
                            }
                            if (f.tags.isNotEmpty()) {
                                append("  ·  ")
                                append(f.tags.joinToString(" ") { "#$it" })
                            }
                        }
                        Text(text = meta, style = MetaMono)
                    }
                }
                item("rule") {
                    Spacer(modifier = Modifier.height(4.dp))
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Rule),
                    )
                }
                items(marginalia, key = { it.id }) { m ->
                    MarginaliaBlock(body = m.body)
                }
                item("affordance") {
                    AffordanceRow(
                        state = streamState,
                        hasExisting = marginalia.isNotEmpty(),
                        hasApiKey = hasApiKey,
                        onRequest = vm::requestMarginalia,
                    )
                }
            }
        }
    }
}

@Composable
private fun MarginaliaBlock(body: String) {
    Row(modifier = Modifier.padding(start = 16.dp)) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .background(AccentBorder),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = renderLightMarkdown(body),
            style = Marginal,
        )
    }
}

@Composable
private fun AffordanceRow(
    state: MarginaliaStreamState,
    hasExisting: Boolean,
    hasApiKey: Boolean,
    onRequest: () -> Unit,
) {
    when (state) {
        MarginaliaStreamState.Loading -> {
            Box(modifier = Modifier.padding(start = 16.dp)) { PulsingDots() }
        }
        is MarginaliaStreamState.Streaming -> {
            MarginaliaBlock(body = state.text)
        }
        is MarginaliaStreamState.Error -> {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (state.partial != null) {
                    MarginaliaBlock(body = state.partial)
                }
                Text(
                    text = AnnotatedString("couldn’t reach Claude — ${state.message}"),
                    style = ErrorItalic,
                    modifier = Modifier.padding(start = 16.dp),
                )
                Text(
                    text = "try again",
                    style = MetaMono.copy(color = Accent),
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .clickable(onClick = onRequest)
                        .padding(vertical = 4.dp),
                )
            }
        }
        MarginaliaStreamState.Idle -> {
            if (!hasApiKey) {
                Text(
                    text = "Add an Anthropic API key in Settings to request a marginal note.",
                    style = ErrorItalic,
                )
            } else {
                Text(
                    text = if (hasExisting) "another reading" else "request a marginal note",
                    style = MetaMono.copy(color = Accent),
                    modifier = Modifier
                        .clickable(onClick = onRequest)
                        .padding(vertical = 4.dp),
                )
            }
        }
    }
}

