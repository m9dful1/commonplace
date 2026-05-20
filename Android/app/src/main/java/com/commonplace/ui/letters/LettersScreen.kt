package com.commonplace.ui.letters

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.commonplace.CommonplaceApp
import com.commonplace.data.Letter
import com.commonplace.ui.common.ConfirmDialog
import com.commonplace.ui.common.rememberViewModel
import com.commonplace.ui.theme.Accent
import com.commonplace.ui.theme.ErrorItalic
import com.commonplace.ui.theme.LetterBody
import com.commonplace.ui.theme.MetaMono
import com.commonplace.ui.theme.Muted
import com.commonplace.ui.theme.Paper
import com.commonplace.ui.theme.PlaceholderItalic

@Composable
fun LettersScreen(app: CommonplaceApp) {
    val vm: LettersViewModel = rememberViewModel {
        LettersViewModel(app.letters, app.letterGenerator, app.settings)
    }
    val items by vm.items.collectAsState()
    val hasApiKey by vm.hasApiKey.collectAsState()
    val composeState by vm.composeState.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(Paper)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 24.dp, vertical = 16.dp,
            ),
        ) {
            item("compose") {
                ComposeAffordance(
                    state = composeState,
                    hasApiKey = hasApiKey,
                    onCompose = vm::compose,
                )
                Spacer(modifier = Modifier.height(40.dp))
            }
            if (items.isEmpty()) {
                if (composeState !is ComposeState.Writing) {
                    item("empty") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "No Letters yet.",
                                style = PlaceholderItalic,
                            )
                            Text(
                                text = "A Letter is a 250–400 word epistle from Claude, written against your recent fragments — addressed to you, considered, signed off. Letters are slower than chat. Capture some fragments first, then come back when you'd like one.",
                                style = PlaceholderItalic.copy(fontSize = 13.sp),
                            )
                        }
                    }
                }
            } else {
                items(items, key = { it.id }) { l ->
                    LetterRow(letter = l, onDelete = { vm.delete(l.id) })
                    Spacer(modifier = Modifier.height(56.dp))
                }
            }
        }
    }
}

@Composable
private fun ComposeAffordance(
    state: ComposeState,
    hasApiKey: Boolean,
    onCompose: () -> Unit,
) {
    when (state) {
        ComposeState.Writing -> Text(
            text = "Claude is writing…",
            style = LetterBody.copy(
                color = Muted,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
            ),
        )
        is ComposeState.Error -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "couldn’t reach Claude — ${state.message}", style = ErrorItalic)
            Text(
                text = "try again",
                style = MetaMono.copy(color = Accent),
                modifier = Modifier
                    .clickable(onClick = onCompose)
                    .padding(vertical = 4.dp),
            )
        }
        ComposeState.Idle -> {
            if (!hasApiKey) {
                Text(
                    text = "Add an Anthropic API key in Settings to compose a Letter.",
                    style = ErrorItalic,
                )
            } else {
                Text(
                    text = "compose a letter",
                    style = MetaMono.copy(color = Accent),
                    modifier = Modifier
                        .clickable(onClick = onCompose)
                        .padding(vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun LetterRow(letter: Letter, onDelete: () -> Unit) {
    var showMenu by remember(letter.id) { mutableStateOf(false) }
    var showConfirm by remember(letter.id) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { /* tapping a letter opens the menu off; nothing to do */ },
                onLongClick = { showMenu = true },
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = letter.createdAt.take(10), style = MetaMono)
            Spacer(modifier = Modifier.weight(1f))
            if (showMenu) {
                Text(
                    text = "delete",
                    style = MetaMono.copy(color = Accent),
                    modifier = Modifier
                        .clickable {
                            showMenu = false
                            showConfirm = true
                        }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                )
                Text(
                    text = "cancel",
                    style = MetaMono,
                    modifier = Modifier
                        .clickable { showMenu = false }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        for (para in letter.body.trim().split(Regex("\\n{2,}"))) {
            Text(
                text = para,
                style = LetterBody,
                modifier = Modifier.padding(bottom = 14.dp),
            )
        }
    }

    if (showConfirm) {
        ConfirmDialog(
            title = "Delete this Letter?",
            body = "This Letter will be removed from your collection. The action can't be undone.",
            confirmLabel = "delete",
            onConfirm = onDelete,
            onDismiss = { showConfirm = false },
        )
    }
}
