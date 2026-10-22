plugins {
  `kotlin-dsl`
}

dependencies {
  implementation(platform(libs.kotlin.bom))

  implementation(libs.gradlePlugin.kotlin)
  implementation(libs.gradlePlugin.kotlinSerialization)
  implementation(libs.gradlePlugin.gradleNode)
  implementation(libs.gradlePlugin.kotlinxKover)
  implementation(libs.gradlePlugin.kotlinxKnit)
  implementation(libs.gradlePlugin.kotest)
  implementation(libs.gradlePlugin.kotlinSymbolProcessing)
  implementation(libs.gradlePlugin.nmcp)

  testImplementation(platform(libs.kotest.bom))
  testImplementation(libs.kotest.frameworkEngine)
  testImplementation(libs.kotest.runnerJUnit5)
}

tasks.test {
  useJUnitPlatform()
}
