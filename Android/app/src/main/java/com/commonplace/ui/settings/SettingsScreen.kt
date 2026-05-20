package com.commonplace.ui.settings

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.commonplace.CommonplaceApp
import com.commonplace.ui.common.rememberViewModel
import com.commonplace.ui.theme.Accent
import com.commonplace.ui.theme.ErrorItalic
import com.commonplace.ui.theme.Ink
import com.commonplace.ui.theme.LedgerBody
import com.commonplace.ui.theme.MetaMono
import com.commonplace.ui.theme.Muted
import com.commonplace.ui.theme.Paper
import com.commonplace.ui.theme.Rule

private const val ANTHROPIC_CONSOLE = "https://console.anthropic.com/settings/keys"

@Composable
fun SettingsScreen(
    app: CommonplaceApp,
    onOpenAbout: () -> Unit,
) {
    val vm: SettingsViewModel = rememberViewModel {
        SettingsViewModel(app.settings, app.anthropic, app.fragments, app.marginalia, app.ledger, app.letters)
    }
    val saved by vm.savedKey.collectAsState()
    val masked by vm.maskedKey.collectAsState()
    val testState by vm.testState.collectAsState()
    val saveFlash by vm.saveFlash.collectAsState()
    val exportState by vm.exportState.collectAsState()

    val context = LocalContext.current
    var key by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    val hasKey = !saved.isNullOrBlank()

    val createDocLauncher = rememberCreateDocumentLauncher { uri ->
        if (uri != null) vm.exportTo(context, uri)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {

            Text(
                text = "Settings",
                style = TextStyle(
                    fontFamily = FontFamily.Serif,
                    fontSize = 22.sp,
                    color = Ink,
                ),
            )

            // ----- API KEY -----
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "Anthropic API key", style = MetaMono)

                Text(
                    text = "Required for marginalia, the Ledger, and Letters. The key is stored on this device only.",
                    style = ErrorItalic,
                )

                if (hasKey) {
                    Text(text = "current: $masked", style = MetaMono.copy(color = Muted))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                ) {
                    BasicTextField(
                        value = key,
                        onValueChange = { key = it },
                        textStyle = MetaMono.copy(color = Ink, fontSize = 14.sp),
                        cursorBrush = SolidColor(Accent),
                        singleLine = true,
                        visualTransformation = if (show) VisualTransformation.None
                        else PasswordVisualTransformation('•'),
                        keyboardOptions = KeyboardOptions(
                            autoCorrectEnabled = false,
                            capitalization = KeyboardCapitalization.None,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        decorationBox = { inner ->
                            if (key.isEmpty()) {
                                Text(
                                    text = if (hasKey) "enter a new key to replace" else "sk-ant-…",
                                    style = MetaMono.copy(
                                        color = Muted.copy(alpha = 0.7f),
                                        fontSize = 14.sp,
                                    ),
                                )
                            }
                            inner()
                        },
                    )
                    Text(
                        text = "paste",
                        style = MetaMono.copy(color = Muted),
                        modifier = Modifier
                            .clickable {
                                val text = readClipboardText(context)
                                if (!text.isNullOrBlank()) key = text.trim()
                            }
                            .padding(8.dp),
                    )
                    Text(
                        text = if (show) "hide" else "show",
                        style = MetaMono.copy(color = Muted),
                        modifier = Modifier
                            .clickable { show = !show }
                            .padding(8.dp),
                    )
                }
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Rule),
                )

                Text(
                    text = "get an API key from the Anthropic Console →",
                    style = MetaMono.copy(color = Accent, fontSize = 12.sp),
                    modifier = Modifier
                        .clickable {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(ANTHROPIC_CONSOLE))
                                )
                            }
                        }
                        .padding(vertical = 8.dp),
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val testing = testState is TestState.Testing
                    Text(
                        text = if (testing) "testing…" else "test key",
                        style = MetaMono.copy(
                            color = if (key.isBlank() || testing) Accent.copy(alpha = 0.4f) else Accent,
                        ),
                        modifier = Modifier
                            .clickable(enabled = key.isNotBlank() && !testing) { vm.runTest(key) }
                            .padding(vertical = 8.dp),
                    )
                    Text(
                        text = "save",
                        style = MetaMono.copy(
                            color = if (key.isBlank()) Accent.copy(alpha = 0.4f) else Accent,
                        ),
                        modifier = Modifier
                            .clickable(enabled = key.isNotBlank()) {
                                vm.save(key) { key = "" }
                            }
                            .padding(vertical = 8.dp),
                    )
                    if (saveFlash) {
                        Text(text = "saved.", style = MetaMono)
                    }
                }

                when (val s = testState) {
                    is TestState.Ok -> Text(
                        text = "✓ key is valid · ${s.model}",
                        style = MetaMono.copy(color = Accent, fontSize = 13.sp),
                    )
                    is TestState.Err -> Text(
                        text = "couldn’t reach Claude — ${s.message}",
                        style = ErrorItalic,
                    )
                    else -> Unit
                }
            }

            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Rule),
            )

            // ----- COST EXPECTATIONS -----
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "what does this cost?", style = MetaMono)
                Text(
                    text = "Anthropic charges per API call, not per app install. The app itself is free. Each marginal note is roughly $0.006 (Sonnet); each Letter is roughly $0.03 (Opus). Ten dollars of Anthropic credit lasts most users a long time.",
                    style = LedgerBody.copy(fontSize = 14.sp),
                )
            }

            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Rule),
            )

            // ----- DATA -----
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "your data", style = MetaMono)
                Text(
                    text = "Everything you write — fragments, marginalia, Ledger entries, Letters — lives in this app's local database. If you uninstall the app, that data is deleted. Export it to keep a copy.",
                    style = LedgerBody.copy(fontSize = 14.sp),
                )
                ExportRow(state = exportState, onExport = {
                    val filename = vm.suggestedExportFilename()
                    createDocLauncher.launch(filename)
                })
            }

            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Rule),
            )

            // ----- LINKS -----
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "about Commonplace →",
                    style = MetaMono.copy(color = Accent, fontSize = 12.sp),
                    modifier = Modifier
                        .clickable(onClick = onOpenAbout)
                        .padding(vertical = 8.dp),
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun ExportRow(state: ExportState, onExport: () -> Unit) {
    when (state) {
        ExportState.Idle -> Text(
            text = "export all data as JSON →",
            style = MetaMono.copy(color = Accent, fontSize = 12.sp),
            modifier = Modifier
                .clickable(onClick = onExport)
                .padding(vertical = 8.dp),
        )
        ExportState.Working -> Text(
            text = "preparing export…",
            style = ErrorItalic,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        is ExportState.Done -> Text(
            text = "✓ exported to ${state.location}",
            style = MetaMono.copy(color = Accent, fontSize = 12.sp),
            modifier = Modifier.padding(vertical = 8.dp),
        )
        is ExportState.Error -> Text(
            text = "couldn’t export — ${state.message}",
            style = ErrorItalic,
            modifier = Modifier.padding(vertical = 8.dp),
        )
    }
}

private fun readClipboardText(context: Context): String? {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        ?: return null
    val clip = cm.primaryClip ?: return null
    if (clip.itemCount == 0) return null
    return clip.getItemAt(0).coerceToText(context).toString()
}
