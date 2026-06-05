package com.nibm.autocare

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI

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
                // "530i" → "BMW_5_Series", "M3" → "BMW_M3", "i4" → "BMW_i4"
                Regex("^(\\d)").find(model)?.let { queries.add("BMW_${it.value}_Series") }
                if (Regex("^M\\d").containsMatchIn(model)) queries.add("BMW_M${model.drop(1).substringBefore(" ")}")
                if (model.startsWith("i") && model.length <= 3) queries.add("BMW_${model.substringBefore(" ")}")
            }
            "Mercedes-Benz" -> {
                // "C200" → "Mercedes-Benz_C-Class", "AMG GT" → "Mercedes-AMG_GT"
                Regex("^([A-Z]+)(?=\\d)").find(model)?.let {
                    queries.add("Mercedes-Benz_${it.value}-Class")
                }
                if (model.startsWith("AMG")) {
                    queries.add("Mercedes-AMG_${model.removePrefix("AMG").trim().replace(" ", "_")}")
                }
            }
            "Audi" -> {
                // "A4 2.0T" → "Audi_A4", "RS6" → "Audi_RS_6", "R8" → "Audi_R8"
                Regex("^([A-Z]\\d)").find(model)?.let { queries.add("Audi_${it.value}") }
                if (model.startsWith("RS")) queries.add("Audi_RS_${model.removePrefix("RS").trim().substringBefore(" ")}")
                if (model.startsWith("R8")) queries.add("Audi_R8")
                if (model.startsWith("e-tron")) queries.add("Audi_e-tron_GT")
            }
            "Porsche" -> {
                // "911 Carrera" → "Porsche_911", "718 Boxster" → "Porsche_718_Boxster"
                Regex("^(\\d+)").find(model)?.let { queries.add("Porsche_${it.value}") }
                listOf("Taycan", "Panamera", "Cayenne", "Macan").forEach { base ->
                    if (model.startsWith(base)) queries.add("Porsche_${base}")
                }
            }
            "Ferrari" -> {
                val clean = model.replace(" ", "_")
                queries.add("Ferrari_${clean}")
            }
            "Lamborghini" -> {
                // Huracán → try ASCII fallback "Huracan"
                val ascii = model.replace("á", "a").replace("é", "e")
                    .replace("ó", "o").replace("ú", "u").replace(" ", "_")
                queries.add("Lamborghini_${ascii}")
            }
            "McLaren" -> {
                Regex("^(\\d+[A-Z]*)").find(model)?.let { queries.add("McLaren_${it.value}") }
            }
            "Rolls-Royce" -> {
                queries.add("Rolls-Royce_${m}")
                queries.add("Rolls-Royce_Motor_Cars")
            }
            "Bentley" -> {
                queries.add("Bentley_${m}")
                queries.add("Bentley_Motors")
            }
            "Aston Martin" -> {
                queries.add("Aston_Martin_${m}")
                queries.add("Aston_Martin")
            }
            "Maserati" -> {
                queries.add("Maserati_${m}")
            }
            "Tesla" -> {
                queries.add("Tesla_${m}")
            }
            "Toyota" -> {
                if (model.startsWith("GR")) queries.add("Toyota_${m}")
                if (model == "Land Cruiser") queries.add("Toyota_Land_Cruiser")
                if (model.startsWith("Supra")) queries.add("Toyota_Supra")
            }
            "Honda" -> {
                if (model.contains("Type R")) queries.add("Honda_Civic_Type_R")
                if (model == "NSX") queries.add("Honda_NSX")
                if (model == "S2000") queries.add("Honda_S2000")
            }
            "Subaru" -> {
                if (model.contains("STI") || model.contains("STi")) {
                    queries.add("Subaru_WRX_STI")
                    queries.add("Subaru_Impreza_WRX_STi")
                }
                if (model == "WRX") queries.add("Subaru_WRX")
                if (model == "BRZ") queries.add("Subaru_BRZ")
            }
            "Volkswagen" -> {
                if (model.startsWith("Golf")) queries.add("Volkswagen_Golf")
                if (model.startsWith("ID.")) {
                    val num = model.removePrefix("ID.").substringBefore(" ")
                    queries.add("Volkswagen_ID.$num")
                }
            }
            "Nissan" -> {
                if (model == "GT-R") queries.add("Nissan_GT-R")
                if (model.endsWith("Z")) queries.add("Nissan_Z_(2023)")
            }
            "Lexus" -> {
                // "IS 300" → "Lexus_IS", "RX 350" → "Lexus_RX"
                Regex("^([A-Z]+)").find(model)?.let { queries.add("Lexus_${it.value}") }
            }
            "Land Rover" -> {
                if (model.startsWith("Range Rover")) {
                    val suffix = model.removePrefix("Range Rover").trim().replace(" ", "_")
                    queries.add(if (suffix.isEmpty()) "Range_Rover" else "Range_Rover_${suffix}")
                }
                queries.add("Land_Rover_${m}")
            }
            "Jaguar" -> {
                if (model == "F-Type") queries.add("Jaguar_F-Type")
                if (model.startsWith("F-Pace")) queries.add("Jaguar_F-Pace")
            }
            "Volvo" -> {
                Regex("^([A-Z][A-Z0-9]+)").find(model)?.let { queries.add("Volvo_${it.value}") }
            }
            "Kia" -> {
                if (model == "EV6") queries.add("Kia_EV6")
                if (model == "Stinger") queries.add("Kia_Stinger")
            }
            "Hyundai" -> {
                if (model.startsWith("Ioniq")) queries.add("Hyundai_${m}")
            }
            "Chevrolet" -> {
                if (model.startsWith("Corvette")) queries.add("Chevrolet_Corvette")
                if (model.startsWith("Camaro")) queries.add("Chevrolet_Camaro")
            }
            "Dodge" -> {
                if (model.startsWith("Challenger")) queries.add("Dodge_Challenger")
                if (model.startsWith("Viper")) queries.add("Dodge_Viper")
            }
            "Polestar" -> {
                Regex("(\\d)").find(model)?.let { queries.add("Polestar_${it.value}") }
            }
            "Alfa Romeo" -> {
                queries.add("Alfa_Romeo_${m}")
            }
            "Bugatti" -> {
                queries.add("Bugatti_${m}")
            }
        }

        // Fallback: brand article gives a representative image
        queries.add(b)
        return queries.distinct()
    }

    private fun fetchThumbnail(articleTitle: String): String? {
        return try {
            // URI constructor handles accented chars (e.g. Huracán → %C3%A1)
            val uri = URI("https", "en.wikipedia.org", "/api/rest_v1/page/summary/$articleTitle", null)
            val conn = (uri.toURL().openConnection() as HttpURLConnection).apply {
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
