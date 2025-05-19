import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.call.*
import kotlinx.coroutines.*
import java.io.File

suspend fun main() {
    val client = HttpClient(CIO)

    val query = """
        [out:json][timeout:25];
        area["name"="Maribor"]->.searchArea;
        node["highway"="bus_stop"](area.searchArea);
        out body;
        >;
        out skel qt;
    """.trimIndent()

    val response: HttpResponse = client.post("https://overpass-api.de/api/interpreter") {
        setBody("data=$query")
        headers.append("Content-Type", "application/x-www-form-urlencoded")
    }

    val json = response.bodyAsText()
    File("../../sharedLibraries/bus_stops_maribor.json").writeText(json)
    println("✔ Saved Overpass JSON to bus_stops_maribor.json")

    client.close()
}