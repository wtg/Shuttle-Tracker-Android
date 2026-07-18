package edu.rpi.shuttletracker.core.network

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

fun normalizeBaseUrl(value: String): String? {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return null

    val candidate = if (trimmed.endsWith('/')) trimmed else "$trimmed/"
    val url = candidate.toHttpUrlOrNull() ?: return null

    if (!url.isHttps) return null
    if (url.username.isNotEmpty() || url.password.isNotEmpty()) return null
    if (url.query != null || url.fragment != null) return null

    return url.toString()
}
