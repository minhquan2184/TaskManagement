package vn.edu.usth.taskmanagement.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import vn.edu.usth.taskmanagement.data.local.WorkspaceEntity

@Dao
interface WorkspaceDao {
    @Query("SELECT * FROM workspaces ORDER BY isPersonal DESC")
    fun getAllWorkspaces(): Flow<List<WorkspaceEntity>>

    @Query("SELECT * FROM workspaces WHERE isPersonal = :isPersonal LIMIT 1")
    suspend fun getPersonalWorkspace(isPersonal: Boolean = true): WorkspaceEntity?

    @Query("SELECT * FROM workspaces WHERE isPersonal = :isPersonal")
    fun getGroupWorkspaces(isPersonal: Boolean = false): Flow<List<WorkspaceEntity>>

    @Query("SELECT * FROM workspaces WHERE id = :id")
    suspend fun getWorkspaceById(id: String): WorkspaceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(workspaces: List<WorkspaceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(workspace: WorkspaceEntity)

    @Query("DELETE FROM workspaces")
    suspend fun deleteAll()

    @Query("DELETE FROM workspaces WHERE id = :id")
    suspend fun deleteById(id: String)
}

