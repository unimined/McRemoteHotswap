import java.net.URI

plugins {
    id("java")
    id("xyz.wagyourtail.unimined") version "1.0.0-SNAPSHOT"
    `maven-publish`
}

version = if (project.hasProperty("version_snapshot")) project.properties["version"] as String + "-SNAPSHOT" else project.properties["version"] as String
group = project.properties["maven_group"] as String

base {
    archivesName.set(project.properties["archives_base_name"] as String)
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
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

tasks.register("fabricJar", Jar::class) {
    from(
        sourceSets["fabric"].output,
        sourceSets["main"].output
    )

    archiveClassifier.set("fabric")
}

tasks.register("forgeJar", Jar::class) {
    from(
        sourceSets["forge"].output,
        sourceSets["main"].output
    )

    archiveClassifier.set("forge")
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
    val targetVersion = 8
    if (JavaVersion.current().isJava9Compatible) {
        options.release.set(targetVersion)
    }
}

repositories {
    maven("https://repo.spongepowered.org/maven")
}

dependencies {
    compileOnly("org.spongepowered:mixin:0.8.5")
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


publishing {
    repositories {
        maven {
            name = "WagYourMaven"
            url = if (project.hasProperty("version_snapshot")) {
                URI.create("https://maven.wagyourtail.xyz/snapshots/")
            } else {
                URI.create("https://maven.wagyourtail.xyz/releases/")
            }
            credentials {
                username = project.findProperty("mvn.user") as String? ?: System.getenv("USERNAME")
                password = project.findProperty("mvn.key") as String? ?: System.getenv("TOKEN")
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group as String
            artifactId = project.properties["archives_base_name"] as String? ?: project.name
            version = project.version as String

            artifact(tasks["fabricJar"]) {
                classifier = "fabric"
            }
            artifact(tasks["forgeJar"]) {
                classifier = "forge"
            }
        }
    }
}