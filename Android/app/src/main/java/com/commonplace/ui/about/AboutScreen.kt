package com.commonplace.ui.about

import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.commonplace.ui.theme.Accent
import com.commonplace.ui.theme.ErrorItalic
import com.commonplace.ui.theme.Ink
import com.commonplace.ui.theme.LedgerBody
import com.commonplace.ui.theme.MetaMono
import com.commonplace.ui.theme.Paper

private const val ANTHROPIC_CONSOLE = "https://console.anthropic.com/settings/keys"
private const val REPO_URL = "https://github.com/m9dful1/commonplace"

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val versionName = remember {
        try {
            val info: PackageInfo =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getPackageInfo(
                        context.packageName,
                        PackageManager.PackageInfoFlags.of(0L),
                    )
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(context.packageName, 0)
                }
            info.versionName ?: "?"
        } catch (_: Throwable) {
            "?"
        }
    }

    fun open(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(
                text = "← settings",
                style = MetaMono,
                modifier = Modifier
                    .clickable(onClick = onBack)
                    .padding(vertical = 6.dp),
            )

            Text(
                text = "About",
                style = TextStyle(
                    fontFamily = FontFamily.Serif,
                    fontSize = 22.sp,
                    color = Ink,
                ),
            )

            Text(
                text = "Commonplace is a digital commonplace book for thinking with Claude. You capture fragments. Claude writes brief margin notes when you ask. Over time, a Ledger accumulates that describes the shape of what you've been gathering. Once in a while, you can request a Letter.",
                style = LedgerBody,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Section("version") {
                Text(text = "Commonplace $versionName", style = LedgerBody)
                Text(text = "powered by Claude (Sonnet 4.6 / Opus 4.7)", style = MetaMono)
            }

            Section("privacy") {
                Text(
                    text = "Your fragments, marginalia, Ledger entries, and Letters are stored on this device only. There are no servers, no accounts, no analytics. Nothing follows you between phones unless you export it yourself, from Settings.",
                    style = LedgerBody,
                )
                Text(
                    text = "When you ask for a margin note, the fragment and a small amount of context (recent fragments, prior margin notes on the same fragment) is sent to Anthropic's Claude API using your own API key. The same is true for the Ledger and for Letters. Anthropic processes those calls under their own privacy terms. The app does not communicate with anyone else.",
                    style = LedgerBody,
                )
                Text(
                    text = "The API key is stored in this app's local database, in plain text. It is not sent anywhere except to Anthropic. If you uninstall the app, the database — including the key — is deleted. If you want to back up your data first, use Export from Settings.",
                    style = LedgerBody,
                )
            }

            Section("links") {
                LinkRow(label = "Anthropic Console (where API keys live) →") { open(ANTHROPIC_CONSOLE) }
                LinkRow(label = "source code →") { open(REPO_URL) }
            }

            Section("license") {
                Text(text = "MIT", style = LedgerBody)
                Text(
                    text = "The app is free. You pay Anthropic directly for the API calls Claude makes on your behalf.",
                    style = ErrorItalic,
                )
            }

            Section("a note") {
                Text(
                    text = "This app was built across several conversations with Claude — a different instance each time, leaving notes for the next one in CLAUDE_LOG.md. The Log tab is that document. It's worth reading.",
                    style = LedgerBody,
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text = title, style = MetaMono.copy(color = Accent))
        content()
    }
}

@Composable
private fun LinkRow(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = MetaMono.copy(color = Accent, fontSize = 12.sp),
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    )
}
