package ch.arka.projecthub.domain

import ch.arka.projecthub.data.MilestoneEntity
import ch.arka.projecthub.data.ProjectEntity
import ch.arka.projecthub.data.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ModelsTest {

    // Data fissa: i test non devono dipendere da LocalDate.now().
    private val today: LocalDate = LocalDate.of(2026, 3, 10)
    private var nextId = 1L

    /** Milestone con un task per ogni valore di [done] (true = fatto). */
    private fun milestone(vararg done: Boolean): MilestoneUi {
        val id = nextId++
        return MilestoneUi(
            milestone = MilestoneEntity(
                id = id,
                projectId = 1,
                title = "Fase $id",
                position = id.toInt(),
                dueDay = today.toEpochDay(),
            ),
            tasks = done.map { TaskEntity(id = nextId++, milestoneId = id, projectId = 1, title = "Task", done = it) },
        )
    }

    private fun project(vararg milestones: MilestoneUi, deadline: LocalDate = today.plusDays(30)) = ProjectUi(
        project = ProjectEntity(
            id = 1,
            name = "Progetto",
            goal = "",
            category = Category.SOFTWARE.name,
            createdDay = today.toEpochDay(),
            deadlineDay = deadline.toEpochDay(),
        ),
        milestones = milestones.toList(),
    )

    @Test
    fun ratio_zeroTotal_isZeroNotNaN() {
        // assertEquals con delta fallisce anche se il valore è NaN
        assertEquals(0f, ratio(0, 0), 0f)
    }

    @Test
    fun ratio_regularValues() {
        assertEquals(0f, ratio(0, 4), 0f)
        assertEquals(0.25f, ratio(1, 4), 1e-6f)
        assertEquals(1f, ratio(3, 3), 0f)
    }

    @Test
    fun projectWithoutTasks_zeroProgressAndNotComplete() {
        // nessun milestone
        val empty = project()
        assertEquals(0, empty.totalTasks)
        assertEquals(0f, empty.progress, 0f)
        assertFalse(empty.isComplete)
        assertNull(empty.nextMilestone)

        // milestone presenti ma senza task
        val noTasks = project(milestone(), milestone())
        assertEquals(0f, noTasks.progress, 0f)
        assertFalse(noTasks.isComplete)
        noTasks.milestones.forEach {
            assertEquals(0f, it.progress, 0f)
            assertFalse(it.isComplete)
        }
    }

    @Test
    fun milestoneProgress_doneOverTotal() {
        val m = milestone(true, false, false, true)
        assertEquals(2, m.done)
        assertEquals(4, m.total)
        assertEquals(0.5f, m.progress, 1e-6f)
        assertFalse(m.isComplete)
        assertTrue(milestone(true, true).isComplete)
    }

    @Test
    fun projectProgress_countsTasksNotAverageOfMilestones() {
        // 1/1 + 0/3: per task è 1/4, la media delle percentuali darebbe 0.5
        val p = project(milestone(true), milestone(false, false, false))
        assertEquals(1, p.doneTasks)
        assertEquals(4, p.totalTasks)
        assertEquals(0.25f, p.progress, 1e-6f)
    }

    @Test
    fun nextMilestone_isFirstIncompleteInOrder() {
        val first = milestone(true, true)
        val second = milestone(true, false)
        val third = milestone(false)
        assertSame(second, project(first, second, third).nextMilestone)
    }

    @Test
    fun completedProject_isCompleteWithoutNextMilestone() {
        val p = project(milestone(true, true), milestone(true))
        assertTrue(p.isComplete)
        assertEquals(1f, p.progress, 0f)
        assertNull(p.nextMilestone)
    }

    @Test
    fun overallProgress_withoutTasksIsZero() {
        assertEquals(0f, overallProgress(emptyList()), 0f)
        assertEquals(0f, overallProgress(listOf(project(), project(milestone()))), 0f)
    }

    @Test
    fun overallProgress_weightsByTasks() {
        // 1/2 e 0/8: il totale è 1/10, non la media dei due progetti (0.25)
        val a = project(milestone(true, false))
        val b = project(milestone(false, false, false, false), milestone(false, false, false, false))
        assertEquals(0.1f, overallProgress(listOf(a, b)), 1e-6f)
    }

    @Test
    fun daysLeft_signRelativeToDeadline() {
        assertEquals(5L, project(deadline = today.plusDays(5)).daysLeft(today))
        assertEquals(0L, project(deadline = today).daysLeft(today))
        assertEquals(-3L, project(deadline = today.minusDays(3)).daysLeft(today))
    }

    @Test
    fun epochDays_decodedToDates() {
        val p = project(milestone(false), deadline = LocalDate.of(2026, 12, 31))
        assertEquals(LocalDate.of(2026, 12, 31), p.deadline)
        assertEquals(today, p.milestones.single().due)
    }

    @Test
    fun category_fromName() {
        assertEquals(Category.SOFTWARE, project().category)
        Category.entries.forEach { assertEquals(it, Category.fromName(it.name)) }
        // valore sconosciuto (es. categoria rinominata in futuro): ricade su PERSONALE invece di crashare
        assertEquals(Category.PERSONALE, Category.fromName("INESISTENTE"))
    }
}
