import org.jsoup.Jsoup
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.io.File
import com.google.gson.GsonBuilder
import java.time.LocalTime
import java.time.format.DateTimeParseException


data class BusStopInfoNext(
    val id: String,
    val number: String,
    val name: String
)


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

class MarpromScraper {
    private val baseUrl = "https://vozniredi.marprom.si"
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private fun getDateString(date: LocalDate): String {
        return date.format(dateFormatter)
    }


    fun scrapeAllStopsForDateRange(
        today: LocalDate,
        numberOfPastDays: Int,
        numberOfFutureDays: Int
    ): Map<String, List<BusStop>> {
        val dailyBusSchedules = mutableMapOf<String, List<BusStop>>()


        val stopsListInfo = getAllStopsInfo()
        if (stopsListInfo.isEmpty()) {
            System.err.println("No bus stop information found. Cannot proceed with scraping.")
            return emptyMap()
        }


        val startScrapeDate = today.minusDays(numberOfPastDays.toLong())
        val endScrapeDate = today.plusDays(numberOfFutureDays.toLong() - 1) 

        var currentDate = startScrapeDate
        while (!currentDate.isAfter(endScrapeDate)) {
            val dateString = getDateString(currentDate)
            println("Scraping data for date: $dateString")

            val busStopsForThisDate = mutableListOf<BusStop>()

            for (stopInfo in stopsListInfo) {

                val departuresForStopAndDate = scrapeStopDetailsForDate(stopInfo.id, currentDate)


                busStopsForThisDate.add(
                    BusStop(
                        id = stopInfo.id,
                        number = stopInfo.number,
                        name = stopInfo.name,
                        departures = departuresForStopAndDate
                    )
                )
            }
            dailyBusSchedules[dateString] = busStopsForThisDate
            currentDate = currentDate.plusDays(1)
        }

        return dailyBusSchedules
    }

    private fun getAllStopsInfo(): List<BusStopInfoNext> {
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


                    println("Scraping $stopName")

                    val stopDetails = scrapeStopDetails(stopId)

                    stops.add(BusStop(stopId, stopNumber, stopName, stopDetails))

                    stops.add(BusStopInfoNext(stopId, stopNumber, stopName))

                }
            }
        } catch (e: IOException) {
            System.err.println("Error fetching stop data: ${e.message}")
        } catch (e: Exception) {
            System.err.println("An unexpected error occurred while fetching stops: ${e.message}")
        }
        return stops
    }


    private fun scrapeStopDetailsForDate(stopId: String, date: LocalDate): List<Departure> {
        val departures = mutableListOf<Departure>()
        try {
            val dateString = getDateString(date)
            val doc = Jsoup.connect("$baseUrl/?stop=$stopId&datum=$dateString").get()

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
                                .filter { it.isNotBlank() && isValidTimeFormat(it) }
                                .sortedWith(compareBy { LocalTime.parse(it) })


                            if (times.isNotEmpty()) {
                                departures.add(
                                    Departure(
                                        line = line,
                                        direction = direction,
                                        times = times,
                                        date = dateString // Include the date in the Departure object
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: IOException) {
            System.err.println("Error fetching details for stop $stopId on $date: ${e.message}")
        }
        return departures
    }

    private fun isValidTimeFormat(time: String): Boolean {
        return try {
            LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"))
            true
        } catch (e: DateTimeParseException) {
            false
        }
    }
}

fun main() {
    val scraper = MarpromScraper()


    val today = LocalDate.now()
    val numberOfPastDays = 1 
    val numberOfFutureDays = 2 


    println("Starting to scrape data for dates from ${today.minusDays(numberOfPastDays.toLong())} to ${today.plusDays(numberOfFutureDays.toLong() - 1)}")

    val dailyBusSchedules = scraper.scrapeAllStopsForDateRange(
        today,
        numberOfPastDays,
        numberOfFutureDays
    )

    val gson = GsonBuilder().setPrettyPrinting().create()
    val jsonOutput = gson.toJson(dailyBusSchedules)

    val outputDir = File("../../sharedLibraries")
    outputDir.mkdirs() // Ensure the output directory exists

    val outputFile = File(outputDir, "bus_schedules_daily_snapshot.json")
    outputFile.writeText(jsonOutput)
    println("Daily bus schedules saved to ${outputFile.absolutePath}")

    println("Scraping completed successfully!")
    println("Total dates scraped: ${dailyBusSchedules.size}")
    println("Example entry for today (${today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}) has ${dailyBusSchedules[today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))]?.size ?: 0} bus stops.")
}