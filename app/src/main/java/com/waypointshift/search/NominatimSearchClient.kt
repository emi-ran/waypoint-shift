package com.waypointshift.search

import com.waypointshift.model.GeoPoint
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class PlaceSearchResult(
    val label: String,
    val point: GeoPoint,
)

interface PlaceSearchClient {
    suspend fun search(query: String): List<PlaceSearchResult>
}

class NominatimSearchClient : PlaceSearchClient {
    override suspend fun search(query: String): List<PlaceSearchResult> = withContext(Dispatchers.IO) {
        if (query.trim().length < 3) return@withContext emptyList()
        val encoded = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8.name())
        val connection = (java.net.URL(
            "https://nominatim.openstreetmap.org/search?format=jsonv2&limit=5&q=$encoded",
        ).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8_000
            readTimeout = 8_000
            setRequestProperty("User-Agent", "WaypointShift/0.1 (personal Android mock-location app)")
            setRequestProperty("Accept", "application/json")
        }
        try {
            check(connection.responseCode == HttpURLConnection.HTTP_OK) { "Address search is temporarily unavailable" }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            Json.parseToJsonElement(body).jsonArray.mapNotNull { item ->
                val objectValue = item.jsonObject
                val latitude = objectValue["lat"]?.jsonPrimitive?.content?.toDoubleOrNull()
                val longitude = objectValue["lon"]?.jsonPrimitive?.content?.toDoubleOrNull()
                val label = objectValue["display_name"]?.jsonPrimitive?.content
                if (latitude != null && longitude != null && label != null) {
                    runCatching { PlaceSearchResult(label, GeoPoint.of(latitude, longitude)) }.getOrNull()
                } else null
            }
        } finally {
            connection.disconnect()
        }
    }
}
