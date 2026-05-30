package com.easymd.data.storage

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class StorageManager(context: Context) {

    companion object {
        const val ALL_LIBRARIES_ID = "__all_libraries__"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences("storage_prefs", Context.MODE_PRIVATE)

    fun getLibraries(): List<NoteLibrary> {
        val json = prefs.getString("libraries", null)
        if (json.isNullOrBlank()) {
            val default = NoteLibrary(storageConfig = StorageConfig())
            saveLibraries(listOf(default))
            setActiveLibraryId(ALL_LIBRARIES_ID)
            return listOf(default)
        }
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                parseLibrary(arr.getJSONObject(i))
            }
        } catch (e: Exception) {
            val default = NoteLibrary(storageConfig = StorageConfig())
            saveLibraries(listOf(default))
            setActiveLibraryId(ALL_LIBRARIES_ID)
            listOf(default)
        }
    }

    fun saveLibraries(libraries: List<NoteLibrary>) {
        val arr = JSONArray()
        libraries.forEach { arr.put(serializeLibrary(it)) }
        prefs.edit().putString("libraries", arr.toString()).apply()
    }

    fun getActiveLibraryId(): String {
        return prefs.getString("active_library_id", "") ?: ""
    }

    fun setActiveLibraryId(id: String) {
        prefs.edit().putString("active_library_id", id).apply()
    }

    fun getActiveLibrary(): NoteLibrary {
        val activeId = getActiveLibraryId()
        val libraries = getLibraries()
        return libraries.find { it.id == activeId } ?: libraries.firstOrNull()
            ?: NoteLibrary()
    }

    fun addLibrary(library: NoteLibrary) {
        val libraries = getLibraries().toMutableList()
        libraries.add(library)
        saveLibraries(libraries)
    }

    fun updateLibrary(library: NoteLibrary) {
        val libraries = getLibraries().toMutableList()
        val index = libraries.indexOfFirst { it.id == library.id }
        if (index >= 0) {
            libraries[index] = library
        }
        saveLibraries(libraries)
    }

    fun deleteLibrary(libraryId: String) {
        val libraries = getLibraries().toMutableList()
        libraries.removeAll { it.id == libraryId }
        if (libraries.isEmpty()) {
            val default = NoteLibrary()
            libraries.add(default)
            setActiveLibraryId(ALL_LIBRARIES_ID)
        } else if (getActiveLibraryId() == libraryId) {
            setActiveLibraryId(ALL_LIBRARIES_ID)
        }
        saveLibraries(libraries)
    }

    fun createStorage(context: Context, config: StorageConfig): NoteStorage {
        return when (config.type) {
            StorageType.LOCAL -> LocalStorage(context, config.localPath.ifBlank { null })
            StorageType.WEBDAV -> WebDAVStorage(config)
            StorageType.S3 -> S3Storage(config)
        }
    }

    private fun serializeLibrary(library: NoteLibrary): JSONObject {
        return JSONObject().apply {
            put("id", library.id)
            put("storageConfig", serializeConfig(library.storageConfig))
        }
    }

    private fun parseLibrary(json: JSONObject): NoteLibrary {
        return NoteLibrary(
            id = json.optString("id", ""),
            storageConfig = parseConfig(json.optJSONObject("storageConfig"))
        )
    }

    private fun serializeConfig(config: StorageConfig): JSONObject {
        return JSONObject().apply {
            put("type", config.type.name)
            put("localPath", config.localPath)
            put("webdavUrl", config.webdavUrl)
            put("webdavUsername", config.webdavUsername)
            put("webdavPassword", config.webdavPassword)
            put("webdavPath", config.webdavPath)
            put("s3Endpoint", config.s3Endpoint)
            put("s3Region", config.s3Region)
            put("s3Bucket", config.s3Bucket)
            put("s3AccessKey", config.s3AccessKey)
            put("s3SecretKey", config.s3SecretKey)
            put("s3Prefix", config.s3Prefix)
        }
    }

    private fun parseConfig(json: JSONObject?): StorageConfig {
        if (json == null) return StorageConfig()
        val typeStr = json.optString("type", StorageType.LOCAL.name)
        val type = try { StorageType.valueOf(typeStr) } catch (_: Exception) { StorageType.LOCAL }
        return StorageConfig(
            type = type,
            localPath = json.optString("localPath", ""),
            webdavUrl = json.optString("webdavUrl", ""),
            webdavUsername = json.optString("webdavUsername", ""),
            webdavPassword = json.optString("webdavPassword", ""),
            webdavPath = json.optString("webdavPath", "/notes/"),
            s3Endpoint = json.optString("s3Endpoint", ""),
            s3Region = json.optString("s3Region", ""),
            s3Bucket = json.optString("s3Bucket", ""),
            s3AccessKey = json.optString("s3AccessKey", ""),
            s3SecretKey = json.optString("s3SecretKey", ""),
            s3Prefix = json.optString("s3Prefix", "notes/")
        )
    }
}
