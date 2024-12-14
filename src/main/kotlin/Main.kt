import models.Optimization
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
    val labelFileName = getParameter(params, "-l")

    //Get library dir path
    val libraryDirPath = getParameter(params, "-ld")

    //Get optimizations
    val optims = getParameter(params, "-o")
        ?.map { flag ->
            Optimization.entries.firstOrNull { it.commandLineFlag == flag }
                ?: throw Exception("Unrecognized optimization flag: $flag")
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
        optimizations = optims
    )
}

private fun getParameter(params: MutableList<String>, parameterName: String): String? =
    params.indexOf(parameterName)
        .takeIf { it >= 0 }
        ?.let {
            params.removeAt(it)
            params.removeAt(it)
        }

private fun printUsage() {
    println("preic v${BuildVersion.getVersion()}\n")
    println("Usage: java -jar preic.jar <input BASIC source file> [-l <label list file>] [-o <opt flags>] [output pre-processed file]\n")
    println("<input BASIC source file> - BASIC source file to be pre-processed\n")
    println("-l <label list file> - optional path to a file for label definition dump\n")
    println("-ld <library dir> - optional path to a directory where included files will be searched also\n")
    println(
        "-o <opt flags> - optional optimisation flags, when set then the relevant processing will be completed on the output:\n" +
                "  * `w` - remove white space from lines where not required, white space remains unchanged after `REM` command and\n" +
                "          inside strings.\n" +
                "  * `j` - join BASIC lines, when set then processing attempts to join as many lines as safely possible.\n" +
                "          _Warning_: since the tool does not interpret the source, this optimization could cause runtime issues\n" +
                "           with some specific source code."
    )
    println("[output pre-processed file] - optional output pre-processed file, default is stdout")
}
