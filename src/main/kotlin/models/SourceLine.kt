package models

import java.io.File

data class SourceLine(
    val file: File? = null,
    val lineNumber: Int = 0,
    val content: String,
) {
    override fun toString(): String = file
        ?.let { "${it.absolutePath}:$lineNumber\n\"$content\"" }
        ?: "generated"
}

data class SourceLineWithNumber(
    val sourceLine: SourceLine,
    val basicLineNumber: Int,
) {
    override fun toString(): String = sourceLine.toString()
}
