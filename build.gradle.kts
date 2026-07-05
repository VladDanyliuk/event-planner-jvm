plugins {
    kotlin("jvm") version "2.0.21"
    scala
    application
    kotlin("plugin.serialization") version "2.0.21"
    id("org.openjfx.javafxplugin") version "0.1.0"
    idea
}

group = "com.eventplanner"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

javafx {
    version = "21"
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.graphics")
}

dependencies {
    // Kotlin
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Scala
    implementation("org.scala-lang:scala-library:2.13.12")

    // JavaFX
    implementation("org.openjfx:javafx-controls:21")
    implementation("org.openjfx:javafx-fxml:21")

    // Testing
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("com.eventplanner.MainKt")
}

tasks.test {
    useJUnitPlatform()
}

// Set JVM target
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

// Configure Scala compilation
tasks.withType<ScalaCompile> {
    scalaCompileOptions.apply {
        additionalParameters = listOf("-release", "17")
    }
}

sourceSets {
    main {
        kotlin {
            srcDirs("src/main/kotlin")
        }
        scala {
            srcDirs("src/main/scala")
        }
        resources {
            srcDirs("src/main/resources")
        }
    }
}

// Configure IDEA module
idea {
    module {
        // Mark Scala sources
        sourceDirs.addAll(files("src/main/scala"))

        // Ensure Scala classes are indexed by IntelliJ
        isDownloadSources = true
        isDownloadJavadoc = false

        // Inherit output dirs from Gradle
        inheritOutputDirs = true
    }
}

// Configure proper compilation order for mixed Scala-Kotlin project
// Break the circular dependency by making tasks independent where possible
project.afterEvaluate {
    // Make Scala compile first, independent of Java
    tasks.named<ScalaCompile>("compileScala") {
        setDependsOn(emptyList<Any>())
        classpath = sourceSets.main.get().compileClasspath
    }

    // Kotlin compiles after Scala, with Scala classes on classpath
    val compileScalaTask = tasks.named("compileScala")
    tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileKotlin") {
        setDependsOn(listOf(compileScalaTask))
        libraries.from(files(layout.buildDirectory.dir("classes/scala/main")))
    }

    // Java compiles last
    tasks.named<JavaCompile>("compileJava") {
        setDependsOn(listOf(tasks.named("compileKotlin")))
    }
}
