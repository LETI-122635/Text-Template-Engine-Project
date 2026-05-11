package project

import project.json.parseJson
import project.template.TemplateEngine
import java.io.File

fun main(args: Array<String>) {
	if (args.size < 3) {
		System.err.println("Usage: <template-file> <input-json> <output-file>")
		return
	}
	val templateFile = args[0]
	val inputFile = args[1]
	val outputFile = args[2]

	try {
		val templateText = File(templateFile).readText()

		val inputText = File(inputFile).readText()

		val inputObj = parseJson(inputText)

		val result = TemplateEngine.render(templateText, inputObj)

		File(outputFile).writeText(result)
	} catch (e: Exception) {
		System.err.println("ERROR: ${e.message}")
		e.printStackTrace()
	}
}
