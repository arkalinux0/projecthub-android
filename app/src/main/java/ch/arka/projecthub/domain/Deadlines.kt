package ch.arka.projecthub.domain

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/** Giorni di anticipo con cui una fase non completata viene segnalata. */
const val DUE_SOON_DAYS = 3L

/** Ora del controllo giornaliero delle scadenze. */
const val DAILY_CHECK_HOUR = 9

/** Una fase con task ancora da fare, in scadenza o già scaduta. */
data class DeadlineAlert(
    val projectId: Long,
    val projectName: String,
    val milestoneTitle: String,
    val due: LocalDate,
    /** Giorni alla scadenza: 0 = oggi, negativo = scaduta da quel numero di giorni. */
    val daysLeft: Long,
)

data class DeadlineAlerts(
    /** Scadenza tra oggi e [DUE_SOON_DAYS] giorni. */
    val dueSoon: List<DeadlineAlert>,
    /** Scadenza già passata. */
    val overdue: List<DeadlineAlert>,
) {
    val isEmpty: Boolean get() = dueSoon.isEmpty() && overdue.isEmpty()
}

/**
 * Fasi da segnalare: solo quelle con almeno un task non fatto. Una fase senza task
 * o completata non genera avvisi, nemmeno se scaduta. Ordinate dalla più urgente.
 */
fun deadlineAlerts(
    projects: List<ProjectUi>,
    today: LocalDate = LocalDate.now(),
    soonDays: Long = DUE_SOON_DAYS,
): DeadlineAlerts {
    val open = projects.flatMap { p ->
        p.milestones
            .filter { it.done < it.total }
            .map { m ->
                DeadlineAlert(
                    projectId = p.project.id,
                    projectName = p.project.name,
                    milestoneTitle = m.milestone.title,
                    due = m.due,
                    daysLeft = ChronoUnit.DAYS.between(today, m.due),
                )
            }
    }.sortedBy { it.due }
    return DeadlineAlerts(
        dueSoon = open.filter { it.daysLeft in 0..soonDays },
        overdue = open.filter { it.daysLeft < 0 },
    )
}

/** Tempo da [now] al prossimo controllo delle [hour]:00 (oggi se non è ancora passato, altrimenti domani). */
fun delayUntilNextCheck(now: LocalDateTime, hour: Int = DAILY_CHECK_HOUR): Duration {
    val todayAt = now.toLocalDate().atTime(hour, 0)
    val next = if (now.isBefore(todayAt)) todayAt else todayAt.plusDays(1)
    return Duration.between(now, next)
}
