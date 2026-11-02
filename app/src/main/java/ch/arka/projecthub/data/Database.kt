package ch.arka.projecthub.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY deadlineDay ASC")
    fun observeProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM milestones ORDER BY projectId, position")
    fun observeMilestones(): Flow<List<MilestoneEntity>>

    @Query("SELECT * FROM tasks ORDER BY id")
    fun observeTasks(): Flow<List<TaskEntity>>

    @Insert
    suspend fun insertProject(project: ProjectEntity): Long

    @Insert
    suspend fun insertMilestone(milestone: MilestoneEntity): Long

    @Insert
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Query("UPDATE tasks SET done = :done WHERE id = :id")
    suspend fun setTaskDone(id: Long, done: Boolean)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProject(id: Long)
}

@Database(
    entities = [ProjectEntity::class, MilestoneEntity::class, TaskEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): ProjectDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "projecthub.db",
                ).build().also { instance = it }
            }
    }
}
