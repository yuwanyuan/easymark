package com.easymd.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.easymd.data.Note
import com.easymd.data.NoteRepository
import com.easymd.data.attachment.AttachmentManager
import com.easymd.data.storage.NoteLibrary
import com.easymd.data.storage.StorageConfig
import com.easymd.data.storage.StorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NoteListViewModel(application: Application) : AndroidViewModel(application) {

    private val storageManager = StorageManager(application.applicationContext)
    private var repository = NoteRepository(application.applicationContext)
    private var attachmentManager = AttachmentManager(application.applicationContext, storageManager.getActiveLibraryId())

    private var allLibrariesRepository: NoteRepository? = null

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes

    private val _allNotes = MutableStateFlow<List<Note>>(emptyList())

    private val _tags = MutableStateFlow<List<String>>(emptyList())
    val tags: StateFlow<List<String>> = _tags

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _filterTag = MutableStateFlow<String?>(null)
    val filterTag: StateFlow<String?> = _filterTag

    private val _libraries = MutableStateFlow<List<NoteLibrary>>(emptyList())
    val libraries: StateFlow<List<NoteLibrary>> = _libraries

    private val _activeLibrary = MutableStateFlow<NoteLibrary?>(null)
    val activeLibrary: StateFlow<NoteLibrary?> = _activeLibrary

    private val _totalNoteCount = MutableStateFlow(0)
    val totalNoteCount: StateFlow<Int> = _totalNoteCount

    private val _totalAttachmentCount = MutableStateFlow(0)
    val totalAttachmentCount: StateFlow<Int> = _totalAttachmentCount

    init {
        loadLibraries()
        loadNotes()
    }

    private fun loadLibraries() {
        val realLibraries = storageManager.getLibraries()
        _libraries.value = realLibraries
        _activeLibrary.value = resolveActiveLibrary()
    }

    private fun resolveActiveLibrary(): NoteLibrary? {
        val activeId = storageManager.getActiveLibraryId()
        return if (activeId == ALL_LIBRARIES_ID) {
            ALL_LIBRARIES_VIRTUAL
        } else {
            storageManager.getActiveLibrary()
        }
    }

    private fun loadNotes() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val isAllLibraries = storageManager.getActiveLibraryId() == ALL_LIBRARIES_ID
                if (isAllLibraries) {
                    val allNotes = mutableListOf<Note>()
                    var totalAttach = 0
                    val allTags = mutableSetOf<String>()
                    val realLibraries = storageManager.getLibraries()
                    for (lib in realLibraries) {
                        val libRepo = NoteRepository(getApplication<Application>().applicationContext, lib.storageConfig)
                        val libNotes = libRepo.getAllNotes()
                        allNotes.addAll(libNotes)
                        val libAttachManager = AttachmentManager(getApplication<Application>().applicationContext, lib.id)
                        libNotes.forEach { note ->
                            totalAttach += libAttachManager.getEntriesForNote(note.id).size
                        }
                        allTags.addAll(libRepo.getAllTags())
                    }
                    _allNotes.value = allNotes.sortedByDescending { it.updatedAt }
                    _totalNoteCount.value = allNotes.size
                    _totalAttachmentCount.value = totalAttach
                    _tags.value = allTags.toList().sorted()
                } else {
                    _allNotes.value = repository.getAllNotes()
                    _totalNoteCount.value = _allNotes.value.size

                    var totalAttach = 0
                    _allNotes.value.forEach { note ->
                        totalAttach += attachmentManager.getEntriesForNote(note.id).size
                    }
                    _totalAttachmentCount.value = totalAttach

                    _tags.value = repository.getAllTags()
                }
            }
            applyFilter()
        }
    }

    fun refresh() {
        loadLibraries()
        val activeId = storageManager.getActiveLibraryId()
        if (activeId != ALL_LIBRARIES_ID) {
            val activeLib = storageManager.getActiveLibrary()
            repository = NoteRepository(getApplication<Application>().applicationContext, activeLib.storageConfig)
            attachmentManager = AttachmentManager(getApplication<Application>().applicationContext, activeLib.id)
        }
        loadNotes()
    }

    fun switchLibrary(libraryId: String) {
        storageManager.setActiveLibraryId(libraryId)
        if (libraryId == ALL_LIBRARIES_ID) {
            _activeLibrary.value = ALL_LIBRARIES_VIRTUAL
            _searchQuery.value = ""
            loadNotes()
        } else {
            val lib = _libraries.value.find { it.id == libraryId }
            if (lib != null) {
                _activeLibrary.value = lib
                repository.switchStorage(lib.storageConfig)
                _searchQuery.value = ""
                loadNotes()
            }
        }
    }

    fun addLibrary(library: NoteLibrary) {
        storageManager.addLibrary(library)
        loadLibraries()
    }

    fun deleteLibrary(libraryId: String) {
        storageManager.deleteLibrary(libraryId)
        loadLibraries()
        val activeId = storageManager.getActiveLibraryId()
        if (activeId == ALL_LIBRARIES_ID) {
            _activeLibrary.value = ALL_LIBRARIES_VIRTUAL
        } else {
            val activeLib = storageManager.getActiveLibrary()
            _activeLibrary.value = activeLib
            repository.switchStorage(activeLib.storageConfig)
        }
        loadNotes()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        _filterTag.value = null
        applyFilter()
    }

    fun setFilterTag(tag: String?) {
        _filterTag.value = tag
        _searchQuery.value = ""
        applyFilter()
    }

    private fun applyFilter() {
        var result = _allNotes.value

        val filterTag = _filterTag.value
        if (filterTag != null) {
            result = result.filter { filterTag in it.tags }
        }

        val query = _searchQuery.value
        if (query.isNotBlank()) {
            val lowerQuery = query.lowercase()
            result = result.filter { note ->
                note.title.lowercase().contains(lowerQuery) ||
                        note.content.lowercase().contains(lowerQuery) ||
                        note.tags.any { it.lowercase().contains(lowerQuery) }
            }
        }

        _notes.value = result
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (storageManager.getActiveLibraryId() == ALL_LIBRARIES_ID) {
                    val realLibraries = storageManager.getLibraries()
                    for (lib in realLibraries) {
                        val libRepo = NoteRepository(getApplication<Application>().applicationContext, lib.storageConfig)
                        val note = libRepo.getNoteById(noteId)
                        if (note != null) {
                            libRepo.deleteNote(noteId)
                            break
                        }
                    }
                } else {
                    repository.deleteNote(noteId)
                }
            }
            loadNotes()
        }
    }

    fun deleteNotes(noteIds: List<String>) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (storageManager.getActiveLibraryId() == ALL_LIBRARIES_ID) {
                    val realLibraries = storageManager.getLibraries()
                    for (noteId in noteIds) {
                        for (lib in realLibraries) {
                            val libRepo = NoteRepository(getApplication<Application>().applicationContext, lib.storageConfig)
                            val note = libRepo.getNoteById(noteId)
                            if (note != null) {
                                libRepo.deleteNote(noteId)
                                break
                            }
                        }
                    }
                } else {
                    noteIds.forEach { repository.deleteNote(it) }
                }
            }
            loadNotes()
        }
    }

    companion object {
        const val ALL_LIBRARIES_ID = "__all_libraries__"
        val ALL_LIBRARIES_VIRTUAL = NoteLibrary(
            id = ALL_LIBRARIES_ID,
            storageConfig = StorageConfig()
        )
    }
}
