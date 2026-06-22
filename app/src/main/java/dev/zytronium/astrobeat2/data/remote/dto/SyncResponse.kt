package dev.zytronium.astrobeat2.data.remote.dto

data class SyncResponse(
    val tracks: List<TrackDto>,
    val evictBefore: String,
)