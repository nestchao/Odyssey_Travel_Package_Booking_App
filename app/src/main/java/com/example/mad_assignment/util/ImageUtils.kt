package com.example.mad_assignment.util

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

fun base64ToDataUri(base64String: String?): String? {
    if (base64String.isNullOrBlank()) {
        return null
    }
    // This regex correctly handles if the prefix is already there.
    val dataUriRegex = Regex("^data:image/[^;]+;base64,")
    if (base64String.startsWith("data:image")) {
        return base64String // It's already in the correct format
    }
    return "data:image/jpeg;base64,$base64String"
}

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

suspend fun uriToBase64(context: Context, uri: Uri): String? {
    return withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bytes = inputStream.readBytes()
                Base64.encodeToString(bytes, Base64.DEFAULT)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}