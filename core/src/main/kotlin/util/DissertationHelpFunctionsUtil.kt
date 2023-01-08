package util

import dataProcessor.inputData.DissertationResult
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.commons.io.FileUtils
import org.eclipse.jgit.lib.RepositoryCache
import org.eclipse.jgit.util.FS
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import kotlin.math.log2

object DissertationHelpFunctionsUtil {
    val json = Json { encodeDefaults = true }



    fun createParentFolder(file: File) {
        val folder = File(file.parent)
        folder.mkdirs()
    }

    // TODO: try catch logic for encode and file
    inline fun <reified T> saveToJson(file: File, data: T) {
        createParentFolder(file)
        val jsonString = json.encodeToString(data)
        file.writeText(jsonString)
    }

    inline fun <reified T> saveToJson(file: File, data: T, serializer: SerializationStrategy<T>) {
        createParentFolder(file)
        val jsonString = json.encodeToString(serializer, data)
        file.writeText(jsonString)
    }
    inline fun  saveToCsv(file: File, data: Map<String, DissertationResult>) {
        createParentFolder(file)
        var csvString = "Month,Number of commits, Size of commits\n"
        data.keys.forEach {

            csvString=csvString.plus(it)
                .plus(",")
                .plus(data[it]?.numberOfCommits)
                .plus(",")
                .plus(data[it]?.sizeOfCommits)
                .plus("\n")
        }
        file.writeText(csvString)
    }
}
