package uk.co.undergroundbunker.audiopirate.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import uk.co.undergroundbunker.audiopirate.data.AppDatabase
import uk.co.undergroundbunker.audiopirate.data.ServerEntity
import uk.co.undergroundbunker.audiopirate.data.ServerRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ServerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ServerRepository

    val servers: StateFlow<List<ServerEntity>>

    init {
        val serverDao = AppDatabase.getDatabase(application).serverDao()
        repository = ServerRepository(serverDao)

        servers = repository.getAllServers()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }

    fun addServer(name: String, url: String, password: String = "audiopirate", description: String = "") {
        viewModelScope.launch {
            repository.insertServer(
                ServerEntity(
                    name = name,
                    url = url,
                    password = password,
                    description = description
                )
            )
        }
    }

    fun updateServer(server: ServerEntity) {
        viewModelScope.launch {
            repository.updateServer(server)
        }
    }

    fun deleteServer(server: ServerEntity) {
        viewModelScope.launch {
            repository.deleteServer(server)
        }
    }

    fun toggleServerActive(server: ServerEntity) {
        viewModelScope.launch {
            repository.updateServer(server.copy(isActive = !server.isActive))
        }
    }
}
