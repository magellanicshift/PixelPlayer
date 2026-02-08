package com.theveloper.pixelplay.data.service.player

import android.net.Uri
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaQueueItem
import com.google.android.gms.cast.MediaSeekOptions
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.common.images.WebImage
import com.theveloper.pixelplay.data.model.Song
import org.json.JSONObject
import timber.log.Timber
import java.util.Locale

class CastPlayer(private val castSession: CastSession) {

    private val remoteMediaClient: RemoteMediaClient? = castSession.remoteMediaClient

    /**
     * Load a queue of songs onto the Cast device.
     * Includes a 15-second timeout to prevent stuck "Connecting..." states.
     */
    fun loadQueue(
        songs: List<Song>,
        startIndex: Int,
        startPosition: Long,
        repeatMode: Int,
        serverAddress: String,
        autoPlay: Boolean,
        onComplete: (Boolean) -> Unit
    ) {
        val client = remoteMediaClient
        if (client == null) {
            onComplete(false)
            return
        }

        // Track whether callback has been fired to prevent double-calling
        var callbackFired = false
        val timeoutHandler = android.os.Handler(android.os.Looper.getMainLooper())
        val timeoutRunnable = Runnable {
            if (!callbackFired) {
                callbackFired = true
                Timber.e("Cast loadQueue timed out after 15 seconds")
                onComplete(false)
            }
        }

        try {
            val mediaItems = songs.map { song ->
                song.toMediaQueueItem(serverAddress)
            }.toTypedArray()

            // Start 15-second timeout
            timeoutHandler.postDelayed(timeoutRunnable, 15000)

            client.queueLoad(
                mediaItems,
                startIndex,
                repeatMode,
                startPosition,
                null
            ).setResultCallback { result ->
                // Cancel timeout since we got a response
                timeoutHandler.removeCallbacks(timeoutRunnable)
                
                if (callbackFired) {
                    // Timeout already fired, ignore this late callback
                    Timber.w("Cast loadQueue result received after timeout, ignoring")
                    return@setResultCallback
                }
                callbackFired = true
                
                if (result.status.isSuccess) {
                    if (autoPlay) {
                        client.play()
                    }
                    // Immediately acknowledge success and request a status update to avoid UI stalls.
                    onComplete(true)
                    client.requestStatus()
                } else {
                    Timber.e("Remote media client failed to load queue: ${result.status.statusMessage}")
                    onComplete(false)
                }
            }
        } catch (e: Exception) {
            timeoutHandler.removeCallbacks(timeoutRunnable)
            Timber.e(e, "Error loading queue to cast device")
            if (!callbackFired) {
                callbackFired = true
                onComplete(false)
            }
        }
    }

    private fun Song.toMediaQueueItem(serverAddress: String): MediaQueueItem {
        val encodedSongId = Uri.encode(this.id)
        val contentType = resolveCastContentType()

        val mediaMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK)
        mediaMetadata.putString(MediaMetadata.KEY_TITLE, this.title)
        mediaMetadata.putString(MediaMetadata.KEY_ARTIST, this.displayArtist)
        val artUrl = "$serverAddress/art/$encodedSongId"
        mediaMetadata.addImage(WebImage(Uri.parse(artUrl)))

        val mediaUrl = "$serverAddress/song/$encodedSongId"
        val mediaInfo = MediaInfo.Builder(mediaUrl)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType(contentType)
            .setStreamDuration(this.duration.coerceAtLeast(0L))
            .setMetadata(mediaMetadata)
            .build()

        return MediaQueueItem.Builder(mediaInfo)
            .setCustomData(
                JSONObject()
                    .put("songId", this.id)
                    .put("mimeType", contentType)
            )
            .build()
    }

    private fun Song.resolveCastContentType(): String {
        val normalizedMimeType = mimeType
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.lowercase(Locale.ROOT)

        if (normalizedMimeType != null && normalizedMimeType.startsWith("audio/")) {
            return normalizedMimeType
        }

        val extension = path.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return when (extension) {
            "mp3" -> "audio/mpeg"
            "flac" -> "audio/flac"
            "aac" -> "audio/aac"
            "m4a", "mp4" -> "audio/mp4"
            "wav" -> "audio/wav"
            "ogg", "oga" -> "audio/ogg"
            "opus" -> "audio/ogg"
            else -> "audio/mpeg"
        }
    }

    fun seek(position: Long) {
        val client = remoteMediaClient ?: return
        try {
            Timber.d("Seeking to position: $position ms")
            val seekOptions = MediaSeekOptions.Builder()
                .setPosition(position)
                .build()

            client.seek(seekOptions)
            // Force status update to prevent UI bouncing
            client.requestStatus()
        } catch (e: Exception) {
            Timber.e(e, "Error seeking cast device")
        }
    }

    fun play() {
        remoteMediaClient?.play()?.setResultCallback {
            if (!it.status.isSuccess) {
                Timber.w("Remote failed to play: %s", it.status.statusMessage)
            }
            remoteMediaClient?.requestStatus()
        }
    }

    fun pause() {
        remoteMediaClient?.pause()?.setResultCallback {
            if (!it.status.isSuccess) {
                Timber.w("Remote failed to pause: %s", it.status.statusMessage)
            }
            remoteMediaClient?.requestStatus()
        }
    }

    fun next() {
        val client = remoteMediaClient ?: return
        val status = client.mediaStatus
        val queueItems = status?.queueItems ?: emptyList()
        val currentItemId = status?.currentItemId
        val currentIndex = queueItems.indexOfFirst { it.itemId == currentItemId }
        val nextItem = if (currentIndex >= 0) queueItems.getOrNull(currentIndex + 1) else null

        if (nextItem != null) {
            jumpToItem(nextItem.itemId, 0L)
            return
        }

        client.queueNext(null)?.setResultCallback {
            if (!it.status.isSuccess) {
                Timber.w("Remote failed to advance to next item: %s", it.status.statusMessage)
            }
            remoteMediaClient?.requestStatus()
        }
    }

    fun previous() {
        val client = remoteMediaClient ?: return
        val status = client.mediaStatus
        val queueItems = status?.queueItems ?: emptyList()
        val currentItemId = status?.currentItemId
        val currentIndex = queueItems.indexOfFirst { it.itemId == currentItemId }
        val previousItem = if (currentIndex > 0) queueItems.getOrNull(currentIndex - 1) else null

        if (previousItem != null) {
            jumpToItem(previousItem.itemId, 0L)
            return
        }

        client.queuePrev(null)?.setResultCallback {
            if (!it.status.isSuccess) {
                Timber.w("Remote failed to go to previous item: %s", it.status.statusMessage)
            }
            remoteMediaClient?.requestStatus()
        }
    }

    fun jumpToItem(itemId: Int, position: Long) {
        remoteMediaClient?.queueJumpToItem(itemId, position, null)?.setResultCallback { result ->
            if (!result.status.isSuccess) {
                Timber.w("Remote failed to jump to item %d: %s", itemId, result.status.statusMessage)
            }
            remoteMediaClient?.requestStatus()
        }
    }

    fun setRepeatMode(repeatMode: Int) {
        remoteMediaClient?.queueSetRepeatMode(repeatMode, null)
    }
}
