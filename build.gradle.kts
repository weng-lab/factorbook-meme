import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "1.3.21"
    id("com.github.johnrengelman.shadow") version "4.0.2"
    id("application")
}

group = "com.genomealmanac.motif"
version = "1.1.0"
val artifactID = "factorbook-meme"

repositories {
    mavenCentral()
}

val biojavaVersion = "5.2.1"

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    compile("com.github.ajalt", "clikt", "2.2.0")
    compile("io.github.microutils","kotlin-logging","1.6.10")
    compile("ch.qos.logback", "logback-classic","1.2.3")
    compile("org.biojava", "biojava-core", biojavaVersion) {
        exclude("org.apache.logging.log4j")
    }
    compile("org.biojava", "biojava-genome", biojavaVersion) {
        exclude("org.apache.logging.log4j")
    }
    compile("org.apache.commons", "commons-math3", "3.6.1")
    compile("org.eclipse.collections", "eclipse-collections-api", "10.0.0")
    compile("org.eclipse.collections", "eclipse-collections", "10.0.0")

    implementation("com.squareup.moshi", "moshi-kotlin", "1.8.0")
    testImplementation("org.junit.jupiter", "junit-jupiter", "5.4.0")
    testCompile("org.assertj", "assertj-core", "3.11.1")
    testCompile("org.knowm.xchart", "xchart", "3.6.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

application {
    mainClassName = "AppKt"
}
val shadowJar: ShadowJar by tasks
shadowJar.apply {
    baseName = artifactID
    classifier = ""
    destinationDir = file("build")
}
