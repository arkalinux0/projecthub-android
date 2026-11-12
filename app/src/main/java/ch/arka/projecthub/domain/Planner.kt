package ch.arka.projecthub.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

data class PlannedMilestone(
    val title: String,
    val due: LocalDate,
    val tasks: List<String>,
)

/**
 * Scompone un progetto in milestone usando il modello della categoria.
 *
 * Regole (deterministiche, nessuna AI):
 *  - un milestone ogni ~30 giorni tra inizio e scadenza, minimo 3, massimo 12:
 *    con scadenze vicine si comprimono le date dei milestone (anche più di uno
 *    nello stesso giorno), non il numero di fasi;
 *  - le fasi del modello vengono distribuite in modo uniforme lungo i milestone:
 *    compaiono sempre la prima, almeno una fase intermedia e l'ultima;
 *  - se i milestone sono più delle fasi, le fasi ripetute prendono il suffisso "parte N";
 *  - le scadenze dei milestone sono equidistanti, l'ultima coincide con la scadenza finale.
 */
object Planner {
    const val MIN_MILESTONES = 3
    const val MAX_MILESTONES = 12
    private const val DAYS_PER_MILESTONE = 30

    fun milestoneCount(start: LocalDate, deadline: LocalDate): Int {
        val days = ChronoUnit.DAYS.between(start, deadline).coerceAtLeast(1)
        return ((days + DAYS_PER_MILESTONE - 1) / DAYS_PER_MILESTONE).toInt().coerceIn(MIN_MILESTONES, MAX_MILESTONES)
    }

    fun plan(category: Category, start: LocalDate, deadline: LocalDate): List<PlannedMilestone> {
        require(!deadline.isBefore(start)) { "La scadenza non può precedere l'inizio" }
        val n = milestoneCount(start, deadline)
        val days = ChronoUnit.DAYS.between(start, deadline).coerceAtLeast(1)
        val phases = category.phases

        // n >= MIN_MILESTONES = 3: il primo milestone prende sempre la prima fase,
        // l'ultimo l'ultima, e quelli in mezzo fasi intermedie.
        val phaseIndex = (0 until n).map { i ->
            (i * phases.lastIndex.toDouble() / (n - 1)).roundToInt()
        }
        val seen = mutableMapOf<Int, Int>()

        return (0 until n).map { i ->
            val idx = phaseIndex[i]
            val phase = phases[idx]
            val occurrence = (seen[idx] ?: 0) + 1
            seen[idx] = occurrence
            val repeated = phaseIndex.count { it == idx } > 1
            val title = if (repeated) "${phase.title} – parte $occurrence" else phase.title
            val due = if (i == n - 1) deadline else start.plusDays(days * (i + 1) / n)
            PlannedMilestone(title, due, phase.tasks)
        }
    }
}
