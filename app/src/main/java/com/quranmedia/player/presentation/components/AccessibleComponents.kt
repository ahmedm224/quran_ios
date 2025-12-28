package com.quranmedia.player.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Accessible button with haptic feedback
 */
@Composable
fun AccessibleIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val haptics = LocalHapticFeedback.current

    IconButton(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier.semantics {
            this.contentDescription = contentDescription
            this.role = Role.Button
        },
        enabled = enabled
    ) {
        content()
    }
}

/**
 * Slider with enhanced accessibility
 */
@Composable
fun AccessibleSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    label: String = "",
    valueDescription: (Float) -> String = { it.toString() }
) {
    val haptics = LocalHapticFeedback.current
    var lastHapticValue = remember { value }

    Slider(
        value = value,
        onValueChange = { newValue ->
            // Provide haptic feedback at intervals
            if (kotlin.math.abs(newValue - lastHapticValue) > (valueRange.endInclusive - valueRange.start) * 0.05f) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                lastHapticValue = newValue
            }
            onValueChange(newValue)
        },
        valueRange = valueRange,
        modifier = modifier.semantics {
            this.contentDescription = "$label: ${valueDescription(value)}"
            this.stateDescription = valueDescription(value)
        }
    )
}

/**
 * Text with enhanced accessibility for Arabic
 */
@Composable
fun AccessibleArabicText(
    text: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge,
    contentDescription: String? = null
) {
    // Get theme styles in Composable context
    val headlineLargeStyle = MaterialTheme.typography.headlineLarge
    val headlineMediumStyle = MaterialTheme.typography.headlineMedium

    Text(
        text = text,
        modifier = modifier.semantics {
            contentDescription?.let {
                this.contentDescription = it
            }
            // Mark as heading if it's a title style
            if (style == headlineLargeStyle ||
                style == headlineMediumStyle) {
                this.heading()
            }
        },
        style = style
    )
}

/**
 * Card with long-press support
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AccessibleCard(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    contentDescription: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }

    Card(
        modifier = modifier
            .combinedClickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material.ripple.rememberRipple(),
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                },
                onLongClick = onLongClick?.let {
                    {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        it()
                    }
                }
            )
            .semantics {
                this.contentDescription = contentDescription
                this.role = Role.Button
            }
    ) {
        Column(content = content)
    }
}
