package com.easymd.data

import android.content.Context
import android.content.SharedPreferences
import com.easymd.data.attachment.AttachmentCheckResult
import com.easymd.data.attachment.AttachmentEntry
import com.easymd.data.attachment.AttachmentManager
import com.easymd.data.storage.NoteStorage
import com.easymd.data.storage.StorageConfig
import com.easymd.data.storage.StorageManager
import java.util.Date

class NoteRepository(private val context: Context, private val storageConfig: StorageConfig? = null) {

    private val storageManager = StorageManager(context)
    private val activeLibraryId = storageManager.getActiveLibraryId()
    private val attachmentManager = AttachmentManager(context, activeLibraryId)

    private val fileNamePrefs: SharedPreferences =
        context.getSharedPreferences("filename_map${if (activeLibraryId.isNotBlank()) "_$activeLibraryId" else ""}", Context.MODE_PRIVATE)

    private var storage: NoteStorage = if (storageConfig != null) {
        storageManager.createStorage(context, storageConfig)
    } else {
        storageManager.createStorage(context, storageManager.getActiveLibrary().storageConfig)
    }

    private val uuidToFileName = mutableMapOf<String, String>()

    fun switchStorage(config: StorageConfig) {
        storage = storageManager.createStorage(context, config)
        uuidToFileName.clear()
    }

    fun getAttachmentManager(): AttachmentManager = attachmentManager

    suspend fun getAllNotes(): List<Note> {
        val fileNames = storage.listNoteFiles()
        return fileNames.mapNotNull { fileName ->
            val content = storage.readNoteFile(fileName)
            if (content != null) {
                val note = parseNoteFromContent(fileName, content)
                uuidToFileName[note.id] = fileName
                saveFileNameMapping(note.id, fileName)
                note
            } else null
        }.distinctBy { it.id }.sortedByDescending { it.updatedAt }
    }

    suspend fun getNoteById(uuid: String): Note? {
        val fileName = resolveFileName(uuid)
        val content = storage.readNoteFile(fileName) ?: return null
        return parseNoteFromContent(fileName, content)
    }

    suspend fun saveNote(note: Note, forceUpdateTimestamp: Boolean = true): Note {
        val existingFileName = resolveFileName(note.id)
        val existingNote = if (existingFileName.isNotBlank()) {
            val content = storage.readNoteFile(existingFileName)
            if (content != null) parseNoteFromContent(existingFileName, content) else null
        } else null

        if (existingNote != null && !forceUpdateTimestamp) {
            val titleChanged = existingNote.title != note.title
            val contentChanged = existingNote.content != note.content
            val tagsChanged = existingNote.tags != note.tags
            if (!titleChanged && !contentChanged && !tagsChanged) {
                return note
            }
        }

        val newFileName = resolveFileNameForTitle(note.title, note.id)

        if (existingFileName.isNotBlank() && existingFileName != newFileName) {
            storage.renameNoteFile(existingFileName, newFileName)
        }

        val updatedAt = System.currentTimeMillis()
        val content = buildString {
            appendLine("---")
            appendLine("id: ${note.id}")
            appendLine("title: ${note.title}")
            appendLine("createdAt: ${note.createdAt.time}")
            appendLine("updatedAt: $updatedAt")
            if (note.tags.isNotEmpty()) {
                appendLine("tags: ${note.tags.joinToString(",")}")
            }
            appendLine("---")
            appendLine()
            append(note.content)
        }
        storage.writeNoteFile(newFileName, content)

        uuidToFileName[note.id] = newFileName
        saveFileNameMapping(note.id, newFileName)

        return note.copy(updatedAt = Date(updatedAt))
    }

    suspend fun deleteNote(uuid: String) {
        attachmentManager.deleteAttachmentsForNote(uuid)
        val fileName = resolveFileName(uuid)
        if (fileName.isNotBlank()) {
            storage.deleteNoteFile(fileName)
            uuidToFileName.remove(uuid)
            removeFileNameMapping(uuid)
        }
    }

    suspend fun createNote(title: String = "", content: String = ""): Note {
        val note = Note(title = title, content = content)
        saveNote(note)
        return note
    }

    suspend fun addAttachment(noteId: String, entry: AttachmentEntry, data: ByteArray) {
        storage.writeAttachment(entry.storedName, data)
    }

    suspend fun getAttachmentsForNote(noteId: String): List<AttachmentEntry> {
        return attachmentManager.getEntriesForNote(noteId)
    }

    suspend fun checkAttachmentIntegrity(noteId: String, content: String): AttachmentCheckResult {
        return attachmentManager.checkReferences(noteId, content)
    }

    suspend fun cleanOrphanAttachments(noteId: String, content: String): Int {
        return attachmentManager.cleanOrphanAttachments(noteId, content)
    }

    suspend fun moveNoteAttachments(oldNoteId: String, newNoteId: String, content: String): String {
        val entries = attachmentManager.getEntriesForNote(oldNoteId)
        for (entry in entries) {
            val file = attachmentManager.getAttachmentFile(entry)
            if (file != null && file.exists()) {
                storage.writeAttachment(entry.storedName, file.readBytes())
            }
        }
        val updatedContent = attachmentManager.moveAttachmentsToNote(oldNoteId, newNoteId, content)
        return updatedContent
    }

