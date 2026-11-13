package ch.arka.projecthub.domain

import ch.arka.projecthub.data.MilestoneEntity
import ch.arka.projecthub.data.ProjectEntity
import ch.arka.projecthub.data.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

class DeadlinesTest {

    private val today: LocalDate = LocalDate.of(2026, 11, 18)
    private var nextId = 1L

    /** Fase che scade tra [dueIn] giorni (negativo = già scaduta) con un task per ogni valore di [done]. */
    private fun milestone(title: String, dueIn: Long, vararg done: Boolean): MilestoneUi {
        val id = nextId++
        return MilestoneUi(
            MilestoneEntity(id = id, projectId = 1, title = title, position = id.toInt(), dueDay = today.plusDays(dueIn).toEpochDay()),
            done.map { TaskEntity(id = nextId++, milestoneId = id, projectId = 1, title = "Task", done = it) },
        )
    }

    private fun project(name: String, vararg milestones: MilestoneUi): ProjectUi {
        val id = nextId++
        return ProjectUi(
            ProjectEntity(
                id = id,
                name = name,
                goal = "",
                category = Category.SOFTWARE.name,
                createdDay = today.minusDays(30).toEpochDay(),
                deadlineDay = today.plusDays(30).toEpochDay(),
            ),
            milestones.toList(),
        )
    }

    private fun titles(alerts: List<DeadlineAlert>) = alerts.map { it.milestoneTitle }

    @Test
    fun noProjects_noAlerts() {
        assertTrue(deadlineAlerts(emptyList(), today).isEmpty)
    }

    @Test
    fun dueSoon_coversTodayUpToThreeDays() {
        val alerts = deadlineAlerts(
            listOf(
                project(
                    "P",
                    milestone("oggi", 0, false),
                    milestone("tra 3", 3, false),
                    milestone("tra 4", 4, false),
                ),
            ),
            today,
        )
        assertEquals(listOf("oggi", "tra 3"), titles(alerts.dueSoon))
        assertEquals(listOf(0L, 3L), alerts.dueSoon.map { it.daysLeft })
        assertTrue(alerts.overdue.isEmpty())
    }

    @Test
    fun overdue_isSeparateFromDueSoon() {
        val alerts = deadlineAlerts(
            listOf(project("P", milestone("ieri", -1, false), milestone("domani", 1, false))),
            today,
        )
        assertEquals(listOf("ieri"), titles(alerts.overdue))
        assertEquals(-1L, alerts.overdue.single().daysLeft)
        assertEquals(listOf("domani"), titles(alerts.dueSoon))
    }

    @Test
    fun completedMilestones_neverAlert() {
        val alerts = deadlineAlerts(
            listOf(project("P", milestone("scaduta ma fatta", -5, true, true), milestone("in scadenza ma fatta", 1, true))),
            today,
        )
        assertTrue(alerts.isEmpty)
    }

    @Test
    fun partiallyDoneMilestone_stillAlerts() {
        val alerts = deadlineAlerts(listOf(project("P", milestone("a metà", 2, true, false))), today)
        assertEquals(listOf("a metà"), titles(alerts.dueSoon))
    }

    @Test
    fun milestoneWithoutTasks_doesNotAlert() {
        // niente task = niente da completare: non deve generare un avviso che non si può spegnere
        val alerts = deadlineAlerts(listOf(project("P", milestone("vuota", -2), milestone("vuota 2", 1))), today)
        assertTrue(alerts.isEmpty)
    }

    @Test
    fun alerts_spanAllProjects_sortedByDueDate() {
        val alerts = deadlineAlerts(
            listOf(
                project("A", milestone("A tra 3", 3, false), milestone("A da 1", -1, false)),
                project("B", milestone("B oggi", 0, false), milestone("B da 10", -10, false)),
            ),
            today,
        )
        assertEquals(listOf("B oggi", "A tra 3"), titles(alerts.dueSoon))
        assertEquals(listOf("B da 10", "A da 1"), titles(alerts.overdue))
        assertEquals(listOf("B", "A"), alerts.dueSoon.map { it.projectName })
    }

    @Test
    fun soonDays_isConfigurable() {
        val projects = listOf(project("P", milestone("tra 5", 5, false)))
        assertTrue(deadlineAlerts(projects, today).isEmpty)
        assertEquals(listOf("tra 5"), titles(deadlineAlerts(projects, today, soonDays = 7).dueSoon))
    }

    @Test
    fun delayUntilNextCheck_beforeNineIsToday_afterIsTomorrow() {
        val day = LocalDate.of(2026, 11, 18)
        assertEquals(Duration.ofMinutes(90), delayUntilNextCheck(LocalDateTime.of(day, java.time.LocalTime.of(7, 30))))
        assertEquals(Duration.ofHours(24), delayUntilNextCheck(LocalDateTime.of(day, java.time.LocalTime.of(9, 0))))
        assertEquals(Duration.ofHours(10), delayUntilNextCheck(LocalDateTime.of(day, java.time.LocalTime.of(23, 0))))
    }

    @Test
    fun delayUntilNextCheck_isAlwaysPositiveAndAtMostOneDay() {
        val start = LocalDateTime.of(2026, 11, 18, 0, 0)
        (0 until 24 * 60 step 7).forEach { minutes ->
            val delay = delayUntilNextCheck(start.plusMinutes(minutes.toLong()))
            assertTrue("$minutes", !delay.isZero && !delay.isNegative && delay <= Duration.ofDays(1))
        }
    }
}
