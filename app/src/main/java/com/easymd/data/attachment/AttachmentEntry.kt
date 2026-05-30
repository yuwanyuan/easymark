package com.easymd.data.attachment

data class AttachmentEntry(
    val attachId: String,
    val originalName: String,
    val storedName: String,
    val mimeType: String,
    val size: Long,
    val noteId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val checksum: String = ""
) {
    fun isImage(): Boolean {
        return mimeType.startsWith("image/") ||
                storedName.substringAfterLast('.', "").lowercase() in
                setOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg")
    }
}
