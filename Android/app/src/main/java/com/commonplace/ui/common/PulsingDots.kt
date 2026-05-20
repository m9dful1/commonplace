package com.commonplace.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.commonplace.ui.theme.Accent

/**
 * Three indigo dots that pulse out of phase. Mirrors the cp-pulse keyframe in
 * src/app/globals.css — same 1.4s cycle, same 180ms stagger, same range.
 */
@Composable
fun PulsingDots(label: String = "Claude is writing") {
    Row(
        modifier = Modifier.semantics {
            contentDescription = label
            role = Role.Image
        },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Dot(delayMillis = 0)
        Dot(delayMillis = 180)
        Dot(delayMillis = 360)
    }
}

@Composable
private fun Dot(delayMillis: Int) {
    val transition = rememberInfiniteTransition(label = "cp-pulse-$delayMillis")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(delayMillis),
        ),
        label = "alpha-$delayMillis",
    )
    Spacer(
        modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .alpha(alpha)
            .background(Accent),
    )
}
