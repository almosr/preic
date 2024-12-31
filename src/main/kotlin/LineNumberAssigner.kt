import Constants.LABEL_PREFIX_LINE
import Constants.MAX_LABEL_ITERATIONS
import Tools.findAllValues
import Tools.processStartingLabel
import models.Label
import models.SourceLine
import models.SourceLineWithNumber

class LineNumberAssigner(
    private val labels: MutableMap<String, Label>,
) {

    private var basicLineNumber = 0

    fun execute(source: List<SourceLine>): List<SourceLineWithNumber> =
        source.generateNumberedLines()
            .replaceLineLabels()

    //Scan for line labels and record them, also assign line numbers to the lines
    private fun List<SourceLine>.generateNumberedLines() =
        mapNotNull { line ->
            val lineContent = if (line.content[0].isDigit()) {
                processLineNumber(line)
            } else {
                line.content
            }

            val newLineContent = if (lineContent.startsWith("{")) {
                processStartingLabel(labels, line, lineContent) { label, restOfLine ->
                    if (label[0] == LABEL_PREFIX_LINE) {
                        Pair(
                            Label.Line(
                                name = label.drop(1),
                                originalFormat = "{${label}}",
                                basicLineNumber = basicLineNumber
                            ),
                            //Return line content without the label
                            restOfLine
                        )
                    } else {
                        null
                    }
                }
            } else {
                lineContent
            }

            //Skip empty lines
            if (newLineContent.isBlank()) {
                return@mapNotNull null
            }

            SourceLineWithNumber(line.copy(content = newLineContent), basicLineNumber).also {
                basicLineNumber++
                if (basicLineNumber > MAX_BASIC_LINE_NUMBER) {
                    throw Exception("Maximum BASIC line number exceeded, valid range: 0-$MAX_BASIC_LINE_NUMBER")
                }
            }
        }

    //Replace line labels with line numbers
    private fun List<SourceLineWithNumber>.replaceLineLabels() =
        map { line ->
            var lineContent = line.sourceLine.content
            var replaceCount = 0

            do {
                //Find all variable and literal labels in the current line
                val lineLabels = LABEL_REGEX.findAllValues(lineContent)

                //Replace labels in current line with actual line numbers they represent
                lineLabels.forEach { labelStr ->
                    val originalLabel = "{$labelStr}"
                    val foundLabel = labels[originalLabel] ?: throw Exception("Undefined label: $labelStr\n$line")

                    lineContent = lineContent.replace(originalLabel, foundLabel.output)
                }

                replaceCount++
                if (replaceCount > MAX_LABEL_ITERATIONS) {
                    throw Exception("Maximum number of label processing iterations reached, possible recursive labels\n$line")
                }

                //Keep searching for labels in the current line until nothing left
            } while (lineLabels.isNotEmpty())

            //Output processed line
            line.copy(sourceLine = line.sourceLine.copy(content = lineContent))
        }

    private fun processLineNumber(line: SourceLine): String {
        //Find the end of the line number
        val index = line.content.indexOfFirst { !it.isDigit() }
            .takeIf { it >= 0 } ?: line.content.length
        val lineNumber = line.content.substring(0, index).toInt()

        //Line number cannot be lower than the current BASIC line number
        if (lineNumber < basicLineNumber) {
            throw Exception("BASIC line number is lower than current line number: $basicLineNumber\n$line")
        }

        if (lineNumber > MAX_BASIC_LINE_NUMBER) {
            throw Exception("Maximum BASIC line number exceeded, valid range: 0-$MAX_BASIC_LINE_NUMBER")
        }

        //Set line number from source as current BASIC line number
        basicLineNumber = lineNumber

        //Remove line number from line content and return it for further processing
        return line.content.substring(index).trim()
    }

    companion object {
        private const val MAX_BASIC_LINE_NUMBER = 63999

        private val LABEL_REGEX = Regex("\\{($LABEL_PREFIX_LINE.*?)}")
    }
}