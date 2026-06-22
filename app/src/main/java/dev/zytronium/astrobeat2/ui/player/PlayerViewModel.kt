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

    fun loadTrack(id: String) {
        trackId = id
        viewModelScope.launch {
            _track.value = repository.getTrackById(id)
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
