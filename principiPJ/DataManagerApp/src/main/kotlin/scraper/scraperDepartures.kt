package scraper

import org.jsoup.Jsoup
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.io.File
import com.google.gson.GsonBuilder
import java.time.LocalTime
import java.time.format.DateTimeParseException

data class BusStop(
    val id: String,
    val number: String,
    val name: String,
    val departures: List<Departure>
)

data class Departure(
    val line: String,
    val direction: String,
    val times: List<String>,
    val date: String
)

data class BusStopInfoNext(
    val id: String,
    val number: String,
    val name: String
)

class MarpromScraper {
    private val baseUrl = "https://vozniredi.marprom.si"
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private fun getDateString(date: LocalDate): String = date.format(dateFormatter)

    fun scrapeAllStopsForDateRange(today: LocalDate, numberOfPastDays: Int, numberOfFutureDays: Int): Map<String, List<BusStop>> {
        val schedules = mutableMapOf<String, List<BusStop>>()
        val stopsInfo = getAllStopsInfo()

        if (stopsInfo.isEmpty()) {
            System.err.println("No bus stop information found. Cannot proceed with scraping.")
            return emptyMap()
        }

        val startDate = today.minusDays(numberOfPastDays.toLong())
        val endDate = today.plusDays((numberOfFutureDays - 1).toLong())

        var currentDate = startDate
        while (!currentDate.isAfter(endDate)) {
            val dateString = getDateString(currentDate)
            println("Scraping data for date: $dateString")

            val stopsForDate = stopsInfo.map { stopInfo ->
                val departures = scrapeStopDetailsForDate(stopInfo.id, currentDate)
                BusStop(stopInfo.id, stopInfo.number, stopInfo.name, departures)
            }

            schedules[dateString] = stopsForDate
            currentDate = currentDate.plusDays(1)
        }

        return schedules
    }

    private fun getAllStopsInfo(): List<BusStopInfoNext> {
        val stops = mutableListOf<BusStopInfoNext>()
        try {
            val doc = Jsoup.connect("$baseUrl/").userAgent("Mozilla/5.0").get()
            val rows = doc.select("table#TableOfStops > tbody > tr")

            for (row in rows) {
                val onclickAttr = row.attr("onclick")
                val stopId = onclickAttr.substringAfter("stop=").substringBefore("&").trim()
                val tds = row.select("td")
                if (tds.size >= 2) {
                    val stopNumber = tds[1].select("b.paddingTd").first()?.text()?.trim() ?: ""
                    val stopName = tds[1].select("b:not(.paddingTd)").first()?.text()?.trim() ?: ""
                    stops.add(BusStopInfoNext(stopId, stopNumber, stopName))
                }
            }
        } catch (e: Exception) {
            System.err.println("Failed to fetch bus stops: ${e.message}")
        }
        return stops
    }

    private fun scrapeStopDetailsForDate(stopId: String, date: LocalDate): List<Departure> {
        val departures = mutableListOf<Departure>()
        try {
            val dateStr = getDateString(date)
            val doc = Jsoup.connect("$baseUrl/?stop=$stopId&datum=$dateStr").userAgent("Mozilla/5.0").get()
            val tables = doc.select("div.modal-body table.table-bordered")

            if (tables.size > 2) {
                for (i in 2 until tables.size) {
                    val table = tables[i]
                    val title = table.previousElementSibling()

                    if (title != null && title.text().contains("Naslednji odhodi za linijo")) {
                        val line = title.select("span.modal-btn-route-position").text().trim()
                        val rows = table.select("tbody tr:not(:has(td.tdBackColor))")

                        for (row in rows) {
                            val direction = row.select("td.tdWidth").text().trim()
                            val timesText = row.select("td:not(.tdWidth)").text().trim()

                            val times = timesText.split("\\s+".toRegex())
                                .filter { it.isNotBlank() && isValidTimeFormat(it) }
                                .sortedWith(compareBy { LocalTime.parse(it) })

                            if (times.isNotEmpty()) {
                                departures.add(Departure(line, direction, times, dateStr))
                            }
                        }
                    }
                }
            }
        } catch (e: IOException) {
            System.err.println("Error fetching stop $stopId for $date: ${e.message}")
        }
        return departures
    }

    private fun isValidTimeFormat(time: String): Boolean = try {
        LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"))
        true
    } catch (e: DateTimeParseException) {
        false
    }
}

suspend fun runDeparturesScraperToLocation(outputPath: String, daysBack: Int = 0, daysForward: Int = 1) {
    val scraper = MarpromScraper()
    val today = LocalDate.now()
    val result = scraper.scrapeAllStopsForDateRange(today, daysBack, daysForward)

    val gson = GsonBuilder().setPrettyPrinting().create()
    val jsonOutput = gson.toJson(result)

    try {
        File(outputPath).writeText(jsonOutput)
        println("Scraping completed. Saved to $outputPath. Total dates: ${result.size}")
    } catch (e: IOException) {
        println("Error writing file: ${e.message}")
    }
}

suspend fun runDeparturesScraperAsJson(daysBack: Int = 0, daysForward: Int = 1): String {
    val scraper = MarpromScraper()
    val today = LocalDate.now()
    val result = scraper.scrapeAllStopsForDateRange(today, daysBack, daysForward)

    val gson = GsonBuilder().setPrettyPrinting().create()
    return gson.toJson(result)
}

fun main() {
    val scraper = MarpromScraper()
    val today = LocalDate.now()
    val result = scraper.scrapeAllStopsForDateRange(today, 2, 3)

    val gson = GsonBuilder().setPrettyPrinting().create()
    val output = gson.toJson(result)

    val outputFile = File("../../sharedLibraries/bus_schedules_daily_snapshot.json")
    outputFile.parentFile.mkdirs()
    outputFile.writeText(output)

    println("Saved to ${outputFile.absolutePath}")
}
