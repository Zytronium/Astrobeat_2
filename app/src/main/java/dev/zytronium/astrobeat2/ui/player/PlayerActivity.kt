package dev.zytronium.astrobeat2.ui.player

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import dagger.hilt.android.AndroidEntryPoint
import dev.zytronium.astrobeat2.databinding.ActivityPlayerBinding
import dev.zytronium.astrobeat2.playback.PlaybackController
import dev.zytronium.astrobeat2.playback.PlaybackService
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private val viewModel: PlayerViewModel by viewModels()

    @Inject
    lateinit var playbackController: PlaybackController

    private var hasLoggedPlay = false
    private var pendingTrack: PendingTrack? = null

    private data class PendingTrack(
        val isDownloaded: Boolean,
        val localFilePath: String?,
        val title: String,
        val artist: String,
        val album: String?
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // -------- ensure playback service is running --------
        startService(Intent(this, PlaybackService::class.java))

        binding.buttonBack.setOnClickListener { finish() }

        val trackId = intent.getStringExtra(EXTRA_TRACK_ID)
        if (trackId == null) {
            finish()
            return
        }

        viewModel.loadTrack(trackId)

        viewModel.track.observe(this) { track ->
            if (track == null) return@observe

            val artist = track.artist ?: "Unknown Artist"
            val album = track.album
            binding.textTitle.text = track.title
            binding.textArtistAlbum.text = if (album != null) "$artist - $album" else artist

            pendingTrack = PendingTrack(
                isDownloaded = track.isDownloaded,
                localFilePath = track.localFilePath,
                title = track.title,
                artist = artist,
                album = album
            )
            connectAndPlay()
        }
    }

    private fun connectAndPlay() {
        playbackController.connect {
            val controller = playbackController.controller ?: return@connect
            binding.playerView.player = controller

            val pending = pendingTrack ?: return@connect
            pendingTrack = null

            controller.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY &&
                        controller.playWhenReady &&
                        !hasLoggedPlay
                    ) {
                        hasLoggedPlay = true
                        viewModel.logPlay()
                    }
                }
            })

            val metadata = MediaMetadata.Builder()
                .setTitle(pending.title)
                .setArtist(pending.artist)
                .setAlbumTitle(pending.album)
                .build()

            if (pending.isDownloaded && pending.localFilePath != null) {
                val mediaItem = MediaItem.Builder()
                    .setUri(Uri.fromFile(File(pending.localFilePath)))
                    .setMediaMetadata(metadata)
                    .build()
                controller.setMediaItem(mediaItem)
                controller.prepare()
                controller.playWhenReady = true
            } else {
                lifecycleScope.launch {
                    try {
                        val url = viewModel.getStreamUrl()
                        val mediaItem = MediaItem.Builder()
                            .setUri(Uri.parse(url))
                            .setMediaMetadata(metadata)
                            .build()
                        controller.setMediaItem(mediaItem)
                        controller.prepare()
                        controller.playWhenReady = true
                    } catch (e: Exception) {
                        // stream url fetch failed, nothing to play
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.playerView.player = null
        playbackController.release()
    }

    companion object {
        const val EXTRA_TRACK_ID = "extra_track_id"
    }
}
