package com.easymd.data.storage

import android.content.Context
import java.io.File

class LocalStorage(
    private val context: Context,
    private val customPath: String? = null
) : NoteStorage {

    private val notesDir: File by lazy {
        if (!customPath.isNullOrBlank()) {
            File(customPath).apply { if (!exists()) mkdirs() }
        } else {
            File(context.filesDir, "notes").apply { if (!exists()) mkdirs() }
        }
    }

    private fun assetsDir(): File {
        return File(notesDir, "assets").apply { if (!exists()) mkdirs() }
    }

    override suspend fun listNoteFiles(): List<String> {
        return notesDir.listFiles { file -> file.extension == "md" }
            ?.map { it.nameWithoutExtension }
            ?: emptyList()
    }

    override suspend fun readNoteFile(id: String): String? {
        val file = File(notesDir, "$id.md")
        return if (file.exists()) file.readText() else null
    }

    override suspend fun writeNoteFile(id: String, content: String) {
        val file = File(notesDir, "$id.md")
        file.writeText(content)
    }

    override suspend fun deleteNoteFile(id: String) {
        val file = File(notesDir, "$id.md")
        if (file.exists()) file.delete()
    }

    override suspend fun renameNoteFile(oldId: String, newId: String): Boolean {
        val oldFile = File(notesDir, "$oldId.md")
        val newFile = File(notesDir, "$newId.md")
        return if (oldFile.exists() && !newFile.exists()) {
            oldFile.renameTo(newFile)
        } else {
            false
        }
    }

    override suspend fun fileExists(id: String): Boolean {
        return File(notesDir, "$id.md").exists()
    }

    override suspend fun testConnection(): Boolean {
        return try {
            notesDir.exists() || notesDir.mkdirs()
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun listAttachments(noteId: String): List<String> {
        val dir = assetsDir()
        return dir.listFiles()?.map { it.name } ?: emptyList()
    }

    override suspend fun readAttachment(attachName: String): ByteArray? {
        val file = File(assetsDir(), attachName)
        return if (file.exists()) file.readBytes() else null
    }

    override suspend fun writeAttachment(attachName: String, data: ByteArray) {
        val dir = assetsDir()
        val file = File(dir, attachName)
        file.writeBytes(data)
    }

    override suspend fun deleteAttachment(attachName: String) {
        val file = File(assetsDir(), attachName)
        if (file.exists()) file.delete()
    }

    override suspend fun deleteAttachmentsForNote(noteId: String) {
    }
}
