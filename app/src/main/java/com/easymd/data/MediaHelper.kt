package com.easymd.data

import android.content.Context
import android.net.Uri
import com.easymd.data.attachment.AttachmentEntry
import com.easymd.data.attachment.AttachmentManager

object MediaHelper {

    private var attachmentManager: AttachmentManager? = null

    fun init(context: Context) {
        if (attachmentManager == null) {
            attachmentManager = AttachmentManager(context.applicationContext)
        }
    }

    fun getAttachmentManager(context: Context): AttachmentManager {
        init(context)
        return attachmentManager!!
    }

    fun getFileName(context: Context, uri: Uri): String {
        var name = "unknown"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }

    @Deprecated("Use AttachmentManager.addAttachment instead")
    fun copyFileToMediaDir(context: Context, uri: Uri): String? {
        return try {
            val fileName = getFileName(context, uri)
            val uniqueName = "${java.util.UUID.randomUUID()}_$fileName"
            val destFile = java.io.File(java.io.File(context.filesDir, "media"), uniqueName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            "media/$uniqueName"
        } catch (e: Exception) {
            null
        }
    }

    fun isImageFile(path: String): Boolean {
        val ext = path.substringAfterLast('.', "").lowercase()
        return ext in setOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg")
    }

    fun getMediaFile(context: Context, relativePath: String): java.io.File? {
        val file = java.io.File(context.filesDir, relativePath)
        return if (file.exists()) file else null
    }

    fun addAttachmentToNote(context: Context, noteId: String, uri: Uri): AttachmentEntry? {
        return getAttachmentManager(context).addAttachment(noteId, uri)
    }

    fun getAttachmentFile(context: Context, entry: AttachmentEntry): java.io.File? {
        return getAttachmentManager(context).getAttachmentFile(entry)
    }
}
