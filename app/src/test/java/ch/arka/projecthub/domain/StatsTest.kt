package ch.arka.projecthub.domain

import ch.arka.projecthub.data.MilestoneEntity
import ch.arka.projecthub.data.ProjectEntity
import ch.arka.projecthub.data.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class StatsTest {

    // Data fissa: mercoledì 18 novembre 2026.
    private val today: LocalDate = LocalDate.of(2026, 11, 18)
    private var nextId = 1L

    private fun daysAgo(n: Long): LocalDate = today.minusDays(n)

    /** Progetto con un task per ogni data: una data = fatto quel giorno, null = non fatto. */
    private fun project(category: Category, vararg completedOn: LocalDate?): ProjectUi =
        projectOf(category, completedOn.map { day -> task(done = day != null, completedOn = day) })

    private fun task(done: Boolean, completedOn: LocalDate?) = TaskEntity(
        id = nextId++,
        milestoneId = 1,
        projectId = 1,
        title = "Task",
        done = done,
        completedAtDay = completedOn?.toEpochDay(),
    )

    private fun projectOf(category: Category, tasks: List<TaskEntity>): ProjectUi {
        val id = nextId++
        return ProjectUi(
            project = ProjectEntity(
                id = id,
                name = "Progetto $id",
                goal = "",
                category = category.name,
                createdDay = today.toEpochDay(),
                deadlineDay = today.plusDays(60).toEpochDay(),
            ),
            milestones = listOf(
                MilestoneUi(
                    MilestoneEntity(id = id, projectId = id, title = "Fase", position = 0, dueDay = today.toEpochDay()),
                    tasks,
                ),
            ),
        )
    }

    @Test
    fun today_isWednesday() {
        // i test su settimana e mese dipendono da questo
        assertEquals(DayOfWeek.WEDNESDAY, today.dayOfWeek)
    }

    @Test
    fun streak_noCompletions_isZero() {
        assertEquals(0, currentStreak(emptyList(), today))
    }

    @Test
    fun streak_countsConsecutiveDaysEndingToday() {
        assertEquals(1, currentStreak(listOf(today), today))
        assertEquals(3, currentStreak(listOf(today, daysAgo(1), daysAgo(2)), today))
    }

    @Test
    fun streak_staysAliveUntilEndOfToday() {
        // oggi ancora niente: vale la serie che arriva fino a ieri
        assertEquals(1, currentStreak(listOf(daysAgo(1)), today))
        assertEquals(2, currentStreak(listOf(daysAgo(1), daysAgo(2)), today))
    }

    @Test
    fun streak_brokenByAFullDayWithoutTasks() {
        assertEquals(0, currentStreak(listOf(daysAgo(2), daysAgo(3)), today))
        // il buco a tre giorni fa ferma il conteggio
        assertEquals(3, currentStreak(listOf(today, daysAgo(1), daysAgo(2), daysAgo(4), daysAgo(5)), today))
    }

    @Test
    fun streak_severalTasksOnTheSameDayCountOnce() {
        assertEquals(2, currentStreak(listOf(today, today, today, daysAgo(1), daysAgo(1)), today))
    }

    @Test
    fun streak_ignoresDatesInTheFuture() {
        assertEquals(0, currentStreak(listOf(today.plusDays(1), today.plusDays(2)), today))
        assertEquals(1, currentStreak(listOf(today, today.plusDays(1)), today))
    }

    @Test
    fun streak_crossesMonthAndYearBoundaries() {
        val newYear = LocalDate.of(2027, 1, 1)
        val days = (0L..3L).map { newYear.minusDays(it) }
        assertEquals(4, currentStreak(days, newYear))
    }

    @Test
    fun statsOf_noProjects_isAllZero() {
        val stats = statsOf(emptyList(), today)
        assertEquals(Stats(streak = 0, doneToday = 0, doneThisWeek = 0, doneThisMonth = 0, byCategory = emptyList()), stats)
        assertFalse(stats.streakAtRisk)
    }

    @Test
    fun statsOf_weekStartsOnMonday() {
        // oggi è mercoledì: lunedì e martedì contano, domenica scorsa no
        val monday = daysAgo(2)
        val sunday = daysAgo(3)
        val stats = statsOf(listOf(project(Category.SOFTWARE, today, daysAgo(1), monday, sunday)), today)
        assertEquals(DayOfWeek.MONDAY, monday.dayOfWeek)
        assertEquals(3, stats.doneThisWeek)
        assertEquals(4, stats.doneThisMonth)
        assertEquals(1, stats.doneToday)
    }

    @Test
    fun statsOf_onMonday_weekIsOnlyToday() {
        val monday = LocalDate.of(2026, 11, 16)
        val stats = statsOf(listOf(project(Category.SOFTWARE, monday, monday.minusDays(1))), monday)
        assertEquals(1, stats.doneThisWeek)
        assertEquals(2, stats.doneThisMonth)
    }

    @Test
    fun statsOf_monthStartsOnTheFirst() {
        val first = LocalDate.of(2026, 11, 1)
        val lastOfOctober = LocalDate.of(2026, 10, 31)
        val stats = statsOf(listOf(project(Category.SOFTWARE, first, lastOfOctober)), today)
        assertEquals(1, stats.doneThisMonth)
        assertEquals(0, stats.doneThisWeek)
    }

    @Test
    fun statsOf_streakAtRiskOnlyWhenNothingDoneToday() {
        val untilYesterday = statsOf(listOf(project(Category.SOFTWARE, daysAgo(1), daysAgo(2))), today)
        assertEquals(2, untilYesterday.streak)
        assertTrue(untilYesterday.streakAtRisk)

        val withToday = statsOf(listOf(project(Category.SOFTWARE, today, daysAgo(1))), today)
        assertEquals(2, withToday.streak)
        assertFalse(withToday.streakAtRisk)
    }

    @Test
    fun statsOf_combinesTasksOfAllProjects() {
        val a = project(Category.SOFTWARE, today, null)
        val b = project(Category.SCUOLA, daysAgo(1), daysAgo(2))
        val stats = statsOf(listOf(a, b), today)
        assertEquals(3, stats.streak)
        assertEquals(3, stats.doneThisWeek)
    }

    @Test
    fun statsOf_doneTaskWithoutDate_countsOnlyInCategoryTotals() {
        // task spuntato con lo schema v1, prima che la data venisse registrata
        val legacy = projectOf(Category.HARDWARE, listOf(task(done = true, completedOn = null), task(done = false, completedOn = null)))
        val stats = statsOf(listOf(legacy), today)
        assertEquals(0, stats.streak)
        assertEquals(0, stats.doneThisMonth)
        assertEquals(listOf(CategoryStats(Category.HARDWARE, done = 1, total = 2)), stats.byCategory)
    }

    @Test
    fun statsOf_ignoresDateOfTaskNoLongerDone() {
        // difensivo: una data rimasta su un task non più spuntato non deve contare
        val stale = projectOf(Category.SOFTWARE, listOf(task(done = false, completedOn = today)))
        val stats = statsOf(listOf(stale), today)
        assertEquals(0, stats.streak)
        assertEquals(0, stats.doneToday)
    }

    @Test
    fun statsOf_byCategory_sumsProjectsAndKeepsEnumOrder() {
        val stats = statsOf(
            listOf(
                project(Category.SOFTWARE, today, null, null),
                project(Category.SCUOLA, null, null),
                project(Category.SOFTWARE, today),
            ),
            today,
        )
        assertEquals(
            listOf(
                CategoryStats(Category.SCUOLA, done = 0, total = 2),
                CategoryStats(Category.SOFTWARE, done = 2, total = 4),
            ),
            stats.byCategory,
        )
        assertEquals(0.5f, stats.byCategory[1].progress, 1e-6f)
    }
}
