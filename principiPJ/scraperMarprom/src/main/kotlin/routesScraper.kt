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

<<<<<<< HEAD

=======
>>>>>>> 94035348ab0f31ce71a902353fd988e4e577f437
    private fun scrapeRoutePath(route: String): List<List<Double>> {
        val url = "$baseUrl/?datum=$date&route1=&route=$route"
        val coords = mutableListOf<List<Double>>()
        try {
            val doc = Jsoup.connect(url).get()
            val scripts = doc.select("script").map { it.html() }
            val arrayRegex = Regex("path\\s*:\\s*\\[([\\s\\S]*?)\\]")
            val latLngPattern = Pattern.compile("new google\\.maps\\.LatLng\\((-?[0-9]+\\.[0-9]+),\\s*(-?[0-9]+\\.[0-9]+)\\)")

            for (scriptHtml in scripts) {
                if (scriptHtml.contains("new google.maps.Polyline")) {
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

fun main() {
    val date = "2025-05-19"
    val scraper = MarpromRouteScraper(date)
    val routes = scraper.scrapeAllRoutes()

    val gson = GsonBuilder().setPrettyPrinting().create()
    val jsonOutput = gson.toJson(routes)
    File("../../sharedLibraries/routes_maribor_$date.json").writeText(jsonOutput)

    println("Scraping completed. Total routes: ${'$'}{routes.size}")
}
