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
 * successful. The verdict of the task comes from the TeamCity service messages that the
 * runner prints, and a runner that fails before it prints any reports none.
 *
 * This checks that the task produced a JUnit XML report. It does not check that the reports
 * cover every test: a filter that matches no test still produces a report, and a runner that
 * fails halfway through produces a partial one.
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
      No JUnit XML report was produced by '$taskPath': the JavaScript test runner reported no
      results. The Kotlin Gradle plugin discards the exit code of the mocha runner, so a runner
      that fails before it reports anything leaves the task successful. Look for a runner error
      in the task output above - mocha prints 'Exception during run:' when it cannot load its
      reporter. Expected JUnit XML reports in $directory.
      """.trimIndent()
    }
  }
}
