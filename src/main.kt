package project

import project.json.parseJson
import project.template.TemplateEngine
import java.io.File

/**
 * main.kt is the entry point for using the language as a template renderer.
 *
 * This program automatically processes the template and generates output.
 */
fun main() {
	// Default file paths
	val templateFile = "src/template.txt"
	val inputFile = "src/input.json"
	val outputFile = "src/output.txt"

	try {
		// Read template and input.
		val templateText = File(templateFile).readText()
		val inputText = File(inputFile).readText()

		// Parse input as JSON.
		val inputObj = parseJson(inputText)

		// Render template using the language runtime via TemplateEngine frontend.
		val result = TemplateEngine.render(templateText, inputObj)

		// Write result to output file.
		File(outputFile).writeText(result)
	} catch (e: Exception) {
		System.err.println("ERROR: ${e.message}")
		e.printStackTrace()
	}
}
