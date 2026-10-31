package buildsrc.config

import groovy.json.JsonOutput
import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import java.io.File

/**
 * One advisory, with the versions the lockfile locked and the pin that holds
 * them, so a red build says what to edit rather than what to look up.
 */
data class AuditFinding(
  val id: String,
  val severity: String,
  val packageName: String,
  val versions: List<String>,
  val title: String,
  val url: String,
  val patched: String? = null,
  val pin: AuditPin? = null,
)

/**
 * The dependency pin that holds a package at a version: a `resolution()` call
 * in the Kotlin/JS build, or an override in the site's workspace file.
 */
data class AuditPin(val file: String, val line: Int, val version: String?)

/**
 * What one advisory audit found, and where it read it from.
 */
data class AuditOutcome(
  val kind: String,
  val lockFile: String,
  val checkedPackages: Int,
  val findings: List<AuditFinding>,
  val accepted: List<AuditFinding> = emptyList(),
  val acceptedReasons: Map<String, String> = emptyMap(),
)

/**
 * Publishes an advisory audit where it is read: the build log, a JSON report
 * for tooling, the GitHub job summary, and a diff annotation on the pin that
 * has to change.
 *
 * The advisory database changes without the repository changing, so the audit
 * fails on a branch that did not cause the failure. The report carries the pin
 * location for exactly that reason: the reader needs to know whose line to
 * edit, not which step to open.
 */
object AuditReport {
  /** Title of the markdown block, per audit. */
  private val labels = mapOf("yarn" to "Kotlin/JS", "site" to "documentation site")

  /**
   * Writes the report, annotates the pins, and fails the build when there is
   * an advisory the repository has not accepted.
   *
   * The report file and the logger arrive as plain values so a task action can
   * call this under the configuration cache.
   */
  fun publish(logger: Logger, report: File, outcome: AuditOutcome) {
    report.parentFile.mkdirs()
    report.writeText(json(outcome))
    logger.lifecycle("Advisory audit report: $report")

    System.getenv("GITHUB_STEP_SUMMARY")?.let { File(it).appendText(markdown(outcome) + "\n") }
    if (System.getenv("GITHUB_ACTIONS") == "true") {
      commands(outcome).forEach { println(it) }
    }

    if (outcome.findings.isNotEmpty()) throw GradleException(message(outcome, report))
  }

  /** The failure message, keeping the wording the audits have always used. */
  fun message(outcome: AuditOutcome, report: File): String {
    val details =
      outcome.findings.joinToString(separator = "\n") { finding ->
        val pin = finding.pin?.let { " (pinned at ${it.file}:${it.line})" } ?: " (no pin)"
        "  ${finding.id} ${finding.severity} ${finding.packageName}${pin}\n    ${finding.url}"
      }
    val label = labels[outcome.kind] ?: outcome.kind
    return "The $label dependencies have advisories that are not accepted:\n" +
      "$details\n" +
      "The report, with every version and pin, is in ${report.path}."
  }

  /** The job summary: one table row per advisory, plus the accepted ones. */
  fun markdown(outcome: AuditOutcome): String {
    val label = labels[outcome.kind] ?: outcome.kind
    val blocked = outcome.findings.size
    val pluralAdvisories = plural(blocked, "advisory", "advisories")
    val headline =
      if (blocked == 0) {
        "### $label advisory audit: clean"
      } else {
        "### $label advisory audit: $blocked unaccepted $pluralAdvisories"
      }
    val summary =
      "`${outcome.lockFile}`, ${outcome.checkedPackages} " +
        "${plural(outcome.checkedPackages, "package", "packages")} checked.\n"

    val table =
      if (blocked == 0) {
        ""
      } else {
        buildString {
          append("\n| Advisory | Severity | Package | Locked | Fixed in | Pinned at |\n")
          append("| --- | --- | --- | --- | --- | --- |\n")
          outcome.findings.forEach { finding ->
            append(
              "| [${finding.id}](${finding.url}) | ${finding.severity} | ${finding.packageName} " +
                "| ${finding.versions.joinToString()} | ${finding.patched ?: "see advisory"} " +
                "| ${pinCell(finding)} |\n",
            )
          }
        }
      }

    val accepted =
      if (outcome.accepted.isEmpty()) {
        ""
      } else {
        "\nAccepted, with the reason from the build:\n\n" +
          outcome.accepted.joinToString(separator = "\n") { finding ->
            "- ${finding.id} ${finding.severity} ${finding.packageName} " +
              "${finding.versions.joinToString()} — ${outcome.acceptedReasons[finding.id] ?: "no reason given"}"
          } +
          "\n"
      }

    return headline + "\n\n" + summary + table + accepted
  }

  /** The report for tooling and for anyone reading the artifact. */
  fun json(outcome: AuditOutcome): String = JsonOutput.prettyPrint(
    JsonOutput.toJson(
      mapOf(
        "kind" to outcome.kind,
        "lockFile" to outcome.lockFile,
        "checkedPackages" to outcome.checkedPackages,
        "unaccepted" to outcome.findings.map(::findingJson),
        "accepted" to outcome.accepted.map(::findingJson),
      ),
    ),
  )

  /**
   * GitHub Actions workflow commands: one error annotation per advisory, on the
   * pin that has to change, or on the lockfile when no pin holds it.
   */
  fun commands(outcome: AuditOutcome): List<String> = outcome.findings.map { finding ->
    val file = finding.pin?.file ?: outcome.lockFile
    val line = finding.pin?.let { ",line=${it.line}" } ?: ""
    val text =
      "${finding.packageName} ${finding.versions.joinToString()} is ${finding.severity}: " +
        "${finding.id} ${finding.title}"
    "::error file=${escapeProperty(
      file,
    )}$line,title=${escapeProperty(finding.id)}::${escapeData(text)}"
  }

  private fun findingJson(finding: AuditFinding): Map<String, Any?> = mapOf(
    "id" to finding.id,
    "severity" to finding.severity,
    "package" to finding.packageName,
    "versions" to finding.versions,
    "title" to finding.title,
    "url" to finding.url,
    "patched" to finding.patched,
    "pin" to
      finding.pin?.let { mapOf("file" to it.file, "line" to it.line, "version" to it.version) },
  )

  private fun pinCell(finding: AuditFinding): String {
    val pin = finding.pin ?: return "no pin; transitive"
    val version = pin.version?.let { " ($it)" } ?: ""
    return "${pin.file.substringAfterLast('/')}:${pin.line}$version"
  }

  private fun plural(count: Int, one: String, many: String): String = if (count == 1) one else many

  /** Workflow commands escape data with %/CR/LF, and properties also : and ,. */
  fun escapeData(value: String): String =
    value.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")

  fun escapeProperty(value: String): String =
    escapeData(value).replace(":", "%3A").replace(",", "%2C")
}
