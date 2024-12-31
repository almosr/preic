import models.Label
import java.io.PrintStream

class LabelWriter(
    private val labels: Map<String, Label>,
    private val labelFileName: String?
) {

    fun execute() {
        //When label file was not specified in parameters then skip
        val labelFile = labelFileName?.let { PrintStream(it) } ?: return

        labelFile.println("Label count: ${labels.size}")

        with(labels.values) {

            labelFile.println("\nLine labels:")

            filterIsInstance<Label.Line>()
                .sortedBy { it.basicLineNumber }
                .forEach { labelFile.println("${it.basicLineNumber}: ${it.name}") }

            labelFile.println("\nVariable labels:")

            filterIsInstance<Label.Variable>()
                .sortedBy { it.basicName }
                .forEach {
                    labelFile.println(
                        "${it.basicName}: ${it.name}" +
                                //Add frequent flag when set
                                if (it.frequent) {
                                    " - frequent"
                                } else {
                                    ""
                                }
                    )
                }

            labelFile.println("\nLiteral labels:")

            filterIsInstance<Label.Literal>()
                .sortedBy { it.name }
                .forEach { labelFile.println("${it.name}: ${it.value}") }
        }
    }
}