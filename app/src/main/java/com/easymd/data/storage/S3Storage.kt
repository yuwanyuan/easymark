package com.easymd.data.storage

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.w3c.dom.Document
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class S3Storage(private val config: StorageConfig) : NoteStorage {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val endpoint = config.s3Endpoint.trimEnd('/')
    private val bucket = config.s3Bucket
    private val prefix = config.s3Prefix.trimEnd('/') + "/"
    // Use path-style access if endpoint contains a path; otherwise virtual-hosted style
    private val s3Host: String by lazy {
        if (endpoint.contains("/")) endpoint else "$bucket.$endpoint"
    }
    private val s3PathPrefix: String by lazy {
        if (endpoint.contains("/")) "/$bucket/$prefix" else "/$prefix"
    }

    override suspend fun listNoteFiles(): List<String> {
        val host = "$bucket.$endpoint"
        val path = "/$prefix"
        val queryString = "list-type=2&prefix=${encodeS3(prefix)}&delimiter=/"

        val request = buildS3Request("GET", host, path, queryString)

        return try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return emptyList()
                parseS3List(body)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseS3List(xml: String): List<String> {
        val ids = mutableListOf<String>()
        return try {
            val factory = javax.xml.parsers.DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val doc: Document = builder.parse(ByteArrayInputStream(xml.toByteArray()))

            val contents = doc.getElementsByTagName("Contents")
            for (i in 0 until contents.length) {
                val node = contents.item(i)
                val children = node.childNodes
                for (j in 0 until children.length) {
                    val child = children.item(j)
                    if (child.nodeName == "Key") {
                        val key = child.textContent
                        val fileName = key.substringAfterLast('/').removeSuffix(".md")
                        if (key.endsWith(".md") && !key.contains("assets/")) {
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
        val host = "$bucket.$endpoint"
        val objectKey = "$prefix$id.md"
        val path = "/$objectKey"

        val request = buildS3Request("GET", host, path)

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
        val host = "$bucket.$endpoint"
        val objectKey = "$prefix$id.md"
        val path = "/$objectKey"

        val payloadHash = sha256Hex(content.toByteArray())
        val request = buildS3Request("PUT", host, path, body = content, payloadHash = payloadHash)

        try {
            client.newCall(request).execute()
        } catch (_: Exception) {
        }
    }

    override suspend fun deleteNoteFile(id: String) {
        val host = "$bucket.$endpoint"
        val objectKey = "$prefix$id.md"
        val path = "/$objectKey"

        val request = buildS3Request("DELETE", host, path)

        try {
            client.newCall(request).execute()
        } catch (_: Exception) {
        }
    }

    override suspend fun renameNoteFile(oldId: String, newId: String): Boolean {
        val content = readNoteFile(oldId) ?: return false
        writeNoteFile(newId, content)
        deleteNoteFile(oldId)
        return true
    }

    override suspend fun fileExists(id: String): Boolean {
        val host = "$bucket.$endpoint"
        val objectKey = "$prefix$id.md"
        val path = "/$objectKey"

        val request = buildS3Request("HEAD", host, path)

        return try {
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun testConnection(): Boolean {
        return try {
            val host = "$bucket.$endpoint"
            val path = "/$prefix"
            val queryString = "list-type=2&max-keys=1"

            val request = buildS3Request("GET", host, path, queryString)
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun listAttachments(noteId: String): List<String> {
        val host = "$bucket.$endpoint"
        val assetPrefix = "${prefix}assets/"
        val path = "/$assetPrefix"
        val queryString = "list-type=2&prefix=${encodeS3(assetPrefix)}"

        val request = buildS3Request("GET", host, path, queryString)

        return try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return emptyList()
                parseS3AssetList(body, assetPrefix)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseS3AssetList(xml: String, assetPrefix: String): List<String> {
        val names = mutableListOf<String>()
        return try {
            val factory = javax.xml.parsers.DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val doc: Document = builder.parse(ByteArrayInputStream(xml.toByteArray()))

            val contents = doc.getElementsByTagName("Contents")
            for (i in 0 until contents.length) {
                val node = contents.item(i)
                val children = node.childNodes
                for (j in 0 until children.length) {
                    val child = children.item(j)
                    if (child.nodeName == "Key") {
                        val key = child.textContent
                        if (key.startsWith(assetPrefix) && key != assetPrefix) {
                            names.add(key.substringAfterLast('/'))
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
        val host = "$bucket.$endpoint"
        val objectKey = "${prefix}assets/$attachName"
        val path = "/$objectKey"

        val request = buildS3Request("GET", host, path)

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
        val host = "$bucket.$endpoint"
        val objectKey = "${prefix}assets/$attachName"
        val path = "/$objectKey"

        val payloadHash = sha256Hex(data)
        val request = buildS3Request("PUT", host, path, bodyBytes = data, payloadHash = payloadHash, contentType = "application/octet-stream")

        try {
            client.newCall(request).execute()
        } catch (_: Exception) {
        }
    }

    override suspend fun deleteAttachment(attachName: String) {
        val host = "$bucket.$endpoint"
        val objectKey = "${prefix}assets/$attachName"
        val path = "/$objectKey"

        val request = buildS3Request("DELETE", host, path)

        try {
            client.newCall(request).execute()
        } catch (_: Exception) {
        }
    }

    override suspend fun deleteAttachmentsForNote(noteId: String) {
    }

    private fun buildS3Request(
        method: String,
        host: String,
        path: String,
        queryString: String = "",
        body: String? = null,
        bodyBytes: ByteArray? = null,
        payloadHash: String = "UNSIGNED-PAYLOAD",
        contentType: String = "text/markdown; charset=utf-8"
    ): Request {
        val now = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())

        val dateStamp = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())

        // AWS SigV4 requires canonical headers to be sorted by header name (ASCII order)
        val amzHeaders = listOf(
            "host" to host,
            "x-amz-content-sha256" to payloadHash,
            "x-amz-date" to now
        ).sortedBy { it.first }
        val signedHeaders = amzHeaders.joinToString(";") { it.first }

        val canonicalHeaders = amzHeaders.joinToString("\n") { "${it.first}:${it.second}" } + "\n"

        val canonicalRequest = listOf(
            method,
            path,
            queryString,
            canonicalHeaders,
            signedHeaders,
            payloadHash
        ).joinToString("\n")

        val credentialScope = "$dateStamp/${config.s3Region}/s3/aws4_request"
        val stringToSign = listOf(
            "AWS4-HMAC-SHA256",
            now,
            credentialScope,
            sha256Hex(canonicalRequest.toByteArray())
        ).joinToString("\n")

        val signingKey = getSignatureKey(config.s3SecretKey, dateStamp, config.s3Region, "s3")
        val signature = hmacSha256Hex(signingKey, stringToSign.toByteArray())

        val authHeader = "AWS4-HMAC-SHA256 Credential=${config.s3AccessKey}/$credentialScope, SignedHeaders=$signedHeaders, Signature=$signature"

        val urlBuilder = StringBuilder("https://$host$path")
        if (queryString.isNotEmpty()) {
            urlBuilder.append("?$queryString")
        }

        val requestBuilder = Request.Builder()
            .url(urlBuilder.toString())
            .header("Authorization", authHeader)
            .header("x-amz-content-sha256", payloadHash)
            .header("x-amz-date", now)

        when (method) {
            "GET" -> requestBuilder.get()
            "DELETE" -> requestBuilder.delete()
            "HEAD" -> requestBuilder.head()
            "PUT" -> {
                val reqBody = if (bodyBytes != null) {
                    bodyBytes.toRequestBody(contentType.toMediaType())
                } else {
                    (body ?: "").toRequestBody(contentType.toMediaType())
                }
                requestBuilder.put(reqBody)
            }
        }

        return requestBuilder.build()
    }

    private fun encodeS3(s: String): String {
        // URLEncoder encodes spaces as '+' but S3 expects '%20'
        return java.net.URLEncoder.encode(s, "UTF-8").replace("+", "%20")
    }

    private fun sha256Hex(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(data).joinToString("") { "%02x".format(it) }
    }

    private fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    private fun hmacSha256Hex(key: ByteArray, data: ByteArray): String {
        return hmacSha256(key, data).joinToString("") { "%02x".format(it) }
    }

    private fun getSignatureKey(key: String, dateStamp: String, regionName: String, serviceName: String): ByteArray {
        val kDate = hmacSha256("AWS4$key".toByteArray(), dateStamp.toByteArray())
        val kRegion = hmacSha256(kDate, regionName.toByteArray())
        val kService = hmacSha256(kRegion, serviceName.toByteArray())
        return hmacSha256(kService, "aws4_request".toByteArray())
    }
}
