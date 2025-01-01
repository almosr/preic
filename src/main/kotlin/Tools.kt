import models.Label

object Tools {

    /**
     * Try to find a label at the start of a source code line and call a processing lambda for the label when exists,
     * then add it to the map of labels.
     *
     * @param labels labels map for registering the new labels.
     * @param line source code line, used for error reporting only, must implement [toString] method.
     * @param content line content to process.
     * @param processLabel lambda callback for processing the found label, parameters: `label` is the found label as
     *                     string, `restOfLine` is the remaining part of the line after the label, should return either
     *                     a [Pair] of [Label] instance and remaining line content, or `null` to skip the label.
     * @return remaining line after recognised label has been removed.
     */
    fun processStartingLabel(
        labels: MutableMap<String, Label>,
        line: Any,
        content: String,
        processLabel: (label: String, restOfLine: String) -> Pair<Label, String>?
    ): String {
        val index = content.indexOf("}")
        val label = content.substring(1, index)
        if (label.isBlank()) throw Exception("Invalid label\n$line")

        val originalLabel = "{$label}"
        val result = processLabel(label, content.substring(index + 1).trim()) ?: return content

        //Each label should be defined only once
        if (labels.containsKey(originalLabel)) throw Exception("Duplicate label: $originalLabel\n$line")

        //Label must not be empty
        if (result.first.name.isBlank()) throw Exception("Blank label: $label\n$line")

        labels[originalLabel] = result.first

        return result.second
    }

    /**
     * Find all values based on a [Regex] rule and return it as a list.
     *
     * @param input input string
     * @return list of values in the string that matches the [Regex] rule.
     */
    fun Regex.findAllValues(input: String) = findAll(input).map { it.groupValues[1] }.toList()

    /**
     * @return string without sub-strings that are inside double quotes (double quotes are removed also).
     */
    fun String.removeQuotedContent(): String {
        val result = StringBuilder()
        var insideString = false
        forEach {
            if (it == '\"') {
                insideString = !insideString
            } else {
                if (!insideString) result.append(it)
            }
        }

        return result.toString()
    }
}