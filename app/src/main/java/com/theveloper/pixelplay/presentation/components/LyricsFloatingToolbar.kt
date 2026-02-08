package com.theveloper.pixelplay.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.theveloper.pixelplay.presentation.components.subcomps.PlayerSeekBar

/**
 * Material 3 Expressive-style control bar for the lyrics screen.
 * Two-row layout:
 * - Top: Seekbar in its own pill (optional)
 * - Bottom: Back, Play/Pause, More buttons - fully rounded in normal state
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LyricsFloatingToolbar(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit,
    showSyncedLyrics: Boolean?,
    onShowSyncedLyricsChange: (Boolean) -> Unit,
    onMoreClick: () -> Unit,
    backgroundColor: Color,
    onBackgroundColor: Color,
    accentColor: Color,
    onAccentColor: Color,
    // Playback control parameters
    isPlaying: Boolean = false,
    onPlayPause: () -> Unit = {},
    currentPosition: Long = 0L,
    totalDuration: Long = 0L,
    onSeek: (Long) -> Unit = {},
    showSeekbar: Boolean = true
) {
    if (showSyncedLyrics == null) return

    // Material 3 Expressive motion curve
    val emphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Row: Seekbar in its own fully rounded pill (optional)
        if (showSeekbar && totalDuration > 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50.dp)) // Fully rounded outer container
                    .background(backgroundColor)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                PlayerSeekBar(
                    backgroundColor = Color.Transparent,
                    onBackgroundColor = onBackgroundColor,
                    primaryColor = accentColor,
                    currentPosition = currentPosition,
                    totalDuration = totalDuration,
                    onSeek = onSeek,
                    isPlaying = isPlaying,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                )
            }
        }

        // Bottom Row: Control buttons - fully rounded outer container
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50.dp)) // Fully rounded outer pill
                .background(backgroundColor)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back Button - fully rounded with press animation
            val backInteractionSource = remember { MutableInteractionSource() }
            val backPressed by backInteractionSource.collectIsPressedAsState()
            val backScale by animateFloatAsState(
                targetValue = if (backPressed) 0.92f else 1f,
                animationSpec = tween(durationMillis = 100, easing = emphasizedDecelerate),
                label = "backScale"
            )
            val backCornerRadius by animateDpAsState(
                targetValue = if (backPressed) 20.dp else 50.dp, // Fully rounded -> slight square on press
                animationSpec = tween(durationMillis = 150, easing = emphasizedDecelerate),
                label = "backCorner"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .graphicsLayer { scaleX = backScale; scaleY = backScale }
                    .clip(RoundedCornerShape(backCornerRadius))
                    .background(onBackgroundColor.copy(alpha = 0.1f))
                    .clickable(
                        interactionSource = backInteractionSource,
                        indication = null,
                        onClick = onNavigateBack
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = onBackgroundColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Play/Pause Button - fully rounded, accent colored, animated on press
            val playInteractionSource = remember { MutableInteractionSource() }
            val playPressed by playInteractionSource.collectIsPressedAsState()
            val playScale by animateFloatAsState(
                targetValue = if (playPressed) 0.92f else 1f,
                animationSpec = tween(durationMillis = 100, easing = emphasizedDecelerate),
                label = "playScale"
            )
            val playCornerRadius by animateDpAsState(
                targetValue = if (playPressed) 18.dp else if (isPlaying) 22.dp else 50.dp,
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                label = "playCorner"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .graphicsLayer { scaleX = playScale; scaleY = playScale }
                    .clip(RoundedCornerShape(playCornerRadius))
                    .background(accentColor)
                    .clickable(
                        interactionSource = playInteractionSource,
                        indication = null,
                        onClick = onPlayPause
                    ),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = isPlaying,
                    label = "playPauseIconAnimation"
                ) { playing ->
                    Icon(
                        modifier = Modifier.size(28.dp),
                        imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (playing) "Pause" else "Play",
                        tint = onAccentColor
                    )
                }
            }

            // More Button - fully rounded with press animation
            val moreInteractionSource = remember { MutableInteractionSource() }
            val morePressed by moreInteractionSource.collectIsPressedAsState()
            val moreScale by animateFloatAsState(
                targetValue = if (morePressed) 0.92f else 1f,
                animationSpec = tween(durationMillis = 100, easing = emphasizedDecelerate),
                label = "moreScale"
            )
            val moreCornerRadius by animateDpAsState(
                targetValue = if (morePressed) 20.dp else 50.dp,
                animationSpec = tween(durationMillis = 150, easing = emphasizedDecelerate),
                label = "moreCorner"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .graphicsLayer { scaleX = moreScale; scaleY = moreScale }
                    .clip(RoundedCornerShape(moreCornerRadius))
                    .background(onBackgroundColor.copy(alpha = 0.1f))
                    .clickable(
                        interactionSource = moreInteractionSource,
                        indication = null,
                        onClick = onMoreClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "More options",
                    tint = onBackgroundColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
