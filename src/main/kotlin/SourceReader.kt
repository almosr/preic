import models.SourceLine
import java.io.File

class SourceReader(
    private val inputFileName: String,
    private val libraryDirPath: String?,
) {

    fun execute() = readSource(inputFileName)

    private fun readSource(fileName: String): List<SourceLine> {

        //Create the File instance from file name, or when it doesn't exist then also search the library dir
        val file = findFile(fileName)

        try {
            val source = mutableListOf<SourceLine>()

            var lineNumber = 1
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

    private fun findFile(fileName: String) =

        //When the file exists as it is referred by the file name then use it
        File(fileName).takeIf { it.exists() } ?: run {
            //File doesn't exist, try to find it in library dir
            libraryDirPath?.let {
                File(it).resolve(fileName)
            }?.apply {
                if (!exists()) {
                    throw Exception("File could not be found: $fileName, also searched in: ${libraryDirPath}")
                }
            } ?: throw Exception("File could not be found: $fileName")
        }
}