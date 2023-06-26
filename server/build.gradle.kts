import java.net.URI

plugins {
    id("java")
    id("xyz.wagyourtail.unimined") version "1.0.0-SNAPSHOT"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    `maven-publish`
}

version = if (project.hasProperty("version_snapshot")) project.properties["version"] as String + "-SNAPSHOT" else project.properties["version"] as String
group = project.properties["maven_group"] as String

base {
    archivesName.set(project.properties["archives_base_name"] as String + "mod")
}

java {
    sourceCompatibility = JavaVersion.VERSION_16
    targetCompatibility = JavaVersion.VERSION_16
}

fun SourceSet.outputOf(sourceSet: SourceSet) {
    compileClasspath += sourceSet.output
    runtimeClasspath += sourceSet.output
}

fun SourceSet.outputOf(vararg sourceSets: SourceSet) {
    for (sourceSet in sourceSets) {
        outputOf(sourceSet)
    }
}

sourceSets {
    create("fabric") {
        outputOf(main.get())
    }
    create("forge") {
        outputOf(main.get())
    }
}

unimined.minecraft {
    version(project.properties["minecraft_version"] as String)

    mappings {
        intermediary()
        mojmap()
        parchment("1.19.3", "2023.03.12-nightly-SNAPSHOT")

        devFallbackNamespace("intermediary")
    }

    defaultRemapJar = false
}

unimined.minecraft(sourceSets["fabric"]) {
    version(project.properties["minecraft_version"] as String)

    mappings {
        mojmap()
        parchment("1.19.3", "2023.03.12-nightly-SNAPSHOT")
    }

    fabric {
        loader(project.properties["fabric_version"] as String)
    }
}

unimined.minecraft(sourceSets["forge"]) {
    version(project.properties["minecraft_version"] as String)

    mappings {
        intermediary()
        mojmap()
        parchment("1.19.3", "2023.03.12-nightly-SNAPSHOT")

        devFallbackNamespace("intermediary")
    }

    forge {
        forge(project.properties["forge_version"] as String)
        mixinConfig("modid.mixins.json")
    }
}

tasks.withType<JavaCompile> {
    val targetVersion = 16
    if (JavaVersion.current().isJava9Compatible) {
        options.release.set(targetVersion)
    }
}

repositories {
    maven("https://repo.spongepowered.org/maven")
}

dependencies {
    //bytebuddy
    implementation("net.bytebuddy:byte-buddy-agent:1.14.5")
}


tasks.jar {
    from(
        sourceSets["fabric"].output,
        sourceSets["forge"].output,
        sourceSets["main"].output
    )

    manifest {
        attributes(
            "Implementation-Version" to project.version
        )
    }
}

tasks.getByName("processFabricResources") {
    this as ProcessResources
    inputs.property("version", project.version)

    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

tasks.getByName("processForgeResources") {
    this as ProcessResources
    inputs.property("version", project.version)

    filesMatching("META-INF/mods.toml") {
        expand("version" to project.version)
    }
}