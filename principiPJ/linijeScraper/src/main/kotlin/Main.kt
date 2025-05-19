import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import java.io.File

suspend fun main() {
    val client = HttpClient(CIO)

    val query = """
        [out:json][timeout:120];
        area["name"="Maribor"]["admin_level"="8"]->.maribor;
        (
          relation["route"="bus"][~"^(operator|network)${'$'}"~"Marprom",i](area.maribor);
          relation["route"="bus"]["ref"~"^(1[0-2]?|[2-9]|[1-9][A-Z])${'$'}"](area.maribor); // Typical Marprom numbering
        );
        
        /*added by auto repair*/
        (._;>;);
        /*end of auto repair*/
        out body;
        (
          node(r:"stop")[~"^name|local_ref"~"."](area.maribor);
          way(r:"stop")[~"^name|local_ref"~"."](area.maribor);
        );
        
        /*added by auto repair*/
        (._;>;);
        /*end of auto repair*/
        out;
    """.trimIndent()



    val response: HttpResponse = client.post("https://overpass-api.de/api/interpreter") {
        setBody("data=$query")
        headers.append("Content-Type", "application/x-www-form-urlencoded")
    }

    val json = response.bodyAsText()
    File("../../sharedLibraries/bus_routes_maribor.json").writeText(json)
    println("✔ Saved Overpass JSON to bus_stops_maribor.json")

    client.close()



}