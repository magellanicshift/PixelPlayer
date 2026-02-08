package com.theveloper.pixelplay.presentation.components

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.theveloper.pixelplay.data.model.Song
import com.theveloper.pixelplay.presentation.components.subcomps.AutoSizingTextToFill
import com.theveloper.pixelplay.utils.formatDuration
import com.theveloper.pixelplay.utils.shapes.RoundedStarShape
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import androidx.core.net.toUri
import com.theveloper.pixelplay.presentation.viewmodel.PlaylistViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.theveloper.pixelplay.data.ai.SongMetadata
import com.theveloper.pixelplay.data.media.CoverArtUpdate
import com.theveloper.pixelplay.ui.theme.MontserratFamily
import kotlinx.coroutines.launch
import java.io.File

import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import com.theveloper.pixelplay.presentation.screens.TabAnimation
import com.theveloper.pixelplay.ui.theme.GoogleSansRounded

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SongInfoBottomSheet(
    song: Song,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onDismiss: () -> Unit,
    onPlaySong: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddNextToQueue: () -> Unit,
    onAddToPlayList: () -> Unit,
    onDeleteFromDevice: (activity: Activity, song: Song, onResult: (Boolean) -> Unit) -> Unit,
    onNavigateToAlbum: () -> Unit,
    onNavigateToArtist: () -> Unit,
    onEditSong: (title: String, artist: String, album: String, genre: String, lyrics: String, trackNumber: Int, coverArtUpdate: CoverArtUpdate?) -> Unit,
    generateAiMetadata: suspend (List<String>) -> Result<SongMetadata>,
    removeFromListTrigger: () -> Unit
) {
    val context = LocalContext.current
    var showEditSheet by remember { mutableStateOf(false) }

    val evenCornerRadiusElems = 26.dp

    val listItemShape = AbsoluteSmoothCornerShape(
        cornerRadiusTR = 20.dp, smoothnessAsPercentBR = 60, cornerRadiusBR = 20.dp,
        smoothnessAsPercentTL = 60, cornerRadiusTL = 20.dp, smoothnessAsPercentBL = 60,
        cornerRadiusBL = 20.dp, smoothnessAsPercentTR = 60
    )
    val albumArtShape = AbsoluteSmoothCornerShape(
        cornerRadiusTR = evenCornerRadiusElems, smoothnessAsPercentBR = 60, cornerRadiusBR = evenCornerRadiusElems,
        smoothnessAsPercentTL = 60, cornerRadiusTL = evenCornerRadiusElems, smoothnessAsPercentBL = 60,
        cornerRadiusBL = evenCornerRadiusElems, smoothnessAsPercentTR = 60
    )
    val playButtonShape = AbsoluteSmoothCornerShape(
        cornerRadiusTR = evenCornerRadiusElems, smoothnessAsPercentBR = 60, cornerRadiusBR = evenCornerRadiusElems,
        smoothnessAsPercentTL = 60, cornerRadiusTL = evenCornerRadiusElems, smoothnessAsPercentBL = 60,
        cornerRadiusBL = evenCornerRadiusElems, smoothnessAsPercentTR = 60
    )

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { true }
    )

    val favoriteButtonCornerRadius by animateDpAsState(
        targetValue = if (isFavorite) evenCornerRadiusElems else 60.dp,
        animationSpec = tween(durationMillis = 300), label = "FavoriteCornerAnimation"
    )
    val favoriteButtonContainerColor by animateColorAsState(
        targetValue = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(durationMillis = 300), label = "FavoriteContainerColorAnimation"
    )
    val favoriteButtonContentColor by animateColorAsState(
        targetValue = if (isFavorite) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 300), label = "FavoriteContentColorAnimation"
    )

    val favoriteButtonShape = AbsoluteSmoothCornerShape(
        cornerRadiusTR = favoriteButtonCornerRadius, smoothnessAsPercentBR = 60, cornerRadiusBR = favoriteButtonCornerRadius,
        smoothnessAsPercentTL = 60, cornerRadiusTL = favoriteButtonCornerRadius, smoothnessAsPercentBL = 60,
        cornerRadiusBL = favoriteButtonCornerRadius, smoothnessAsPercentTR = 60
    )

    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { 2 })
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = {
            if (!showEditSheet) {
                onDismiss()
            }
        },
        sheetState = sheetState,
    ) {
        Box(
            modifier = Modifier
                .animateContentSize(
                    animationSpec = tween(durationMillis = 200),
                    alignment = Alignment.BottomCenter
                )
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(animationSpec = tween(durationMillis = 200))
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                ) {
                    // Fila para la carátula del álbum y el título (Always visible)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SmartImage(
                            model = song.albumArtUriString,
                            contentDescription = "Album Art",
                            shape = albumArtShape,
                            modifier = Modifier.size(80.dp),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            AutoSizingTextToFill(
                                modifier = Modifier.padding(end = 4.dp),
                                fontWeight = FontWeight.Light,
                                text = song.title
                            )
                        }
                        FilledTonalIconButton(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(vertical = 6.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceBright,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            onClick = { showEditSheet = true },
                        ) {
                            Icon(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = "Edit song metadata"
                            )
                        }
                    }


                }

                Spacer(modifier = Modifier.height(16.dp))

                // Swipeable Content
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .wrapContentHeight() // Allow sizing to content
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) { page ->
                    when (page) {
                        0 -> { // Options / Actions
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // ========== GROUP 1: PLAYBACK ==========
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(36.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    tonalElevation = 1.dp
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(72.dp)
                                            .padding(6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Play Button
                                        PillActionButton(
                                            modifier = Modifier.weight(1f),
                                            onClick = onPlaySong,
                                            icon = Icons.Rounded.PlayArrow,
                                            label = "Play",
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                        
                                        // Favorite Button
                                        PillActionButton(
                                            modifier = Modifier.weight(1f),
                                            onClick = onToggleFavorite,
                                            icon = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                            label = if (isFavorite) "Liked" else "Like",
                                            containerColor = if (isFavorite) 
                                                MaterialTheme.colorScheme.primaryContainer 
                                            else 
                                                MaterialTheme.colorScheme.surfaceContainerHighest,
                                            contentColor = if (isFavorite) 
                                                MaterialTheme.colorScheme.onPrimaryContainer 
                                            else 
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        
                                        // Share Button
                                        PillActionButton(
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                try {
                                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                        type = "audio/*"
                                                        putExtra(Intent.EXTRA_STREAM, song.contentUriString.toUri())
                                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    }
                                                    context.startActivity(Intent.createChooser(shareIntent, "Share Song File Via"))
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Could not share song: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                                }
                                            },
                                            icon = Icons.Rounded.Share,
                                            label = "Share",
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                
                                // ========== GROUP 2: QUEUE ==========
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(36.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    tonalElevation = 1.dp
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(72.dp)
                                            .padding(6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Add to Queue
                                        PillActionButton(
                                            modifier = Modifier.weight(1f),
                                            onClick = onAddToQueue,
                                            icon = Icons.AutoMirrored.Rounded.QueueMusic,
                                            label = "Add to Queue",
                                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                        
                                        // Play Next
                                        PillActionButton(
                                            modifier = Modifier.weight(1f),
                                            onClick = onAddNextToQueue,
                                            icon = Icons.AutoMirrored.Filled.QueueMusic,
                                            label = "Play Next",
                                            containerColor = MaterialTheme.colorScheme.tertiary,
                                            contentColor = MaterialTheme.colorScheme.onTertiary
                                        )
                                    }
                                }
                                
                                // ========== GROUP 3: LIBRARY ==========
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(36.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    tonalElevation = 1.dp
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(72.dp)
                                            .padding(6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Add to Playlist
                                        PillActionButton(
                                            modifier = Modifier.weight(1f),
                                            onClick = onAddToPlayList,
                                            icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                                            label = "Playlist",
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        
                                        // Delete
                                        PillActionButton(
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                (context as? Activity)?.let { activity ->
                                                    onDeleteFromDevice(activity, song) { result ->
                                                        if (result) {
                                                            removeFromListTrigger()
                                                            onDismiss()
                                                        }
                                                    }
                                                }
                                            },
                                            icon = Icons.Default.DeleteForever,
                                            label = "Delete",
                                            containerColor = MaterialTheme.colorScheme.errorContainer,
                                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                                
                                Spacer(Modifier.height(80.dp))
                            }
                        }
                        1 -> { // Details / Info
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                item {
                                    ListItem(
                                        modifier = Modifier.clip(shape = listItemShape),
                                        headlineContent = { Text("Duration") },
                                        supportingContent = { Text(formatDuration(song.duration)) },
                                        leadingContent = { Icon(Icons.Rounded.Schedule, contentDescription = "Duration icon") }
                                    )
                                }

                                if (!song.genre.isNullOrEmpty()) {
                                    item {
                                        ListItem(
                                            modifier = Modifier.clip(shape = listItemShape),
                                            headlineContent = { Text("Genre") },
                                            supportingContent = { Text(song.genre) },
                                            leadingContent = { Icon(Icons.Rounded.MusicNote, contentDescription = "Genre icon") }
                                        )
                                    }
                                }

                                item {
                                    ListItem(
                                        modifier = Modifier
                                            .clip(shape = listItemShape)
                                            .clickable(onClick = onNavigateToAlbum),
                                        headlineContent = { Text("Album") },
                                        supportingContent = { Text(song.album) },
                                        leadingContent = { Icon(Icons.Rounded.Album, contentDescription = "Album icon") }
                                    )
                                }

                                item {
                                    ListItem(
                                        modifier = Modifier
                                            .clip(shape = listItemShape)
                                            .clickable(onClick = onNavigateToArtist),
                                        headlineContent = { Text("Artist") },
                                        supportingContent = { Text(song.displayArtist) },
                                        leadingContent = { Icon(Icons.Rounded.Person, contentDescription = "Artist icon") }
                                    )
                                }
                                item {
                                    ListItem(
                                        modifier = Modifier
                                            .clip(shape = listItemShape),
                                        headlineContent = { Text("Path") },
                                        supportingContent = { Text(song.path) },
                                        leadingContent = { Icon(Icons.Rounded.AudioFile, contentDescription = "File icon") }
                                    )
                                }
                                item {
                                    Spacer(Modifier.height(80.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Tab Bar
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(5.dp),
                containerColor = Color.Transparent,
                divider = {},
                indicator = {}
            ) {
                 TabAnimation(
                    index = 0,
                    title = "Options",
                    selectedIndex = pagerState.currentPage,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(0)
                        }
                    },
                    transformOrigin = TransformOrigin(0f, 0.5f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Menu,
                            contentDescription = "Options",
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "OPTIONS",
                            fontFamily = GoogleSansRounded,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                
                TabAnimation(
                    index = 1,
                    title = "Details",
                    selectedIndex = pagerState.currentPage,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(1)
                        }
                    },
                    transformOrigin = TransformOrigin(1f, 0.5f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Info,
                            contentDescription = "Details",
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "INFO",
                            fontFamily = GoogleSansRounded,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    EditSongSheet(
        visible = showEditSheet,
        song = song,
        onDismiss = { showEditSheet = false },
        onSave = { title, artist, album, genre, lyrics, trackNumber, coverArt ->
            onEditSong(title, artist, album, genre, lyrics, trackNumber, coverArt)
            showEditSheet = false
        },
        generateAiMetadata = generateAiMetadata
    )
}

/**
 * A pill-shaped action button with Material 3 Expressive microinteractions.
 * Features scale-on-press animation with spring physics.
 */
@Composable
private fun PillActionButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color
) {
    var isPressed by remember { mutableStateOf(false) }
    
    // Scale animation with spring physics (M3 Expressive)
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
        ),
        label = "buttonScale"
    )
    
    // Animated colors for smooth transitions
    val animatedContainerColor by animateColorAsState(
        targetValue = containerColor,
        animationSpec = tween(durationMillis = 250),
        label = "containerColor"
    )
    val animatedContentColor by animateColorAsState(
        targetValue = contentColor,
        animationSpec = tween(durationMillis = 250),
        label = "contentColor"
    )
    
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            },
        shape = CircleShape,
        color = animatedContainerColor,
        contentColor = animatedContentColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}
