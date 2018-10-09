package de.usu.usickaag.urltester

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.MissingParameter
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.restrictTo
import java.io.File

class TestCommand : CliktCommand() {
    //update names and help function
    val inputFile: File by option("-i","--input-file", help="Input file where every non-empty line is an url to test").file().required()
    val fromInclusive: Int? by option("-f","--from", help="Start index, inclusive (based on non-empty lines of inputFile)").int().restrictTo(0)
    val toExclusive: Int? by option("-t","--to", help="End index, exclusive (based on non-empty lines of inputFile)").int().restrictTo(0)
    val outputFile: File by option("-o","--output-file", help="Output file (where to write the CSV results)").file().default(File("output.csv"))
    val prefixRemove: String by option("-r","--prefix-remove", help="Prefix to remove from each url (won't test any url that does not match that prefix)").default("")
    val prefixAdd: String by option("-a","--prefix-add", help="Prefix to add to each url after removing other prefix").default("")

    override fun run() {
        //transform input into correct values and call UrlTester
        val inputFileContent: List<String> = inputFile.readLines(Charsets.UTF_8).filter { it.isNotBlank() }
        val fromInclusive: Int = this.fromInclusive?:0
        val toExclusive: Int = this.toExclusive?:(inputFileContent.size)
        val outputFile: File = this.outputFile
        if (!outputFile.exists()) {
            outputFile.createNewFile()
        }
        if (!outputFile.canWrite()) {
            throw MissingParameter("Output file parameter '${outputFile.absolutePath}' was not a writeable file, aborting")
        }
        val existingOutputFileContent: List<String> = outputFile.readLines(Charsets.UTF_8).filter { it.isNotBlank() }
        val prefixToRemove: String = this.prefixRemove
        val prefixToAdd: String = this.prefixAdd

        val tester = UrlTester(inputFileContent, fromInclusive, toExclusive, existingOutputFileContent, outputFile, prefixToRemove, prefixToAdd)
        tester.process()
    }
}

fun main(args: Array<String>) = TestCommand().main(args)