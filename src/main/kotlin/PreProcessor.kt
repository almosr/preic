import models.Label
import models.ProcessingFlag
import models.SourceLine
import models.VariableType
import java.io.PrintStream

class PreProcessor(
    private val optimiser: Optimiser,
    private val processingFlags: List<ProcessingFlag>
) {
    private val variableNameRepository = VariableNameRepository()
    private val labels = mutableMapOf<String, Label>()
    private var basicLineNumber = 0

    fun execute(outputFile: PrintStream, input: List<SourceLine>) {
        input
            .convertHexadecimalNumbers()
            .processLabels()
            .outputProcessedSource(outputFile)
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun List<SourceLine>.convertHexadecimalNumbers(): List<SourceLine> {
        //Is converting hexadecimal numbers enabled?
        if (!processingFlags.contains(ProcessingFlag.CONVERT_HEXADECIMAL_NUMBERS)) {
            //Not enabled, don't change the source
            return this
        }

        return map { line ->
            try {
                val newContent = StringBuilder(line.content)
                do {
                    val number = HEXADECIMAL_NUMBER_REGEX.findAll(newContent)
                        .firstOrNull()

                    if (number != null) {
                        //Convert hexadecimal to decimal, remove leading $$ signs
                        val decimal = number.value.drop(2).hexToLong()
                        newContent.replace(number.range.first, number.range.last + 1, decimal.toString())
                    }

                } while (number != null)

                line.copy(content = newContent.toString())
            } catch (e: Exception) {
                throw Exception("Error while parsing hexadecimal number\n$line", e)
            }
        }
    }

    private fun List<SourceLine>.processLabels(): List<SourceLine> {

        //First scan for variable labels
        forEach { line ->
            LABEL_VARIABLE_REGEX.findAll(line.content).forEach {

                //Remove wrapping characters
                val name = it.value.drop(2).dropLast(1)

                //Is it flagged as frequent?
                val frequent = name.startsWith(LABEL_VARIABLE_PREFIX_FREQUENT)
                val variableName = if (frequent) {
                    //Remove frequent flag character
                    name.drop(1)
                } else {
                    name
                }

                //Re-create the original label without frequent flag
                val originalLabel = "{$LABEL_PREFIX_VARIABLE$variableName}"
                val existingLabel = labels[originalLabel] as? Label.Variable
                labels[originalLabel] = when {
                    //When doesn't exist then create it
                    existingLabel == null ->
                        createVariableLabel(variableName, originalLabel, frequent)

                    //When exists, but flagged as frequent then update flag
                    frequent -> existingLabel.copy(frequent = true)

                    //When exists and not flagged as frequent then no need to do anything with it
                    else -> existingLabel
                }
            }
        }

        //Produce the list of frequent variable initialisation
        val frequentVariables = labels.values
            .filterIsInstance<Label.Variable>()
            .filter { it.frequent }
        val initFrequentVariablesSrc = if (frequentVariables.isEmpty()) {
            //No frequent variables
            emptyList()
        } else {
            //Create program line to initialise frequent variables
            listOf(
                SourceLine(
                    content = frequentVariables.joinToString(":") {
                        val initValue = when (it.type) {
                            VariableType.STRING -> "\"\""
                            VariableType.FLOAT,
                            VariableType.INTEGER -> "0"
                        }

                        "${it.output}=$initValue"
                    }
                )
            )
        }

        //Add frequent variable list to the first line, process other label types and assign line numbers
        return (initFrequentVariablesSrc + this).mapNotNull { line ->

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
    }

    private fun List<SourceLine>.outputProcessedSource(outputFile: PrintStream) {
        //Output pre-processed source with line numbers
        forEach { line ->
            //When the line did not receive a BASIC line number then we should skip it
            val basicLineNumber = line.basicLineNumber ?: return@forEach
            var lineContent = line.content

            checkLabelBlocks(line)

            var replaceCount = 0
            do {
                //Find all labels in the current line
                val lineLabels = LABEL_REGEX.findAll(lineContent).map { it.groupValues[1] }.toList()

                //Replace labels in current line with actual line numbers they represent
                lineLabels.forEach { labelStr ->
                    //When label starts with variable prefix and frequent flag then remove flag
                    val originalLabel =
                        if (labelStr.startsWith("$LABEL_PREFIX_VARIABLE$LABEL_VARIABLE_PREFIX_FREQUENT")) {
                            "{${LABEL_PREFIX_VARIABLE}${labelStr.drop(2)}}"
                        } else {
                            "{$labelStr}"
                        }
                    val foundLabel = labels[originalLabel] ?: throw Exception("Undefined label: $labelStr\n$line")

                    lineContent = lineContent.replace("{$labelStr}", foundLabel.output)
                }

                replaceCount++
                if (replaceCount > MAX_LABEL_ITERATIONS) {
                    throw Exception("Maximum number of label processing iterations reached, possible recursive labels\n$line")
                }

                //Keep searching for labels in the current line until nothing left
            } while (lineLabels.isNotEmpty())

            //Optimise white space in line when enabled in optimisation flags
            lineContent = optimiser.optimiseWhiteSpace(lineContent)

            //Output processed line
            outputFile.println("$basicLineNumber $lineContent")
        }
    }

    private fun createVariableLabel(name: String, originalLabel: String, frequent: Boolean): Label.Variable {
        //Is this variable name coming with a type postfix?
        //Separate base name and postfix
        val (baseName, postfix) = if (name.last() in listOf('$', '%')) {
            (name.dropLast(1) to name.last().toString())
        } else {
            (name to "")
        }

        return Label.Variable(
            name = name,
            originalFormat = originalLabel,
            type = when (postfix) {
                "$" -> VariableType.STRING
                "%" -> VariableType.INTEGER
                else -> VariableType.FLOAT
            },
            frequent = frequent,
            basicName = variableNameRepository.getNewName(baseName) + postfix
        ).also { labels[originalLabel] = it }
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

    fun printLabels(labelFile: PrintStream) {
        labelFile.println("Label count: ${labels.size}")

        with(
            labels.toList().map { it.second }
        ) {
            labelFile.println("\nLine labels:")

            filterIsInstance<Label.Line>()
                .sortedBy { it.basicLineNumber }
                .forEach { labelFile.println("${it.basicLineNumber}: ${it.name}") }

            labelFile.println("\nVariable labels:")

            filterIsInstance<Label.Variable>()
                .sortedBy { it.basicName }
                .forEach {
                    labelFile.println(
                        "${it.basicName}: ${it.name}" +
                                //Add frequent flag when set
                                if (it.frequent) {
                                    " - frequent"
                                } else {
                                    ""
                                }
                    )
                }

            labelFile.println("\nLiteral labels:")

            filterIsInstance<Label.Literal>()
                .sortedBy { it.value }
                .forEach { labelFile.println("${it.value}: ${it.name}") }
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

            LABEL_PREFIX_LITERAL -> {
                val value = extractLiteralValue(content.substring(index + 1).trim())

                //When value could not be extracted then this is not a literal label definition,
                //but rather a label used at the beginning of the line.
                    ?: return content

                Pair(
                    Label.Literal(
                        name = label.drop(1),
                        originalFormat = originalLabel,
                        value = value
                    ),
                    //The entire line was used by literal label
                    ""
                )
            }

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

    private fun extractLiteralValue(content: String): String? =
        when {
            //Check for missing value definition, when empty then this is not a definition
            content.isBlank() -> null

            //Check for missing equal sign at the beginning, when missing then this is not a definition
            content[0] != '=' -> null

            //Remove equal sign and return the rest as literal value
            else -> content.drop(1)
        }

    companion object {
        const val LABEL_PREFIX_LINE = '#'
        const val LABEL_PREFIX_VARIABLE = '@'
        const val LABEL_PREFIX_LITERAL = '%'

        const val LABEL_VARIABLE_PREFIX_FREQUENT = '!'

        private const val MAX_BASIC_LINE_NUMBER = 63999

        private const val MAX_LABEL_ITERATIONS = 100
        private val LABEL_PREFIXES = listOf(LABEL_PREFIX_LINE, LABEL_PREFIX_VARIABLE, LABEL_PREFIX_LITERAL)
        private val LABEL_PREFIXES_AS_STRING = LABEL_PREFIXES.joinToString("")

        private val LABEL_REGEX = Regex("\\{([$LABEL_PREFIXES_AS_STRING].*?)}")

        private val LABEL_VARIABLE_REGEX = Regex("\\{($LABEL_PREFIX_VARIABLE.*?)}")

        private val HEXADECIMAL_NUMBER_REGEX = Regex("\\$\\$[0-9a-fA-F]+")
    }
}