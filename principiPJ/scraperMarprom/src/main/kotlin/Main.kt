import org.jsoup.Jsoup
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.io.File
import com.google.gson.GsonBuilder

data class BusStop(
    val id: String,
    val number: String,
    val name: String,
    val departures: List<Departure>
)

data class Departure(
    val line: String,
    val direction: String,
    val times: List<String>
)

class MarpromScraper {
    private val baseUrl = "https://vozniredi.marprom.si"
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private fun getTodayDateString(): String {
        return LocalDate.now().format(dateFormatter)
    }

    fun scrapeAllStops(): List<BusStop> {
        val stops = mutableListOf<BusStop>()

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

                    println("Scraping $stopName")

                    val stopDetails = scrapeStopDetails(stopId)

                    stops.add(BusStop(stopId, stopNumber, stopName, stopDetails))
                }
            }
        } catch (e: IOException) {
            println("Napaka pri pridobivanju podatkov: ${e.message}")
        } catch (e: Exception) {
            println("Neka random napaka: ${e.message}")
        }

        return stops
    }

    private fun scrapeStopDetails(stopId: String): List<Departure> {
        val departures = mutableListOf<Departure>()

        try {
            val todayDate = getTodayDateString()
            val doc = Jsoup.connect("$baseUrl/?stop=$stopId&datum=$todayDate").get()

            val tables = doc.select("div.modal-body table.table-bordered")

            if (tables.size > 2) {
                for (i in 2 until tables.size) {
                    val table = tables[i]
                    val titleElement = table.previousElementSibling()

                    if (titleElement != null && titleElement.text().contains("Naslednji odhodi za linijo")) {
                        val line = titleElement.select("span.modal-btn-route-position").text().trim()

                        val rows = table.select("tbody tr:not(:has(td.tdBackColor))")

                        for (row in rows) {
                            val direction = row.select("td.tdWidth").text().trim()
                            val timesText = row.select("td:not(.tdWidth)").text().trim()

                            val times = timesText.split("\\s+".toRegex())
                                .filter { it.isNotBlank() && it.matches(Regex("""\d{2}:\d{2}""")) }

                            if (times.isNotEmpty()) {
                                departures.add(Departure(line, direction, times))
                            }
                        }
                    }
                }
            }
        } catch (e: IOException) {
            println("Napaka pri pridobivanju podrobnosti postaje $stopId: ${e.message}")
        }

        return departures
    }
}

fun main() {
    val scraper = MarpromScraper()
    val allStops = scraper.scrapeAllStops()

    val gson = GsonBuilder().setPrettyPrinting().create()
    val json = gson.toJson(allStops)

    val outputFile = File("../../sharedLibraries/bus_arrival_times_stops_maribor.json")
    outputFile.writeText(json)
}