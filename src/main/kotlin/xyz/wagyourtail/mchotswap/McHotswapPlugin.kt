package xyz.wagyourtail.mchotswap

import org.gradle.api.Plugin
import org.gradle.api.Project

class McHotswapPlugin : Plugin<Project> {

    override fun apply(target: Project) {

        target.extensions.extraProperties.set("UploadToDevServer", UploadToDevServerTask::class.java)

    }

}
