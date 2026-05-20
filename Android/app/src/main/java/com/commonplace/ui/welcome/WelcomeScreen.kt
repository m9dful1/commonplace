package com.commonplace.ui.welcome

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.commonplace.CommonplaceApp
import com.commonplace.data.repo.SettingsRepository
import com.commonplace.ui.common.rememberViewModel
import com.commonplace.ui.theme.Accent
import com.commonplace.ui.theme.Ink
import com.commonplace.ui.theme.MetaMono
import com.commonplace.ui.theme.Muted
import com.commonplace.ui.theme.Paper
import kotlinx.coroutines.launch

private const val ANTHROPIC_CONSOLE_URL = "https://console.anthropic.com/settings/keys"

class WelcomeViewModel(
    private val settings: SettingsRepository,
) : ViewModel() {
    fun finish() {
        viewModelScope.launch { settings.setWelcomeSeen() }
    }
}

@Composable
fun WelcomeScreen(
    app: CommonplaceApp,
    onFinishToCapture: () -> Unit,
    onFinishToSettings: () -> Unit,
) {
    val vm: WelcomeViewModel = rememberViewModel { WelcomeViewModel(app.settings) }
    val context = LocalContext.current
    var page by remember { mutableIntStateOf(0) }
    val total = 3

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
            .padding(horizontal = 28.dp, vertical = 32.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Title bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Commonplace",
                    style = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontSize = 22.sp,
                        color = Ink,
                    ),
                )
                if (page < total - 1) {
                    Text(
                        text = "skip",
                        style = MetaMono,
                        modifier = Modifier
                            .clickable {
                                vm.finish()
                                onFinishToCapture()
                            }
                            .padding(8.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    (fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) togetherWith
                        fadeOut(animationSpec = androidx.compose.animation.core.tween(150)))
                },
                label = "welcome-pager",
            ) { p ->
                when (p) {
                    0 -> Page1()
                    1 -> Page2(
                        onGetKey = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ANTHROPIC_CONSOLE_URL))
                            context.startActivity(intent)
                        },
                    )
                    2 -> Page3()
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(48.dp))

            // Page dots — three small indigo circles, only the current one filled.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                repeat(total) { i ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (i == page) Accent else Muted.copy(alpha = 0.3f)),
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (page > 0) {
                    Text(
                        text = "back",
                        style = MetaMono,
                        modifier = Modifier
                            .clickable { page-- }
                            .padding(8.dp),
                    )
                } else {
                    Spacer(modifier = Modifier.size(width = 1.dp, height = 1.dp))
                }
                if (page < total - 1) {
                    Text(
                        text = "next →",
                        style = MetaMono.copy(color = Accent),
                        modifier = Modifier
                            .clickable { page++ }
                            .padding(8.dp),
                    )
                } else {
                    Text(
                        text = "begin →",
                        style = MetaMono.copy(color = Accent),
                        modifier = Modifier
                            .clickable {
                                vm.finish()
                                onFinishToSettings()
                            }
                            .padding(8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PageContainer(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        content()
    }
}

private val PageTitle = TextStyle(
    fontFamily = FontFamily.Serif,
    fontSize = 28.sp,
    fontWeight = FontWeight.Normal,
    color = Ink,
    lineHeight = 36.sp,
)

private val PageBody = TextStyle(
    fontFamily = FontFamily.Serif,
    fontSize = 17.sp,
    fontWeight = FontWeight.Normal,
    color = Ink,
    lineHeight = 28.sp,
)

private val PageBodyAccent = PageBody.copy(
    color = Accent,
    fontStyle = FontStyle.Italic,
)

@Composable
private fun Page1() = PageContainer {
    Text(text = "A commonplace book.", style = PageTitle)
    Text(
        text = "For centuries, people kept personal anthologies — fragments, quotes, half-formed thoughts — as a way of metabolizing what they read. Marcus Aurelius kept one. So did Locke, Montaigne, Virginia Woolf.",
        style = PageBody,
    )
    Text(
        text = "Commonplace continues that practice. You capture fragments. Claude writes brief margin notes when you ask. Over time, a Ledger accumulates that describes what you've been gathering. Once in a while, you can request a Letter.",
        style = PageBody,
    )
    Text(
        text = "It is not a chatbot. It opens fast, stays out of the way, and rewards slow attention.",
        style = PageBodyAccent,
    )
}

@Composable
private fun Page2(onGetKey: () -> Unit) = PageContainer {
    Text(text = "An API key, briefly.", style = PageTitle)
    Text(
        text = "The AI features — marginalia, the Ledger, Letters — call Anthropic's Claude models directly from your phone. To do that, you'll need an Anthropic API key.",
        style = PageBody,
    )
    Text(
        text = "Visit console.anthropic.com, create an account, add about ten dollars of credit, and copy a key. That covers a long time of marginalia (about $0.006 each) and a few Letters (about $0.03 each).",
        style = PageBody,
    )
    Text(
        text = "The key is stored locally on this device. It is yours; it is not sent anywhere except to Anthropic when the app makes a call on your behalf.",
        style = PageBody,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "open the Anthropic Console →",
        style = MetaMono.copy(color = Accent, fontSize = 12.sp),
        modifier = Modifier
            .clickable(onClick = onGetKey)
            .padding(vertical = 8.dp),
    )
}

@Composable
private fun Page3() = PageContainer {
    Text(text = "What stays, what leaves.", style = PageTitle)
    Text(
        text = "Your fragments, your margin notes, your Ledger entries, your Letters — all stored on this device, in a local database. There are no accounts, no servers, no analytics. Nothing follows you between phones unless you export it yourself.",
        style = PageBody,
    )
    Text(
        text = "What leaves the device: when you ask for a margin note, the fragment and a small amount of context (recent fragments, prior marginalia on the same fragment) is sent to Claude. The same is true for the Ledger and for Letters. Anthropic processes those calls under their own privacy terms.",
        style = PageBody,
    )
    Text(
        text = "There are no notifications. The app expects you to come to it, not the other way around.",
        style = PageBodyAccent,
    )
}
