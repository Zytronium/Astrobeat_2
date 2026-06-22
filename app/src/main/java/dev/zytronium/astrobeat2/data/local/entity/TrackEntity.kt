package dev.zytronium.astrobeat2.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.zytronium.astrobeat2.data.remote.dto.TrackDto

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: String,
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
    // -------- local state --------
    val localFilePath: String?,  // null = not downloaded
    val isDownloaded: Boolean = false,
    val lastPlayedAt: String?,   // ISO string, null = never played
) {
    companion object {
        fun fromDto(dto: TrackDto, existing: TrackEntity? = null) = TrackEntity(
            id            = dto.id,
            title         = dto.title,
            artist        = dto.artist,
            album         = dto.album,
            genre         = dto.genre,
            tags          = dto.tags,
            durationSecs  = dto.durationSecs,
            r2Key         = dto.r2Key,
            fileName      = dto.fileName,
            fileSizeBytes = dto.fileSizeBytes,
            mimeType      = dto.mimeType,
            uploadedAt    = dto.uploadedAt,
            // -------- preserve local state if track already exists --------
            localFilePath = existing?.localFilePath,
            isDownloaded  = existing?.isDownloaded ?: false,
            // -------- prefer server lastPlayedAt, fall back to local --------
            lastPlayedAt  = dto.lastPlayedAt ?: existing?.lastPlayedAt,
        )
    }
}
