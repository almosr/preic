import models.SourceLine
import java.io.File

class SourceReader(
    private val inputFileName: String,
    private val libraryDirPath: String?,
    private val preprocessingFlags: Set<String>,
) {

    //Returns the normal source lines first then the lines marked as frequently called
    fun execute() = readSource(inputFileName, preprocessingFlags.toMutableSet())

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
                                        ?: throw Exception("#else directive without matching #ifdef")

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
                                        ?: throw Exception("#endif directive without matching #ifdef")
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
                            line.startsWith("#include") -> {
                                val includedFile = getDirectiveParameter(line, file, lineNumber)
                                val (includedSrc, includedFreqSrc) = readSource(includedFile, flags)
                                frequentSource.addAll(includedFreqSrc)

                                includedSrc
                            }

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
                throw Exception("Unfinished #ifdef-#endif pre-processing direction structure")
            }

            //Find any sections marked as frequently called and separate it
            val normalSource = source.extractFrequentSections(frequentSource)

            return Pair(normalSource, frequentSource)

        } catch (e: Exception) {
            throw Exception("Failed to read source file: $fileName", e)
        }
    }

    private fun List<SourceLine>.extractFrequentSections(frequentSource: MutableList<SourceLine>): List<SourceLine> {
        var frequentMode = false
        val normalSource = mapNotNull { line ->

            when {

                //Start of frequently called section
                line.content.startsWith("#frequent") -> {
                    if (frequentMode) {
                        throw Exception("Section already marked as frequent\n$line")
                    }
                    frequentMode = true

                    null
                }

                //End of frequently called section
                line.content.startsWith("#endfrequent") -> {
                    if (!frequentMode) {
                        throw Exception("Missing #frequent directive\n$line")
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
            throw Exception("#frequent directive was not closed in file ${this[0].file?.absolutePath}")
        }

        return normalSource
    }

    private fun getDirectiveParameter(line: String, file: File, lineNumber: Int): String {
        val start = line.indexOfFirst { it == ' ' }
        if (start == -1) {
            throw Exception("Parameter is missing for pre-processing directive\n${file.absolutePath}:$lineNumber\n\"$line\"")
        }

        return line.substring(start + 1).trim()
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
}