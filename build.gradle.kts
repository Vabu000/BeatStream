// Top-level build file where you can add configuration options common to all sub-projects/modules.
// ЛР №1: Плагін SonarQube для статичного аналізу (SonarCloud)
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    id("org.sonarqube") version "5.1.0.4882"
}