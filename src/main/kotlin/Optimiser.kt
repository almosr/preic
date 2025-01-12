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
            .joinLines()

    fun optimiseWhiteSpace(source: List<SourceLineWithNumber>): List<SourceLineWithNumber> {
        //Is white-space optimisation enabled?
        if (!optimisationFlags.contains(OptimisationFlag.REMOVE_WHITE_SPACE)) {
            //Not enabled, don't change the line
            return source
        }

        return source.map { line ->

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

    private fun List<SourceLine>.joinLines(): List<SourceLine> {
        if (!optimisationFlags.contains(OptimisationFlag.JOIN_LINES)) {
            //Join lines optim is off, return original source without any change
            return this
        }

        val output = mutableListOf<SourceLine>()

        var currentLine: SourceLine? = null
        forEach { line ->

            //If the current line is too long already,
            //or there is no line processed yet,
            //or there is a line or literal label or line number at the beginning,
            //or there is any special command somewhere in the current line already?
            val currentContent = currentLine?.content ?: ""

            //Does this line start with label?
            if (line.content.matches(JOIN_LINE_STARTS_WITH_REGEX)) {
                //Not safe to join, dump accumulated content and replace it with this line
                currentLine?.let { output.add(it) }
                currentLine = line
                return@forEach
            }

            //Can we add this line to the accumulated content?
            //We can't when no content yet, or accumulated content is too long already,
            //or it contains special commands which can derail the execution.
            if (currentLine == null ||
                currentContent.length > MAX_BASIC_SOURCE_LINE_LENGTH ||
                JOIN_LINE_SPECIAL_COMMANDS.any { currentContent.removeQuotedContent().contains(it) }
            ) {
                //Close previous accumulated content and start a new one
                currentLine?.let { output.add(it) }
                currentLine = line
                return@forEach
            }

            //Safe to join lines
            //When there is only a line label or line number in the current line then
            //separate the lines by space otherwise by semicolon (:).
            val separator = if (currentContent.matches(JOIN_LINE_ONLY_LINE_LABEL_OR_NUMBER)) {
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
    }

}