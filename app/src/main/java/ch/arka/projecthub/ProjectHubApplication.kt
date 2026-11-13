package ch.arka.projecthub

import android.app.Application
import ch.arka.projecthub.notify.DeadlineNotifications
import ch.arka.projecthub.notify.DeadlineWorker

class ProjectHubApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // I canali vanno creati prima di qualsiasi notifica; il controllo giornaliero
        // si pianifica qui e non nell'activity perché deve esistere anche se l'app
        // viene avviata dal sistema solo per eseguire il worker.
        DeadlineNotifications.createChannels(this)
        DeadlineWorker.schedule(this)
    }
}
