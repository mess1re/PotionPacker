import org.gradle.api.attributes.java.TargetJvmVersion

plugins {
    java
}

group = "me.mss1r.ppacker"
version = "1.2.2"

val pluginVersion = version.toString()
val paperApiVersion = "26.2.build.87-stable"
val pluginApiVersion = "1.21"
val javaToolchainVersion = 25
val javaTargetVersion = 21
val junitVersion = "5.13.4"
val mockitoVersion = "5.18.0"

val mockitoAgent by configurations.creating

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

    testImplementation("io.papermc.paper:paper-api:$paperApiVersion")
    testImplementation(platform("org.junit:junit-bom:$junitVersion"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:$mockitoVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    mockitoAgent("org.mockito:mockito-core:$mockitoVersion") {
        isTransitive = false
    }
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

tasks.test {
    useJUnitPlatform()
    jvmArgs("-javaagent:${mockitoAgent.singleFile.absolutePath}")
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
