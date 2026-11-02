package ch.arka.projecthub.data

import androidx.room.withTransaction
import ch.arka.projecthub.domain.Category
import ch.arka.projecthub.domain.MilestoneUi
import ch.arka.projecthub.domain.Planner
import ch.arka.projecthub.domain.ProjectUi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

class Repository(private val db: AppDatabase) {
    private val dao = db.dao()

    fun observe(): Flow<List<ProjectUi>> =
        combine(dao.observeProjects(), dao.observeMilestones(), dao.observeTasks()) { projects, milestones, tasks ->
            val tasksByMilestone = tasks.groupBy { it.milestoneId }
            val milestonesByProject = milestones.groupBy { it.projectId }
            projects.map { p ->
                ProjectUi(
                    project = p,
                    milestones = milestonesByProject[p.id].orEmpty().map { m ->
                        MilestoneUi(m, tasksByMilestone[m.id].orEmpty())
                    },
                )
            }
        }

    /** Crea il progetto e lo scompone in milestone/task in un'unica transazione. */
    suspend fun createProject(
        name: String,
        goal: String,
        category: Category,
        deadline: LocalDate,
        today: LocalDate = LocalDate.now(),
    ): Long = db.withTransaction {
        val projectId = dao.insertProject(
            ProjectEntity(
                name = name.trim(),
                goal = goal.trim(),
                category = category.name,
                createdDay = today.toEpochDay(),
                deadlineDay = deadline.toEpochDay(),
            ),
        )
        Planner.plan(category, today, deadline).forEachIndexed { index, planned ->
            val milestoneId = dao.insertMilestone(
                MilestoneEntity(
                    projectId = projectId,
                    title = planned.title,
                    position = index,
                    dueDay = planned.due.toEpochDay(),
                ),
            )
            dao.insertTasks(
                planned.tasks.map { TaskEntity(milestoneId = milestoneId, projectId = projectId, title = it) },
            )
        }
        projectId
    }

    suspend fun setTaskDone(taskId: Long, done: Boolean) = dao.setTaskDone(taskId, done)

    suspend fun deleteProject(id: Long) = dao.deleteProject(id)
}
