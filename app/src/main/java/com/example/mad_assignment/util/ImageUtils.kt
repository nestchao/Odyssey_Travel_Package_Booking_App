package com.example.mad_assignment.util

import android.util.Base64
import android.util.Log

fun toDataUri(base64Data: String?): ByteArray? {
    if (base64Data.isNullOrEmpty()) {
        return null
    }

    var cleanBase64String = base64Data

    val dataUriRegex = Regex("^data:image/[^;]+;base64,")

    val match = dataUriRegex.find(base64Data)
    if (match != null) {
        cleanBase64String = base64Data.substringAfter(match.value)
    }

    try {
        return Base64.decode(cleanBase64String, Base64.DEFAULT)
    } catch (e: IllegalArgumentException) {
        Log.e("toDataUri", "Failed to decode Base64 string: ${e.message}. Original data (truncated): ${base64Data.take(100)}...")
        return null
    }
}