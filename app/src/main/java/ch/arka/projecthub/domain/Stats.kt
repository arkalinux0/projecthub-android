package ch.arka.projecthub.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Task fatti e totali di tutti i progetti di una categoria. */
data class CategoryStats(val category: Category, val done: Int, val total: Int) {
    val progress: Float get() = ratio(done, total)
}

data class Stats(
    /** Giorni consecutivi con almeno un task completato, vedi [currentStreak]. */
    val streak: Int,
    val doneToday: Int,
    /** Da lunedì a oggi. */
    val doneThisWeek: Int,
    /** Dal primo del mese a oggi. */
    val doneThisMonth: Int,
    /** Solo le categorie che hanno almeno un progetto, nell'ordine dell'enum. */
    val byCategory: List<CategoryStats>,
) {
    /** La serie è ancora viva ma oggi non è stato completato nulla: si interrompe a mezzanotte. */
    val streakAtRisk: Boolean get() = streak > 0 && doneToday == 0
}

/**
 * Giorni consecutivi con almeno un task completato, contati all'indietro da [today].
 * Se oggi non c'è ancora nessun completamento la serie resta quella che arriva fino
 * a ieri: si azzera solo quando passa un giorno intero senza task. Le date nel
 * futuro (orologio spostato) vengono ignorate.
 */
fun currentStreak(completedDays: Collection<LocalDate>, today: LocalDate): Int {
    val days = completedDays.filterTo(HashSet()) { !it.isAfter(today) }
    var day = if (today in days) today else today.minusDays(1)
    var streak = 0
    while (day in days) {
        streak++
        day = day.minusDays(1)
    }
    return streak
}

/**
 * Statistiche calcolate dai task così come sono ora: contano solo quelli ancora
 * spuntati e con una data di completamento. Un task spuntato prima che la data
 * venisse registrata (schema v1) entra nei totali per categoria ma non in serie,
 * settimana e mese.
 */
fun statsOf(projects: List<ProjectUi>, today: LocalDate = LocalDate.now()): Stats {
    val completedDays = projects
        .flatMap { p -> p.milestones.flatMap { it.tasks } }
        .filter { it.done }
        .mapNotNull { it.completedAtDay }
        .map { LocalDate.ofEpochDay(it) }
        .filter { !it.isAfter(today) }

    val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val monthStart = today.withDayOfMonth(1)

    val byCategory = projects.groupBy { it.category }
    return Stats(
        streak = currentStreak(completedDays, today),
        doneToday = completedDays.count { it == today },
        doneThisWeek = completedDays.count { !it.isBefore(weekStart) },
        doneThisMonth = completedDays.count { !it.isBefore(monthStart) },
        byCategory = Category.entries.mapNotNull { category ->
            byCategory[category]?.let { list ->
                CategoryStats(category, list.sumOf { it.doneTasks }, list.sumOf { it.totalTasks })
            }
        },
    )
}
