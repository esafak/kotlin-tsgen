package buildsrc.config

import org.gradle.api.Project
import org.gradle.kotlin.dsl.findByType
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension


/**
 * `kotlin-js` adds a directory in the root-dir for the Yarn lock.
 * That's a bit annoying. It's a little neater if it's in the
 * gradle dir, next to the version-catalog.
 */
fun Project.relocateKotlinJsStore() {
  afterEvaluate {
    rootProject.extensions.findByType<YarnRootExtension>()?.apply {
      lockFileDirectory = project.rootDir.resolve("gradle/kotlin-js-store")

      // Keep transitive dependencies in the Kotlin/JS Yarn lockfile past
      // versions with open Dependabot advisories. Before adding a resolution,
      // try unpinning stale resolutions; remove each one when the upstream
      // dependency graph requires a patched version naturally.
      resolution("diff", "8.0.3") // GHSA-73rr-hh4g-fpgx / CVE-2026-24001
      resolution("qs", "6.16.0") // GHSA-4mjr-xmp4-gh2g, GHSA-x5fp-wj9c-mxmx
      resolution("serialize-javascript", "7.0.5") // GHSA-qj8w-gfj5-8c6v, GHSA-5c6j-r48x-rmvq
      resolution("webpack", "5.104.1") // GHSA-8fgc-7cc6-rx7x, GHSA-38r7-794h-5758
    }
  }
}
