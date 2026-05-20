package com.commonplace.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Mirrors tailwind.config.ts. The web app's palette is the only palette;
 * dark mode is intentionally not supported (the closing entry on the web
 * project explicitly noted that warmth/dark drift would betray the
 * hand-bound-notebook aesthetic — same logic applies here).
 */
val Paper = Color(0xFFF7F3EC)
val Ink = Color(0xFF1A1714)
val Muted = Color(0xFF7A7268)
val Rule = Color(0xFFE4DDD0)
val Accent = Color(0xFF5B5E8C)

// Helpers used in a few places (selection highlights, faded accent borders).
val AccentSoft = Color(0x2E5B5E8C) // ~18% indigo, matches the web's ::selection
val AccentBorder = Color(0x4D5B5E8C) // ~30% indigo for blockquote left rules
