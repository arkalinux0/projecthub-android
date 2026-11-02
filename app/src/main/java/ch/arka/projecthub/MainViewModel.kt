package ch.arka.projecthub

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ch.arka.projecthub.data.AppDatabase
import ch.arka.projecthub.data.Repository
import ch.arka.projecthub.domain.Category
import ch.arka.projecthub.domain.ProjectUi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository(AppDatabase.get(app))

    val projects: StateFlow<List<ProjectUi>> = repo.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun create(name: String, goal: String, category: Category, deadline: LocalDate, onCreated: (Long) -> Unit) {
        viewModelScope.launch { onCreated(repo.createProject(name, goal, category, deadline)) }
    }

    fun setTaskDone(taskId: Long, done: Boolean) {
        viewModelScope.launch { repo.setTaskDone(taskId, done) }
    }

    fun delete(projectId: Long) {
        viewModelScope.launch { repo.deleteProject(projectId) }
    }
}
