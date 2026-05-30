package com.easymd.data.attachment

data class ReferenceCheck(
    val reference: String,
    val attachId: String?,
    val exists: Boolean,
    val entry: AttachmentEntry?
)

data class AttachmentCheckResult(
    val totalReferences: Int,
    val validReferences: Int,
    val brokenReferences: List<String>,
    val orphanAttachments: List<AttachmentEntry>
)
