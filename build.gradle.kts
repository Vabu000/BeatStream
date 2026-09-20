// Top-level build file where you can add configuration options common to all sub-projects/modules.
// ЛР №1: SonarCloud аналіз запускається через standalone sonar-scanner CLI (scan.bat)
// Gradle plugin не сумісний з AGP 9.x (видалено AppExtension)
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
}