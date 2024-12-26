package models

import java.io.File

data class SourceLine(
    val file: File? = null,
    val lineNumber: Int = 0,
    val content: String,
    val basicLineNumber: Int? = null,
) {
    override fun toString(): String = file
        ?.let { "${it.absolutePath}:$lineNumber\n\"$content\"" }
        ?: "generated"
}
