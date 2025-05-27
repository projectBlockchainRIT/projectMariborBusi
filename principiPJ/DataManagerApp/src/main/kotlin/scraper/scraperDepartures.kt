package scraper

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
            val doc = Jsoup.connect("$baseUrl/").userAgent("Mozilla/5.0").get()

            //postajalisce - iskanje po id=TableOfStops
            val stopRows = doc.select("table#TableOfStops > tbody > tr")
            println("Najdenih vrstic: ${stopRows.size}")
            //podrobnost postajalisca
            for (row in stopRows) {
                //onClick vsebuje ID med 'stop=' in '&'
                val onclickAttr = row.attr("onclick")
                val stopId = onclickAttr.substringAfter("stop=").substringBefore("&").trim()

                //stevilka & ime postaje
                val tds = row.select("td")
                if (tds.size >= 2) {
                    val stopNumber = tds[1].select("b.paddingTd").first()?.text()?.trim() ?: ""
                    val stopName = tds[1].select("b:not(.paddingTd)").first()?.text()?.trim() ?: ""

                    println("Scraping $stopName")

                    //odhodne linije & casi
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
            val doc = Jsoup.connect("$baseUrl/?stop=$stopId&datum=$todayDate").userAgent("Mozilla/5.0").get()

            //odhodi PO linijah
            val tables = doc.select("div.modal-body table.table-bordered")

            //prvi dve tabeli NIMATA linij
            if (tables.size > 2) {
                for (i in 2 until tables.size) {
                    val table = tables[i]
                    val titleElement = table.previousElementSibling()

                    //ali je naslov "Naslednji odhodi za linijo"
                    if (titleElement != null && titleElement.text().contains("Naslednji odhodi za linijo")) {
                        //stevilka LINIJE
                        val line = titleElement.select("span.modal-btn-route-position").text().trim()

                        //casi odhodov iz linije
                        val rows = table.select("tbody tr:not(:has(td.tdBackColor))")

                        for (row in rows) {
                            //levi del tabele(ime postaje) ma CLASS tdWidth, desni(casi odhoda) NIMA
                            val direction = row.select("td.tdWidth").text().trim()
                            val timesText = row.select("td:not(.tdWidth)").text().trim()

                            //case razdeli na posamezne stringe xx:xx
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

suspend fun runDeparturesScraperToLocation(outputPath: String) {
    val stopsDepartures = MarpromScraper().scrapeAllStops()
    val gson = GsonBuilder().setPrettyPrinting().create()
    val jsonOutput = gson.toJson(stopsDepartures)

    try {
        File(outputPath).writeText(jsonOutput)
        println("Scraping completed. Saved to $outputPath. Total routes: ${stopsDepartures.size}")
    } catch (e: IOException) {
        println("Error writing file: ${e.message}")
    }
}

suspend fun runDeparturesScraperAsJson(): String {
    val stopsDepartures = MarpromScraper().scrapeAllStops()
    val gson = GsonBuilder().setPrettyPrinting().create()
    return gson.toJson(stopsDepartures)
}

fun main() {
    val scraper = MarpromScraper()
    val allStops = scraper.scrapeAllStops()

    val gson = GsonBuilder().setPrettyPrinting().create()
    val json = gson.toJson(allStops)

    val outputFile = File("../../sharedLibraries/NEW_bus_stops_departures.json")
    outputFile.writeText(json)
}