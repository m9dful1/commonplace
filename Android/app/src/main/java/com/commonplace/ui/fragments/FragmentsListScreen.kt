package com.commonplace.ui.fragments

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import com.commonplace.CommonplaceApp
import com.commonplace.data.Fragment
import com.commonplace.ui.common.formatTimestamp
import com.commonplace.ui.common.rememberViewModel
import com.commonplace.ui.theme.Accent
import com.commonplace.ui.theme.FragmentBody
import com.commonplace.ui.theme.Ink
import com.commonplace.ui.theme.MetaMono
import com.commonplace.ui.theme.Muted
import com.commonplace.ui.theme.PlaceholderItalic
import com.commonplace.ui.theme.Rule

/**
 * Capture textarea on top, reverse-chronological list below. Mirrors
 * src/components/FragmentForm.tsx + src/components/FragmentList.tsx.
 */
@Composable
fun FragmentsListScreen(
    app: CommonplaceApp,
    onOpen: (String) -> Unit,
) {
    val vm: FragmentsViewModel = rememberViewModel {
        FragmentsViewModel(
            fragments = app.fragments,
            ledger = app.ledger,
            ledgerGenerator = app.ledgerGenerator,
            settings = app.settings,
            app = app,
        )
    }
    val items by vm.items.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(com.commonplace.ui.theme.Paper)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 24.dp,
                vertical = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            item("form") {
                CaptureForm(onSave = vm::save)
                Spacer(modifier = Modifier.height(16.dp))
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Rule),
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
            if (items.isEmpty()) {
                item("empty") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Nothing yet. Drop in a quote, a thought, anything you might lose if you didn't write it down.",
                            style = PlaceholderItalic,
                        )
                        Text(
                            text = "After you save a fragment, tap it to ask Claude for a margin note.",
                            style = PlaceholderItalic.copy(fontSize = 13.sp),
                        )
                    }
                }
            } else {
                items(items, key = { it.id }) { f ->
                    FragmentRow(f, onClick = { onOpen(f.id) })
                    Spacer(modifier = Modifier.height(28.dp))
                }
            }
        }
    }
}

@Composable
private fun CaptureForm(
    onSave: (body: String, source: String?, tagsRaw: String?) -> Unit,
) {
    var body by remember { mutableStateOf("") }
    var source by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var showMeta by remember { mutableStateOf(false) }

    Column {
        BasicTextField(
            value = body,
            onValueChange = { body = it },
            textStyle = FragmentBody,
            cursorBrush = androidx.compose.ui.graphics.SolidColor(Accent),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Default,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            decorationBox = { inner ->
                if (body.isEmpty()) {
                    Text(
                        text = "A fragment…",
                        style = FragmentBody.copy(color = Muted.copy(alpha = 0.7f)),
                    )
                }
                inner()
            },
        )

        if (showMeta) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                MetaField(
                    value = source,
                    onChange = { source = it },
                    placeholder = "source (optional)",
                )
                MetaField(
                    value = tags,
                    onChange = { tags = it },
                    placeholder = "tags, comma separated",
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (showMeta) "− metadata" else "+ source / tags",
                style = MetaMono,
                modifier = Modifier
                    .clickable { showMeta = !showMeta }
                    .padding(vertical = 4.dp),
            )
            Text(
                text = "keep",
                style = MetaMono.copy(color = Accent),
                modifier = Modifier
                    .clickable(enabled = body.isNotBlank()) {
                        if (body.isNotBlank()) {
                            onSave(body, source.ifBlank { null }, tags.ifBlank { null })
                            body = ""
                            source = ""
                            tags = ""
                            showMeta = false
                        }
                    }
                    .padding(vertical = 4.dp, horizontal = 4.dp),
            )
        }
    }
}

@Composable
private fun MetaField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
) {
    Column {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = MetaMono.copy(color = Ink),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(Accent),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MetaMono.copy(color = Muted.copy(alpha = 0.7f)),
                    )
                }
                inner()
            },
        )
        Spacer(
            modifier = Modifier
                .padding(top = 4.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(Rule),
        )
    }
}

@Composable
private fun FragmentRow(f: Fragment, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = f.body,
            style = FragmentBody,
        )
        FragmentMeta(f)
    }
}

@Composable
private fun FragmentMeta(f: Fragment) {
    val text = buildAnnotatedString {
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
    Text(text = text, style = MetaMono)
}
