package vn.edu.usth.taskmanagement.domain.repository

import kotlinx.coroutines.flow.Flow
import vn.edu.usth.taskmanagement.domain.model.Workspace

interface WorkspaceRepository {
    fun getWorkspaces(): Flow<List<Workspace>>
    fun getGroupWorkspaces(): Flow<List<Workspace>>
    suspend fun getPersonalWorkspace(): Workspace?
    suspend fun refreshWorkspaces()
    suspend fun createWorkspace(title: String, description: String?): Workspace
    suspend fun updateWorkspace(id: String, title: String?, description: String?): Workspace
    suspend fun deleteWorkspace(id: String)
    suspend fun inviteMember(workspaceId: String, email: String)
    suspend fun leaveWorkspace(workspaceId: String)
    suspend fun getWorkspaceMembers(workspaceId: String): List<vn.edu.usth.taskmanagement.domain.model.Member>
}

