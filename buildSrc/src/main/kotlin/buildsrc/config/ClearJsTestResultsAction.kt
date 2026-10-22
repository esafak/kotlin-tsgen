package buildsrc.config

import org.gradle.api.Action
import org.gradle.api.GradleException
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
 *
 * A report that cannot be removed - a locked file, say - would satisfy the check even when
 * the runner produced nothing, so the task fails instead. The check cannot tell such a report
 * apart from the report of the current run.
 */
class ClearJsTestResultsAction(private val junitXml: Provider<Directory>) : Action<Task> {
  override fun execute(task: Task) {
    val stale = junitXml.get().asFile
      .listFiles { file -> file.isFile && file.name.endsWith(".xml") }
      ?.filter { report -> !report.delete() }
      .orEmpty()

    if (stale.isNotEmpty()) {
      throw GradleException(
        "The stale test reports ${stale.joinToString()} could not be removed, so the check " +
          "for the test results of '${task.path}' cannot tell them apart from the reports of " +
          "the current run. Remove them by hand and run the task again.",
      )
    }
  }
}
