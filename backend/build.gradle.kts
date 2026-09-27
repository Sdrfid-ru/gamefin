plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

kotlin { jvmToolchain(17) }

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.call.id)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)
    implementation(libs.postgresql)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(kotlin("test"))
}

application { mainClass.set("ru.findrug.backend.ApplicationKt") }
tasks.test { useJUnitPlatform() }
