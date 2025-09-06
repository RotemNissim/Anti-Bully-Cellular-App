package com.example.antibully.utils

private val CAT_TAG_RE = Regex("""\s*##cats:([a-z\-|]+)$""", RegexOption.IGNORE_CASE)

fun extractCatsFromSummary(summary: String): List<String> =
    CAT_TAG_RE.find(summary)
        ?.groupValues?.getOrNull(1)
        ?.split("|")
        ?.map { it.trim().lowercase() }
        ?.filter { it.isNotEmpty() }
        ?: emptyList()

fun humanSummary(summary: String): String =
    summary.replace(CAT_TAG_RE, "").trim()

fun isImageFromSummary(summary: String): Boolean {
    // Fallback heuristic based on your human text patterns:
    val s = summary.lowercase()
    return s.contains(" image was detected near your child")
            || (s.contains(" posted a ") && s.contains(" image"))
}