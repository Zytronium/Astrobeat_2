package dev.zytronium.astrobeat2.ui.tracklist

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.zytronium.astrobeat2.data.local.entity.TrackEntity
import dev.zytronium.astrobeat2.data.repository.TrackRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrackListViewModel @Inject constructor(
    private val repository: TrackRepository
) : ViewModel() {

    val tracks: LiveData<List<TrackEntity>> = repository.getAllTracks().asLiveData()

    init {
        viewModelScope.launch {
            repository.sync()
        }
    }
}
