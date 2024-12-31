import models.*
import java.io.PrintStream
import kotlin.system.exitProcess

fun main(args: Array<String>) {

    try {
        val labels = mutableMapOf<String, Label>()

        val parameters = getCommandLineParameters(args)

        //Output goes to the specified output file, if not specified then standard output
        val outputFile: PrintStream = parameters.outputFileName?.let { PrintStream(it) } ?: System.out

        //Read source file together with included files and
        //trim white space from beginning and end of lines.
        val (normalSource, frequentSource) = SourceReader(
            inputFileName = parameters.inputFileName,
            libraryDirPath = parameters.libraryDirPath,
            preprocessingFlags = parameters.preProcessingFlags,
        ).execute()

        //Pre-process combined normal and frequent source
        val preProcessedSource = PreProcessor(labels, parameters.processingFlags).execute(normalSource, frequentSource)

        //Execute optimisations on pre-processed source
        val optimiser = Optimiser(parameters.optimisationFlags)
        val optimisedSource = optimiser.execute(preProcessedSource)

        //Assign BASIC line numbers to source lines
        val numberedSource = LineNumberAssigner(labels).execute(optimisedSource)

        //Optimise white space after all processing is done
        val finalisedSource = optimiser.optimiseWhiteSpace(numberedSource)

        //Write source to output
        SourceWriter(outputFile).execute(finalisedSource)

        //Dump labels to file when requested
        LabelWriter(labels, parameters.labelFileName).execute()

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

    //Get optimisation flags
    val optimFlags = getParameter(params, CommandLineParameter.OPTIMISATION_FLAGS)
        ?.map { flag ->
            OptimisationFlag.entries.firstOrNull { it.commandLineFlag == flag }
                ?: throw Exception("Unrecognized optimisation flag: $flag")
        } ?: emptyList()

    //Get processing flags
    val procFlags = getParameter(params, CommandLineParameter.PROCESSING_FLAGS)
        ?.map { flag ->
            ProcessingFlag.entries.firstOrNull { it.commandLineFlag == flag }
                ?: throw Exception("Unrecognized processing flag: $flag")
        } ?: emptyList()

    //Get pre-processing flag name defines, multiple round is needed to pick up all
    val preProcFlagNames = mutableSetOf<String>()
    do {
        val name = getParameter(params, CommandLineParameter.PRE_PROCESSING_FLAG_DEFINE)
            ?.also { preProcFlagNames.add(it) }
    } while (name != null)

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
        optimisationFlags = optimFlags,
        processingFlags = procFlags,
        preProcessingFlags = preProcFlagNames,
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
