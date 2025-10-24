import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.LibraryVariant
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import sp.kx.gradlex.GitHub
import sp.kx.gradlex.Markdown
import sp.kx.gradlex.Maven
import sp.kx.gradlex.add
import sp.kx.gradlex.asFile
import sp.kx.gradlex.assemble
import sp.kx.gradlex.buildDir
import sp.kx.gradlex.buildSrc
import sp.kx.gradlex.camelCase
import sp.kx.gradlex.check
import sp.kx.gradlex.create
import sp.kx.gradlex.dir
import sp.kx.gradlex.eff
import sp.kx.gradlex.get

version = "0.0.1"

val maven = Maven.Artifact(
    group = "com.github.kepocnhh",
    id = rootProject.name,
)

val gh = GitHub.Repository(
    owner = "StanleyProjects",
    name = rootProject.name,
)

repositories {
    google()
    mavenCentral()
}

plugins {
    id("com.android.library")
    id("kotlin-android")
}

fun BaseVariant.getVersion(): String {
    return when (flavorName) {
        "unstable" -> {
            when (buildType.name) {
                "debug" -> "${version}u-SNAPSHOT"
                else -> error("Build type \"${buildType.name}\" is not supported for flavor \"$flavorName\"!")
            }
        }
        else -> error("Flavor name \"$flavorName\" is not supported!")
    }
}

/*
fun checkReadme(variant: BaseVariant) {
    tasks.create("check", variant.name, "Readme") {
        doLast {
            when (variant.name) {
                "unstableDebug" -> {
                    val badge = Markdown.image(
                        text = "version",
                        url = Badge.url(
                            label = "version",
                            message = variant.getVersion(),
                            color = "2962ff",
                        ),
                    )
                    val expected = setOf(
                        badge,
//                        Markdown.link("Maven", Maven.Snapshot.url(maven, variant.getVersion())), // todo
                        "implementation(\"${maven.moduleName(variant.getVersion())}\")",
                    )
                    val report = buildDir()
                        .dir("reports/analysis/readme")
                        .dir(variant.name)
                        .asFile("index.html")
                    rootDir.resolve("README.md").check(
                        expected = expected,
                        report = report,
                    )
                }
                else -> error("Variant \"${variant.name}\" is not supported!")
            }
        }
    }
}
*/

/*
fun assemblePom(variant: BaseVariant) {
    tasks.create("assemble", variant.name, "Pom") {
        doLast {
            val file = buildDir()
                .dir("xml")
                .dir(variant.name)
                .file("maven.pom.xml")
                .assemble(
                    maven.pom(
                        version = variant.getVersion(),
                        packaging = "aar",
                    ),
                )
            println("POM: ${file.absolutePath}")
        }
    }
}
*/

/*
fun assembleSource(variant: BaseVariant) {
    task<Jar>("assemble", variant.name, "Source") {
        val sourceSets = variant.sourceSets.flatMap { it.kotlinDirectories }.distinctBy { it.absolutePath }
        from(sourceSets)
        val dir = buildDir()
            .dir("sources")
            .asFile(variant.name)
        val file = File(dir, "${maven.name(variant.getVersion())}-sources.jar")
        outputs.upToDateWhen {
            file.exists()
        }
        doLast {
            dir.mkdirs()
            val renamed = archiveFile.get().asFile.existing().file().filled().renameTo(file)
            check(renamed)
            println("Archive: ${file.absolutePath}")
        }
    }
}
*/

fun assembleMetadata(variant: BaseVariant) {
    tasks.create("assemble", variant.name, "Metadata") {
        doLast {
            val target = buildDir().dir("yml").file("metadata.yml")
            val file = gh.assemble(version = variant.getVersion(), target = target)
            println("Metadata: ${file.absolutePath}")
        }
    }
}

fun assembleMavenMetadata(variant: BaseVariant) {
    tasks.create("assemble", variant.name, "MavenMetadata") {
        doLast {
            val target = buildDir().dir("yml").file("maven-metadata.yml")
            val file = maven.assemble(version = variant.getVersion(), target = target)
            println("Maven metadata: ${file.absolutePath}")
        }
    }
}

android {
    namespace = "sp.useless.android"
    compileSdk = Version.Android.compileSdk

    defaultConfig {
        minSdk = Version.Android.minSdk
    }

    productFlavors {
        mapOf("stability" to setOf("unstable")).forEach { (dimension, flavors) ->
            flavorDimensions += dimension
            flavors.forEach { flavor ->
                create(flavor) {
                    this.dimension = dimension
                }
            }
        }
    }

    fun onVariant(variant: LibraryVariant) {
        val supported = setOf("unstableDebug")
        if (!supported.contains(variant.name)) {
            tasks.getByName(camelCase("pre", variant.name, "Build")) {
                doFirst {
                    error("Variant \"${variant.name}\" is not supported!")
                }
            }
            return
        }
        val output = variant.outputs.single()
        check(output is com.android.build.gradle.internal.api.LibraryVariantOutputImpl)
        output.outputFileName = "${rootProject.name}-${variant.getVersion()}.aar"
//        checkReadme(variant)
//        assemblePom(variant)
//        assembleSource(variant)
        assembleMetadata(variant = variant)
        assembleMavenMetadata(variant = variant)
        afterEvaluate {
            tasks.getByName<JavaCompile>(camelCase("compile", variant.name, "JavaWithJavac")) {
                targetCompatibility = Version.jvmTarget
            }
            tasks.getByName<KotlinCompile>(camelCase("compile", variant.name, "Kotlin")) {
                kotlinOptions {
                    jvmTarget = Version.jvmTarget
                    freeCompilerArgs = freeCompilerArgs + setOf("-module-name", maven.moduleName(separator = '-'))
                }
            }
        }
    }

    libraryVariants.all {
        onVariant(this)
    }
}
