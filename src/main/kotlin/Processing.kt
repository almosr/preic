import models.Label
import models.Optimizations
import models.ProcessingParameters
import models.SourceLine
import java.io.File
import java.io.PrintStream

class Processing(
    private val processingParameters: ProcessingParameters
) {

    //Output goes to the specified output file, if not specified then standard output
    private val outputFile: PrintStream = processingParameters.outputFileName?.let { PrintStream(it) } ?: System.out

    private val labelFile: PrintStream? = processingParameters.labelFileName?.let { PrintStream(it) }

    private val variableNameRepository = VariableNameRepository()
    private val labels = mutableMapOf<String, Label>()
    private var basicLineNumber = 0

    fun processSource() {

        //Read source fil together with included files and
        //trim white space from beginning and end of lines.
        val source = readSource(processingParameters.inputFileName)

        //Join optimization on source lines
        val joinedSource = joinLines(source)

        //Scan for labels first and number each line
        val preprocessedSource = scanLabels(joinedSource)

        //Generate and output processed source
        outputProcessedSource(preprocessedSource)

        //Print labels to file when requested
        printLabels()
    }

    private fun outputProcessedSource(source: List<SourceLine>) {

        //Turn on white space otimization when included in optimization flags
        val optimizeWhiteSpace = processingParameters.optimizations.contains(Optimizations.REMOVE_WHITE_SPACE)

        //Output pre-processed source with line numbers
        source.forEach { line ->
            //When the line did not receive a BASIC line number then we should skip it
            val basicLineNumber = line.basicLineNumber ?: return@forEach
            var lineContent = line.content

            checkLabelBlocks(line)

            //Find all labels in the current line
            val lineLabels = LABEL_REGEX.findAll(lineContent).map { it.groupValues[1] }.toList()

            //Replace labels in current line with actual line numbers they represent
            lineLabels.forEach { labelStr ->
                val originalLabel = "{$labelStr}"
                val foundLabel = labels[originalLabel] ?: let {
                    //Is the missing label for a variable?
                    if (labelStr.startsWith(LABEL_PREFIX_VARIABLE)) {
                        //Then create a new label
                        createVariableLabel(labelStr.drop(1), originalLabel)
                    } else {
                        throw Exception("Undefined label: $labelStr\n$line")
                    }
                }

                lineContent = lineContent.replace(originalLabel, foundLabel.output)
            }

            if (optimizeWhiteSpace) {
                lineContent = optimizeWhiteSpace(lineContent)
            }

            //Output processed line
            outputFile.println("$basicLineNumber $lineContent")
        }
    }

    private fun createVariableLabel(name: String, originalLabel: String) =
        Label.Variable(
            name = name,
            originalFormat = originalLabel,
            basicName = variableNameRepository.getNewName(name)
        ).also { labels[originalLabel] = it }

    private fun optimizeWhiteSpace(lineContent: String): String {
        //Is there a REM command in the line? If yes then stop processing white space there.
        //Note: this lookup is a bit naive, doesn't parse the BASIC line
        val endOfLine = lineContent.indexOf("rem")
            .takeIf { it != -1 } ?: lineContent.length

        val result = StringBuilder()
        var inString = false

        //Walk through the line until the end (or until REM command)
        for (index in (0 until endOfLine)) {
            val char = lineContent[index]

            //When double quotes found then turn in-string mode on/off
            if (char == '"') {
                inString = !inString
            }

            //When this is not a white space, or it is inside a string then
            //output to result.
            if (!char.isWhitespace() || inString) {
                result.append(char)
            }
        }

        //Copy any remaining string when REM command found earlier
        if (endOfLine < lineContent.length) {
            result.append(lineContent.substring(endOfLine))
        }

        return result.toString()
    }

    private fun printLabels() {
        if (labelFile == null) {
            return
        }

        labelFile.println("Label count: ${labels.size}")

        with(
            labels.toList()
                .map { it.second }
        ) {
            labelFile.println("\nLine labels:")

            filterIsInstance<Label.Line>()
                .sortedBy { it.basicLineNumber }
                .forEach { labelFile.println("${it.basicLineNumber}: ${it.name}") }

            labelFile.println("\nVariable labels:")

            filterIsInstance<Label.Variable>()
                .sortedBy { it.basicName }
                .forEach { labelFile.println("${it.basicName}: ${it.name}") }

            labelFile.println("\nLiteral labels:")

            filterIsInstance<Label.Literal>()
                .sortedBy { it.value }
                .forEach { labelFile.println("${it.value}: ${it.name}") }
        }
    }

    private fun readSource(fileName: String): List<SourceLine> {

        try {
            val source = mutableListOf<SourceLine>()

            var lineNumber = 1
            val file = File(fileName)
            file.inputStream().use { inputStream ->
                inputStream.bufferedReader().forEachLine {

                    //Pre-process source: remove any comments and white space around the lines
                    val line = it.split("//")[0].trim()

                    source.addAll(
                        when {

                            //Skip empty lines
                            line.isBlank() -> emptyList()

                            //Process included file
                            line.startsWith("#include") -> {

                                //Space delimits the include directive and the file name
                                val includedFile = line.split(" ")[1]
                                readSource(includedFile)
                            }

                            //Not a special line, add to source
                            else -> listOf(
                                SourceLine(file, lineNumber, line)
                            )
                        }
                    )

                    lineNumber++
                }
            }

            return source
        } catch (e: Exception) {
            throw Exception("Failed to read source file: $fileName", e)
        }
    }

    private fun joinLines(input: List<SourceLine>): List<SourceLine> {
        if (!processingParameters.optimizations.contains(Optimizations.JOIN_LINES)) {
            //Join lines optim is off, return original source without any change
            return input
        }

        val output = mutableListOf<SourceLine>()

        var currentLine: SourceLine? = null
        input.forEach { line ->

            //If the current line is too long already,
            //or there is no line processed yet,
            //or there is a line or literal label or line number at the beginning,
            //or there is any special command somewhere in the current line already?
            val currentContent = currentLine?.content ?: ""
            if (currentLine == null ||
                currentContent.length > MAX_BASIC_SOURCE_LINE_LENGTH ||
                line.content.matches(JOIN_LINE_STARTS_WITH_REGEX) ||
                JOIN_LINE_SPECIAL_COMMANDS.any { currentContent.contains(it) }
            ) {
                //Close previous line and start a new one
                currentLine?.let { output.add(it) }
                currentLine = line
                return@forEach
            }

            //Join lines
            //When there is only a line label in the current line then
            //separate the lines by space otherwise by semicolon (:).
            val separator = if (currentContent.matches(JOIN_LINE_ONLY_LINE_LABEL)) {
                " "
            } else {
                ":"
            }
            currentLine = currentLine!!.copy(
                content = "$currentContent$separator${line.content}"
            )

        }

        currentLine?.let { output.add(it) }

        return output
    }

    private fun checkLabelBlocks(line: SourceLine) {
        try {

            //Simple check for unmatched label blocks:
            //each opening bracket must be matched with a closing before another
            //opening bracket comes.
            //At the end there should be no opening bracket left without closing bracket.
            var inLabel = false
            line.content.forEach {
                if (it == '{') {
                    if (inLabel) throw Exception()
                    inLabel = true
                } else if (it == '}') {
                    if (!inLabel) throw Exception()
                    inLabel = false
                }
            }
            if (inLabel) throw Exception()
        } catch (e: Exception) {
            throw Exception("Unmatched label block\n$line")
        }
    }

    private fun scanLabels(source: List<SourceLine>): List<SourceLine> =
        source.mapNotNull { line ->

            //Line starts with a line number
            val lineContent = if (line.content[0].isDigit()) {
                processLineNumber(line)
            } else {
                line.content
            }

            //Line starts or continues with a label
            val finalLineContent = if (lineContent.startsWith("{")) {
                processLabel(line, lineContent)
            } else {
                lineContent
            }

            //When line content is empty after processing line number and label processing
            //then it can be removed from processing without increasing the BASIC line number.
            if (finalLineContent.isBlank()) {
                return@mapNotNull null
            }

            //Recreate the line with the new content and BASIC line number and return it.
            line.copy(
                basicLineNumber = basicLineNumber,
                content = finalLineContent
            ).also {
                //Increment BASIC line number after line processing.
                basicLineNumber++
                if (basicLineNumber > MAX_BASIC_LINE_NUMBER) {
                    throw Exception("Maximum BASIC line number exceeded, valid range: 0-$MAX_BASIC_LINE_NUMBER")
                }
            }
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

        //Look for any line labels that were defined with the current BASIC line number
        labels.entries
            .filter { (it.value as? Label.Line)?.basicLineNumber == basicLineNumber }
            .forEach {
                //Found lines with the old line number, adjust them to the new
                labels[it.key] = (it.value as Label.Line).copy(basicLineNumber = lineNumber)
            }

        //Set line number from source as current BASIC line number
        basicLineNumber = lineNumber

        //Remove line number from line content and return it for further processing
        return line.content.substring(index).trim()
    }

    private fun processLabel(line: SourceLine, content: String): String {
        val index = content.indexOf("}")
        val label = content.substring(1, index)
        if (label.isBlank()) throw Exception("Invalid label\n$line")

        val originalLabel = "{$label}"
        val (labelInstance, restOfLine) = when (label[0]) {
            LABEL_PREFIX_LINE -> (Label.Line(
                name = label.drop(1),
                originalFormat = originalLabel,
                basicLineNumber = basicLineNumber
            )
                    //Return line content without the label
                    to content.substring(index + 1).trim())

            LABEL_PREFIX_LITERAL -> (Label.Literal(
                name = label.drop(1),
                originalFormat = originalLabel,
                value = extractLiteralValue(content, content.substring(index + 1).trim())
            )
                    //The entire line was used by literal label
                    to "")

            //Other kind of labels or escaped characters
            else -> {
                //Skip the rest of the label processing and
                //return line in original form
                return content
            }
        }

        //Each label should be defined only once
        if (labels.containsKey(originalLabel)) throw Exception("Duplicate label: $originalLabel\n$line")

        //Label must not be empty
        if (labelInstance.name.isBlank()) throw Exception("Blank label: $label\n$line")

        labels[originalLabel] = labelInstance

        return restOfLine
    }

    private fun extractLiteralValue(line: String, content: String): String {
        //Check for missing value definition
        if (content.isBlank()) throw Exception("Empty literal value for label: $line")

        //Check for missing equal sign at the beginning
        if (content[0] != '=') throw Exception("Invalid literal value for label: $line")

        //Remove equal sign and return the rest as literal value
        return content.drop(1)
    }

    companion object {
        private const val MAX_BASIC_LINE_NUMBER = 63999
        private const val MAX_BASIC_SOURCE_LINE_LENGTH = 256

        private const val LABEL_PREFIX_LINE = '#'
        private const val LABEL_PREFIX_VARIABLE = '@'
        private const val LABEL_PREFIX_LITERAL = '%'
        private val LABEL_PREFIXES = listOf(LABEL_PREFIX_LINE, LABEL_PREFIX_VARIABLE, LABEL_PREFIX_LITERAL)
        private val LABEL_PREFIXES_AS_STRING = LABEL_PREFIXES.joinToString("")

        private val LABEL_REGEX = Regex("\\{([$LABEL_PREFIXES_AS_STRING].*?)}")

        private val JOIN_LINE_SPECIAL_COMMANDS = listOf("goto", "go to", "if", "then", "return", "rem")
        private val JOIN_LINE_STARTS_WITH_REGEX = Regex("^(\\{[$LABEL_PREFIX_LINE|$LABEL_PREFIX_LITERAL]+|[0-9]+).*")
        private val JOIN_LINE_ONLY_LINE_LABEL = Regex("^\\{$LABEL_PREFIX_LINE[^}]*}$")
    }
}