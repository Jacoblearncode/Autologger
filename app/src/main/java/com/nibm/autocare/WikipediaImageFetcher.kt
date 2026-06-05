package com.nibm.autocare

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object WikipediaImageFetcher {

    suspend fun fetchCarImageUrl(brand: String, model: String): String? =
        withTimeoutOrNull(4000) {
            withContext(Dispatchers.IO) {
                buildQueries(brand, model).firstNotNullOfOrNull { fetchThumbnail(it) }
            }
        }

    private fun buildQueries(brand: String, model: String): List<String> {
        val b = brand.replace(" ", "_")
        val m = model.replace(" ", "_")
        val queries = mutableListOf("${b}_${m}")

        when (brand) {
            "BMW" -> {
                // "530i" → "BMW_5_Series", "M3" stays as "BMW_M3"
                Regex("^(\\d)").find(model)?.let { queries.add("BMW_${it.value}_Series") }
            }
            "Mercedes-Benz" -> {
                // "C200" → "Mercedes-Benz_C-Class", "GLC200" → "Mercedes-Benz_GLC-Class"
                Regex("^([A-Z]+)(?=\\d)").find(model)?.let {
                    queries.add("Mercedes-Benz_${it.value}-Class")
                }
            }
            "Audi" -> {
                // "A4 2.0T" → "Audi_A4"
                Regex("^([A-Z]\\d)").find(model)?.let { queries.add("Audi_${it.value}") }
            }
            "Volkswagen" -> {
                queries.add("Volkswagen_${m}")
            }
        }

        // Fallback: just the brand article (gives brand logo/representative image)
        queries.add(b)
        return queries.distinct()
    }

    private fun fetchThumbnail(articleTitle: String): String? {
        return try {
            val conn = (URL("https://en.wikipedia.org/api/rest_v1/page/summary/$articleTitle")
                .openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Autologger-Android/1.0 (educational project)")
                connectTimeout = 5000
                readTimeout = 5000
            }
            if (conn.responseCode != 200) { conn.disconnect(); return null }
            val json = JSONObject(conn.inputStream.bufferedReader().readText())
            conn.disconnect()

            // Prefer originalimage (full res) over compressed thumbnail
            val imageUrl = json.optJSONObject("originalimage")?.optString("source")?.takeIf { it.isNotEmpty() }
                ?: json.optJSONObject("thumbnail")?.optString("source")?.takeIf { it.isNotEmpty() }

            // Reject SVG (logos/flags) — we want actual car photos
            if (imageUrl != null && !imageUrl.endsWith(".svg", ignoreCase = true)) imageUrl else null
        } catch (e: Exception) {
            null
        }
    }
}
