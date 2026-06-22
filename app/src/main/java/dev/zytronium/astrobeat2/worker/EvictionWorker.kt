package dev.zytronium.astrobeat2.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.zytronium.astrobeat2.data.repository.TrackRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit

@HiltWorker
class EvictionWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: TrackRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                val evictBefore = Instant.now().minus(90, ChronoUnit.DAYS).toString()
                val tracksToEvict = repository.getTracksToEvict(evictBefore)

                for (track in tracksToEvict) {
                    track.localFilePath?.let { path ->
                        val file = File(path)
                        if (file.exists()) {
                            file.delete()
                        }
                    }
                    repository.markEvicted(track.id)
                }

                Result.success()
            } catch (e: Exception) {
                Result.retry()
            }
        }
    }

    companion object {
        const val WORK_NAME = "evict_tracks_work"
    }
}
