package com.easymd.data.storage

enum class StorageType {
    LOCAL, WEBDAV, S3
}

data class StorageConfig(
    val type: StorageType = StorageType.LOCAL,
    val localPath: String = "",
    val webdavUrl: String = "",
    val webdavUsername: String = "",
    val webdavPassword: String = "",
    val webdavPath: String = "/notes/",
    val s3Endpoint: String = "",
    val s3Region: String = "",
    val s3Bucket: String = "",
    val s3AccessKey: String = "",
    val s3SecretKey: String = "",
    val s3Prefix: String = "notes/"
)
