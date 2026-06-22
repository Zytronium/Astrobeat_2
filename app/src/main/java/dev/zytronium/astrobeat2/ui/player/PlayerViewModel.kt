package dev.zytronium.astrobeat2.ui.player

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.zytronium.astrobeat2.data.local.entity.TrackEntity
import dev.zytronium.astrobeat2.data.repository.TrackRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val repository: TrackRepository
) : ViewModel() {

    private val _track = MutableLiveData<TrackEntity?>()
    val track: LiveData<TrackEntity?> = _track

    private var trackId: String? = null
    private var trackList: List<TrackEntity> = emptyList()

    fun loadTrack(id: String) {
        trackId = id
        viewModelScope.launch {
            if (trackList.isEmpty()) {
                trackList = repository.getAllTracksOnce()
            }
            _track.value = repository.getTrackById(id)
        }
    }

    fun playNext(shuffle: Boolean) {
        if (shuffle) navigateRandom() else navigateRelative(1)
    }

    fun playPrevious(shuffle: Boolean) {
        if (shuffle) navigateRandom() else navigateRelative(-1)
    }

    private fun navigateRandom() {
        val currentId = trackId ?: return
        viewModelScope.launch {
            if (trackList.isEmpty()) {
                trackList = repository.getAllTracksOnce()
            }
            if (trackList.size <= 1) return@launch

            var newTrack: TrackEntity
            do {
                newTrack = trackList.random()
            } while (newTrack.id == currentId)

            trackId = newTrack.id
            _track.value = newTrack
        }
    }

    private fun navigateRelative(delta: Int) {
        val currentId = trackId ?: return
        viewModelScope.launch {
            if (trackList.isEmpty()) {
                trackList = repository.getAllTracksOnce()
            }
            if (trackList.isEmpty()) return@launch

            val currentIndex = trackList.indexOfFirst { it.id == currentId }
            if (currentIndex == -1) return@launch

            val newIndex = (currentIndex + delta + trackList.size) % trackList.size
            val newTrack = trackList[newIndex]
            trackId = newTrack.id
            _track.value = newTrack
        }
    }

    suspend fun getStreamUrl(): String {
        val id = trackId ?: throw IllegalStateException("Track not loaded")
        return repository.getStreamUrl(id)
    }

    fun logPlay() {
        val id = trackId ?: return
        viewModelScope.launch {
            repository.logPlay(id)
        }
    }
}
