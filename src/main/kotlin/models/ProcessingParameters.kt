package models

/**
 * Processing parameters for the current session.
 *
 * @param inputFileName input source file name/path.
 * @param outputFileName output (processed) source file name/path, or `null` when not specified.
 * @param libraryDirPath library directory search path, or `null` when not specified.
 * @param labelFileName label definition dump file name/path, or `null` when not specified.
 * @param optimisations list of optimisation flags.
 */
data class ProcessingParameters(
    val inputFileName: String,
    val outputFileName: String?,
    val libraryDirPath: String?,
    val labelFileName: String?,
    val optimisations: List<Optimisation>,
)

/**
 * Optimisation flags
 *
 * @param commandLineFlag flag character among command line parameters.
 * @param description description of the flag for printing to the command line as help.
 */
enum class Optimisation(val commandLineFlag: Char, val description: String) {

    REMOVE_WHITE_SPACE(
        'w',
        "Remove white space from lines where not required, white space remains unchanged after `REM` command and inside strings."
    ),

    JOIN_LINES(
        'j',
        "Join BASIC lines, when set then processing attempts to join as many lines as safely possible. " +
                "_Warning_: since the tool does not interpret the source, this optimisation could cause runtime issues with some specific source code."
    ),
}

/**
 * Command line parameters
 *
 * @param commandLineFlag command characters among command line parameters.
 * @param additionalParameter additional parameter to the command for printing to the command line as help.
 * @param description description of the command for printing to the command line as help.
 *
 */
enum class CommandLineParameter(val commandLineFlag: String, val additionalParameter: String, val description: String) {
    LABEL_FILE_NAME(
        "l",
        "<label list file>",
        "optional path to a file for label definition dump."
    ),

    LIBRARY_DIRECTORY_PATH(
        "ld",
        "<library dir>",
        "optional path to a directory where included files will be searched also"

    ),

    OPTIMISATION_FLAGS(
        "o",
        "<opt flags>",
        "optional optimisation flags, when set then the relevant processing will be completed on the output:\n" +
                Optimisation.entries.joinToString("\n") {
                    "* `${it.commandLineFlag} - ${it.description}"
                }
    )
}