package models

/**
 * Processing parameters for the current session.
 *
 * @param inputFileName input source file name/path.
 * @param outputFileName output (processed) source file name/path, or `null` when not specified.
 * @param libraryDirPath library directory search path, or `null` when not specified.
 * @param labelFileName label definition dump file name/path, or `null` when not specified.
 * @param optimizations list of optimization flags.
 */
data class ProcessingParameters(
    val inputFileName: String,
    val outputFileName: String?,
    val libraryDirPath: String?,
    val labelFileName: String?,
    val optimizations: List<Optimization>,
)

enum class Optimization(val commandLineFlag: Char) {
    REMOVE_WHITE_SPACE('w'),
    JOIN_LINES('j'),
}