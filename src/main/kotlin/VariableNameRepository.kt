class VariableNameRepository {

    private val variableNames = mutableListOf<String>()

    fun getNewName(name: String): String {

        //Is this variable name coming with a type postfix?
        val postfix = if (name.last() in listOf('$', '%')) {
            name.last().toString()
        } else {
            ""
        }

        //Very unlikely, but have we reached the theoretical limit of names?
        if (variableNames.size == MAX_NAMES) {
            throw Exception("Too many variable names have been generated")
        }

        //Try to use the first two characters of the name if possible
        val simplifiedName = name.take(2).lowercase() + postfix
        if (simplifiedName.isValidName()) {
            variableNames.add(simplifiedName)
            return simplifiedName
        }

        //This name already exists, generate a new name by incrementing the characters
        var newName = simplifiedName
        while (!newName.isValidName()) {

            //Bump second character
            val secondChar = POSSIBLE_CHARACTERS.indexOf(newName[1]) + 1
            if (POSSIBLE_CHARACTERS.size > secondChar) {
                //Still within bounds
                newName = name(newName[0], POSSIBLE_CHARACTERS[secondChar], postfix)
                continue
            }

            //Ran out of characters for the second character, bump the first one
            val firstChar = LETTERS.indexOf(newName[0]) + 1
            if (LETTERS.size > firstChar) {
                //Still within bounds
                newName = name(LETTERS[firstChar], POSSIBLE_CHARACTERS[0], postfix)
                continue
            }

            //Ran out of letters, start from the beginning of the combinations
            name(LETTERS[0], POSSIBLE_CHARACTERS[0], postfix)
        }

        variableNames.add(newName)
        return newName
    }

    private fun String.isValidName() =
        !variableNames.contains(this) && !FORBIDDEN_NAMES.contains(this.take(2))

    private fun name(firstChar: Char, secondChar: Char, postfix: String) = "$firstChar$secondChar$postfix"

    companion object {
        private val DIGITS = ('0'..'9').toList()
        private val LETTERS = ('a'..'z').toList()

        //For combined set try digits first
        private val POSSIBLE_CHARACTERS = DIGITS + LETTERS

        //Maximum number of possibilities for a valid BASIC variable name:
        //first character should always be a letter, second can be letter or digit.
        private val MAX_NAMES = LETTERS.size * POSSIBLE_CHARACTERS.size

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

}