import LexicalAnalyzer
import java.io.File

fun main() {
    var LA = LexicalAnalyzer()

    File("test.txt").bufferedReader().use { reader ->
        reader.forEachLine { line ->
            var tokens = LA.getTokens(line)

            println(tokens)
        }
    }
}
