package com.commonplace.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle

private val ColorScheme = lightColorScheme(
    primary = Accent,
    onPrimary = Paper,
    secondary = Accent,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    error = Accent,
    onError = Paper,
    outline = Rule,
    outlineVariant = Rule,
)

@Composable
fun CommonplaceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorScheme,
        typography = CommonplaceTypography,
    ) {
        // Default text style for any unstyled Text — body serif on ink.
        CompositionLocalProvider(
            LocalContentColor provides Ink,
            LocalTextStyle provides TextStyle(
                fontFamily = FontFamily.Serif,
                color = Ink,
            ),
            content = content,
        )
    }
}
