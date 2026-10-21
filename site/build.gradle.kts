import com.github.gradle.node.pnpm.task.PnpmTask
import java.net.URI
import buildsrc.config.*

plugins {
  buildsrc.convention.base
  com.github.`node-gradle`.node
  buildsrc.convention.`knit-files`
}

description = "builds the kotlin-tsgen website"

// pnpm is provided by mise (aqua backend). The node plugin's pnpmSetup would
// fetch `pnpm@latest` from npm into .gradle/pnpm, which drifts from the pinned
// version and is not the pnpm that runs (pnpm resolves from PATH). Disable it
// so mise stays the single source of pnpm.
tasks.pnpmSetup {
  enabled = false
}

dependencies {
  knitDocs(projects.docs.code)
}

/**
 * Fails the build when the site's dependency tree has a high or critical
 * advisory. Dependabot only reports what it recognizes, so this is what
 * surfaces the rest.
 *
 * `pnpm audit` reads the lockfile, not `node_modules`, and the advisories
 * live in `pnpm-workspace.yaml` under `audit.ignore`. Every entry there needs
 * a reason: a patched version that upstream does not release yet.
 *
 * The audit runs through mise, because the ignore list needs the pnpm version
 * that mise.toml pins. An older pnpm on PATH does not read it.
 */
val pnpmAudit by tasks.registering(Exec::class) {
  group = "verification"
  description = "fails when the site has high or critical dependency advisories"

  workingDir = projectDir
  commandLine("mise", "x", "aqua:pnpm/pnpm", "--", "pnpm", "audit", "--audit-level=high")

  inputs.file(layout.projectDirectory.file("pnpm-lock.yaml"))
  inputs.file(layout.projectDirectory.file("pnpm-workspace.yaml"))
  // The advisory database changes without the lockfile changing.
  outputs.upToDateWhen { false }
}

tasks.check {
  dependsOn(pnpmAudit)
}

tasks.assemble {
  dependsOn(updateSiteKnitDocs)
  dependsOn(tasks.pnpmInstall)
}

val docusaurusRun by tasks.registering(PnpmTask::class) {
  group = "documentation"
  args("run start")

  dependsOn(tasks.assemble)
}

val docusaurusBuild by tasks.registering(PnpmTask::class) {
  group = "documentation"
  args("run build")

  dependsOn(tasks.assemble)
}

val updateSiteKnitDocs by tasks.registering(Sync::class) {
  from(configurations.knitDocs) {
    // fix all the relative links to non-markdown files
    filter { line ->
      line.replace(Regex("""\((?<file>(?:.\/|..\/)\S+[^.md])\)""")) { match ->
        val (file) = match.destructured
        val uri = URI
          .create("https://github.com/esafak/kotlin-tsgen/blob/main/docs/$file")
          .normalize()
        "($uri)"
      }
    }
  }
  into(layout.projectDirectory.dir("docs/examples"))

}

tasks.clean {
  delete(layout.projectDirectory.dir(".docusaurus"))
  delete(layout.projectDirectory.dir("node_modules"))
}
