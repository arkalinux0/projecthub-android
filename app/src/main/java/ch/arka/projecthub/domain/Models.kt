package ch.arka.projecthub.domain

import ch.arka.projecthub.data.MilestoneEntity
import ch.arka.projecthub.data.ProjectEntity
import ch.arka.projecthub.data.TaskEntity
import java.time.LocalDate
import java.time.temporal.ChronoUnit

fun ratio(done: Int, total: Int): Float = if (total == 0) 0f else done.toFloat() / total

data class MilestoneUi(
    val milestone: MilestoneEntity,
    val tasks: List<TaskEntity>,
) {
    val done: Int get() = tasks.count { it.done }
    val total: Int get() = tasks.size
    val progress: Float get() = ratio(done, total)
    val isComplete: Boolean get() = total > 0 && done == total
    val due: LocalDate get() = LocalDate.ofEpochDay(milestone.dueDay)
}

data class ProjectUi(
    val project: ProjectEntity,
    val milestones: List<MilestoneUi>,
) {
    val category: Category get() = Category.fromName(project.category)
    val doneTasks: Int get() = milestones.sumOf { it.done }
    val totalTasks: Int get() = milestones.sumOf { it.total }
    val progress: Float get() = ratio(doneTasks, totalTasks)
    val deadline: LocalDate get() = LocalDate.ofEpochDay(project.deadlineDay)
    val isComplete: Boolean get() = totalTasks > 0 && doneTasks == totalTasks

    /** Prossimo milestone non completato. */
    val nextMilestone: MilestoneUi? get() = milestones.firstOrNull { !it.isComplete }

    fun daysLeft(today: LocalDate = LocalDate.now()): Long =
        ChronoUnit.DAYS.between(today, deadline)
}

/** Progresso complessivo su tutti i task di tutti i progetti. */
fun overallProgress(projects: List<ProjectUi>): Float =
    ratio(projects.sumOf { it.doneTasks }, projects.sumOf { it.totalTasks })
