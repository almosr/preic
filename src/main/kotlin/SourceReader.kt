import models.BasicFunction
import models.SourceLine
import java.io.File

class SourceReader(
    private val inputFileName: String,
    private val libraryDirPath: String?,
    private val preprocessingFlags: Set<String>,
) {

    private val functions = mutableMapOf<String, BasicFunction>()

    //Returns the normal source lines first then the lines marked as frequently called
    fun execute() = readSource(inputFileName, preprocessingFlags.toMutableSet()).let {
        //Resolve function calls in both source code groups
        it.first.resolveFunctionCalls() to it.second.resolveFunctionCalls()
    }

    private fun readSource(fileName: String, flags: MutableSet<String>): Pair<List<SourceLine>, List<SourceLine>> {

        //Create the File instance from file name, or when it doesn't exist then also search the library dir
        val file = findFile(fileName)

        try {
            val source = mutableListOf<SourceLine>()
            val frequentSource = mutableListOf<SourceLine>()

            var lineNumber = 1
            val conditionalFlags = mutableListOf<ConditionalFlag>()
            var skippedConditionalFlagCounter = 0
            file.inputStream().use { inputStream ->
                inputStream.bufferedReader().forEachLine {

                    //Pre-process source: remove any comments and white space around the lines
                    val line = it.split("//")[0].trim()

                    //Check if this line should be skipped due to conditional compiling
                    val conditionalFlag = conditionalFlags.lastOrNull()

                    //Should skip line when conditional flag exists at the top of the stack
                    val shouldSkipLine = conditionalFlag != null &&
                            //and it must be present, but yet not present
                            ((conditionalFlag.whenPresent && !flags.contains(conditionalFlag.name)) ||
                                    //or must not be present, but yet present
                                    (!conditionalFlag.whenPresent && flags.contains(conditionalFlag.name)))

                    source.addAll(
                        when {

                            //Beginning of conditional compiling when line is skipped
                            line.startsWith("#ifdef") && shouldSkipLine -> {
                                skippedConditionalFlagCounter++
                                emptyList()
                            }

                            //Else part of conditional compiling when line is skipped
                            line.startsWith("#else") -> {
                                //When there is at least one skipped conditional flag then ignore this #else
                                if (skippedConditionalFlagCounter == 0) {
                                    //Otherwise flip when exist flag on the top item in the conditional flag stack
                                    val flag = conditionalFlags.removeLastOrNull()
                                        ?: throw SourceReadException(
                                            "#else directive without matching #ifdef",
                                            file, lineNumber, line
                                        )

                                    conditionalFlags.add(flag.copy(whenPresent = false))
                                }
                                emptyList()
                            }

                            //Closing part of conditional compiling
                            line.startsWith("#endif") -> {
                                //Remove top item from either conditional flag stack when that stack is not empty yet
                                if (skippedConditionalFlagCounter > 0) {
                                    skippedConditionalFlagCounter--
                                } else {
                                    //No skipped flags, remove it actual stack
                                    conditionalFlags.removeLastOrNull()
                                        ?: throw SourceReadException(
                                            "#endif directive without matching #ifdef",
                                            file, lineNumber, line
                                        )
                                }
                                emptyList()
                            }

                            //When the line should be skipped then skip it
                            //**** Important: below this line the rest of the conditions
                            //will not be considered when the line is skipped!
                            shouldSkipLine -> emptyList()

                            //Skip empty lines
                            line.isBlank() -> emptyList()

                            //Define pre-processing flag when line is not skipped
                            line.startsWith("#define") -> {
                                flags.add(getDirectiveParameter(line, file, lineNumber))
                                emptyList()
                            }

                            //Un-define (remove) pre-processing flag when line is not skipped
                            line.startsWith("#undef") -> {
                                flags.remove(getDirectiveParameter(line, file, lineNumber))
                                emptyList()
                            }

                            //Beginning of conditional compiling when line is not skipped
                            line.startsWith("#ifdef") -> {
                                conditionalFlags.add(
                                    ConditionalFlag(
                                        getDirectiveParameter(line, file, lineNumber),
                                        whenPresent = true
                                    )
                                )
                                emptyList()
                            }

                            //Process included file when line is not skipped
                            line.startsWith("#include") -> includeFile(line, file, lineNumber, flags, frequentSource)

                            //Process function declaration when line is not skipped
                            line.startsWith("#function") -> functionDeclaration(line, file, lineNumber)

                            //Not a special line, add to source when line is not skipped
                            else -> {
                                listOf(
                                    SourceLine(file, lineNumber, line)
                                )
                            }
                        }
                    )

                    lineNumber++
                }
            }

            //At the end conditional flags stacks must be empty otherwise a condition was not matched
            if (conditionalFlags.isNotEmpty() || skippedConditionalFlagCounter > 0) {
                throw SourceReadException(
                    "Unfinished #ifdef-#endif pre-processing direction structure",
                    file, lineNumber, null
                )
            }

            //Find any sections marked as frequently called and separate it
            val normalSource = source.extractFrequentSections(frequentSource, file, lineNumber)

            return Pair(normalSource, frequentSource)

        } catch (e: Exception) {
            throw Exception("Failed to read source file: $fileName", e)
        }
    }

    private fun includeFile(
        line: String,
        file: File,
        lineNumber: Int,
        flags: MutableSet<String>,
        frequentSource: MutableList<SourceLine>
    ): List<SourceLine> {

        val parameters = getDirectiveParameterList(line, file, lineNumber)

        //Last part is always the included file
        val includedFile = parameters.removeLast()

        //Do we have first parameter?
        val method = if (parameters.isNotEmpty()) {
            val type = parameters.removeFirst()
            IncludeFileMethod.entries.firstOrNull { it.id == type }
                ?: throw SourceReadException(
                    "Unknown include directive file type: $type",
                    file, lineNumber, line
                )
        } else {
            //Code is the default
            IncludeFileMethod.CODE
        }

        //Do we have start offset parameter?
        val startOffset = parameters.getOffsetOrNull(file, lineNumber, line)

        //Do we have end offset parameter?
        val endOffset = parameters.getOffsetOrNull(file, lineNumber, line)

        //Any further parameter is unrecognised
        if (parameters.isNotEmpty()) {
            throw SourceReadException(
                "Unrecognised include directive parameters: ${parameters.joinToString(",")}",
                file, lineNumber, line
            )
        }

        return when (method) {
            IncludeFileMethod.CODE -> {
                if (startOffset != null) {
                    throw SourceReadException(
                        "Code include does not support offset parameters",
                        file, lineNumber, line
                    )
                }
                val (includedSrc, includedFreqSrc) = readSource(includedFile, flags)
                frequentSource.addAll(includedFreqSrc)

                includedSrc
            }

            IncludeFileMethod.DATA -> {
                readBinaryFile(includedFile, startOffset, endOffset, file, lineNumber, line)
                    .chunked(MAX_BYTE_IN_DATA)
                    .map {
                        SourceLine(file, content = "data ${it.joinToString(",")}")
                    }
            }

            IncludeFileMethod.PRINT -> {
                readBinaryFile(
                    includedFile, startOffset, endOffset,
                    file, lineNumber, line
                ).chunked(128)
                    .map { chunk ->

                        //Flag for inverse print mode on/off
                        var inverse = false
                        val printedChunk = chunk.map {
                            when {
                                //When byte is higher or equal to 128 and inverse mode is off then turn it on
                                it >= 128.toUByte() && !inverse -> {
                                    inverse = true
                                    "{rvon}" + PRINTED_BYTE[it.toInt() and 127]
                                }

                                //When byte is higher or equal to 128 and inverse mode is on then keep it on
                                it >= 128.toUByte() -> {
                                    PRINTED_BYTE[it.toInt() and 127]
                                }

                                //Byte is lower than 128 and inverse mode is on, turn it off
                                inverse -> {
                                    inverse = false
                                    "{rvof}" + PRINTED_BYTE[it.toInt()]
                                }

                                //Otherwise byte is lower than 128 and inverse mode is off then keep it off
                                else -> PRINTED_BYTE[it.toInt()]
                            }
                        }

                        SourceLine(
                            file, content = "print\"" +
                                    printedChunk.joinToString("") +
                                    //If inverse mode remained on at the end then turn it off
                                    ("{rvof}".takeIf { inverse } ?: "") +
                                    "\";"
                        )
                    }
            }

            IncludeFileMethod.REMARK -> {
                val binaryFile = readBinaryFile(
                    includedFile, startOffset, endOffset,
                    file, lineNumber, line
                )

                //Zero bytes in REM lines are not working
                if (binaryFile.any { it == 0.toUByte() }) {
                    throw SourceReadException(
                        "Included binary file contains zero bytes that cause issues with BASIC REM command, use print method instead",
                        file, lineNumber, line
                    )
                }

                binaryFile.chunked(256)
                    .map { chunk ->
                        SourceLine(
                            file,
                            content = "rem\"" +
                                    chunk.joinToString("") { "{\$${"%02x".format(it.toByte())}}" }
                        )
                    }
            }
        }
    }

    private fun functionDeclaration(line: String, file: File, lineNumber: Int): List<SourceLine> {
        val parameters = getDirectiveParameterList(line, file, lineNumber)

        //Add function call
        val name = parameters.removeFirst()
        functions.putIfAbsent(
            name,
            BasicFunction(name, parameters)
        )?.let {
            //When this call returns a non-null value then this function name already used
            throw SourceReadException("Function already declared with name `$name`", file, lineNumber, line)
        }

        return listOf(SourceLine(file, lineNumber, "{#$name}"))
    }

    private fun List<SourceLine>.resolveFunctionCalls(): List<SourceLine> =
        map { line ->

            //Try to find #call directive
            if (!line.content.startsWith("#call")) {
                //Not call directive, return original line
                return@map listOf(line)
            }

            val parameters = getDirectiveParameterList(line.content, line.file!!, line.lineNumber)

            //First parameter is the target function name
            val functionName = parameters.removeFirst()

            //Try to find the function
            val function = functions[functionName]
                ?: throw with(line) {
                    SourceReadException(
                        "Call directive without function definition, target function: `$functionName`",
                        file!!, lineNumber, content
                    )
                }

            //Parameter lists must match between function declaration and call
            if (function.parameters.size != parameters.size) {
                throw with(line) {
                    SourceReadException(
                        "Call directive parameter list doesn't match target function: ${
                            function.parameters.joinToString(",")
                        }",
                        file!!, lineNumber, content
                    )
                }
            }

            //Initialise each parameter variable
            parameters.mapIndexed { index, parameter ->
                line.copy(content = "{@${function.parameters[index]}}=$parameter")
            } +
                    //Add call to subroutine with BASIC GOSUB command
                    listOf(line.copy(content = "gosub {#${function.name}}"))
        }.flatten()

    private fun MutableList<String>.getOffsetOrNull(file: File, lineNumber: Int, line: String) =
        removeFirstOrNull()
            ?.takeIf { it.isNotEmpty() }
            ?.let {
                try {
                    //Convert the string to integer
                    it.toInt()
                } catch (e: Exception) {
                    throw SourceReadException(
                        "Failed to parse include file start offset",
                        file, lineNumber, line
                    )
                }
            }

    @OptIn(ExperimentalUnsignedTypes::class)
    private fun readBinaryFile(
        fileName: String,
        startOffset: Int?,
        endOffset: Int?,
        file: File,
        lineNumber: Int,
        line: String?
    ): List<UByte> =
        try {
            //Create the File instance from file name, or when it doesn't exist then also search the library dir
            findFile(fileName)
                .readBytes()
                .toUByteArray()
                .run {
                    val finalStartOffset = startOffset ?: 0
                    val finalEndOffset = endOffset ?: (indices.last + 1)

                    if (finalStartOffset !in (0..indices.last)) {
                        throw SourceReadException(
                            "Invalid start offset for include directive: $finalStartOffset, must be in range (0 .. ${indices.last})",
                            file, lineNumber, line
                        )
                    }

                    if (finalEndOffset !in (finalStartOffset + 1..indices.last + 1)) {
                        throw SourceReadException(
                            "Invalid end offset for include directive: $finalEndOffset, must be in range (${finalStartOffset + 1} .. ${indices.last + 1})",
                            file, lineNumber, line
                        )
                    }

                    slice(finalStartOffset until finalEndOffset)
                }
        } catch (e: Exception) {
            throw Exception("Failed to read binary file: $fileName", e)
        }

    private fun List<SourceLine>.extractFrequentSections(
        frequentSource: MutableList<SourceLine>,
        file: File,
        lineNumber: Int
    ): List<SourceLine> {
        var frequentMode = false
        val normalSource = mapNotNull { line ->

            when {

                //Start of frequently called section
                line.content.startsWith("#frequent") -> {
                    if (frequentMode) {
                        throw SourceReadException("Section already marked as frequent", file, lineNumber, line.content)
                    }
                    frequentMode = true

                    null
                }

                //End of frequently called section
                line.content.startsWith("#endfrequent") -> {
                    if (!frequentMode) {
                        throw SourceReadException("Missing #frequent directive", file, lineNumber, line.content)
                    }
                    frequentMode = false

                    null
                }

                //A line of a frequently called section move it into the special list
                frequentMode -> {
                    frequentSource.add(line)

                    null
                }

                //An ordinary line, remains in the source code
                else -> line
            }
        }

        if (frequentMode) {
            throw SourceReadException("#frequent directive was not closed", file, lineNumber, null)
        }

        return normalSource
    }

    private fun getDirectiveParameter(line: String, file: File, lineNumber: Int): String {
        val start = line.indexOfFirst { it == ' ' }
        if (start == -1) {
            throw SourceReadException("Parameter is missing for pre-processing directive", file, lineNumber, line)
        }

        return line.substring(start + 1).trim()
    }

    private fun getDirectiveParameterList(line: String, file: File, lineNumber: Int): MutableList<String> {
        val input = getDirectiveParameter(line, file, lineNumber)

        //Find all commas that are not inside quoting marks
        val commas = COMMA_REGEX.findAll(input)

        //Split up parameters by found comma positions
        val result = mutableListOf<String>()
        var index = 0
        commas.forEach {
            result.add(input.substring(index, it.range.first))
            index = it.range.last + 1
        }

        //Add remaining line
        result.add(input.substring(index, input.length))

        //Trim white space around parameters and return parameter list as mutable
        return result.map { it.trim() }.toMutableList()
    }

    private fun findFile(fileName: String) =

        //When the file exists as it is referred by the file name then use it
        File(fileName).takeIf { it.exists() } ?: run {
            //File doesn't exist, try to find it in library dir
            libraryDirPath?.let {
                File(it).resolve(fileName)
            }?.apply {
                if (!exists()) {
                    throw Exception("File could not be found: $fileName, also searched in: $libraryDirPath")
                }
            } ?: throw Exception("File could not be found: $fileName")
        }

    /**
     * Conditional flag for current processing.
     *
     * @param name name of the flag to look for in the flag name set.
     * @param whenPresent when `true` then flag must be present in the set to keep the lines,
     *                    otherwise flag must not be present in the set to keep the lines.
     */
    private data class ConditionalFlag(val name: String, val whenPresent: Boolean)

    /**
     * File inclusion method.
     *
     * @param id ID of the method for parsing `#include` directive parameters.
     */
    private enum class IncludeFileMethod(val id: String) {
        CODE("code"),
        DATA("data"),
        PRINT("print"),
        REMARK("remark"),
    }

    private class SourceReadException(message: String, file: File, lineNumber: Int, line: String?) : Exception(
        "$message\n" +
                "${file.absolutePath}:$lineNumber\n" +
                ("\"$line\"".takeIf { line != null } ?: "")
    )

    companion object {

        //Regex for matching all commas in a string that are not inside quoting marks
        private val COMMA_REGEX = Regex(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*\$)")

        //Maximum number of bytes turned into one DATA line from a binary file
        private const val MAX_BYTE_IN_DATA = 150

        //Printed form of each screen character between 0 and 127 ($7F),
        //the same characters are repeated between 128 ($80) and 255 ($FF) in inverse,
        //so no need to store those in this list.
        private val PRINTED_BYTE = listOf(
            "@", //00
            "a", //01
            "b", //02
            "c", //03
            "d", //04
            "e", //05
            "f", //06
            "g", //07
            "h", //08
            "i", //09
            "j", //0A
            "k", //0B
            "l", //0C
            "m", //0D
            "n", //0E
            "o", //0F

            "p", //10
            "q", //11
            "r", //12
            "s", //13
            "t", //14
            "u", //15
            "v", //16
            "w", //17
            "x", //18
            "y", //19
            "z", //1A
            "[", //1B
            "\\", //1C
            "]", //1D
            "^", //1E
            "_", //1F

            " ", //20
            "!", //21

            //printing double quote triggers the quoting mode,
            //so we close it with another one and backspace
            //to remove the second one
            "\";chr\$(34);chr\$(34);\"{del}", //22

            "#", //23
            "$", //24
            "%", //25
            "&", //26
            "'", //27
            "(", //28
            ")", //29
            "*", //2A
            "+", //2B
            ",", //2C
            "-", //2D
            ".", //2E
            "/", //2F

            "0", //30
            "1", //31
            "2", //32
            "3", //33
            "4", //34
            "5", //35
            "6", //36
            "7", //37
            "8", //38
            "9", //39
            ":", //3A
            ";", //3B
            "<", //3C
            "=", //3D
            ">", //3E
            "?", //3F

            "{SHIFT-*}", //40
            "A", //41
            "B", //42
            "C", //43
            "D", //44
            "E", //45
            "F", //46
            "G", //47
            "H", //48
            "I", //49
            "J", //4A
            "K", //4B
            "L", //4C
            "M", //4D
            "N", //4E
            "O", //4F

            "P", //50
            "Q", //51
            "R", //52
            "S", //53
            "T", //54
            "U", //55
            "V", //56
            "W", //57
            "X", //58
            "Y", //59
            "Z", //5A
            "{SHIFT-+}", //5B
            "{CBM--}", //5C
            "{SHIFT--}", //5D
            "~", //5E
            "{CBM-*}", //5F

            "{\$a0}", //60
            "{CBM-K}", //61
            "{CBM-I}", //62
            "{CBM-T}", //63
            "{CBM-@}", //64
            "{CBM-G}", //65
            "{CBM-+}", //66
            "{CBM-M}", //67
            "{CBM-POUND}", //68
            "{SHIFT-POUND}", //69
            "{CBM-N}", //6A
            "{CBM-Q}", //6B
            "{CBM-D}", //6C
            "{CBM-Z}", //6D
            "{CBM-S}", //6E
            "{CBM-P}", //6F

            "{CBM-A}", //70
            "{CBM-E}", //71
            "{CBM-R}", //72
            "{CBM-W}", //73
            "{CBM-H}", //74
            "{CBM-J}", //75
            "{CBM-L}", //76
            "{CBM-Y}", //77
            "{CBM-U}", //78
            "{CBM-O}", //79
            "{SHIFT-@}", //7A
            "{CBM-F}", //7B
            "{CBM-C}", //7C
            "{CBM-X}", //7D
            "{CBM-V}", //7E
            "{CBM-B}", //7F
        )
    }
}