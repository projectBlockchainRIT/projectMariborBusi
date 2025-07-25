import org.jsoup.Jsoup
import java.io.IOException
import java.io.File
import com.google.gson.GsonBuilder
import java.util.regex.Pattern

data class BusStopInfoNext(
    val id: String,
    val number: String,
    val name: String,
    val latitude: Double?,
    val longitude: Double?
)

class MarpromStopScraper {
    private val baseUrl = "https://vozniredi.marprom.si"

    fun scrapeAllStops(): List<BusStopInfoNext> {
        val stops = mutableListOf<BusStopInfoNext>()

        try {
            val doc = Jsoup.connect("$baseUrl/").get()

            val stopRows = doc.select("table#TableOfStops > tbody > tr")

            for (row in stopRows) {
                val onclickAttr = row.attr("onclick")
                val stopId = onclickAttr.substringAfter("stop=").substringBefore("&").trim()

                val tds = row.select("td")
                if (tds.size >= 2) {
                    val stopNumber = tds[1].select("b.paddingTd").first()?.text()?.trim() ?: ""
                    val stopName = tds[1].select("b:not(.paddingTd)").first()?.text()?.trim() ?: ""

                    println("Scraping stop info for: $stopName")

                    val coordinates = scrapeStopCoordinates(stopId)

                    stops.add(BusStopInfoNext(
                        id = stopId,
                        number = stopNumber,
                        name = stopName,
                        latitude = coordinates.first,
                        longitude = coordinates.second
                    ))
                }
            }
        } catch (e: IOException) {
            println("Error fetching data: ${e.message}")
        } catch (e: Exception) {
            println("Unexpected error: ${e.message}")
        }

        return stops
    }

    private fun scrapeStopCoordinates(stopId: String): Pair<Double?, Double?> {
        try {
            val doc = Jsoup.connect("$baseUrl/?stop=$stopId").get()

            val scripts = doc.select("script")

            for (script in scripts) {
                val scriptContent = script.html()

                if (scriptContent.contains("new google.maps.Map") && scriptContent.contains("new google.maps.LatLng")) {
                    val latlngPattern = Pattern.compile("new google\\.maps\\.LatLng\\((\\d+\\.\\d+), (\\d+\\.\\d+)\\)")
                    val matcher = latlngPattern.matcher(scriptContent)

                    if (matcher.find()) {
                        val lat = matcher.group(1).toDoubleOrNull()
                        val lng = matcher.group(2).toDoubleOrNull()
                        return Pair(lat, lng)
                    }
                }
            }
        } catch (e: Exception) {
            println("Error getting coordinates for stop $stopId: ${e.message}")
        }

        return Pair(null, null)
    }
}

fun main() {
    val scraper = MarpromStopScraper()
    val busStops = scraper.scrapeAllStops()

    val gson = GsonBuilder().setPrettyPrinting().create()
    val json = gson.toJson(busStops)

    val outputFile = File("../../sharedLibraries/bus_stops_maribor.json")
    outputFile.writeText(json)

    println("Scraping completed successfully.")
    println("Total stops: ${busStops.size}")

    val stopsWithCoordinates = busStops.count { it.latitude != null && it.longitude != null }
    println("Stops with coordinates: $stopsWithCoordinates")
}