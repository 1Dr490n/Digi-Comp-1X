plugins {
    kotlin("jvm") version "2.0.0"

    application
}

application {
    mainClass.set("de.drgn.digicomp1x.MainKt")
}

group = "de.drgn"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    // Source: https://mvnrepository.com/artifact/org.python/jython-standalone
    implementation("org.python:jython-standalone:2.7.4")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "de.drgn.digicomp1x.MainKt"
    }

    configurations["compileClasspath"].forEach { file: File ->
        from(zipTree(file.absoluteFile))
    }
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    archiveFileName.set("Digi-Comp-1X.jar")
    java.sourceSets["main"].java {
        srcDir("src/main/resources")
    }
}