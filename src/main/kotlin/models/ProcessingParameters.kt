package models

/**
 * Processing parameters for the current session.
 *
 * @param inputFileName input source file name/path.
 * @param outputFileName output (processed) source file name/path, or `null` when not specified.
 * @param libraryDirPath library directory search path, or `null` when not specified.
 * @param labelFileName label definition dump file name/path, or `null` when not specified.
 * @param optimisationFlags list of optimisation flags.
 * @param processingFlags list of processing flags.
 * @param preProcessingFlags set of pre-processing flag names that are considered set at the beginning of processing.
 */
data class ProcessingParameters(
    val inputFileName: String,
    val outputFileName: String?,
    val libraryDirPath: String?,
    val labelFileName: String?,
    val optimisationFlags: List<OptimisationFlag>,
    val processingFlags: List<ProcessingFlag>,
    val preProcessingFlags: Set<String>,
)

/**
 * Optimisation flags
 *
 * @param commandLineFlag flag character among command line parameters.
 * @param description description of the flag for printing to the command line as help.
 */
enum class OptimisationFlag(val commandLineFlag: Char, val description: String) {

    JOIN_LINES(
        'j',
        "Join BASIC lines, when set then processing attempts to join as many lines as safely possible. "
    ),

    REMOVE_REM_COMMANDS(
        'r',
        "Remove REM BASIC commands from source to make it run faster and occupy less memory."
    ),

    REMOVE_GOTO_AFTER_THEN_OR_ELSE(
        't',
        "Remove GOTO BASIC command after THEN and ELSE commands which is unnecessary for jumping to a line."
    ),

    REMOVE_WHITE_SPACE(
        'w',
        "Remove white space from lines where not required, white space remains unchanged after `REM` command and inside strings."
    )
}

/**
 * Processing flags
 *
 * @param commandLineFlag flag character among command line parameters.
 * @param description description of the flag for printing to the command line as help.
 */
enum class ProcessingFlag(val commandLineFlag: Char, val description: String) {

    CONVERT_HEXADECIMAL_NUMBERS(
        '$',
        "convert hexadecimal numbers to decimal, hexadecimal numbers should be prefixed with double dollar signs ($$).",
    ),

    SHORT_VARIABLE_NAMES(
        'v',
        "use as many one character long name as possible instead of trying to keep any resemblance with the original variable names.",
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
    PRE_PROCESSING_FLAG_DEFINE(
        "d",
        "<pre-processing flag name>",
        "optional definition of a pre-processing flag that will be set as existing at the beginning of the processing."
    ),

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

    PROCESSING_FLAGS(
        "p",
        "<processing flags>",
        "optional processing flags, when set then relevant processing will be completed on the output:\n" +
                ProcessingFlag.entries.joinToString("\n") {
                    "  * `${it.commandLineFlag} - ${it.description}"
                }
    ),

    OPTIMISATION_FLAGS(
        "o",
        "<opt flags>",
        "optional optimisation flags, when set then the relevant processing will be completed on the output:\n" +
                OptimisationFlag.entries.joinToString("\n") {
                    "  * `${it.commandLineFlag} - ${it.description}"
                } +
                "\n  _Warning_: since the tool does not interpret the source, optimisations could cause runtime issues with some specific source code."
    )
}