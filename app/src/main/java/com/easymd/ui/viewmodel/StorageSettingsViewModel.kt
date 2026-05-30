package com.easymd.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.easymd.data.storage.NoteLibrary
import com.easymd.data.storage.StorageConfig
import com.easymd.data.storage.StorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StorageSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val storageManager = StorageManager(application.applicationContext)

    private val _library = MutableStateFlow<NoteLibrary?>(null)
    val library: StateFlow<NoteLibrary?> = _library

    private val _config = MutableStateFlow(StorageConfig())
    val config: StateFlow<StorageConfig> = _config

    private val _testResult = MutableStateFlow<Boolean?>(null)
    val testResult: StateFlow<Boolean?> = _testResult

    private val _isTesting = MutableStateFlow(false)
    val isTesting: StateFlow<Boolean> = _isTesting

    fun loadLibrary(libraryId: String?) {
        if (libraryId != null) {
            val lib = storageManager.getLibraries().find { it.id == libraryId }
            if (lib != null) {
                _library.value = lib
                _config.value = lib.storageConfig
            }
        } else {
            val activeLib = storageManager.getActiveLibrary()
            _library.value = activeLib
            _config.value = activeLib.storageConfig
        }
    }

    fun updateConfig(newConfig: StorageConfig) {
        _config.value = newConfig
        _library.value = _library.value?.copy(storageConfig = newConfig)
        _testResult.value = null
    }

    fun saveConfig() {
        val lib = _library.value ?: return
        val updatedLib = lib.copy(storageConfig = _config.value)
        if (storageManager.getLibraries().any { it.id == updatedLib.id }) {
            storageManager.updateLibrary(updatedLib)
        } else {
            storageManager.addLibrary(updatedLib)
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            _isTesting.value = true
            _testResult.value = null
            try {
                val result = withContext(Dispatchers.IO) {
                    val storage = storageManager.createStorage(
                        getApplication<Application>().applicationContext,
                        _config.value
                    )
                    storage.testConnection()
                }
                _testResult.value = result
            } catch (e: Exception) {
                _testResult.value = false
            } finally {
                _isTesting.value = false
            }
        }
    }
}
