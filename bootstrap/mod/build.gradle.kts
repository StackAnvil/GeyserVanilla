plugins {
    id("geyser.modded-conventions")
}

architectury {
    common("neoforge", "fabric")
}

afterEvaluate {
    // We don't need these
    tasks {
        named("mergeShadowAndJarJar").configure {
            enabled = false
        }

        named("renameModrinthJar").configure {
            enabled = false
        }

        modrinth.configure {
            enabled = false
        }
    }
}

val vanillaTestRuntime by configurations.creating

dependencies {
    api(projects.core)
    api(projects.vanillaHost)
    compileOnly(libs.mixin)
    compileOnly(libs.mixinextras)

    // Only here to suppress "unknown enum constant EnvType.CLIENT" warnings. DO NOT USE!
    compileOnly(libs.fabric.loader)

    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation(libs.junit)
    testImplementation(libs.mockito)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    vanillaTestRuntime(project(path = ":vanilla-protocol", configuration = "vanillaClient"))
}
tasks.test {
    useJUnitPlatform()
    classpath = vanillaTestRuntime + classpath
}
