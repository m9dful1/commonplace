package com.commonplace.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.commonplace.ui.theme.Accent
import com.commonplace.ui.theme.Ink
import com.commonplace.ui.theme.LedgerBody
import com.commonplace.ui.theme.MetaMono
import com.commonplace.ui.theme.Paper
import com.commonplace.ui.theme.Rule

/**
 * A small paper-and-ink confirmation dialog. Material3's AlertDialog ships
 * with a colored container and rounded corners that fight the app's tone;
 * this is a hand-built replacement that uses the same typography as the rest
 * of the app.
 */
@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String = "delete",
    cancelLabel: String = "cancel",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(2.dp))
                .background(Paper)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = title,
                style = TextStyle(
                    fontFamily = FontFamily.Serif,
                    fontSize = 18.sp,
                    color = Ink,
                ),
            )
            Text(text = body, style = LedgerBody.copy(fontSize = 14.sp))
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Rule),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    text = cancelLabel,
                    style = MetaMono,
                    modifier = Modifier
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
                Text(
                    text = confirmLabel,
                    style = MetaMono.copy(color = Accent),
                    modifier = Modifier
                        .clickable(onClick = {
                            onConfirm()
                            onDismiss()
                        })
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}
