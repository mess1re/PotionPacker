import org.gradle.api.attributes.java.TargetJvmVersion

plugins {
    java
}

group = "me.mss1r.ppacker"
version = "1.2.2"

val pluginVersion = version.toString()
val paperApiVersion = "26.1.2.build.63-stable"
val pluginApiVersion = "1.21"
val javaToolchainVersion = 25
val javaTargetVersion = 21

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaToolchainVersion))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperApiVersion")
}

configurations.configureEach {
    if (isCanBeResolved) {
        attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, javaToolchainVersion)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(javaTargetVersion)
}

tasks.processResources {
    inputs.properties(
        mapOf(
            "pluginVersion" to pluginVersion,
            "pluginApiVersion" to pluginApiVersion,
        )
    )

    filesMatching("plugin.yml") {
        expand(
            mapOf(
                "pluginVersion" to pluginVersion,
                "pluginApiVersion" to pluginApiVersion,
            )
        )
    }
}
