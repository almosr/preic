package models

data class ProcessingParameters(
    val inputFileName: String,
    val outputFileName: String?,
    val labelFileName: String?,
    val optimizations: List<Optimizations>,
)

enum class Optimizations(val commandLineFlag: Char) {
    REMOVE_WHITE_SPACE('w'),
    JOIN_LINES('j'),
}