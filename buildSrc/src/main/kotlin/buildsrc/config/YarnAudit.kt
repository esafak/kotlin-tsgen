package buildsrc.config

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.GradleException
import org.gradle.api.Project
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

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
  "GHSA-vfj7-8cjw-p6xm" to
    "braces 3.0.3 is the latest release",
)

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
  val lockFile = rootProject.layout.projectDirectory.file("gradle/kotlin-js-store/yarn.lock")
  val task = tasks.register("yarnAudit") {
    group = "verification"
    description = "fails when the Kotlin/JS dependencies have an unaccepted advisory"

    inputs.file(lockFile)
    // The advisory database changes without the lockfile changing.
    outputs.upToDateWhen { false }

    // The lockfile is the output of the Kotlin plugin's store task, so the
    // audit has to read it after that task wrote it.
    mustRunAfter(tasks.matching { it.name == "kotlinStoreYarnLock" })

    doLast {
      val advisories = YarnAudit.advisoriesIn(lockFile.asFile.readText())
      val unaccepted = advisories.filter { it.id !in acceptedAdvisories }
      if (unaccepted.isNotEmpty()) {
        val details =
          unaccepted.joinToString(separator = "\n") {
            "  ${it.id} ${it.severity} ${it.packageName} ${it.title}\n    ${it.url}"
          }
        throw GradleException(
          "The Kotlin/JS dependencies have advisories that are not accepted:\n$details",
        )
      }
      logger.lifecycle("The Kotlin/JS dependencies have no unaccepted advisories.")
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

  fun advisoriesIn(lockFile: String): List<Advisory> {
    val versions = mutableMapOf<String, MutableSet<String>>()
    for (block in lockFile.split("\n\n")) {
      val lines = block.lines()
      if (lines.isEmpty() || !lines[0].endsWith(":")) continue
      val version = VERSION.find(block)?.groupValues?.get(1) ?: continue
      for (key in lines[0].dropLast(1).split(", ")) {
        val name = key.trim('"').substringBefore('@')
        if (name.isNotEmpty() && '@' !in name) {
          versions.getOrPut(name) { mutableSetOf() } += version
        }
      }
    }
    if (versions.isEmpty()) return emptyList()

    val body = JsonOutput.toJson(versions.mapValues { it.value.sorted() })
    val request = HttpRequest
      .newBuilder()
      .uri(URI("https://registry.npmjs.org/-/npm/v1/security/advisories/bulk"))
      .header("Content-Type", "application/json")
      .header("Accept", "application/json")
      .POST(HttpRequest.BodyPublishers.ofString(body))
      .build()
    val response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString())
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

  private val VERSION = Regex("""^\s+version "([^"]+)"""", RegexOption.MULTILINE)
}
