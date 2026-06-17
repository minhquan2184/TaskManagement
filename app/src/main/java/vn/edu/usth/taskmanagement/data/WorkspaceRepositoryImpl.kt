package vn.edu.usth.taskmanagement.data

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import vn.edu.usth.taskmanagement.service.WorkspaceDao
import vn.edu.usth.taskmanagement.service.TaskDao
import vn.edu.usth.taskmanagement.data.toDomain
import vn.edu.usth.taskmanagement.data.toEntity
import vn.edu.usth.taskmanagement.api.WorkspaceRemoteDataSource
import vn.edu.usth.taskmanagement.domain.model.Workspace
import vn.edu.usth.taskmanagement.domain.model.Member
import vn.edu.usth.taskmanagement.domain.repository.WorkspaceRepository

class WorkspaceRepositoryImpl(
    private val remoteDataSource: WorkspaceRemoteDataSource,
    private val workspaceDao: WorkspaceDao,
    private val taskDao: TaskDao
) : WorkspaceRepository {

    override fun getWorkspaces(): Flow<List<Workspace>> {
        return workspaceDao.getAllWorkspaces().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getGroupWorkspaces(): Flow<List<Workspace>> {
        return workspaceDao.getGroupWorkspaces().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getPersonalWorkspace(): Workspace? {
        return workspaceDao.getPersonalWorkspace()?.toDomain()
    }

    override suspend fun refreshWorkspaces() {
        try {
            val remoteDtos = remoteDataSource.getWorkspaces()
            val entities = remoteDtos.map { it.toEntity() }
            workspaceDao.deleteAll()
            workspaceDao.insertAll(entities)
        } catch (e: Exception) {
            Log.e("WorkspaceRepo", "Failed to refresh workspaces: ${e.message}")
        }
    }

    override suspend fun createWorkspace(title: String, description: String?): Workspace {
        val dto = remoteDataSource.createWorkspace(title, description)
        workspaceDao.insert(dto.toEntity())
        return dto.toDomain()
    }

    override suspend fun updateWorkspace(id: String, title: String?, description: String?): Workspace {
        val dto = remoteDataSource.updateWorkspace(id, title, description)
        workspaceDao.insert(dto.toEntity())
        return dto.toDomain()
    }

    override suspend fun deleteWorkspace(id: String) {
        remoteDataSource.deleteWorkspace(id)
        taskDao.deleteByWorkspace(id)
        workspaceDao.deleteById(id)
    }

    override suspend fun inviteMember(workspaceId: String, email: String) {
        remoteDataSource.inviteMember(workspaceId, email)
    }

    override suspend fun leaveWorkspace(workspaceId: String) {
        remoteDataSource.leaveWorkspace(workspaceId)
        taskDao.deleteByWorkspace(workspaceId)
        workspaceDao.deleteById(workspaceId)
    }

    override suspend fun getWorkspaceMembers(workspaceId: String): List<Member> {
        return try {
            val dto = remoteDataSource.getWorkspaceById(workspaceId)
            dto.members?.map { 
                Member(it.userId, it.email, it.fullName, it.avatarUrl, it.role) 
            } ?: emptyList()
        } catch (e: Exception) {
            Log.e("WorkspaceRepo", "Failed to get members: ${e.message}")
            emptyList()
        }
    }
}

