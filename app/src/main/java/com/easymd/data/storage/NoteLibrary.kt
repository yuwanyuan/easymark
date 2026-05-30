package com.easymd.data.storage

import java.util.UUID

data class NoteLibrary(
    val id: String = UUID.randomUUID().toString(),
    val storageConfig: StorageConfig = StorageConfig()
) {
    val name: String
        get() = deriveName(storageConfig)

    companion object {
        fun deriveName(config: StorageConfig): String {
            return when (config.type) {
                StorageType.LOCAL -> {
                    if (config.localPath.isNotBlank()) {
                        val path = config.localPath.trimEnd('/')
                        path.substringAfterLast('/')
                    } else {
                        "默认笔记库"
                    }
                }
                StorageType.WEBDAV -> {
                    val path = config.webdavPath.trimEnd('/')
                    if (path.isNotBlank()) {
                        path.substringAfterLast('/')
                    } else {
                        "WebDAV"
                    }
                }
                StorageType.S3 -> {
                    val prefix = config.s3Prefix.trimEnd('/')
                    if (prefix.isNotBlank()) {
                        prefix.substringAfterLast('/')
                    } else {
                        config.s3Bucket.ifBlank { "S3" }
                    }
                }
            }
        }
    }
}
