package uk.co.undergroundbunker.audiopirate.data

import kotlinx.coroutines.flow.Flow

class ServerRepository(private val serverDao: ServerDao) {

    fun getAllServers(): Flow<List<ServerEntity>> = serverDao.getAllServers()

    fun getActiveServers(): Flow<List<ServerEntity>> = serverDao.getActiveServers()

    suspend fun getServerById(id: Long): ServerEntity? = serverDao.getServerById(id)

    suspend fun insertServer(server: ServerEntity): Long = serverDao.insertServer(server)

    suspend fun updateServer(server: ServerEntity) = serverDao.updateServer(server)

    suspend fun deleteServer(server: ServerEntity) = serverDao.deleteServer(server)

    suspend fun deleteServerById(id: Long) = serverDao.deleteServerById(id)
}
