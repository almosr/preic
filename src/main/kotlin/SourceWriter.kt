import models.SourceLineWithNumber
import java.io.PrintStream

class SourceWriter(private val outputFile: PrintStream) {

    fun execute(source: List<SourceLineWithNumber>) {
        source.forEach {
            outputFile.println("${it.basicLineNumber} ${it.sourceLine.content}")
        }
    }
}