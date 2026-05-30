package com.easymd.data.storage

import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.w3c.dom.Document
import org.w3c.dom.NodeList
import java.io.ByteArrayInputStream
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory

class WebDAVStorage(private val config: StorageConfig) : NoteStorage {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val baseUrl = config.webdavUrl.trimEnd('/')
    private val basePath = config.webdavPath.trimStart('/').trimEnd('/')
    private val credentials = Credentials.basic(config.webdavUsername, config.webdavPassword)

    private fun noteUrl(id: String): String {
        return "$baseUrl/$basePath/$id.md"
    }

    private fun folderUrl(): String {
        return "$baseUrl/$basePath/"
    }

    private fun assetsFolderUrl(): String {
        return "$baseUrl/$basePath/assets/"
    }

    private fun assetUrl(attachName: String): String {
        return "$baseUrl/$basePath/assets/$attachName"
    }

    override suspend fun listNoteFiles(): List<String> {
        val url = folderUrl()
        val propfindBody = """
            <?xml version="1.0" encoding="utf-8"?>
            <propfind xmlns="DAV:">
                <prop>
                    <resourcetype/>
                    <displayname/>
                </prop>
            </propfind>
        """.trimIndent()

        val body = propfindBody.toRequestBody("application/xml; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(url)
            .method("PROPFIND", body)
            .header("Authorization", credentials)
            .header("Depth", "1")
            .build()

        return try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: return emptyList()
                parseWebDAVList(responseBody)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseWebDAVList(xml: String): List<String> {
        val ids = mutableListOf<String>()
        return try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            // Disable XXE (External Entity Injection) to prevent SSRF / file disclosure
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            val builder = factory.newDocumentBuilder()
            val doc: Document = builder.parse(ByteArrayInputStream(xml.toByteArray()))

            val responses: NodeList = doc.getElementsByTagNameNS("DAV:", "response")
            for (i in 0 until responses.length) {
                val responseNode = responses.item(i)
                val hrefs = responseNode.childNodes
                for (j in 0 until hrefs.length) {
                    val child = hrefs.item(j)
                    if (child.localName == "href") {
                        val href = child.textContent
                        if (href.endsWith(".md")) {
                            val fileName = href.substringAfterLast('/').removeSuffix(".md")
                            ids.add(fileName)
                        }
                    }
                }
            }
            ids
        } catch (e: Exception) {
            ids
        }
    }

    override suspend fun readNoteFile(id: String): String? {
        val url = noteUrl(id)
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Authorization", credentials)
            .build()

        return try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.string()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun writeNoteFile(id: String, content: String) {
        val url = noteUrl(id)
        ensureFolderExists()

        val body = content.toRequestBody("text/markdown; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(url)
            .put(body)
            .header("Authorization", credentials)
            .build()

        try {
            client.newCall(request).execute()
        } catch (_: Exception) {
        }
    }

    override suspend fun deleteNoteFile(id: String) {
        val url = noteUrl(id)
        val request = Request.Builder()
            .url(url)
            .delete()
            .header("Authorization", credentials)
            .build()

        try {
            client.newCall(request).execute()
        } catch (_: Exception) {
        }
    }

    override suspend fun renameNoteFile(oldId: String, newId: String): Boolean {
        return try {
            val sourceUrl = noteUrl(oldId)
            val destUrl = noteUrl(newId)
            ensureFolderExists()

            val request = Request.Builder()
                .url(sourceUrl)
                .method("MOVE", null)
                .header("Authorization", credentials)
                .header("Destination", destUrl)
                .header("Overwrite", "F")
                .build()

            val response = client.newCall(request).execute()
            response.isSuccessful || response.code == 201
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun fileExists(id: String): Boolean {
        val url = noteUrl(id)
        val request = Request.Builder()
            .url(url)
            .head()
            .header("Authorization", credentials)
            .build()

        return try {
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    private fun ensureFolderExists() {
        val url = folderUrl()
        val request = Request.Builder()
            .url(url)
            .method("MKCOL", null)
            .header("Authorization", credentials)
            .build()

        try {
            client.newCall(request).execute()
        } catch (_: Exception) {
        }
    }

    private fun ensureAssetsFolderExists() {
        ensureFolderExists()

        val assetsUrl = assetsFolderUrl()
        val request = Request.Builder()
            .url(assetsUrl)
            .method("MKCOL", null)
            .header("Authorization", credentials)
            .build()
        try { client.newCall(request).execute() } catch (_: Exception) {}
    }

    override suspend fun testConnection(): Boolean {
        return try {
            val url = folderUrl()
            val propfindBody = """
                <?xml version="1.0" encoding="utf-8"?>
                <propfind xmlns="DAV:">
                    <prop><resourcetype/></prop>
                </propfind>
            """.trimIndent()

            val body = propfindBody.toRequestBody("application/xml; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .method("PROPFIND", body)
                .header("Authorization", credentials)
                .header("Depth", "0")
                .build()

            val response = client.newCall(request).execute()
            response.isSuccessful || response.code == 207
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun listAttachments(noteId: String): List<String> {
        val url = assetsFolderUrl()
        val propfindBody = """
            <?xml version="1.0" encoding="utf-8"?>
            <propfind xmlns="DAV:">
                <prop><resourcetype/><displayname/></prop>
            </propfind>
        """.trimIndent()

        val body = propfindBody.toRequestBody("application/xml; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(url)
            .method("PROPFIND", body)
            .header("Authorization", credentials)
            .header("Depth", "1")
            .build()

        return try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful || response.code == 207) {
                val responseBody = response.body?.string() ?: return emptyList()
                parseWebDAVAssetList(responseBody)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseWebDAVAssetList(xml: String): List<String> {
        val names = mutableListOf<String>()
        return try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            // Disable XXE (External Entity Injection)
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            val builder = factory.newDocumentBuilder()
            val doc: Document = builder.parse(ByteArrayInputStream(xml.toByteArray()))

            val responses: NodeList = doc.getElementsByTagNameNS("DAV:", "response")
            for (i in 0 until responses.length) {
                val responseNode = responses.item(i)
                val hrefs = responseNode.childNodes
                for (j in 0 until hrefs.length) {
                    val child = hrefs.item(j)
                    if (child.localName == "href") {
                        val href = child.textContent
                        val fileName = href.substringAfterLast('/')
                        if (fileName.isNotBlank() && !href.endsWith("/")) {
                            names.add(fileName)
                        }
                    }
                }
            }
            names
        } catch (e: Exception) {
            names
        }
    }

    override suspend fun readAttachment(attachName: String): ByteArray? {
        val url = assetUrl(attachName)
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Authorization", credentials)
            .build()

        return try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.bytes()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun writeAttachment(attachName: String, data: ByteArray) {
        ensureAssetsFolderExists()
        val url = assetUrl(attachName)
        val body = data.toRequestBody("application/octet-stream".toMediaType())
        val request = Request.Builder()
            .url(url)
            .put(body)
            .header("Authorization", credentials)
            .build()

        try {
            client.newCall(request).execute()
        } catch (_: Exception) {
        }
    }

    override suspend fun deleteAttachment(attachName: String) {
        val url = assetUrl(attachName)
        val request = Request.Builder()
            .url(url)
            .delete()
            .header("Authorization", credentials)
            .build()

        try {
            client.newCall(request).execute()
        } catch (_: Exception) {
        }
    }

    override suspend fun deleteAttachmentsForNote(noteId: String) {
    }
}
