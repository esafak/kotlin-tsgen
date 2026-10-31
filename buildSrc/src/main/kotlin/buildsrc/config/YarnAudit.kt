package buildsrc.config

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.Project
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * The advisories this repository accepts, and why.
 *
 * The documentation site keeps its list in site/pnpm-workspace.yaml, under
 * `audit.ignore`. Yarn v1 has no equivalent, so the list lives here.
 */
private val acceptedAdvisories = mapOf(
  // GHSA-vfj7-8cjw-p6xm: braces 3.0.3 is the latest release, so there is no
  // patched version to move to. It is a stack-exhaustion denial of service that
  // needs deeply nested glob patterns, which the build does not take from user
  // input.
  "GHSA-vfj7-8cjw-p6xm" to "braces 3.0.3 is the latest release",
  // GHSA-3g3m-2fjr-6pw4: glob 10.5.0 arrives through mocha 11, and every
  // patched release (14.0.2 and later) removed the `glob.Glob` class and the
  // callback API that Karma 6.4.4 and rimraf 3 use. Yarn v1 resolutions are
  // global and path-scoped keys are not honoured by the Kotlin plugin's yarn,
  // so pinning glob would take the browser test runner with it.
  "GHSA-3g3m-2fjr-6pw4" to "patched glob removes the API Karma and rimraf require",
  // GHSA-g3hv-75wg-m249: every patched release (11.0.1 and later) drops the
  // callable CommonJS export that Karma 6.4.4 requires. Karma's minimatch
  // 3.1.5 is reached only through the configuration globs this repository
  // writes. Track the Karma release that moves off minimatch 3.x.
  "GHSA-g3hv-75wg-m249" to "patched minimatch drops the CommonJS export Karma requires",
)

/** Where the Kotlin/JS dependency pins live, repo-relative. */
private const val PIN_FILE = "buildSrc/src/main/kotlin/buildsrc/config/Kmm.kt"

/** The Kotlin/JS lockfile, repo-relative. */
private const val LOCK_FILE = "gradle/kotlin-js-store/yarn.lock"

/**
 * Fails the build when the Kotlin/JS dependency tree has an advisory that the
 * repository has not accepted.
 *
 * Yarn v1's own audit cannot read this tree: the lockfile is relocated to
 * gradle/kotlin-js-store and the dependencies live in yarn workspaces, so the
 * audit resolves almost nothing. The registry's bulk advisory endpoint is the
 * same source pnpm's audit uses, so the tree is checked against it here.
 */
fun Project.yarnAudit() {
  val lockFile = rootProject.layout.projectDirectory.file(LOCK_FILE)
  val pinSource = rootProject.layout.projectDirectory.file(PIN_FILE)
  val report = rootProject.layout.buildDirectory.file("reports/yarn-audit.json").get().asFile
  val task = tasks.register("yarnAudit") {
    group = "verification"
    description = "fails when the Kotlin/JS dependencies have an unaccepted advisory"

    inputs.file(lockFile)
    inputs.file(pinSource)
    // The advisory database changes without the lockfile changing.
    outputs.upToDateWhen { false }

    // The lockfile is the output of the Kotlin plugin's store task, so the
    // audit has to read it after that task wrote it.
    mustRunAfter(tasks.matching { it.name == "kotlinStoreYarnLock" })

    doLast {
      val packages = YarnAudit.packagesIn(lockFile.asFile.readText())
      val advisories = YarnAudit.advisoriesIn(packages)
      val pins = YarnAudit.pinsIn(pinSource.asFile.readText(), PIN_FILE)
      fun finding(advisory: YarnAudit.Advisory) = AuditFinding(
        id = advisory.id,
        severity = advisory.severity,
        packageName = advisory.packageName,
        versions = packages[advisory.packageName].orEmpty().sorted(),
        title = advisory.title,
        url = advisory.url,
        pin = pins[advisory.packageName],
      )

      val outcome =
        AuditOutcome(
          kind = "yarn",
          lockFile = LOCK_FILE,
          checkedPackages = packages.size,
          findings = advisories.filter { it.id !in acceptedAdvisories }.map(::finding),
          accepted = advisories.filter { it.id in acceptedAdvisories }.map(::finding),
          acceptedReasons = acceptedAdvisories,
        )
      AuditReport.publish(logger, report, outcome)
      logger.lifecycle(
        "The Kotlin/JS dependencies have no unaccepted advisories " +
          "(${packages.size} packages checked).",
      )
    }
  }
  tasks.named("check") { dependsOn(task) }
}

