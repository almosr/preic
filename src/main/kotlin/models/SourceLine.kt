package models

import java.io.File

data class SourceLine(
    val file: File,
    val lineNumber: Int,
    val content: String,
    val basicLineNumber: Int? = null,
) {
    override fun toString(): String = "${file.absolutePath}:$lineNumber\n\"$content\""
}
