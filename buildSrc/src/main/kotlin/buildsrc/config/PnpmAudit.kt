package buildsrc.config

import groovy.json.JsonSlurper

/**
 * Reads `pnpm audit --json` and the site's overrides, so the site's audit
 * reports the same way the Kotlin/JS one does.
 *
 * `pnpm audit` already applies `audit.ignore` from pnpm-workspace.yaml, so
 * whatever it prints is unaccepted by definition. The ignore list is read
 * separately, because an acceptance that never appears in the report is an
 * acceptance nobody reviews.
 */
object PnpmAudit {
  /** Every severity the advisory database uses; anything else is a shape change. */
  private val severities = setOf("info", "low", "moderate", "high", "critical")

  /** The severities `--audit-level=high` fails on. */
  private val blocking = setOf("high", "critical")

  /** The parsed audit: the advisories, and how many packages were checked. */
  data class Result(val checkedPackages: Int, val findings: List<AuditFinding>)

  /**
   * Parses an audit. A payload without an `advisories` object is an error, not
   * a clean result: a changed output shape must not silently pass the audit.
   *
   * Every entry is validated for the same reason. An entry that failed to
   * parse would shrink the report, and a smaller report reads as a cleaner
   * one.
   */
  fun parse(output: String): Result {
    val start = output.indexOf('{')
    val end = output.lastIndexOf('}')
    check(start in 0 until end) { "No audit JSON in the pnpm output:\n$output" }

    val json = JsonSlurper().parseText(output.substring(start, end + 1))
    val root = json as? Map<*, *> ?: error("The pnpm audit output is not an object: $output")
    val advisories =
      root["advisories"] as? Map<*, *> ?: error("No advisories in the pnpm audit output")

    val metadata = root["metadata"] as? Map<*, *>
    val checked = (metadata?.get("totalDependencies") as? Number)?.toInt() ?: 0

    val findings =
      advisories.entries.map { (key, entry) -> finding(key.toString(), entry) }
        .filter { it.severity in blocking }
    return Result(checked, findings)
  }

  private fun finding(key: String, entry: Any?): AuditFinding {
    val advisory =
      entry as? Map<*, *>
        ?: error("The pnpm audit reported advisory $key as ${entry?.javaClass?.simpleName}")

    val severity =
      advisory["severity"]?.toString()?.lowercase()
        ?: error("The pnpm audit reported advisory $key without a severity")
    check(severity in severities) {
      "The pnpm audit reported advisory $key with an unknown severity: $advisory"
    }

    val name =
      advisory["module_name"]?.toString()?.takeIf { it.isNotBlank() }
        ?: error("The pnpm audit reported advisory $key without a package name")
    val id =
      (advisory["github_advisory_id"] as? String)?.takeIf { it.isNotBlank() }
        ?: advisory["id"]?.toString()?.takeIf { it.isNotBlank() }
        ?: error("The pnpm audit reported advisory $key without an identifier")

    return AuditFinding(
      id = id,
      severity = severity,
      packageName = name,
      versions = versionsIn(advisory),
      title = advisory["title"]?.toString().orEmpty(),
      url = advisory["url"]?.toString().orEmpty(),
      patched = (advisory["patched_versions"] as? String)?.takeIf { it.isNotBlank() },
    )
  }

  private fun versionsIn(advisory: Map<*, *>): List<String> =
    (advisory["findings"] as? List<*>).orEmpty()
      .mapNotNull { (it as? Map<*, *>)?.get("version")?.toString() }
      .distinct()
      .sorted()

  /**
   * Fails when the audit exited non-zero without reporting a blocking advisory.
   *
   * `pnpm audit` exits non-zero both when it finds something and when it fails
   * to run at all, and the second case prints no advisories. Read as a clean
   * result, that failure would pass the build.
   */
  fun requireReported(exitValue: Int, findings: List<AuditFinding>, error: String = "") {
    val detail = error.trim().takeIf { it.isNotEmpty() }?.let { "\n$it" }.orEmpty()
    check(exitValue == 0 || findings.isNotEmpty()) {
      "pnpm audit exited with $exitValue without reporting a blocking advisory$detail"
    }
  }

  /**
   * The advisory identifiers `audit.ignore` tells pnpm to skip, with the
   * comment block above each one as its reason.
   *
   * The reason is the only record of why an advisory is accepted, so it is
   * carried into the report instead of staying in a file nobody opens.
   */
  fun ignoredIn(source: String): List<AuditAcceptance> {
    val ignored = mutableListOf<AuditAcceptance>()
    var section: String? = null
    var ignoreIndent: Int? = null
    val comment = mutableListOf<String>()

    for (line in source.lines()) {
      val trimmed = line.trim()
      val indent = line.length - line.trimStart().length
      when {
        trimmed.isEmpty() -> comment.clear()

        trimmed.startsWith("#") -> comment += trimmed.removePrefix("#").trim()

        indent == 0 -> {
          section = if (trimmed.startsWith("audit:")) "audit" else null
          ignoreIndent = null
          comment.clear()
        }

        section == "audit" && trimmed.startsWith("ignore:") -> {
          section = "audit.ignore"
          ignoreIndent = indent
          comment.clear()
        }

        section == "audit.ignore" && !trimmed.startsWith("- ") &&
          ignoreIndent?.let { indent <= it } == true -> {
          section = "audit"
          ignoreIndent = null
          comment.clear()
        }

        section == "audit.ignore" && trimmed.startsWith("- ") -> {
          val id = trimmed.removePrefix("- ").trim().trim('\'', '"')
          val reason =
            comment.joinToString(" ").takeIf { it.isNotBlank() }
              ?.removePrefix("$id:")?.trim()?.takeIf { it.isNotBlank() }
          ignored += AuditAcceptance(id, reason)
          comment.clear()
        }

        else -> comment.clear()
      }
    }
    return ignored
  }

  /**
   * Every override in a pnpm workspace file, by package, with the line that
   * declares it. A package without an override resolves without a pin.
   */
  fun pinsIn(source: String, file: String): Map<String, AuditPin> {
    val pins = mutableMapOf<String, AuditPin>()
    var inOverrides = false
    source.lines().forEachIndexed { index, line ->
      val trimmed = line.trim()
      if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEachIndexed
      if (!line.startsWith(" ")) {
        inOverrides = trimmed.startsWith("overrides:")
        return@forEachIndexed
      }
      if (!inOverrides || !trimmed.contains(":")) return@forEachIndexed
      val key = trimmed.substringBeforeLast(":").trim().trim('\'', '"')
      val version = trimmed.substringAfterLast(":").trim().trim('\'', '"')
      pins[nameOf(key)] = AuditPin(file, index + 1, version)
    }
    return pins
  }

  /**
   * The override that holds a package, with the line that declares it, or null
   * when the package resolves without one.
   */
  fun overridePin(source: String, packageName: String, file: String): AuditPin? =
    pinsIn(source, file)[packageName]

  /** An override key is `name@range`, and a scoped name has an `@` of its own. */
  private fun nameOf(key: String): String =
    if (key.startsWith("@")) key.substringBeforeLast("@") else key.substringBefore("@")
}
