import Constants.LABEL_PREFIX_LINE
import Constants.LABEL_PREFIX_LITERAL
import Tools.removeQuotedContent
import models.OptimisationFlag
import models.SourceLine
import models.SourceLineWithNumber

class Optimiser(
    private val optimisationFlags: List<OptimisationFlag>
) {

    fun execute(source: List<SourceLine>): List<SourceLine> =
        source.removeRemCommands()
            .removeGotoAfterThenOrElse()
            .simplifyNonZeroInIf()
            .replaceZeroByDot()
            .joinLines()

    fun finalise(source: List<SourceLineWithNumber>): List<SourceLineWithNumber> =
        source.optimiseWhiteSpace()
            .removeDataStringDoubleQuotes()
            .removeClosingDoubleQuotes()

    private fun List<SourceLineWithNumber>.optimiseWhiteSpace(): List<SourceLineWithNumber> {
        //Is white-space optimisation enabled?
        if (!optimisationFlags.contains(OptimisationFlag.REMOVE_WHITE_SPACE)) {
            //Not enabled, don't change the line
            return this
        }

        return map { line ->

            val lineContent = line.sourceLine.content

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

            line.copy(sourceLine = line.sourceLine.copy(content = result.toString()))
        }
    }

    private fun List<SourceLine>.removeRemCommands(): List<SourceLine> {
        if (!optimisationFlags.contains(OptimisationFlag.REMOVE_REM_COMMANDS)) {
            //Remove REM commands optim is off, return original source without any change
            return this
        }

        return mapNotNull { line ->
            val content = line.content

            //Try to find REM commands that are standing separated from anything else
            val remCommand = REMOVE_REM_COMMANDS_REGEX.findAll(content)
                .firstOrNull() ?: return@mapNotNull line
            val lineStart = content.substring(0, remCommand.range.first)

            //Is REM inside a string literal?
            if (lineStart.isInsideDoubleQuotes()) {
                //Then don't change the line
                return@mapNotNull line
            }

            //Does the line start with line number that should be preserved when REM removed?
            val lineNumber = content.takeWhile { it.isDigit() }
            if (lineNumber.isNotEmpty() && remCommand.range.contains(lineNumber.length)) {
                //Return the line number
                return@mapNotNull line.copy(content = lineNumber)
            }

            //When line starts with REM then drop the entire line
            if (remCommand.range.first == 0) {
                return@mapNotNull null
            }

            //Remove the entire line starting with REM command
            line.copy(content = lineStart)
        }
    }

    private fun List<SourceLine>.removeGotoAfterThenOrElse(): List<SourceLine> {
        if (!optimisationFlags.contains(OptimisationFlag.REMOVE_GOTO_AFTER_THEN_OR_ELSE)) {
            //Remove GOTO commands after THEN/ELSE optim is off, return original source without any change
            return this
        }

        return REMOVE_GOTO_COMMANDS_AFTER_THEN_OR_ELSE_REGEX.processAllItems(this) { content, item ->
            //Remove this instance from line content
            content.replace(
                //Keep THEN/ELSE at the beginning of removed range
                item.range.first + 4,
                item.range.last + 1,
                ""
            )
        }
    }

    private fun List<SourceLine>.simplifyNonZeroInIf(): List<SourceLine> {
        if (!optimisationFlags.contains(OptimisationFlag.SIMPLIFY_VARIABLE_CHECK_IN_IF)) {
            //Simplify if optim is off, return original source without any change
            return this
        }

        return SIMPLIFY_NON_ZERO_IN_IF_REGEX.processAllItems(this) { content, item ->
            //Replace "if variable<>0 then" with "if variable then"
            content.replace(
                item.range.first,
                item.range.last + 1,
                "if ${item.groupValues[1]} then"
            )
        }
    }

    private fun List<SourceLine>.replaceZeroByDot(): List<SourceLine> {
        if (!optimisationFlags.contains(OptimisationFlag.REPLACE_ZERO_WITH_DOT)) {
            //Replace zero optim is off, return original source without any change
            return this
        }

        return ZERO_NUMERIC_REGEX.processAllItems(this) { content, item ->
            //Replace consecutive zero numeric literals by dot (.)
            content.replace(
                item.range.first,
                item.range.last + 1,
                "."
            )
        }
    }

    private fun List<SourceLine>.joinLines(): List<SourceLine> {
        if (!optimisationFlags.contains(OptimisationFlag.JOIN_LINES)) {
            //Join lines optim is off, return original source without any change
            return this
        }

        val output = mutableListOf<SourceLine>()

        var accumulatedLines: SourceLine? = null
        forEach { currentLine ->

            //When no accumulated lines yet then current line will be the first content.
            val lines = accumulatedLines ?: let {
                accumulatedLines = currentLine
                return@forEach
            }

            //Can we add the current line to the accumulated content?
            //We can't add it when:
            // 1. accumulated content is too long already;
            // 2. the previously added line ends with an open string constant;
            // 3. accumulated content contains special commands which can derail the execution;
            // 4. current line starts with label.
            if (lines.content.length > MAX_BASIC_SOURCE_LINE_LENGTH ||
                lines.content.isInsideDoubleQuotes() ||
                JOIN_LINE_SPECIAL_COMMANDS.any { lines.content.removeQuotedContent().contains(it) } ||
                currentLine.content.matches(JOIN_LINE_STARTS_WITH_REGEX)
            ) {
                //Close previous accumulated content and start a new one with the current line.
                output.add(lines)
                accumulatedLines = currentLine
                return@forEach
            }

            //When there is only a line label or line number in the current line then
            //separate the lines by space otherwise by semicolon (:).
            val separator = if (lines.content.matches(JOIN_LINE_ONLY_LINE_LABEL_OR_NUMBER)) {
                " "
            } else {
                ":"
            }

            //Safe to join lines, merge current line content with previously accumulated lines.
            accumulatedLines = lines.copy(
                content = "${lines.content}$separator${currentLine.content}"
            )
        }

        //Output any remaining accumulated lines
        accumulatedLines?.let { output.add(it) }

        return output
    }

    private fun List<SourceLineWithNumber>.removeDataStringDoubleQuotes(): List<SourceLineWithNumber> {
        if (!optimisationFlags.contains(OptimisationFlag.REMOVE_DATA_STRING_DOUBLE_QUOTES)) {
            //Remove data string double quotes optim is off, return original source without any change
            return this
        }

        return map { line ->
            var lineContent = line.sourceLine.content
            val newContent = mutableListOf<String>()

            //When the line starts with line number and/or white space then copy that to output right away
            //and remove it from processing.
            val lineStarts = STARTING_LINE_NUMBER_AND_WHITE_SPACE_REGEX.findAll(lineContent)
            lineStarts.firstOrNull()?.let {
                newContent.add(it.value)
                lineContent = lineContent.substring(it.range.last + 1)
            }

            //Scan through line, try to find DATA commands and process them
            while (lineContent.isNotEmpty()) {
                val result = StringBuilder()
                var processed = 0

                if (STARTS_WITH_DATA_REGEX.matches(lineContent)) {
                    //DATA command found
                    val currentString = StringBuilder()
                    var inString = false

                    //Walk through the line content until the end (or until colon found)
                    for (index in lineContent.indices) {
                        val char = lineContent[index]
                        processed++

                        when {

                            //When double quotes found then turn in-string mode on/off
                            char == '"' -> {
                                inString = !inString

                                currentString.append(char)

                                //Just finished the string? Then process it.
                                if (!inString) {
                                    //Double quotes can be removed only when the string does not start with white space and
                                    //does not contain comma or colon characters.
                                    if (!currentString.matches(NOT_SAFE_DATA_STRING_REGEX)) {
                                        //Safe string, remove double quotes
                                        currentString.deleteAt(0)
                                        currentString.deleteAt(currentString.length - 1)
                                    }

                                    //Add string to output
                                    result.append(currentString)
                                    currentString.clear()
                                }
                            }

                            //Inside string simply copy character
                            inString -> currentString.append(char)

                            //Not inside string, copy character and check for end of DATA command
                            else -> {
                                result.append(char)

                                //When colon character found outside of string then DATA command ends, stop processing
                                if (char == ':') {
                                    break
                                }
                            }
                        }
                    }

                } else {

                    //Not DATA command at the beginning of the line, try to find colon for next iteration
                    var inString = false

                    //Walk through the line content until the end (or until colon found)
                    for (index in lineContent.indices) {
                        val char = lineContent[index]
                        processed++

                        //Copy current character to output
                        result.append(char)

                        //When double quotes found then turn in-string mode on/off
                        if (char == '"') {
                            inString = !inString
                        }

                        //When not inside string and colon character found then stop iteration
                        if (!inString && char == ':') {
                            break
                        }
                    }
                }

                //Remove processed characters from processing
                lineContent = lineContent.substring(processed)

                //Add copied characters to new content
                newContent.add(result.toString())
            }

            //Modify line content with new value
            line.copy(sourceLine = line.sourceLine.copy(content = newContent.joinToString("")))
        }
    }

    private fun List<SourceLineWithNumber>.removeClosingDoubleQuotes(): List<SourceLineWithNumber> {
        if (!optimisationFlags.contains(OptimisationFlag.REMOVE_CLOSING_DOUBLE_QUOTES)) {
            //Remove closing double quotes optim is off, return original source without any change
            return this
        }

        return map { line ->
            val lineContent = line.sourceLine.content

            //When the line content does not end with double quote or at the end it
            //remains inside a string constant then leave it in original state.
            if (!lineContent.endsWith("\"") || lineContent.isInsideDoubleQuotes()) {
                line
            } else {
                //Remove last character that is the closing double quote.
                line.copy(sourceLine = line.sourceLine.copy(content = lineContent.dropLast(1)))
            }
        }
    }

    //Process each instance of an item that is found by using a Regex expression in all lines.
    private fun Regex.processAllItems(
        lines: List<SourceLine>,
        processItem: (content: StringBuilder, item: MatchResult) -> Unit
    ): List<SourceLine> = lines.map { line ->
        val content = StringBuilder(line.content)

        findAll(content)
            .toList()
            //Go backwards, so we can remove anything from the
            //line content without changing the ranges for the next item
            .reversed()
            .forEach { item ->

                //Is the item inside a string literal or after REM command?
                val lineStart = content.substring(0, item.range.first)
                if (!lineStart.isInsideDoubleQuotes() && !lineStart.contains("rem")) {
                    processItem(content, item)
                }
            }

        line.copy(content = content.toString())
    }

    //Count the number of double quotes in the provided line fragment,
    //if not even number then one is left open before the rest of the line.
    private fun String.isInsideDoubleQuotes() = count { it == '"' } % 2 != 0

    companion object {
        private const val MAX_BASIC_SOURCE_LINE_LENGTH = 256

        private val JOIN_LINE_SPECIAL_COMMANDS = listOf("goto", "go to", "if", "then", "return", "rem", "end")
        private val JOIN_LINE_STARTS_WITH_REGEX = Regex("^(\\{[$LABEL_PREFIX_LINE|$LABEL_PREFIX_LITERAL]+|[0-9]+).*")
        private val JOIN_LINE_ONLY_LINE_LABEL_OR_NUMBER = Regex("^(\\{$LABEL_PREFIX_LINE[^}]*}|[0-9]+)\\s*$")

        private val REMOVE_REM_COMMANDS_REGEX = Regex("(^[0-9]*rem|[\\s:]rem)")
        private val REMOVE_GOTO_COMMANDS_AFTER_THEN_OR_ELSE_REGEX = Regex("(then|else)\\s*(go\\s*to)")

        private val SIMPLIFY_NON_ZERO_IN_IF_REGEX = Regex("if\\s*([a-z][a-z0-9]??)\\s*<\\s*>\\s*0\\s*then")
        private val ZERO_NUMERIC_REGEX = Regex("(?<![0-9a-zA-Z])0+(?!\\.)")

        private val STARTING_LINE_NUMBER_AND_WHITE_SPACE_REGEX = Regex("^[0-9]*\\s*")
        private val STARTS_WITH_DATA_REGEX = Regex("^data.*")
        private val NOT_SAFE_DATA_STRING_REGEX = Regex("^\"\\s+.*|.*,+.*|.*:+.*|.*\\{.*")
    }

}