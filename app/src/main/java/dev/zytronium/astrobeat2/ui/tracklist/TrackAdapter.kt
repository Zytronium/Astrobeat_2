package dev.zytronium.astrobeat2.ui.tracklist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.zytronium.astrobeat2.data.local.entity.TrackEntity
import dev.zytronium.astrobeat2.databinding.ItemTrackBinding

class TrackAdapter(
    private val onTrackClick: (TrackEntity) -> Unit
) : ListAdapter<TrackEntity, TrackAdapter.TrackViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): TrackViewHolder {
        val binding = ItemTrackBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TrackViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TrackViewHolder(
        private val binding: ItemTrackBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(track: TrackEntity) {
            binding.textTitle.text = track.title

            val artist = track.artist ?: "Unknown Artist"
            val album = track.album
            binding.textArtistAlbum.text = if (album != null) "$artist - $album" else artist

            binding.textDuration.text = formatDuration(track.durationSecs)

            binding.iconDownloaded.visibility =
                if (track.isDownloaded) View.VISIBLE else View.GONE

            binding.root.setOnClickListener { onTrackClick(track) }
        }

        private fun formatDuration(totalSecs: Int?): String {
            if (totalSecs == null) return "--:--"
            val minutes = totalSecs / 60
            val seconds = totalSecs % 60
            return String.format("%d:%02d", minutes, seconds)
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<TrackEntity>() {
        override fun areItemsTheSame(oldItem: TrackEntity, newItem: TrackEntity): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: TrackEntity, newItem: TrackEntity): Boolean =
            oldItem == newItem
    }
}