    private suspend fun resolveFileName(uuid: String): String {
        uuidToFileName[uuid]?.let { return it }
        val cached = fileNamePrefs.getString(uuid, null)
        if (cached != null) {
            uuidToFileName[uuid] = cached
            return cached
        }
        rebuildFileNameMapping()
        return uuidToFileName[uuid] ?: ""
    }

    private suspend fun resolveFileNameForTitle(title: String, uuid: String): String {
        val baseName = sanitizeFileName(title.ifBlank { "未命名" })
        val currentMapping = uuidToFileName[uuid]

        if (currentMapping != null && currentMapping == baseName) {
            return baseName
        }

        if (!storage.fileExists(baseName) || currentMapping == baseName) {
            return baseName
        }

        val existingUuid = findUuidByFileName(baseName)
        if (existingUuid == uuid) {
            return baseName
        }

        var counter = 1
        var candidate = "$baseName($counter)"
        while (storage.fileExists(candidate)) {
            val candidateUuid = findUuidByFileName(candidate)
            if (candidateUuid == uuid) {
                return candidate
            }
            counter++
            candidate = "$baseName($counter)"
        }
        return candidate
    }

    private suspend fun findUuidByFileName(fileName: String): String? {
        val content = storage.readNoteFile(fileName) ?: return null
        val note = parseNoteFromContent(fileName, content)
        return note.id
    }

    private suspend fun rebuildFileNameMapping() {
        val fileNames = storage.listNoteFiles()
        for (fileName in fileNames) {
            val content = storage.readNoteFile(fileName) ?: continue
            val note = parseNoteFromContent(fileName, content)
            uuidToFileName[note.id] = fileName
            saveFileNameMapping(note.id, fileName)
        }
    }

    private fun sanitizeFileName(name: String): String {
        val sanitized = name
            .replace('\\', '_')
            .replace('/', '_')
            .replace(':', '_')
            .replace('*', '_')
            .replace('?', '_')
            .replace('"', '_')
            .replace('<', '_')
            .replace('>', '_')
            .replace('|', '_')
            .replace('\n', '_')
            .replace('\r', '_')
            .trim()
            .trimEnd('.')
            .trimEnd(' ')

        return if (sanitized.isBlank()) "未命名" else sanitized.take(200)
    }

    private fun saveFileNameMapping(uuid: String, fileName: String) {
        fileNamePrefs.edit().putString(uuid, fileName).apply()
    }

    private fun removeFileNameMapping(uuid: String) {
        fileNamePrefs.edit().remove(uuid).apply()
    }

    private fun parseNoteFromContent(fileName: String, content: String): Note {
        val lines = content.lines()

        var noteId = ""
        var title = fileName
        var createdAt = Date()
        var updatedAt = Date()
        var tags = emptyList<String>()
        var markdownContent = ""

        if (lines.isNotEmpty() && lines[0] == "---") {
            val endIndex = lines.subList(1, lines.size).indexOf("---").let { if (it >= 0) it + 1 else -1 }
            if (endIndex != -1) {
                val headerLines = lines.subList(1, endIndex)
                headerLines.forEach { line ->
                    val parts = line.split(":", limit = 2)
                    if (parts.size == 2) {
                        val key = parts[0].trim()
                        val value = parts[1].trim()
                        when (key) {
                            "id" -> noteId = value
                            "title" -> title = value
                            "createdAt" -> createdAt = Date(value.toLongOrNull() ?: System.currentTimeMillis())
                            "updatedAt" -> updatedAt = Date(value.toLongOrNull() ?: System.currentTimeMillis())
                            "tags" -> tags = value.split(",").filter { it.isNotBlank() }
                        }
                    }
                }
                markdownContent = lines.subList(endIndex + 1, lines.size)
                    .joinToString("\n")
                    .trim()
            }
        } else {
            markdownContent = content
            title = lines.firstOrNull()?.trimStart('#', ' ') ?: fileName
        }

        if (noteId.isBlank()) {
            noteId = java.util.UUID.nameUUIDFromBytes(fileName.toByteArray()).toString()
        }

        return Note(
            id = noteId,
            title = title,
            content = markdownContent,
            createdAt = createdAt,
            updatedAt = updatedAt,
            tags = tags
        )
    }

    suspend fun searchNotes(query: String): List<Note> {
        if (query.isBlank()) return getAllNotes()
        val lowerQuery = query.lowercase()
        return getAllNotes().filter { note ->
            note.title.lowercase().contains(lowerQuery) ||
                    note.content.lowercase().contains(lowerQuery) ||
                    note.tags.any { it.lowercase().contains(lowerQuery) }
        }
    }

    suspend fun getAllTags(): List<String> {
        return getAllNotes().flatMap { it.tags }.distinct().sorted()
    }

    suspend fun getNotesByTag(tag: String): List<Note> {
        return getAllNotes().filter { tag in it.tags }
    }

    suspend fun testStorageConnection(config: StorageConfig): Boolean {
        return storageManager.createStorage(context, config).testConnection()
    }
}
