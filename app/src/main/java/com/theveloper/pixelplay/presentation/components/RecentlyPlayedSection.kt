package com.theveloper.pixelplay.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.theveloper.pixelplay.data.model.Song
import com.theveloper.pixelplay.presentation.model.RecentlyPlayedSongUiModel
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

private val HomeRecentlyPlayedPillHeight = 58.dp
private val HomeRecentlyPlayedPillSpacing = 8.dp
private const val HomeRecentlyPlayedPillsLimit = 10
private const val HomeRecentlyPlayedPillsPerColumn = 3
private val HomeRecentlyPlayedWidthSteps = listOf(148.dp, 166.dp, 184.dp, 202.dp, 220.dp)
private val HomeRecentlyPlayedDefaultContentPadding = PaddingValues(horizontal = 8.dp)

private data class RecentlyPlayedPillCell(
    val item: RecentlyPlayedSongUiModel,
    val width: Dp
)

private data class RecentlyPlayedPillRow(
    val pills: List<RecentlyPlayedPillCell>,
    val contentWidth: Dp
)

@Composable
fun RecentlyPlayedSection(
    songs: List<RecentlyPlayedSongUiModel>,
    onSongClick: (Song) -> Unit,
    onOpenAllClick: () -> Unit,
    currentSongId: String? = null,
    contentPadding: PaddingValues = HomeRecentlyPlayedDefaultContentPadding,
    modifier: Modifier = Modifier
) {
    val layoutDirection = LocalLayoutDirection.current
    val startContentPadding = remember(contentPadding, layoutDirection) {
        contentPadding.calculateLeftPadding(layoutDirection)
    }
    val endContentPadding = remember(contentPadding, layoutDirection) {
        contentPadding.calculateRightPadding(layoutDirection)
    }

    val visibleSongs = remember(songs) { songs.take(HomeRecentlyPlayedPillsLimit) }
    val songRows = remember(visibleSongs, startContentPadding, endContentPadding) {
        val rows = MutableList(HomeRecentlyPlayedPillsPerColumn) { mutableListOf<RecentlyPlayedPillCell>() }
        visibleSongs.forEachIndexed { index, item ->
            val rowIndex = index % HomeRecentlyPlayedPillsPerColumn
            rows[rowIndex] += RecentlyPlayedPillCell(
                item = item,
                width = resolveRecentlyPlayedPillWidth(item = item, rowIndex = rowIndex)
            )
        }
        rows.map { pills ->
            val pillsWidth = pills.fold(0.dp) { acc, cell -> acc + cell.width }
            val rowSpacing = if (pills.size > 1) HomeRecentlyPlayedPillSpacing * (pills.size - 1) else 0.dp
            val rowWidth = pillsWidth + rowSpacing + startContentPadding + endContentPadding
            RecentlyPlayedPillRow(
                pills = pills,
                contentWidth = rowWidth
            )
        }
    }
    val maxRowContentWidth = remember(songRows) {
        songRows.maxOfOrNull { it.contentWidth } ?: 0.dp
    }
    val sharedScrollState = rememberScrollState()

    val sectionHeight = HomeRecentlyPlayedPillHeight * HomeRecentlyPlayedPillsPerColumn +
            HomeRecentlyPlayedPillSpacing * (HomeRecentlyPlayedPillsPerColumn - 1)

    Column(
        modifier = modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.padding(start = 6.dp),
                text = "Recently Played",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            FilledIconButton(
                modifier = Modifier
                    .height(40.dp)
                    .width(64.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.secondary
                ),
                onClick = onOpenAllClick,
                enabled = songs.isNotEmpty()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        if (visibleSongs.isEmpty()) {
            Card(
                shape = AbsoluteSmoothCornerShape(
                    cornerRadiusTL = 24.dp,
                    smoothnessAsPercentTR = 60,
                    cornerRadiusTR = 24.dp,
                    smoothnessAsPercentBR = 60,
                    cornerRadiusBL = 24.dp,
                    smoothnessAsPercentBL = 60,
                    cornerRadiusBR = 24.dp,
                    smoothnessAsPercentTL = 60
                ),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Play a few songs to populate this section.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                )
            }
        } else {
            // Exactly three stacked rows (staggered look with variable-width pills).
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = sectionHeight)
                    .horizontalScroll(state = sharedScrollState)
            ) {
                Column(
                    modifier = Modifier
                        .width(maxRowContentWidth)
                        .height(sectionHeight),
                    verticalArrangement = Arrangement.spacedBy(HomeRecentlyPlayedPillSpacing),
                    horizontalAlignment = Alignment.Start
                ) {
                    songRows.forEach { row ->
                        if (row.pills.isEmpty()) {
                            Spacer(modifier = Modifier.height(HomeRecentlyPlayedPillHeight))
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(HomeRecentlyPlayedPillHeight),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(HomeRecentlyPlayedPillSpacing)
                            ) {
                                if (startContentPadding > 0.dp) {
                                    Spacer(modifier = Modifier.width(startContentPadding))
                                }
                                row.pills.forEach { cell ->
                                    RecentlyPlayedPill(
                                        item = cell.item,
                                        isCurrentSong = currentSongId == cell.item.song.id,
                                        modifier = Modifier.width(cell.width),
                                        onClick = { onSongClick(cell.item.song) }
                                    )
                                }
                                if (endContentPadding > 0.dp) {
                                    Spacer(modifier = Modifier.width(endContentPadding))
                                }
                                val trailingGap = (maxRowContentWidth - row.contentWidth).coerceAtLeast(0.dp)
                                if (trailingGap > 0.dp) {
                                    Spacer(modifier = Modifier.width(trailingGap))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentlyPlayedPill(
    item: RecentlyPlayedSongUiModel,
    isCurrentSong: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val animatedCorner by animateDpAsState(
        targetValue = if (isCurrentSong) 14.dp else (HomeRecentlyPlayedPillHeight / 2),
        animationSpec = tween(durationMillis = 280),
        label = "pillCorner"
    )
    val animatedContainer by animateColorAsState(
        targetValue = if (isCurrentSong) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        animationSpec = tween(durationMillis = 280),
        label = "pillContainer"
    )
    val titleColor by animateColorAsState(
        targetValue = if (isCurrentSong) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(durationMillis = 280),
        label = "pillTitleColor"
    )
    val artistColor by animateColorAsState(
        targetValue = if (isCurrentSong) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.80f)
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 280),
        label = "pillArtistColor"
    )
    val shape = RoundedCornerShape(animatedCorner)

    Card(
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = animatedContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .height(HomeRecentlyPlayedPillHeight)
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(HomeRecentlyPlayedPillHeight)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SmartImage(
                model = item.song.albumArtUriString,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                shape = CircleShape,
                modifier = Modifier.size(38.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = item.song.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = titleColor
                )
                Text(
                    text = item.song.displayArtist,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = artistColor
                )
            }
        }
    }
}

private fun resolveRecentlyPlayedPillWidth(
    item: RecentlyPlayedSongUiModel,
    rowIndex: Int
): Dp {
    val titleLength = item.song.title.trim().length
    val artistLength = item.song.displayArtist.trim().length
    val weightedTextLength = titleLength + (artistLength * 0.55f)

    val baseStep = when {
        weightedTextLength < 18f -> 0
        weightedTextLength < 28f -> 1
        weightedTextLength < 40f -> 2
        weightedTextLength < 54f -> 3
        else -> 4
    }

    val staggerOffset = when (rowIndex) {
        0 -> -1
        1 -> 0
        else -> 1
    }

    val resolvedStep = (baseStep + staggerOffset).coerceIn(0, HomeRecentlyPlayedWidthSteps.lastIndex)
    return HomeRecentlyPlayedWidthSteps[resolvedStep]
}
