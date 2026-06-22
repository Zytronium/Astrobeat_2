package dev.zytronium.astrobeat2.data.remote.dto

data class TrackDto(
    val id: String,
    val title: String,
    val artist: String?,
    val album: String?,
    val genre: String?,
    val tags: List<String>?,
    val durationSecs: Int?,
    val r2Key: String,
    val fileName: String,
    val fileSizeBytes: Int?,
    val mimeType: String?,
    val uploadedAt: String,
    // populated by /api/sync, null from /api/tracks
    val lastPlayedAt: String?,
)
