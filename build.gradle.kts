plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.jvm) apply false
}

val kotlinFormatter = configurations.create("kotlinFormatter") {
    isTransitive = false // The selected formatter JAR already bundles its dependencies.
}

dependencies {
    kotlinFormatter("com.facebook:ktfmt:0.54:jar-with-dependencies")
}

val kotlinSources = fileTree(rootDir) {
    include("app/src/**/*.kt", "core/domain/src/**/*.kt", "backend/src/**/*.kt")
}

fun registerKotlinFormatTask(taskName: String, checkOnly: Boolean) = tasks.register<JavaExec>(taskName) {
    group = "verification"
    description = if (checkOnly) "Check Kotlin formatting" else "Format Kotlin sources"
    classpath = kotlinFormatter
    mainClass.set("com.facebook.ktfmt.cli.Main")
    args("--kotlinlang-style")
    if (checkOnly) args("--dry-run", "--set-exit-if-changed")
    args(kotlinSources.files.sortedBy { it.path }.map { it.absolutePath })
}

registerKotlinFormatTask("formatKotlin", false)
registerKotlinFormatTask("checkKotlinFormat", true)
