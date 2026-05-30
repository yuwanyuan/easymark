package com.easymd.data.storage

interface NoteStorage {
    suspend fun listNoteFiles(): List<String>
    suspend fun readNoteFile(id: String): String?
    suspend fun writeNoteFile(id: String, content: String)
    suspend fun deleteNoteFile(id: String)
    suspend fun testConnection(): Boolean
    suspend fun renameNoteFile(oldId: String, newId: String): Boolean
    suspend fun fileExists(id: String): Boolean

    suspend fun listAttachments(noteId: String): List<String> = emptyList()
    suspend fun readAttachment(attachName: String): ByteArray? = null
    suspend fun writeAttachment(attachName: String, data: ByteArray) {}
    suspend fun deleteAttachment(attachName: String) {}
    suspend fun deleteAttachmentsForNote(noteId: String) {}
}
