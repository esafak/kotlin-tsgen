package buildsrc.config

import com.github.gradle.node.npm.task.NpmTask
import com.github.gradle.node.pnpm.task.PnpmTask
import org.jetbrains.kotlin.util.parseSpaceSeparatedArgs

fun NpmTask.args(values: String) {
  args.set(parseSpaceSeparatedArgs(values))
}

fun PnpmTask.args(values: String) {
  args.set(parseSpaceSeparatedArgs(values))
}
