package com.example.mad_assignment.util

fun toDataUri(base64Data: String?, mimeType: String = "image/jpeg"): String? {
    if (base64Data.isNullOrEmpty()) {
        return null
    }

    val expectedPrefix = "data:$mimeType;base64,"
    if (base64Data.startsWith(expectedPrefix)) {
        return base64Data
    }
    return "$expectedPrefix$base64Data"
}