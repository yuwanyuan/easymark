package com.easymd.data.attachment

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.provider.OpenableColumns
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.UUID

class AttachmentManager(private val context: Context, libraryId: String = "") {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("attachment_index${if (libraryId.isNotBlank()) "_$libraryId" else ""}", Context.MODE_PRIVATE)

    private val storageManager = com.easymd.data.storage.StorageManager(context)

    private fun getAssetsDir(): File {
        val activeLib = storageManager.getActiveLibrary()
        val config = activeLib.storageConfig
        val baseDir = if (config.type == com.easymd.data.storage.StorageType.LOCAL && config.localPath.isNotBlank()) {
            File(config.localPath)
        } else {
            File(context.filesDir, "notes")
        }
        return File(baseDir, "assets").apply { if (!exists()) mkdirs() }
    }

    fun generateStoredName(originalName: String): String {
        val ext = originalName.substringAfterLast('.', "").lowercase()
        val shortId = UUID.randomUUID().toString().substring(0, 8)
        val timestamp = System.currentTimeMillis().toString(36).takeLast(6)
        return if (ext.isNotBlank()) "${shortId}_${timestamp}.${ext}" else "${shortId}_${timestamp}"
    }

    fun addAttachment(noteId: String, uri: Uri): AttachmentEntry? {
        return try {
            val originalName = getFileName(uri)
            val storedName = generateStoredName(originalName)
            val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"

            val assetsDir = getAssetsDir()
            val destFile = File(assetsDir, storedName)

            var size = 0L
            var checksum = ""
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    val digest = MessageDigest.getInstance("SHA-256")
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        digest.update(buffer, 0, bytesRead)
                        size += bytesRead
                    }
                    checksum = digest.digest().joinToString("") { "%02x".format(it) }.take(16)
                }
            }

            val entry = AttachmentEntry(
                attachId = UUID.randomUUID().toString().substring(0, 12),
                originalName = originalName,
                storedName = storedName,
                mimeType = mimeType,
                size = size,
                noteId = noteId,
                checksum = checksum
            )

            saveEntry(entry)
            entry
        } catch (e: Exception) {
            null
        }
    }

    fun addAttachmentFromBytes(noteId: String, originalName: String, bytes: ByteArray, mimeType: String): AttachmentEntry? {
        return try {
            val storedName = generateStoredName(originalName)
            val assetsDir = getAssetsDir()
            val destFile = File(assetsDir, storedName)

            destFile.writeBytes(bytes)
            val digest = MessageDigest.getInstance("SHA-256")
            val checksum = digest.digest(bytes).joinToString("") { "%02x".format(it) }.take(16)

            val entry = AttachmentEntry(
                attachId = UUID.randomUUID().toString().substring(0, 12),
                originalName = originalName,
                storedName = storedName,
                mimeType = mimeType,
                size = bytes.size.toLong(),
                noteId = noteId,
                checksum = checksum
            )

            saveEntry(entry)
            entry
        } catch (e: Exception) {
            null
        }
    }

    fun getAttachmentFile(entry: AttachmentEntry): File? {
        val file = File(getAssetsDir(), entry.storedName)
        return if (file.exists()) file else null
    }

    fun getAttachmentPath(noteId: String, attachId: String): String? {
        val entry = getEntry(attachId) ?: return null
        if (entry.noteId != noteId) return null
        val file = getAttachmentFile(entry) ?: return null
        return file.absolutePath
    }

    fun getRelativePath(entry: AttachmentEntry): String {
        return "assets/${entry.storedName}"
    }

    fun getMarkdownReference(entry: AttachmentEntry): String {
        val path = getRelativePath(entry)
        return if (entry.isImage()) {
            "![${entry.originalName}]($path)"
        } else {
            "[${entry.originalName}]($path)"
        }
    }

    fun deleteAttachment(attachId: String) {
        val entry = getEntry(attachId) ?: return
        val file = File(getAssetsDir(), entry.storedName)
        if (file.exists()) file.delete()
        removeEntry(attachId)
    }

    fun deleteAttachmentsForNote(noteId: String) {
        val entries = getEntriesForNote(noteId)
        entries.forEach { entry ->
            val file = File(getAssetsDir(), entry.storedName)
            if (file.exists()) file.delete()
            removeEntry(entry.attachId)
        }
    }

    fun moveAttachmentsToNote(oldNoteId: String, newNoteId: String, content: String): String {
        val entries = getEntriesForNote(oldNoteId)
        if (entries.isEmpty()) return content

        var updatedContent = content

        entries.forEach { entry ->
            val updatedEntry = entry.copy(noteId = newNoteId)
            saveEntry(updatedEntry)
        }

        return updatedContent
    }

    fun getEntriesForNote(noteId: String): List<AttachmentEntry> {
        val allEntries = getAllEntries()
        return allEntries.filter { it.noteId == noteId }
    }

    fun checkReferences(noteId: String, content: String): AttachmentCheckResult {
        val references = extractReferences(content)
        val entries = getEntriesForNote(noteId)
        val entryMap = entries.associateBy { it.storedName }

        val brokenRefs = mutableListOf<String>()
        var validCount = 0
        val referencedIds = mutableSetOf<String>()

        for (ref in references) {
            val storedName = ref.substringAfterLast('/')
            val entry = entryMap[storedName]
            if (entry != null) {
                val file = getAttachmentFile(entry)
                if (file != null && file.exists()) {
                    validCount++
                    referencedIds.add(entry.attachId)
                } else {
                    brokenRefs.add(ref)
                }
            } else {
                if (ref.startsWith("assets/")) {
                    brokenRefs.add(ref)
                }
            }
        }

        val orphans = entries.filter { getRelativePath(it) !in references }

        return AttachmentCheckResult(
            totalReferences = references.size,
            validReferences = validCount,
            brokenReferences = brokenRefs,
            orphanAttachments = orphans
        )
    }

    fun cleanOrphanAttachments(noteId: String, content: String): Int {
        val result = checkReferences(noteId, content)
        result.orphanAttachments.forEach { entry ->
            deleteAttachment(entry.attachId)
        }
        return result.orphanAttachments.size
    }

    fun batchUpdateReferences(oldNoteId: String, newNoteId: String, contents: Map<String, String>): Map<String, String> {
        val entries = getEntriesForNote(oldNoteId)
        if (entries.isEmpty()) return contents

        // Update attachment entry ownership
        entries.forEach { entry ->
            saveEntry(entry.copy(noteId = newNoteId))
        }

        // Update content references if the storedName changed (e.g., due to migration)
        // For simple noteId reassignment, paths stay the same, so content is unchanged
        return contents
    }

    private fun extractReferences(content: String): List<String> {
        val refs = mutableListOf<String>()
        val imagePattern = Regex("""!\[.*?\]\((.*?)\)""")
        val linkPattern = Regex("""\[(?:[^!\]].*?)\]\((.*?)\)""")

        imagePattern.findAll(content).forEach { match ->
            refs.add(match.groupValues[1])
        }
        linkPattern.findAll(content).forEach { match ->
            val path = match.groupValues[1]
            if (path.startsWith("assets/")) {
                refs.add(path)
            }
        }

        return refs.distinct()
    }

    private fun getFileName(uri: Uri): String {
        var name = "unknown"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }

    private fun saveEntry(entry: AttachmentEntry) {
        val entries = loadAllEntries().toMutableMap()
        entries[entry.attachId] = entry
        saveAllEntries(entries)
    }

    private fun getEntry(attachId: String): AttachmentEntry? {
        return loadAllEntries()[attachId]
    }

    private fun removeEntry(attachId: String) {
        val entries = loadAllEntries().toMutableMap()
        entries.remove(attachId)
        saveAllEntries(entries)
    }

    private fun getAllEntries(): List<AttachmentEntry> {
        return loadAllEntries().values.toList()
    }

    private fun loadAllEntries(): Map<String, AttachmentEntry> {
        val json = prefs.getString("entries", null) ?: return emptyMap()
        return try {
            val arr = JSONArray(json)
            val map = mutableMapOf<String, AttachmentEntry>()
            for (i in 0 until arr.length()) {
                val entry = parseEntry(arr.getJSONObject(i))
                map[entry.attachId] = entry
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun saveAllEntries(entries: Map<String, AttachmentEntry>) {
        val arr = JSONArray()
        entries.values.forEach { arr.put(serializeEntry(it)) }
        prefs.edit().putString("entries", arr.toString()).apply()
    }

    private fun serializeEntry(entry: AttachmentEntry): JSONObject {
        return JSONObject().apply {
            put("attachId", entry.attachId)
            put("originalName", entry.originalName)
            put("storedName", entry.storedName)
            put("mimeType", entry.mimeType)
            put("size", entry.size)
            put("noteId", entry.noteId)
            put("createdAt", entry.createdAt)
            put("checksum", entry.checksum)
        }
    }

    private fun parseEntry(json: JSONObject): AttachmentEntry {
        return AttachmentEntry(
            attachId = json.optString("attachId", ""),
            originalName = json.optString("originalName", ""),
            storedName = json.optString("storedName", ""),
            mimeType = json.optString("mimeType", "application/octet-stream"),
            size = json.optLong("size", 0),
            noteId = json.optString("noteId", ""),
            createdAt = json.optLong("createdAt", 0),
            checksum = json.optString("checksum", "")
        )
    }
}
