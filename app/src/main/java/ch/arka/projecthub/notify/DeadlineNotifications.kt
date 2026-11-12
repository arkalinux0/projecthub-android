package ch.arka.projecthub.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import ch.arka.projecthub.MainActivity
import ch.arka.projecthub.R
import ch.arka.projecthub.domain.DeadlineAlert
import ch.arka.projecthub.domain.DeadlineAlerts

/**
 * Notifiche locali di scadenza: una per le fasi in scadenza e una, più urgente e
 * su un canale separato, per quelle già scadute. Ognuna elenca tutte le fasi
 * interessate, invece di una notifica per fase.
 */
object DeadlineNotifications {
    private const val CHANNEL_DUE_SOON = "deadline_soon"
    private const val CHANNEL_OVERDUE = "deadline_overdue"
    private const val ID_DUE_SOON = 1
    private const val ID_OVERDUE = 2

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_DUE_SOON, "Fasi in scadenza", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Promemoria per le fasi che scadono entro pochi giorni"
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_OVERDUE, "Fasi scadute", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Avviso per le fasi scadute con task ancora da fare"
            },
        )
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /** Mostra o aggiorna le due notifiche; toglie quella che non ha più nulla da segnalare. */
    fun show(context: Context, alerts: DeadlineAlerts) {
        val manager = NotificationManagerCompat.from(context)
        if (alerts.dueSoon.isEmpty()) manager.cancel(ID_DUE_SOON)
        if (alerts.overdue.isEmpty()) manager.cancel(ID_OVERDUE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        if (alerts.dueSoon.isNotEmpty()) {
            manager.notify(
                ID_DUE_SOON,
                build(
                    context,
                    channel = CHANNEL_DUE_SOON,
                    title = if (alerts.dueSoon.size == 1) "1 fase in scadenza" else "${alerts.dueSoon.size} fasi in scadenza",
                    lines = alerts.dueSoon.map { "${it.label()}: ${dueSoonText(it.daysLeft)}" },
                    priority = NotificationCompat.PRIORITY_DEFAULT,
                ),
            )
        }
        if (alerts.overdue.isNotEmpty()) {
            manager.notify(
                ID_OVERDUE,
                build(
                    context,
                    channel = CHANNEL_OVERDUE,
                    title = if (alerts.overdue.size == 1) "1 fase scaduta" else "${alerts.overdue.size} fasi scadute",
                    lines = alerts.overdue.map { "${it.label()}: ${overdueText(-it.daysLeft)}" },
                    priority = NotificationCompat.PRIORITY_HIGH,
                ),
            )
        }
    }

    private fun build(context: Context, channel: String, title: String, lines: List<String>, priority: Int) =
        NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(lines.first())
            .setStyle(NotificationCompat.InboxStyle().also { style -> lines.forEach(style::addLine) })
            .setPriority(priority)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
            .setAutoCancel(true)
            .build()

    private fun DeadlineAlert.label() = "$projectName · $milestoneTitle"

    private fun dueSoonText(days: Long) = when (days) {
        0L -> "scade oggi"
        1L -> "scade domani"
        else -> "scade tra $days giorni"
    }

    private fun overdueText(days: Long) = if (days == 1L) "scaduta da 1 giorno" else "scaduta da $days giorni"
}
