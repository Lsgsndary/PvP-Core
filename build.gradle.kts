plugins {
    java
    id("com.gradleup.shadow") version "8.3.6"
}

group = "com.pvpcore"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Paper API for Minecraft 1.21.11 (provided by the server at runtime, so compileOnly)
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    processResources {
        filteringCharset = "UTF-8"
        val props = mapOf("version" to project.version)
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    shadowJar {
        // Produces build/libs/PvPCore-1.0.0.jar (no "-all" suffix)
        archiveClassifier.set("")
    }

    jar {
        // Only the shadow jar is meant to be uploaded
        enabled = false
    }

    build {
        dependsOn(shadowJar)
    }
}
