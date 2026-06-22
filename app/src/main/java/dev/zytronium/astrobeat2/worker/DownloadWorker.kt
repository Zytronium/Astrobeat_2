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
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Named

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: TrackRepository,
    @Named("download") private val okHttpClient: OkHttpClient
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                val tracksToDownload = repository.getNotDownloadedTracks()

                val tracksDir = File(applicationContext.filesDir, "tracks").apply {
                    if (!exists()) mkdirs()
                }

                for (track in tracksToDownload) {
                    try {
                        val streamUrl = repository.getStreamUrl(track.id)
                        val destFile = File(tracksDir, "${track.id}.mp3")

                        downloadFile(streamUrl, destFile)

                        repository.markDownloaded(track.id, destFile.absolutePath)
                    } catch (e: Exception) {
                        // -------- skip this track, continue with the rest --------
                        continue
                    }
                }

                Result.success()
            } catch (e: Exception) {
                Result.retry()
            }
        }
    }

    private fun downloadFile(url: String, destFile: File) {
        val request = Request.Builder().url(url).build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Download failed: ${response.code}")
            }

            response.body?.byteStream()?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: throw IllegalStateException("Empty response body")
        }
    }

    companion object {
        const val WORK_NAME = "download_tracks_work"
    }
}
