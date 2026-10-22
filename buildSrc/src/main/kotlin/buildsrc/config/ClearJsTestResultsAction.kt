package buildsrc.config

import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider

/**
 * Removes the JUnit XML reports of a previous run of the task.
 *
 * The reports live in the task's output directory, which survives the task. A runner that
 * fails before it writes a report would therefore leave the reports of the last successful
 * run in place, and a check for "did the runner report anything" would pass on them.
 *
 * Removing them before the task runs keeps the check honest: it only sees the reports that
 * the current run produced.
 */
class ClearJsTestResultsAction(private val junitXml: Provider<Directory>) : Action<Task> {
  override fun execute(task: Task) {
    junitXml.get().asFile
      .listFiles { file -> file.isFile && file.name.endsWith(".xml") }
      ?.forEach { report ->
        // A report that survives the removal would satisfy the check even when the runner
        // produced nothing, so say so rather than fail the build on a locked file.
        if (!report.delete()) {
          task.logger.warn(
            "Could not remove the stale test report {}. The check for the test results of " +
              "'{}' cannot tell it apart from the report of the current run.",
            report,
            task.path,
          )
        }
      }
  }
}
