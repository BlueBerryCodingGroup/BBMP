plugins {
    application
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

java {
    toolchain {
        languageVersion.set(org.gradle.jvm.toolchain.JavaLanguageVersion.of(17))
    }
}

repositories { mavenCentral() }

dependencies {
    implementation("com.formdev:flatlaf:3.4.1")
}

application {
    mainClass.set("net.bbmp.App")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.jar {
    manifest { attributes["Main-Class"] = "net.bbmp.App" }
}

tasks.shadowJar {
    archiveClassifier.set("all")
    mergeServiceFiles()
    // minimize()
}

tasks.build { dependsOn(tasks.shadowJar) }

// (optional) if you want the distribution zips to be ready after shadowing
tasks.startScripts { dependsOn(tasks.shadowJar) }
tasks.distZip { dependsOn(tasks.shadowJar) }
tasks.distTar { dependsOn(tasks.shadowJar) }