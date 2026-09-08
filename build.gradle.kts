import java.util.zip.ZipFile

plugins {
    java
}

group = "net.plexon"
version = "1.1.0"

val pluginVersion = version.toString()

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://api.modrinth.com/maven")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.121-stable")
    compileOnly("maven.modrinth:O4o4mKaq:dGfCZHqk")
    compileOnly("com.zpkdxgames:PlexonCore:1.0.0")

    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:deprecation", "-Xlint:-processing"))
}

tasks.processResources {
    val resourceProperties = mapOf("version" to pluginVersion)
    inputs.properties(resourceProperties)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(resourceProperties)
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    archiveBaseName.set("PlexonClaimFlags")
    archiveVersion.set(pluginVersion)
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
    manifest {
        attributes(
            "Implementation-Title" to "PlexonClaimFlags",
            "Implementation-Version" to pluginVersion,
            "Implementation-Vendor" to "ZpkDxGames"
        )
    }
}

val verifyHotPathSource = tasks.register("verifyHotPathSource") {
    group = "verification"
    description = "Verifies protection listeners do not introduce Core or disk I/O calls into gameplay hot paths."
    doLast {
        val source = file("src/main/java/net/plexon/claimflags/ProtectionListener.java").readText()
        require(!source.contains("PlexonCore")) { "ProtectionListener must not resolve PlexonCore in gameplay hot paths" }
        require(!source.contains("flags.yml")) { "ProtectionListener must not access flags.yml in gameplay hot paths" }
        require(!source.contains(".save(")) { "ProtectionListener must not save files in gameplay hot paths" }
        require(!source.contains(".load(")) { "ProtectionListener must not load files in gameplay hot paths" }
    }
}

val verifyDistribution = tasks.register("verifyDistribution") {
    group = "verification"
    description = "Checks the PlexonClaimFlags distribution, public contract and Core isolation."
    dependsOn(tasks.jar, verifyHotPathSource)
    doLast {
        val archive = tasks.jar.get().archiveFile.get().asFile
        require(archive.isFile && archive.length() > 10_000L) {
            "Runtime JAR is missing or unexpectedly small: $archive"
        }
        ZipFile(archive).use { zip ->
            listOf(
                "plugin.yml",
                "net/plexon/claimflags/PlexonClaimFlags.class",
                "net/plexon/claimflags/api/PlexonClaimFlagsAPI.class",
                "net/plexon/claimflags/api/FlagChangeResult.class",
                "net/plexon/claimflags/event/PlexonClaimFlagChangedEvent.class",
                "net/plexon/claimflags/integration/core/CoreBridge.class"
            ).forEach { entry -> require(zip.getEntry(entry) != null) { "Missing JAR entry: $entry" } }
            require(zip.entries().asSequence().none { it.name.startsWith("com/zpkdxgames/plexoncore/") }) {
                "PlexonCore runtime classes must not be shaded into PlexonClaimFlags"
            }
            val pluginYml = zip.getInputStream(zip.getEntry("plugin.yml")).bufferedReader().readText()
            require(pluginYml.contains("version: 1.1.0")) { "plugin.yml version is not 1.1.0" }
            require(pluginYml.contains("GriefPrevention")) { "plugin.yml must retain the GriefPrevention dependency" }
            require(pluginYml.contains("PlexonCore")) { "plugin.yml must declare PlexonCore as a soft dependency" }
        }
    }
}

tasks.check {
    dependsOn(verifyDistribution)
}
