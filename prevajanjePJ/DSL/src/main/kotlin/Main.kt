import java.io.File

fun main() {
    var LA = LexicalAnalyzer()

    var tokens = listOf<String>()

    File("src/main/kotlin/test.txt").bufferedReader().use { reader ->
        reader.forEachLine { line ->

            tokens = tokens + LA.getTokens(line)

        }
    }
    println(tokens)

    var SA = SyntaxAnalyzer(tokens)

    val ast = SA.parse()
    //val geoJsonConverter = GeoJsonConverter()
    //val geoJson = geoJsonConverter.convertToGeoJson(ast)

    // Save to file for viewing in geojson.io
    //geoJsonConverter.saveToFile(geoJson, "output.geojson")
    println("GeoJSON saved to output.geojson")
}
