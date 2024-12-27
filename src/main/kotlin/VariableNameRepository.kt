import models.VariableType

class VariableNameRepository(
    private val shortNames: Boolean
) {

    private val variableNames = mutableListOf<VariableName>()

    fun getNewName(name: String, type: VariableType): String {

        //Very unlikely, but have we reached the theoretical limit of names?
        if (variableNames.size == MAX_NAMES) {
            throw Exception("Too many variable names have been generated")
        }

        val simplifiedName = if (shortNames) {
            //Short names always start from one letter
            LETTERS[0].toString()
        } else {

            //Try to use the first two characters of the name if possible
            name.take(2).lowercase()
        }

        if (simplifiedName.isValidName(type)) {
            variableNames.add(VariableName(simplifiedName, type))
            return simplifiedName
        }

        //This name already exists, generate a new name by incrementing the characters
        var newName = simplifiedName
        while (!newName.isValidName(type)) {

            if (shortNames) {

                //Bump first character, when doesn't exist then start from beginning
                val bumpedFirstCharName = bumpFirstChar(newName)
                if (bumpedFirstCharName != null) {
                    newName = bumpedFirstCharName
                    continue
                }

                //Ran out of characters for the first character, bump the second one
                val bumpedSecondCharName = bumpSecondChar(newName)
                if (bumpedSecondCharName != null) {
                    newName = bumpedSecondCharName
                    continue
                }
            } else {

                //Bump second character, when doesn't exist then start from beginning
                val bumpedSecondCharName = bumpSecondChar(newName)
                if (bumpedSecondCharName != null) {
                    newName = bumpedSecondCharName
                    continue
                }

                //Ran out of characters for the second character, bump the first one
                val bumpedFirstCharName = bumpFirstChar(newName)
                if (bumpedFirstCharName != null) {
                    newName = bumpedFirstCharName
                    continue
                }
            }

            //Ran out of letters, start from the beginning of the combinations
            newName = name(LETTERS[0], POSSIBLE_CHARACTERS[0])
        }

        variableNames.add(VariableName(newName, type))
        return newName
    }

    private fun bumpFirstChar(name: String): String? {
        val firstChar = LETTERS.indexOf(name[0]) + 1
        return if (LETTERS.size > firstChar) {
            //Still within bounds
            name(
                LETTERS[firstChar],

                //When the name is already two character long then pick a possible character for second
                if (name.length == 2) {
                    POSSIBLE_CHARACTERS[0]
                } else {
                    null
                }
            )
        } else {
            null
        }
    }

    private fun bumpSecondChar(name: String): String? {
        val secondChar = if (name.length > 1) {
            POSSIBLE_CHARACTERS.indexOf(name[1]) + 1
        } else {
            0
        }

        return if (POSSIBLE_CHARACTERS.size > secondChar) {
            //Still within bounds
            name(name[0], POSSIBLE_CHARACTERS[secondChar])
        } else {
            null
        }
    }

    private fun String.isValidName(type: VariableType) =
        VALID_VARIABLE_NAME_REGEX.matches(this) &&
                !variableNames.contains(VariableName(this, type)) &&
                !FORBIDDEN_NAMES.contains(this.take(2))

    private fun name(firstChar: Char, secondChar: Char?) = "$firstChar${secondChar ?: ""}"

    companion object {
        private val DIGITS = ('0'..'9').toList()
        private val LETTERS = ('a'..'z').toList()

        //For combined set try digits first
        private val POSSIBLE_CHARACTERS = DIGITS + LETTERS

        //Maximum number of possibilities for a valid BASIC variable name:
        //first character should always be a letter, second can be letter or digit.
        private val MAX_NAMES = LETTERS.size * POSSIBLE_CHARACTERS.size

        //Valid variable name must start with a lowercase letter and
        //optionally continue with a lowercase letter or digit
        private val VALID_VARIABLE_NAME_REGEX = Regex("^[a-z]+[a-z0-9]*$")

        //Certain character combinations are not valid BASIC names because there
        //is command with the same name in some dialects.
        private val FORBIDDEN_NAMES = listOf(
            "do",
            "ds",
            "el",
            "er",
            "fn",
            "go",
            "if",
            "on",
            "or",
            "pi",
            "st",
            "ti",
            "to",
        )
    }

    private data class VariableName(
        val name: String,
        val type: VariableType
    )
}