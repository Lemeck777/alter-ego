package com.alterego.app.core.scheduler

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.alterego.app.core.content.ContentRepository
import com.alterego.app.core.datastore.UserPreferencesRepository
import com.alterego.app.core.network.ContentApi
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

/**
 * Pulls a newer content bundle from the backend when one exists.
 *
 * This is how a corrected scientific claim reaches users without an app release: the evidence
 * database is server-owned, and the app only ever renders what the curated bundle says.
 */
@HiltWorker
class ContentSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val api: ContentApi,
    private val content: ContentRepository,
    private val prefs: UserPreferencesRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = try {
        val installed = prefs.snapshot().contentVersion
        val response = api.bundle(since = installed)
        val body = response.body()
        when {
            response.code() == 304 -> Result.success()
            response.isSuccessful && body != null && body.version > installed -> {
                content.install(body)
                Result.success()
            }
            response.isSuccessful -> Result.success()
            else -> Result.retry()
        }
    } catch (e: CancellationException) {
        // Never swallow cancellation: WorkManager stopping this job must actually stop it.
        throw e
    } catch (e: Exception) {
        // Offline is a normal state for this app; try again later rather than surfacing an error.
        Result.retry()
    }

    companion object { const val PERIODIC_NAME = "content_sync" }
}
