plugins {
    kotlin("jvm") version "2.1.10"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

application {
    mainClass.set("Main")
}
dependencies {
    testImplementation(kotlin("test"))
    implementation(kotlin("stdlib"))
    implementation("org.jsoup:jsoup:1.15.3")
    implementation("it.skrape:skrapeit:1.2.2")
    implementation("io.ktor:ktor-client-core:2.3.4")
    implementation("io.ktor:ktor-client-cio:2.3.4")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.4")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.4")
    implementation("com.google.code.gson:gson:2.10.1")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}

val jarA by tasks.creating(Jar::class) {
    archiveBaseName.set("scraperA")
    manifest {
        attributes["Main-Class"] = "MainKt"
    }
    from(sourceSets.main.get().output)
    dependsOn(tasks.build)
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

val jarB by tasks.creating(Jar::class) {
    archiveBaseName.set("scraperB")
    manifest {
        attributes["Main-Class"] = "routesScraperKt"
    }
    from(sourceSets.main.get().output)
    dependsOn(tasks.build)
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

val jarC by tasks.creating(Jar::class) {
    archiveBaseName.set("scraperC")
    manifest {
        attributes["Main-Class"] = "GeoMain - improvedKt"
    }
    from(sourceSets.main.get().output)
    dependsOn(tasks.build)
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.register<Jar>("fatJar") {
    group = "build"
    archiveBaseName.set("scraperA")
    archiveVersion.set("1.0-SNAPSHOT")
    archiveClassifier.set("all")

    manifest {
        attributes["Main-Class"] = "RoutesScraperKt"
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith("jar") }
            .map { zipTree(it) }
    })
}
