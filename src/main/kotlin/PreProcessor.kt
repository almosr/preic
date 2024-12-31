import Constants.LABEL_PREFIX_LITERAL
import Constants.LABEL_PREFIX_VARIABLE
import Constants.LABEL_VARIABLE_PREFIX_FREQUENT
import Constants.MAX_LABEL_ITERATIONS
import Tools.findAllValues
import Tools.processStartingLabel
import models.Label
import models.ProcessingFlag
import models.SourceLine
import models.VariableType

class PreProcessor(
    private val labels: MutableMap<String, Label>,
    private val processingFlags: List<ProcessingFlag>
) {
    private val variableNameRepository = VariableNameRepository(
        shortNames = processingFlags.contains(ProcessingFlag.SHORT_VARIABLE_NAMES)
    )

    fun execute(normalSource: List<SourceLine>, frequentSource: List<SourceLine>): List<SourceLine> =
        if (frequentSource.isEmpty()) {

            //No frequently called sections are specified
            normalSource

        } else {

            //There are lines marked as frequently called, put this in front of all
            //other lines and jump over.
            listOf(
                SourceLine(content = "goto $FREQUENT_SECTIONS_SKIP_LABEL")
            ) + frequentSource + listOf(
                SourceLine(content = FREQUENT_SECTIONS_SKIP_LABEL)
            ) + normalSource
        }
            .convertHexadecimalNumbers()
            .processLabels()
            .outputProcessedSource()

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

        //First scan for variable labels and collect them into a map
        val variables = mutableMapOf<String, Variable>()
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
                val existingLabel = variables[originalLabel]
                variables[originalLabel] = when {
                    //When doesn't exist then create it
                    existingLabel == null ->
                        Variable(variableName, originalLabel, frequent)

                    //When exists, but flagged as frequent then update flag
                    frequent -> existingLabel.copy(frequent = true)

                    //When exists and not flagged as frequent then no need to do anything with it
                    else -> existingLabel
                }
            }
        }

        //Create actual variable labels out of the collected map,
        //frequent variables should be considered first for name assignment.
        variables.values
            .sortedBy { !it.frequent }
            .forEach {
                labels[it.originalName] = it.createVariableLabel()
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
            val lineContent = line.content

            //Line starts or continues with a label
            val finalLineContent = if (lineContent.startsWith("{")) {
                processStartingLabel(labels, line, lineContent) { label, restOfLine ->
                    if (label[0] == LABEL_PREFIX_LITERAL) {

                        val value = extractLiteralValue(restOfLine)

                        //When value could not be extracted then this is not a literal label definition,
                        //but rather a label used at the beginning of the line, so skip processing.
                            ?: return@processStartingLabel null

                        Pair(
                            Label.Literal(
                                name = label.drop(1),
                                originalFormat = "{$label}",
                                value = value
                            ),
                            //The entire line was used by literal label
                            ""
                        )
                    } else {
                        null
                    }
                }
            } else {
                lineContent
            }

            //When line content is empty after label processing then it can be removed.
            if (finalLineContent.isBlank()) {
                return@mapNotNull null
            }

            //Recreate the line with the new content.
            line.copy(content = finalLineContent)
        }
    }

    private fun List<SourceLine>.outputProcessedSource(): List<SourceLine> =

        //Output pre-processed source with line numbers
        mapNotNull { line ->
            var lineContent = line.content

            checkLabelBlocks(line)

            var replaceCount = 0
            do {
                //Find all variable and literal labels in the current line
                val lineLabels = LABEL_REGEX.findAllValues(lineContent)

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

            //Output processed line
            line.copy(content = lineContent)
        }

    private fun Variable.createVariableLabel(): Label.Variable {
        //Is this variable name coming with a type postfix?
        //Separate base name and postfix
        val (baseName, postfix) = if (name.last() in listOf('$', '%')) {
            (name.dropLast(1) to name.last().toString())
        } else {
            (name to "")
        }

        val type = when (postfix) {
            "$" -> VariableType.STRING
            "%" -> VariableType.INTEGER
            "" -> VariableType.FLOAT

            else -> throw Exception("Unexpected variable postfix")
        }

        return Label.Variable(
            name = name,
            originalFormat = originalName,
            type = type,
            frequent = frequent,
            basicName = variableNameRepository.getNewName(baseName, type) + postfix
        )
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


    private fun extractLiteralValue(content: String): String? =
        when {
            //Check for missing value definition, when empty then this is not a definition
            content.isBlank() -> null

            //Check for missing equal sign at the beginning, when missing then this is not a definition
            content[0] != '=' -> null

            //Remove equal sign and white space then return the rest as literal value
            else -> content.drop(1).trim()
        }

    private data class Variable(
        val name: String,
        val originalName: String,
        val frequent: Boolean,
    )

    companion object {

        private const val FREQUENT_SECTIONS_SKIP_LABEL = "{#preic_skip_frequent_sections}"

        private val LABEL_PREFIXES = listOf(LABEL_PREFIX_VARIABLE, LABEL_PREFIX_LITERAL)
        private val LABEL_PREFIXES_AS_STRING = LABEL_PREFIXES.joinToString("")

        private val LABEL_REGEX = Regex("\\{([$LABEL_PREFIXES_AS_STRING].*?)}")

        private val LABEL_VARIABLE_REGEX = Regex("\\{($LABEL_PREFIX_VARIABLE.*?)}")

        private val HEXADECIMAL_NUMBER_REGEX = Regex("\\$\\$[0-9a-fA-F]+")
    }
}