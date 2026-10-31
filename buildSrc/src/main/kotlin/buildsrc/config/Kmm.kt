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
      // No patched 7.x release exists for this advisory; 8.0.3 is required.
      resolution("diff", "8.0.3") // GHSA-73rr-hh4g-fpgx / CVE-2026-24001
      // Keep this aligned with the documentation site's patched pin.
      // GHSA-qj8w-gfj5-8c6v, GHSA-5c6j-r48x-rmvq, GHSA-m5mm-p8h7-m29v
      resolution("serialize-javascript", "7.1.4")
      // GHSA-27r3-jxmh-p828: global pin; only consumer is karma's ^1.19.0
      resolution("body-parser", "1.20.9")
      resolution("webpack", "5.104.1") // GHSA-8fgc-7cc6-rx7x, GHSA-38r7-794h-5758
      // GHSA-8r9p-f939-6h3c, GHSA-q3jh-wrc6-jgq8, GHSA-mrwp-f3q8-3g9p: the last
      // one covers 1.16.0 and 1.16.1, the two releases the earlier pins needed.
      resolution("follow-redirects", "1.16.2")
      // GHSA-3g3m-2fjr-6pw4 and GHSA-g3hv-75wg-m249 are not pinned here: the
      // only patched releases (glob 14.0.2, minimatch 11.0.1) removed the
      // `glob.Glob` class and the callable CommonJS export that Karma 6.4.4,
      // the browser test runner, needs. Yarn v1 resolutions are global, so a
      // pin would take Karma with it. See acceptedAdvisories in YarnAudit.kt.
    }
  }
}
