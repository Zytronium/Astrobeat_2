package dev.zytronium.astrobeat2.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.zytronium.astrobeat2.data.local.entity.TrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {

    // -------- observe all tracks, sorted by title --------
    @Query("SELECT * FROM tracks ORDER BY title ASC")
    fun getAllTracks(): Flow<List<TrackEntity>>

    // -------- get all tracks as a plain list (for workers) --------
    @Query("SELECT * FROM tracks ORDER BY title ASC")
    suspend fun getAllTracksList(): List<TrackEntity>

    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun getTrackById(id: String): TrackEntity?

    // -------- upsert - replace metadata but preserve local state via fromDto --------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTracks(tracks: List<TrackEntity>)

    // -------- called by download worker on success --------
    @Query("UPDATE tracks SET localFilePath = :path, isDownloaded = 1 WHERE id = :id")
    suspend fun markDownloaded(id: String, path: String)

    // -------- called by eviction worker --------
    @Query("UPDATE tracks SET localFilePath = NULL, isDownloaded = 0 WHERE id = :id")
    suspend fun markEvicted(id: String)

    // -------- called when user plays a track --------
    @Query("UPDATE tracks SET lastPlayedAt = :timestamp WHERE id = :id")
    suspend fun updateLastPlayed(id: String, timestamp: String)

    // -------- tracks that need downloading --------
    @Query("SELECT * FROM tracks WHERE isDownloaded = 0")
    suspend fun getNotDownloadedTracks(): List<TrackEntity>

    // -------- tracks to purge from device (played before cutoff or never played
    //          but still somehow downloaded) --------
    @Query("""
        SELECT * FROM tracks
        WHERE isDownloaded = 1
        AND (lastPlayedAt IS NULL OR lastPlayedAt < :evictBefore)
    """)
    suspend fun getTracksToEvict(evictBefore: String): List<TrackEntity>
}
