package models

sealed interface Label {
    val name: String
    val originalFormat: String
    val output: String

    //Line label type, refers to a specific line in the source code
    data class Line(
        override val name: String,
        override val originalFormat: String,
        val basicLineNumber: Int,
    ) : Label {
        override val output: String
            get() = basicLineNumber.toString()
    }

    //Variable label type, refers to a variable that is replaced
    //by a valid BASIC name
    data class Variable(
        override val name: String,
        override val originalFormat: String,
        val basicName: String,
        val type: VariableType,
        val frequent: Boolean,
    ) : Label {
        override val output: String
            get() = basicName
    }

    //Literal label type, refers to a string literal that is
    //replaced by specific string value
    data class Literal(
        override val name: String,
        override val originalFormat: String,
        val value: String
    ) : Label {
        override val output: String
            get() = value
    }
}

enum class VariableType {
    STRING,
    INTEGER,
    FLOAT,
}