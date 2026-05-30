package com.easymd.data

import java.util.Date
import java.util.UUID

data class Note(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val content: String = "",
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val tags: List<String> = emptyList()
) {
    fun getPreview(): String {
        return content.lines()
            .filter { it.isNotBlank() }
            .take(5)
            .joinToString("\n")
            .take(200)
    }

    fun getFormattedDate(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return sdf.format(updatedAt)
    }

    fun getRelativeTime(): String {
        val now = System.currentTimeMillis()
        val diff = now - updatedAt.time
        val minutes = diff / 60000
        val hours = diff / 3600000
        val days = diff / 86400000
        return when {
            diff < 300000 -> "不久之前"
            diff < 3600000 -> "${minutes}分钟前"
            diff < 86400000 -> "${hours}小时前"
            diff < 172800000 -> "一天前"
            diff < 259200000 -> "两天前"
            else -> getFormattedDate()
        }
    }
}
