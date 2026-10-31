import buildsrc.config.AuditOutcome
import buildsrc.config.AuditReport
import buildsrc.config.PnpmAudit
import buildsrc.config.args
import com.github.gradle.node.pnpm.task.PnpmTask
import java.io.File
import java.net.URI

plugins {
  buildsrc.convention.base
  com.github.`node-gradle`.node
  buildsrc.convention.`knit-files`
}

description = "builds the kotlin-tsgen website"

// pnpm is provided by mise. The node plugin's pnpmSetup would fetch
// `pnpm@latest` from npm into .gradle/pnpm, which drifts from the pinned
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
 * `pnpm audit` reads the lockfile, not `node_modules`. The advisories it
 * accepts live in `pnpm-workspace.yaml` under `audit.ignore`, which pnpm
 * removes from its own output, so they are read from that file and reported
 * with the reason written above them.
 */
val pnpmAudit by tasks.registering {
  group = "verification"
  description = "fails when the site has high or critical dependency advisories"

  // The advisory database changes without the lockfile changing, so the
  // audit runs every time. The lockfile is not declared as an input, because
  // pnpmInstall produces it and the audit must not force that install.
  outputs.upToDateWhen { false }

  // Configured here, executed when the value is read: a task action may not
  // reach for the project, but it may read a provider.
  val audit =
    providers.exec {
      workingDir = projectDir
      commandLine("mise", "x", "--", "pnpm", "audit", "--json", "--audit-level=high")
      // `pnpm audit` exits non-zero when it finds what this task looks for.
      setIgnoreExitValue(true)
    }
  val workspace = File(projectDir, "pnpm-workspace.yaml")
  val report = rootProject.layout.buildDirectory.file("reports/site-audit.json").get().asFile

  doLast {
    val execution = audit.result.get()
    val parsed = PnpmAudit.parse(audit.standardOutput.asText.get())
    PnpmAudit.requireReported(
      execution.exitValue,
      parsed.findings,
      audit.standardError.asText.get(),
    )
    val pins = PnpmAudit.pinsIn(workspace.readText(), "site/pnpm-workspace.yaml")
    val outcome =
      AuditOutcome(
        kind = "site",
        lockFile = "site/pnpm-lock.yaml",
        checkedPackages = parsed.checkedPackages,
        findings = parsed.findings.map { it.copy(pin = pins[it.packageName]) },
        ignored = PnpmAudit.ignoredIn(workspace.readText()),
      )
    AuditReport.publish(logger, report, outcome)
    logger.lifecycle(
      "The site dependencies have no unaccepted advisories " +
        "(${parsed.checkedPackages} packages checked).",
    )
  }
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
