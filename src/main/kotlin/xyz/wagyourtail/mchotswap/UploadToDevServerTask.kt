package xyz.wagyourtail.mchotswap

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import java.net.URI


@Suppress("LeakingThis")
abstract class UploadToDevServerTask : DefaultTask() {

    @get:InputFile
    abstract val inputFile: RegularFileProperty

    @get:Input
    abstract val modid: Property<String>

    @get:Input
    @get:Optional
    abstract val serverUrl: Property<String>

    @get:Input
    @get:Optional
    abstract val serverKey: Property<String>

    @TaskAction
    fun uploadFile() {
        val file = inputFile.get().asFile
        println("Uploading $file")

        val url = URI.create(serverUrl.get() + "?modid=${modid.get()}&apiKey=${serverKey.get()}").toURL()
        val conn = url.openConnection() as java.net.HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/java-archive")
        conn.doOutput = true
        conn.outputStream.use { os ->
            file.inputStream().use { it.copyTo(os) }
        }
        println("Response: ${conn.responseCode} ${conn.responseMessage}")
    }

    init {
        serverUrl.convention("http://localhost:25401")
        serverKey.convention("changeme!")
        group = "remote-hotswap"
    }

}