/**
 * The advisories for the packages in a yarn.lock, from the registry's bulk
 * advisory endpoint.
 */
object YarnAudit {
  data class Advisory(
    val id: String,
    val packageName: String,
    val severity: String,
    val title: String,
    val url: String,
  )

  fun advisoriesIn(packages: Map<String, Set<String>>): List<Advisory> {
    val body = JsonOutput.toJson(packages.mapValues { it.value.sorted() })
    val request = HttpRequest
      .newBuilder()
      .uri(URI("https://registry.npmjs.org/-/npm/v1/security/advisories/bulk"))
      .header("Content-Type", "application/json")
      .header("Accept", "application/json")
      .timeout(Duration.ofSeconds(60))
      .POST(HttpRequest.BodyPublishers.ofString(body))
      .build()
    val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build()
    val response = client.send(request, HttpResponse.BodyHandlers.ofString())
    check(response.statusCode() == 200) {
      "The advisory endpoint answered ${response.statusCode()}"
    }

    val json = JsonSlurper().parseText(response.body()) as Map<*, *>
    return json.flatMap { (name, advisories) ->
      @Suppress("UNCHECKED_CAST")
      (advisories as List<Map<String, Any?>>).map {
        Advisory(
          id = it["url"].toString().substringAfterLast('/'),
          packageName = name.toString(),
          severity = it["severity"].toString(),
          title = it["title"].toString(),
          url = it["url"].toString(),
        )
      }
    }
  }

  /**
   * The packages in a yarn.lock, with the versions they resolved to.
   */
  fun packagesIn(lockFile: String): Map<String, Set<String>> {
    val versions = mutableMapOf<String, MutableSet<String>>()
    val normalized = lockFile.replace("\r\n", "\n").replace('\r', '\n')
    for (block in normalized.split("\n\n")) {
      val lines = block.lines().dropWhile { it.isBlank() || it.trimStart().startsWith("#") }
      if (lines.isEmpty()) continue

      // Every entry has to be readable. An entry that is skipped would shrink
      // the tree, and the audit would report the smaller tree as clean.
      val header = lines.first()
      check(header.endsWith(":")) { "Invalid yarn.lock entry: $header" }
      val version = VERSION.find(lines.joinToString("\n"))?.groupValues?.get(1)
        ?: error("No version in yarn.lock entry: $header")
      for (key in header.dropLast(1).split(", ")) {
        val name = packageName(key)
        versions.getOrPut(name) { mutableSetOf() } += version
      }
    }
    check(versions.isNotEmpty()) { "No packages were parsed from the yarn.lock" }
    return versions
  }

  /**
   * The number of packages in a yarn.lock.
   */
  fun packageCount(lockFile: String): Int = packagesIn(lockFile).size

  /**
   * The package a yarn.lock selector refers to.
   *
   * A selector is "<name>@<range>", where a scoped name starts with an "@" of
   * its own, and an alias is "<alias>@npm:<name>@<range>". An alias label is
   * not a package of its own, so the aliased name is used.
   */
  fun packageName(key: String): String {
    val raw = key.trim().removeSurrounding("\"")
    val separator = raw.indexOf('@', if (raw.startsWith("@")) 1 else 0)
    require(separator > 0) { "Invalid yarn.lock selector: $key" }

    val range = raw.substring(separator + 1)
    val target = if (range.startsWith("npm:")) range.removePrefix("npm:") else raw
    val end = target.indexOf('@', if (target.startsWith("@")) 1 else 0)
    val name = if (end < 0) target else target.substring(0, end)
    require(name.isNotEmpty()) { "Invalid yarn.lock selector: $key" }
    return name
  }

  /**
   * The pins in a Kotlin source file, by package, with the line that declares
   * each one.
   *
   * Yarn v1 resolutions are global, so a pin is also the only place that can
   * hold a transitive package at a version. A commented-out call is not a pin.
   */
  fun pinsIn(source: String, file: String): Map<String, AuditPin> =
    source.lines().mapIndexedNotNull { index, line ->
      if (line.trimStart().startsWith("//")) return@mapIndexedNotNull null
      val match = RESOLUTION.find(line) ?: return@mapIndexedNotNull null
      match.groupValues[1] to AuditPin(file, index + 1, match.groupValues[2])
    }.toMap()

  private val RESOLUTION = Regex("""resolution\("([^"]+)",\s*"([^"]+)"\)""")

  private val VERSION = Regex("""^\s+version "([^"]+)"""", RegexOption.MULTILINE)
}
