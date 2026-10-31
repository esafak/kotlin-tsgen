package buildsrc.config

import groovy.json.JsonSlurper

/**
 * Reads `pnpm audit --json` and the site's overrides, so the site's audit
 * reports the same way the Kotlin/JS one does.
 *
 * `pnpm audit` already applies `audit.ignore` from pnpm-workspace.yaml, so
 * whatever it prints is unaccepted by definition.
 */
object PnpmAudit {
  /** The severities `--audit-level=high` fails on. */
  private val blocking = setOf("high", "critical")

  /** The parsed audit: the advisories, and how many packages were checked. */
  data class Result(val checkedPackages: Int, val findings: List<AuditFinding>)

  /**
   * Parses an audit. A payload without an `advisories` object is an error, not
   * a clean result: a changed output shape must not silently pass the audit.
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
      advisories.values.mapNotNull { entry ->
        val advisory = entry as? Map<*, *> ?: return@mapNotNull null
        val severity = advisory["severity"].toString()
        if (severity !in blocking) return@mapNotNull null
        AuditFinding(
          id = advisory["github_advisory_id"]?.toString() ?: advisory["id"].toString(),
          severity = severity,
          packageName = advisory["module_name"].toString(),
          versions = versionsIn(advisory),
          title = advisory["title"].toString(),
          url = advisory["url"].toString(),
          patched = (advisory["patched_versions"] as? String)?.takeIf { it.isNotBlank() },
        )
      }
    return Result(checked, findings)
  }

  private fun versionsIn(advisory: Map<*, *>): List<String> =
    (advisory["findings"] as? List<*>).orEmpty()
      .mapNotNull { (it as? Map<*, *>)?.get("version")?.toString() }
      .distinct()
      .sorted()

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
