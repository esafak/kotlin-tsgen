package buildsrc.config

import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider

/**
 * Fails a Kotlin/JS test task when the test runner produced no results.
 *
 * The Kotlin Gradle plugin discards the exit code of the mocha runner: `KotlinMocha` builds
 * its execution spec with `checkExitCode = false`, so a runner that fails leaves the task
 * successful. Gradle's own `failOnNoDiscoveredTests` does not cover this either, because
 * `KotlinJsTest` replaces the test execution spec and never installs the listener that Gradle
 * uses to implement it.
 *
 * A runner that fails before it reports anything therefore leaves the task green while
 * writing no results at all, and the build silently runs no JavaScript tests. A mocha, Node,
 * or plugin upgrade that breaks the TeamCity reporter is enough to cause it.
 */
class VerifyJsTestResultsAction(
  private val junitXml: Provider<Directory>,
  private val taskPath: String,
  private val hasTests: Boolean,
) : Action<Task> {
  override fun execute(task: Task) {
    if (!hasTests) {
      // A JavaScript target without test sources has no results to write.
      return
    }

    val directory = junitXml.get().asFile
    val results = directory.listFiles { file -> file.isFile && file.name.endsWith(".xml") }

    check(results != null && results.isNotEmpty()) {
      """
      The Kotlin/JS test runner produced no results for '$taskPath': the tests did not run.
      The Kotlin Gradle plugin discards the exit code of the mocha runner, so a runner that
      fails to start leaves the task successful. Look for a runner error in the task output
      above - 'Exception during run:' is what mocha prints when it cannot load the reporter.
      Expected JUnit XML reports in $directory.
      """.trimIndent()
    }
  }
}
