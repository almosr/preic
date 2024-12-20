import models.CommandLineParameter
import models.Optimisation
import models.ProcessingParameters
import kotlin.system.exitProcess

fun main(args: Array<String>) {

    try {
        Processing(getCommandLineParameters(args)).processSource()
    } catch (e: Exception) {
        println("\nERROR: $e")
        if (e.cause != null) println(" Cause: ${e.cause}")
        if (e.cause?.cause != null) println("  Cause: ${e.cause!!.cause}")

        exitProcess(-1)
    }

    exitProcess(0)
}

private fun getCommandLineParameters(args: Array<String>): ProcessingParameters {
    if (args.isEmpty()) {
        printUsage()

        throw Exception("Invalid command line parameters")
    }

    val params = args.toMutableList()

    //First parameter is always the input file name
    val inputFileName = args[0]
    params.removeAt(0)

    //Get label file name
    val labelFileName = getParameter(params, CommandLineParameter.LABEL_FILE_NAME)

    //Get library dir path
    val libraryDirPath = getParameter(params, CommandLineParameter.LIBRARY_DIRECTORY_PATH)

    //Get optimisations
    val optims = getParameter(params, CommandLineParameter.OPTIMISATION_FLAGS)
        ?.map { flag ->
            Optimisation.entries.firstOrNull { it.commandLineFlag == flag }
                ?: throw Exception("Unrecognized optimisation flag: $flag")
        } ?: emptyList()

    //Last parameter (if exists) is output file name
    val outputFileName = if (params.size >= 1) params.removeLast() else null

    //If any parameters left those are unknown to the tool
    if (params.size != 0) {
        throw Exception("Unrecognized command line parameters: ${params.joinToString(" ")}")
    }

    return ProcessingParameters(
        inputFileName = inputFileName,
        outputFileName = outputFileName,
        libraryDirPath = libraryDirPath,
        labelFileName = labelFileName,
        optimisations = optims
    )
}

private fun getParameter(params: MutableList<String>, parameter: CommandLineParameter): String? =
    params.indexOf("-${parameter.commandLineFlag}")
        .takeIf { it >= 0 }
        ?.let {
            params.removeAt(it)
            params.removeAt(it)
        }

private fun printUsage() {
    val commandLineParams = CommandLineParameter.entries
        .joinToString(" ") { "[-${it.commandLineFlag} ${it.additionalParameter}]" }

    println("preic v${BuildVersion.getVersion()}\n")
    println("Usage: java -jar preic.jar <input BASIC source file> $commandLineParams [output pre-processed file]\n")
    println("<input BASIC source file> - BASIC source file to be pre-processed")
    println(CommandLineParameter.entries.joinToString("\n") { "-${it.commandLineFlag} ${it.additionalParameter} - ${it.description}" })
    println("[output pre-processed file] - optional output pre-processed file, default is stdout")
}
