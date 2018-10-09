@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package de.usu.usickaag.urltester

import com.egorzh.networkinkt.HTTP
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

class UrlTester(val inputFileContent: List<String>, val fromInclusive: Int, val toExclusive: Int, val existingOutputFileContent: List<String>, val outputFile: File, val prefixToRemove: String, val prefixToAdd: String) {
    //if prefix to remove does not match, let line be empty

    private val counter = AtomicInteger(0)
    private val total = toExclusive - fromInclusive


    private fun counterTick() {
        val current = counter.incrementAndGet()
        if (current % 10 == 0) {
            println("Finished $current out of $total")
        }
    }

    private fun String.toCSVLine() : CSVLine {
        val arr = this.split(";")
        if (arr.size != 3) {
            throw UnsupportedOperationException()
        }
        return CSVLine(arr[0], arr[1], arr[2])
    }

    private fun buildPreparedCSV(): Pair<CopyOnWriteArrayList<CSVLine>, List<Int>> {
        val list = CopyOnWriteArrayList<CSVLine>()
        val indices = mutableListOf<Int>()
        val emptyLinesToAdd: Int = Math.max(0, fromInclusive - existingOutputFileContent.size)
        existingOutputFileContent.take(fromInclusive).forEach { list.add(it.toCSVLine()) }
        (0 until emptyLinesToAdd).forEach {list.add(CSVLine(""))}
        //build from old data or potential new one
        //check for compliance here already, and add temporary url
        //if compliable, add to second list (indices to http get)
        (fromInclusive until toExclusive).forEach{
            val incomingUrl = inputFileContent.get(it)
            var temporaryUrl = ""
            if (incomingUrl.startsWith(prefixToRemove)) {
                temporaryUrl = prefixToAdd + incomingUrl.substring(prefixToRemove.length)
                indices.add(it)
            }
            list.add(CSVLine(incomingUrl, temporaryUrl))
        }

        existingOutputFileContent.drop(toExclusive - 1).forEach { list.add(it.toCSVLine()) }
        return Pair(list, indices)
    }

    private suspend fun replaceSingleLine(lines: CopyOnWriteArrayList<CSVLine>, index: Int) {
        //operate on the one line
        //if compliable, make http call
        val oldEntry = lines[index]
        if (oldEntry.temporaryUrl != null) {
            val text = HTTP.get(oldEntry.temporaryUrl).getText()
            lines[index] = oldEntry.copy(resultUrl = text)
        } else {
            lines[index] = oldEntry.copy(resultUrl = "N/A")
        }
        counterTick()
    }

    private fun writeToFile(lines: CopyOnWriteArrayList<CSVLine>) {
        val copy = ArrayList<String>(lines.size)
        lines.forEach { copy.add(it.toString()) }
        ///write to file
        if(!outputFile.exists()) {
            outputFile.createNewFile()
        }
        outputFile.bufferedWriter(Charsets.UTF_8).use { out ->
            copy.forEach {
                out.write(it)
                out.write("\n")
            }
        }
    }

    fun process() {
        val (list, indices) = buildPreparedCSV()
        runBlocking {
            indices.map { async { replaceSingleLine(list, it) } }.forEach { it.await() }
        }
        writeToFile(list)
    }
}