import org.jsoup.Jsoup
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.io.File
import com.google.gson.GsonBuilder
import java.time.LocalTime
import java.time.format.DateTimeParseException

// Data class for basic bus stop information (used internally for scraping)
// This helps to get all stops once, then iterate through dates
data class BusStopInfoNext(
    val id: String,
    val number: String,
    val name: String
)

// Data classes for the output JSON structure, adjusted for multi-day
data class BusStop(
    val id: String,
    val number: String,
    val name: String,
    val departures: List<Departure>
)

// Crucially, the Departure class now includes the 'date' field
data class Departure(
    val line: String,
    val direction: String,
    val times: List<String>,
    val date: String // Added date to specify which day these times are for
)

class MarpromScraper {
    private val baseUrl = "https://vozniredi.marprom.si"
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private fun getDateString(date: LocalDate): String {
        return date.format(dateFormatter)
    }

    /**
     * Scrapes bus data for a specified number of days in the past and future,
     * maintaining the nested BusStop -> Departures structure for each day.
     *
     * @param today The current date to base the past and future scraping on.
     * @param numberOfPastDays The number of days to scrape before 'today' (e.g., 3 means today-1, today-2, today-3).
     * @param numberOfFutureDays The number of days to scrape from 'today' onwards into the future (e.g., 7 means today, today+1, ..., today+6).
     * @return A Map where keys are date strings (YYYY-MM-DD) and values are lists of BusStop objects for that date.
     */
    fun scrapeAllStopsForDateRange(
        today: LocalDate,
        numberOfPastDays: Int,
        numberOfFutureDays: Int
    ): Map<String, List<BusStop>> {
        val dailyBusSchedules = mutableMapOf<String, List<BusStop>>()

        // First, get the list of all stops only once
        val stopsListInfo = getAllStopsInfo()
        if (stopsListInfo.isEmpty()) {
            System.err.println("No bus stop information found. Cannot proceed with scraping.")
            return emptyMap()
        }

        // Calculate the start and end dates for scraping
        val startScrapeDate = today.minusDays(numberOfPastDays.toLong())
        val endScrapeDate = today.plusDays(numberOfFutureDays.toLong() - 1) // Subtract 1 because numberOfFutureDays includes 'today'

        var currentDate = startScrapeDate
        while (!currentDate.isAfter(endScrapeDate)) {
            val dateString = getDateString(currentDate)
            println("Scraping data for date: $dateString")

            val busStopsForThisDate = mutableListOf<BusStop>()

            for (stopInfo in stopsListInfo) {
                // println("  Scraping stop: ${stopInfo.name} (${stopInfo.id}) for $dateString") // Uncomment for more detailed logging

                // Scrape details for the current stop and current date
                val departuresForStopAndDate = scrapeStopDetailsForDate(stopInfo.id, currentDate)

                // Add the scraped departures to a BusStop object for this date
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

    /**
     * Scrapes the main page to get a list of all bus stops (ID, number, name).
     * This is an internal helper to avoid re-scraping the stop list for every date.
     * @return A list of [BusStopInfo] objects.
     */
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

    /**
     * Scrapes the departure details for a specific bus stop on a given date.
     * @param stopId The ID of the bus stop.
     * @param date The date for which to scrape departures.
     * @return A list of [Departure] objects for the specified stop and date.
     */
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

                            // Filter for valid time formats and sort them
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

    /**
     * Checks if a string is a valid time format (HH:mm).
     */
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

    // --- Configuration for scraping date range ---
    val today = LocalDate.now()
    val numberOfPastDays = 1 // Scrape 3 days BEFORE today (e.g., if today is June 6, scrape June 3, 4, 5)
    val numberOfFutureDays = 2 // Scrape 7 days FROM today (e.g., if today is June 6, scrape June 6, 7, 8, 9, 10, 11, 12)
    // Total days scraped will be numberOfPastDays + numberOfFutureDays

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