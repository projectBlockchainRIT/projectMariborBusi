import java.io.File

fun main() {
    var LA = LexicalAnalyzer()
    var SA = SyntaxAnalyzer()

    File("DSL/src/main/kotlin/test.txt").bufferedReader().use { reader ->
        reader.forEachLine { line ->
            var tokens = LA.getTokens(line)



            println(tokens)
        }
    }
}
