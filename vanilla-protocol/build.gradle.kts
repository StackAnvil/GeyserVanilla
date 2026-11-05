import java.util.Properties

plugins {
    id("geyser.base-conventions")
}

val runtimeBootstrap by configurations.creating

val prepareVanillaRuntime = tasks.register<JavaExec>("prepareVanillaRuntime") {
    group = "build setup"
    description = "Downloads the pinned vanilla client and libraries for compilation and tests."
    classpath = runtimeBootstrap
    mainClass.set("org.geysermc.geyser.vanilla.runtime.VanillaRuntime")
    args(layout.buildDirectory.dir("runtime").get().asFile.absolutePath)
    // Always validate the cache, including existing files, before using executable code.
}

val runtimeManifest = Properties().apply {
    rootProject.file("vanilla-runtime/src/main/resources/vanilla-runtime.properties").inputStream().use { load(it) }
}
check(runtimeManifest.getProperty("version") == libs.versions.minecraft.get()) {
    "Run python3 tools/update-vanilla-runtime.py after changing the Minecraft version"
}
val osName = System.getProperty("os.name").lowercase().let {
    when {
        it.contains("mac") || it.contains("darwin") -> "osx"
        it.contains("win") -> "windows"
        else -> "linux"
    }
}
val vanillaClasspath = files((0 until runtimeManifest.getProperty("artifact.count").toInt()).mapNotNull {
    val prefix = "artifact.$it."
    val os = runtimeManifest.getProperty(prefix + "os", "")
    if (os.isEmpty() || os == osName) {
        layout.buildDirectory.file("runtime/artifacts/${runtimeManifest.getProperty(prefix + "sha1")}.jar")
    } else null
}).builtBy(prepareVanillaRuntime)

val vanillaClient by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    description = "The verified client runtime for platform launchers and integration tests."
}

dependencies {
    vanillaClient(vanillaClasspath)
    runtimeBootstrap(projects.vanillaRuntime)
    api(projects.vanillaRuntime)
    compileOnlyApi(vanillaClasspath)
    testImplementation(vanillaClasspath)
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation(libs.junit)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

indra {
    javaVersions {
        target(25)
    }
}

tasks.test {
    useJUnitPlatform()
    systemProperty("java.awt.headless", "true")
}
