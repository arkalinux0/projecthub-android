package ch.arka.projecthub.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ch.arka.projecthub.data.AppDatabase
import ch.arka.projecthub.data.Repository
import ch.arka.projecthub.domain.deadlineAlerts
import ch.arka.projecthub.domain.delayUntilNextCheck
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/** Controllo giornaliero: legge i progetti dal database locale e aggiorna le notifiche di scadenza. */
class DeadlineWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val projects = Repository(AppDatabase.get(applicationContext)).observe().first()
        DeadlineNotifications.show(applicationContext, deadlineAlerts(projects, LocalDate.now()))
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "deadline-check"

        /**
         * Pianifica il controllo una volta al giorno, la prima volta alle prossime 9:00.
         * KEEP: se è già pianificato non lo tocca, così riaprire l'app non sposta l'orario.
         * Nessun vincolo di rete: l'app non ne ha bisogno.
         */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<DeadlineWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delayUntilNextCheck(LocalDateTime.now()).toMillis(), TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
