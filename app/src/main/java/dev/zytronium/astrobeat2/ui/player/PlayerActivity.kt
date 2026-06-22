package dev.zytronium.astrobeat2.ui.player

import dev.zytronium.astrobeat2.R
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
    private var isShuffleOn = false
    private var isLoopOn = false
    private val speedOptions = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    private var speedIndex = 2
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    // -------- guards against re-registering listeners/click handlers on every track change --------
    private var listenerAdded = false
    private var controlsInitialized = false

    private val progressRunnable = object : Runnable {
        override fun run() {
            val controller = playbackController.controller ?: return
            val pos = controller.currentPosition
            val dur = controller.duration.coerceAtLeast(1)
            binding.seekBar.progress = ((pos.toFloat() / dur) * 1000).toInt()
            binding.textPosition.text = formatMs(pos)
            binding.textDuration.text = formatMs(dur)
            val icon = if (controller.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
            binding.buttonPlayPause.setImageResource(icon)
            handler.postDelayed(this, 500)
        }
    }

    @Inject
    lateinit var playbackController: PlaybackController

    private var hasLoggedPlay = false
    private var pendingTrack: PendingTrack? = null

    private data class PendingTrack(
        val trackId: String,
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

            hasLoggedPlay = false

            val artist = track.artist ?: "Unknown Artist"
            val album = track.album
            binding.textTitle.text = track.title
            binding.textArtistAlbum.text = if (album != null) "$artist - $album" else artist

            pendingTrack = PendingTrack(
                trackId = track.id,
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

            if (!controlsInitialized) {
                controlsInitialized = true
                setupPlayerControls(controller)
            }

            if (!listenerAdded) {
                listenerAdded = true
                controller.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY &&
                            controller.playWhenReady &&
                            !hasLoggedPlay
                        ) {
                            hasLoggedPlay = true
                            viewModel.logPlay()
                        }

                        if (playbackState == Player.STATE_ENDED) {
                            viewModel.playNext(isShuffleOn)
                        }
                    }
                })
            }

            val pending = pendingTrack ?: return@connect
            pendingTrack = null

            // -------- same track already loaded/playing; don't restart it --------
            if (controller.currentMediaItem?.mediaId == pending.trackId) {
                return@connect
            }

            val metadata = MediaMetadata.Builder()
                .setTitle(pending.title)
                .setArtist(pending.artist)
                .setAlbumTitle(pending.album)
                .build()

            if (pending.isDownloaded && pending.localFilePath != null) {
                val mediaItem = MediaItem.Builder()
                    .setMediaId(pending.trackId)
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
                            .setMediaId(pending.trackId)
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

    private fun setupPlayerControls(controller: androidx.media3.session.MediaController) {
        binding.buttonPlayPause.setOnClickListener {
            if (controller.isPlaying) controller.pause() else controller.play()
        }

        binding.buttonPrevious.setOnClickListener {
            viewModel.playPrevious(isShuffleOn)
        }

        binding.buttonNext.setOnClickListener {
            viewModel.playNext(isShuffleOn)
        }

        binding.buttonShuffle.setOnClickListener {
            isShuffleOn = !isShuffleOn
            controller.shuffleModeEnabled = isShuffleOn
            updateToggleButton(binding.buttonShuffle, isShuffleOn)
        }

        binding.buttonLoop.setOnClickListener {
            isLoopOn = !isLoopOn
            controller.repeatMode = if (isLoopOn) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            updateToggleButton(binding.buttonLoop, isLoopOn)
        }

        binding.buttonSpeed.setOnClickListener {
            speedIndex = (speedIndex + 1) % speedOptions.size
            val speed = speedOptions[speedIndex]
            controller.setPlaybackParameters(androidx.media3.common.PlaybackParameters(speed))
            binding.buttonSpeed.text = formatSpeed(speed)
        }

        binding.seekBar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: android.widget.SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser && controller.duration > 0) {
                    controller.seekTo((progress / 1000f * controller.duration).toLong())
                }
            }
            override fun onStartTrackingTouch(sb: android.widget.SeekBar) {}
            override fun onStopTrackingTouch(sb: android.widget.SeekBar) {}
        })

        handler.post(progressRunnable)
    }

    private fun updateToggleButton(button: android.widget.ImageButton, active: Boolean) {
        val bgRes = if (active) R.drawable.bg_button_active_glow else R.drawable.bg_button_inactive
        val tintRes = if (active) R.color.cyan_accent else R.color.text_secondary
        button.background = androidx.core.content.ContextCompat.getDrawable(this, bgRes)
        button.imageTintList = androidx.core.content.ContextCompat.getColorStateList(this, tintRes)
    }

    private fun formatMs(ms: Long): String {
        val totalSec = ms / 1000
        return "%d:%02d".format(totalSec / 60, totalSec % 60)
    }

    private fun formatSpeed(speed: Float): String =
        if (speed == speed.toLong().toFloat()) "${speed.toInt()}x" else "${speed}x"

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(progressRunnable)
        playbackController.release()
    }

    companion object {
        const val EXTRA_TRACK_ID = "extra_track_id"
    }
}
