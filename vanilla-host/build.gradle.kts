plugins {
    id("geyser.base-conventions")
    `java-test-fixtures`
}

indra {
    javaVersions {
        target(21)
    }
}

dependencies {
    api(projects.vanillaRuntime)
    compileOnly(libs.netty.handler)
    testImplementation("io.netty:netty-handler:${libs.versions.netty.legacy.get()}")
    testFixturesImplementation("io.netty:netty-handler:${libs.versions.netty.legacy.get()}")
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation(libs.junit)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
