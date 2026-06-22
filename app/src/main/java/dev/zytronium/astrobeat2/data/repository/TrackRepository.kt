package dev.zytronium.astrobeat2.data.repository

import dev.zytronium.astrobeat2.data.local.dao.TrackDao
import dev.zytronium.astrobeat2.data.local.entity.TrackEntity
import dev.zytronium.astrobeat2.data.remote.AstrobeatApi
import dev.zytronium.astrobeat2.data.remote.dto.PlayRequest
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackRepository @Inject constructor(
    private val api: AstrobeatApi,
    private val dao: TrackDao,
) {
    // -------- UI observes this Flow; updates automatically when DB changes --------
    fun getAllTracks(): Flow<List<TrackEntity>> = dao.getAllTracks()

    suspend fun getTrackById(id: String): TrackEntity? = dao.getTrackById(id)

    // -------- called on app launch and by sync worker --------
    suspend fun sync() {
        val response = try {
            api.sync()
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 401) {
                throw SecurityException("Invalid or missing API secret", e)
            }
            throw e
        }

        // merge remote tracks with existing local state
        val merged = response.tracks.map { dto ->
            val existing = dao.getTrackById(dto.id)
            TrackEntity.fromDto(dto, existing)
        }
        dao.upsertTracks(merged)
    }

    // -------- get a short-lived presigned URL to stream/download from R2 --------
    suspend fun getStreamUrl(trackId: String): String {
        return api.getStreamUrl(trackId).url
    }

    // -------- log play on server + update local lastPlayedAt --------
    suspend fun logPlay(trackId: String) {
        api.logPlay(trackId, PlayRequest())
        dao.updateLastPlayed(trackId, Instant.now().toString())
    }

    suspend fun markDownloaded(trackId: String, filePath: String) {
        dao.markDownloaded(trackId, filePath)
    }

    suspend fun markEvicted(trackId: String) {
        dao.markEvicted(trackId)
    }

    suspend fun getNotDownloadedTracks(): List<TrackEntity> =
        dao.getNotDownloadedTracks()

    suspend fun getTracksToEvict(evictBefore: String): List<TrackEntity> =
        dao.getTracksToEvict(evictBefore)
}
