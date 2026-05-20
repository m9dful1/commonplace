package com.commonplace.ui.ledger

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.commonplace.CommonplaceApp
import com.commonplace.data.LedgerEntry
import com.commonplace.ui.common.ConfirmDialog
import com.commonplace.ui.common.PulsingDots
import com.commonplace.ui.common.renderLightMarkdown
import com.commonplace.ui.common.rememberViewModel
import com.commonplace.ui.theme.Accent
import com.commonplace.ui.theme.ErrorItalic
import com.commonplace.ui.theme.Ink
import com.commonplace.ui.theme.LedgerBody
import com.commonplace.ui.theme.MetaMono
import com.commonplace.ui.theme.Paper
import com.commonplace.ui.theme.PlaceholderItalic
import com.commonplace.ui.theme.Rule

@Composable
fun LedgerScreen(app: CommonplaceApp) {
    val vm: LedgerViewModel = rememberViewModel {
        LedgerViewModel(app.ledger, app.ledgerGenerator, app.settings)
    }
    val entries by vm.entries.collectAsState()
    val hasApiKey by vm.hasApiKey.collectAsState()
    val genState by vm.genState.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(Paper)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 24.dp, vertical = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            item("tagline") {
                Text(
                    text = "what you’ve been gathering — the collection’s own journal",
                    style = MetaMono,
                    modifier = Modifier.padding(bottom = 24.dp),
                )
            }
            if (entries.isEmpty()) {
                item("empty") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "The Ledger is empty.",
                            style = PlaceholderItalic,
                        )
                        Text(
                            text = "After every five fragments, Claude considers whether anything has changed enough in your collection to warrant a Ledger entry. Most of the time the answer is no, and nothing happens. You can also ask for one now.",
                            style = PlaceholderItalic.copy(fontSize = 13.sp),
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            } else {
                items(entries, key = { it.id }) { entry ->
                    LedgerRow(
                        entry = entry,
                        onSave = { id, body -> vm.update(id, body) },
                        onDelete = { vm.delete(entry.id) },
                    )
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
            item("rule") {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Rule),
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            item("affordance") {
                GenerateAffordance(
                    state = genState,
                    hasApiKey = hasApiKey,
                    onGenerate = vm::generate,
                    onDismiss = vm::dismissPass,
                )
            }
        }
    }
}

@Composable
private fun GenerateAffordance(
    state: GenState,
    hasApiKey: Boolean,
    onGenerate: () -> Unit,
    onDismiss: () -> Unit,
) {
    when (state) {
        GenState.Generating -> PulsingDots()
        GenState.Passed -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "pass — nothing has changed enough to warrant a new entry.",
                style = ErrorItalic,
            )
            Text(
                text = "dismiss",
                style = MetaMono,
                modifier = Modifier
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 4.dp),
            )
        }
        is GenState.Error -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "couldn’t reach Claude — ${state.message}", style = ErrorItalic)
            Text(
                text = "try again",
                style = MetaMono.copy(color = Accent),
                modifier = Modifier
                    .clickable(onClick = onGenerate)
                    .padding(vertical = 4.dp),
            )
        }
        GenState.Idle -> {
            if (!hasApiKey) {
                Text(
                    text = "Add an Anthropic API key in Settings to update the Ledger.",
                    style = ErrorItalic,
                )
            } else {
                Text(
                    text = "update the ledger",
                    style = MetaMono.copy(color = Accent),
                    modifier = Modifier
                        .clickable(onClick = onGenerate)
                        .padding(vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun LedgerRow(
    entry: LedgerEntry,
    onSave: (String, String) -> Unit,
    onDelete: () -> Unit,
) {
    var editing by remember(entry.id) { mutableStateOf(false) }
    var draft by remember(entry.id, entry.body) { mutableStateOf(entry.body) }
    var showConfirm by remember(entry.id) { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = entry.createdAt.take(10), style = MetaMono)
            Text(text = entry.author.raw, style = MetaMono.copy(color = Accent))
            Spacer(modifier = Modifier.weight(1f))
            if (editing) {
                Text(
                    text = "save",
                    style = MetaMono.copy(color = Accent),
                    modifier = Modifier
                        .clickable {
                            val next = draft.trim()
                            if (next.isNotEmpty() && next != entry.body.trim()) {
                                onSave(entry.id, next)
                            }
                            editing = false
                        }
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                )
                Text(
                    text = "cancel",
                    style = MetaMono,
                    modifier = Modifier
                        .clickable {
                            draft = entry.body
                            editing = false
                        }
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                )
            } else {
                Text(
                    text = "edit",
                    style = MetaMono,
                    modifier = Modifier
                        .clickable { editing = true }
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                )
                Text(
                    text = "delete",
                    style = MetaMono,
                    modifier = Modifier
                        .clickable { showConfirm = true }
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                )
            }
        }
        if (editing) {
            BasicTextField(
                value = draft,
                onValueChange = { draft = it },
                textStyle = LedgerBody.copy(color = Ink),
                cursorBrush = SolidColor(Accent),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Default,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Rule),
            )
        } else {
            Text(text = renderLightMarkdown(entry.body), style = LedgerBody)
        }
    }

    if (showConfirm) {
        ConfirmDialog(
            title = "Delete this Ledger entry?",
            body = "It'll be removed from your collection. The action can't be undone.",
            confirmLabel = "delete",
            onConfirm = onDelete,
            onDismiss = { showConfirm = false },
        )
    }
}
