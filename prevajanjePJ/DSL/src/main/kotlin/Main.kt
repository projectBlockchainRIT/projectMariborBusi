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
    SA.parseTokens()
    var result = SA.parse()
    println(result)


}
