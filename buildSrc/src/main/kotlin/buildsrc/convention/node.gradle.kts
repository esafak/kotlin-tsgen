package buildsrc.convention

plugins {
  id("com.github.node-gradle.node")
  id("buildsrc.convention.base")
}

val rootGradleDir: Directory = rootProject.layout.projectDirectory.dir(".gradle")

node {
  download.set(true)
  version.set("20.19.0")

  distBaseUrl.set(null as String?) // set in repositories.settings.gradle.kts

  workDir.set(rootGradleDir.dir("nodejs"))
  npmWorkDir.set(rootGradleDir.dir("npm"))
  pnpmVersion.set("10.15.1")
  pnpmWorkDir.set(rootGradleDir.dir("pnpm"))
  yarnWorkDir.set(rootGradleDir.dir("yarn"))
}
