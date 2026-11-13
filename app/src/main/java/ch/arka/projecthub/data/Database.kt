package ch.arka.projecthub.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

    @Query("UPDATE tasks SET done = :done, completedAtDay = :completedAtDay WHERE id = :id")
    suspend fun setTaskDone(id: Long, done: Boolean, completedAtDay: Long?)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProject(id: Long)
}

@Database(
    entities = [ProjectEntity::class, MilestoneEntity::class, TaskEntity::class],
    version = 2,
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
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }

        /**
         * v2 aggiunge la data di completamento dei task (streak e statistiche).
         * I task già spuntati restano spuntati ma senza data: non c'è modo di
         * ricostruire quando sono stati fatti.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN completedAtDay INTEGER")
            }
        }
    }
}
