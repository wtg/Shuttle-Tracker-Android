buildscript {
    dependencies {
        classpath(libs.google.services)
        classpath(libs.secrets.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kotlin.ksp) apply false
    alias(libs.plugins.google.service) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

// pre commit hook to check kotlin style before commits
tasks.register<Copy>("copyPreCommitHook") {
    description = "Copy pre-commit git hook from the scripts to the .git/hooks folder."
    group = "git hooks"
    outputs.upToDateWhen { false }
    from("$rootDir/scripts/pre-commit")
    into("$rootDir/.git/hooks/")
}

