package scraper

import org.jsoup.Jsoup
import java.io.IOException
import java.io.File
import com.google.gson.GsonBuilder
import java.util.regex.Pattern

data class RouteInfo(
    val route: String,
    val date: String,
    val path: List<List<Double>>
)

class MarpromRouteScraper(private val date: String) {
    private val baseUrl = "https://vozniredi.marprom.si"
    private val routes = listOf(
        "G1", "G2", "G3", "G4", "G5", "G6",
        "P7", "P8", "P9", "P10", "P11", "P12", "P13", "P14", "P15", "P16", "P17", "P18", "P19"
    )

    fun scrapeAllRoutes(): List<RouteInfo> {
        val result = mutableListOf<RouteInfo>()
        for (route in routes) {
            println("Scraping route $route for date $date...")
            val coords = scrapeRoutePath(route)
            if (coords.isNotEmpty()) {
                result.add(RouteInfo(route = route, date = date, path = coords))
            } else {
                println("No coordinates found for route $route")
            }
        }
        return result
    }

    /**
     * Fetches the HTML for a single route and extracts all clusters of path coordinates
     * in the exact order within each `path: [ ... ]` array literal, concatenating them.
     */
    private fun scrapeRoutePath(route: String): List<List<Double>> {
        val url = "$baseUrl/?datum=$date&route1=&route=$route"
        val coords = mutableListOf<List<Double>>()
        try {
            val doc = Jsoup.connect(url).get()
            // collect all script blocks
            val scripts = doc.select("script").map { it.html() }
            // pattern to find each PathStyle's array literal
            val arrayRegex = Regex("path\\s*:\\s*\\[([\\s\\S]*?)\\]")
            // pattern to match LatLng within literal
            val latLngPattern = Pattern.compile("new google\\.maps\\.LatLng\\((-?[0-9]+\\.[0-9]+),\\s*(-?[0-9]+\\.[0-9]+)\\)")

            for (scriptHtml in scripts) {
                if (scriptHtml.contains("new google.maps.Polyline")) {
                    // extract each cluster array
                    arrayRegex.findAll(scriptHtml).forEach { match ->
                        val arrayContent = match.groupValues[1]
                        val matcher = latLngPattern.matcher(arrayContent)
                        while (matcher.find()) {
                            val lat = matcher.group(1).toDoubleOrNull()
                            val lng = matcher.group(2).toDoubleOrNull()
                            if (lat != null && lng != null) {
                                coords.add(listOf(lat, lng))
                            }
                        }
                    }
                }
            }
        } catch (e: IOException) {
            println("Error fetching data for route $route: ${'$'}{e.message}")
        } catch (e: Exception) {
            println("Unexpected error for route $route: ${'$'}{e.message}")
        }
        return coords
    }
}

suspend fun runRoutesScraperToLocation(outputPath: String) {
    val stopsRoutes = MarpromRouteScraper("2025-5-22").scrapeAllRoutes()
    val gson = GsonBuilder().setPrettyPrinting().create()
    val jsonOutput = gson.toJson(stopsRoutes)

    try {
        File(outputPath).writeText(jsonOutput)
        println("Scraping completed. Saved to $outputPath. Total routes: ${stopsRoutes.size}")
    } catch (e: IOException) {
        println("Error writing file: ${e.message}")
    }
}

fun main() {
    val date = "2025-05-19" // or accept as command-line arg
    val scraper = MarpromRouteScraper(date)
    val routes = scraper.scrapeAllRoutes()

    // Convert to JSON and write
    val gson = GsonBuilder().setPrettyPrinting().create()
    val jsonOutput = gson.toJson(routes)
    File("../../sharedLibraries/NEW_routes_maribor_$date.json").writeText(jsonOutput)

    println("Scraping completed. Total routes: ${routes.size}")
}