package dev.zytronium.astrobeat2.ui.tracklist

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import dev.zytronium.astrobeat2.databinding.ActivityTrackListBinding
import dev.zytronium.astrobeat2.ui.player.PlayerActivity
import dev.zytronium.astrobeat2.worker.WorkScheduler

@AndroidEntryPoint
class TrackListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTrackListBinding
    private val viewModel: TrackListViewModel by viewModels()
    private lateinit var adapter: TrackAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrackListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        adapter = TrackAdapter { track ->
            val intent = Intent(this, PlayerActivity::class.java).apply {
                putExtra(PlayerActivity.EXTRA_TRACK_ID, track.id)
            }
            startActivity(intent)
        }

        binding.recyclerTracks.layoutManager = LinearLayoutManager(this)
        binding.recyclerTracks.adapter = adapter

        viewModel.tracks.observe(this) { trackList ->
            adapter.submitList(trackList)
        }

        WorkScheduler.scheduleAll(applicationContext)
    }
}
