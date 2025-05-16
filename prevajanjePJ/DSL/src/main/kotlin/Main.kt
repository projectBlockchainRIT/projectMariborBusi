import java.io.File

fun main() {
    var LA = LexicalAnalyzer()
    var SA = SyntaxAnalyzer()

    var tokens = listOf<String>()

    File("src/main/kotlin/test.txt").bufferedReader().use { reader ->
        reader.forEachLine { line ->
            tokens = LA.getTokens(line)


            println(tokens)
        }
    }
}
