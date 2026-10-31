package buildsrc.config

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.io.File

class YarnAuditTest :
  FunSpec({
    test("reads the package name and version of a plain entry") {
      YarnAudit.packagesIn(
        """
      mocha@^11.0.0:
        version "11.7.5"
        resolved "https://registry.yarnpkg.com/mocha/-/mocha-11.7.5.tgz"
        """.trimIndent(),
      ) shouldBe mapOf("mocha" to setOf("11.7.5"))
    }

    test("reads a scoped package") {
      YarnAudit.packagesIn(
        """
      "@types/node@^1.2.3":
        version "1.2.3"
        resolved "https://registry.yarnpkg.com/@types/node/-/node-1.2.3.tgz"
        """.trimIndent(),
      ) shouldBe mapOf("@types/node" to setOf("1.2.3"))
    }

    test("reads several selectors that share one entry") {
      YarnAudit.packagesIn(
        """
      "mocha@^11.0.0", "mocha@^11.7.0":
        version "11.7.5"
        resolved "https://registry.yarnpkg.com/mocha/-/mocha-11.7.5.tgz"
        """.trimIndent(),
      ) shouldBe mapOf("mocha" to setOf("11.7.5"))
    }

    test("reads an aliased package under its real name") {
      YarnAudit.packagesIn(
        """
      string-width-cjs@npm:string-width@^4.2.0:
        version "4.2.3"
        resolved "https://registry.yarnpkg.com/string-width/-/string-width-4.2.3.tgz"
        """.trimIndent(),
      ) shouldBe mapOf("string-width" to setOf("4.2.3"))
    }

    test("reads an alias of a scoped package") {
      YarnAudit.packagesIn(
        """
      "@emotion/css-cjs@npm:@emotion/css@^11.0.0":
        version "11.0.0"
        resolved "https://registry.yarnpkg.com/@emotion/css/-/css-11.0.0.tgz"
        """.trimIndent(),
      ) shouldBe mapOf("@emotion/css" to setOf("11.0.0"))
    }

    test("keeps every version a package resolved to") {
      YarnAudit.packagesIn(
        """
      mocha@^11.0.0:
        version "11.7.5"
        resolved "https://registry.yarnpkg.com/mocha/-/mocha-11.7.5.tgz"

      mocha@^12.0.0:
        version "12.0.1"
        resolved "https://registry.yarnpkg.com/mocha/-/mocha-12.0.1.tgz"
        """.trimIndent(),
      ) shouldBe mapOf("mocha" to setOf("11.7.5", "12.0.1"))
    }

    test("reads a lockfile with windows line endings") {
      YarnAudit.packagesIn(
        "# yarn lockfile v1\r\n" +
          "\r\n" +
          "mocha@^11.0.0:\r\n" +
          "  version \"11.7.5\"\r\n" +
          "  resolved \"https://example.invalid\"\r\n" +
          "\r\n" +
          "\"@types/node@^1.2.3\":\r\n" +
          "  version \"1.2.3\"\r\n",
      ) shouldBe
        mapOf(
          "mocha" to setOf("11.7.5"),
          "@types/node" to setOf("1.2.3"),
        )
    }

    test("fails when the lockfile holds no packages") {
      val error = runCatching { YarnAudit.packagesIn("# a comment\n") }.exceptionOrNull()
      error?.message shouldBe "No packages were parsed from the yarn.lock"
    }

    test("fails when an entry has no version") {
      val error =
        runCatching {
          YarnAudit.packagesIn(
            """
        mocha@^11.0.0:
          version "11.7.5"

        broken@^1.0.0:
          resolved "https://example.invalid"
            """.trimIndent(),
          )
        }.exceptionOrNull()
      error?.message shouldBe "No version in yarn.lock entry: broken@^1.0.0:"
    }

    test("fails when an entry has no header") {
      val error =
        runCatching {
          YarnAudit.packagesIn(
            """
        mocha@^11.0.0:
          version "11.7.5"

        not a header
            """.trimIndent(),
          )
        }.exceptionOrNull()
      error?.message shouldBe "Invalid yarn.lock entry: not a header"
    }

    test("fails on a selector without a package name") {
      val error = runCatching { YarnAudit.packageName("@") }.exceptionOrNull()
      error?.message shouldBe "Invalid yarn.lock selector: @"
    }

    test("reads the packages of the repository lockfile") {
      val lockFile = File("../gradle/kotlin-js-store/yarn.lock")
      val packages = YarnAudit.packagesIn(lockFile.readText())
      // Plain, scoped and aliased packages all have to be in there.
      packages.keys.containsAll(
        setOf("mocha", "@types/node", "string-width", "webpack"),
      ) shouldBe
        true
      packages.getValue("mocha") shouldBe setOf("11.7.6")
    }

    test("reads the package and line of each pin") {
      val pins =
        YarnAudit.pinsIn(
          """
        fun pins() {
          resolution("diff", "8.0.3") // a comment after the call
          resolution("webpack", "5.104.1")
        }
          """.trimIndent(),
          "Kmm.kt",
        )
      pins shouldBe
        mapOf(
          "diff" to AuditPin("Kmm.kt", 2, "8.0.3"),
          "webpack" to AuditPin("Kmm.kt", 3, "5.104.1"),
        )
    }

    test("ignores a commented-out pin") {
      YarnAudit.pinsIn("// resolution(\"diff\", \"8.0.3\")", "Kmm.kt") shouldBe emptyMap()
    }

    test("reads the pins of the repository build") {
      val source = File("../buildSrc/src/main/kotlin/buildsrc/config/Kmm.kt").readText()
      val pins = YarnAudit.pinsIn(source, "Kmm.kt")
      val calls =
        source.lines().count { !it.trimStart().startsWith("//") && it.contains("resolution(") }
      // A pin the parser misses would report an advisory without its owner.
      pins.size shouldBe calls
      pins.keys.contains("follow-redirects") shouldBe true
      (pins.getValue("follow-redirects").line > 0) shouldBe true
    }
  })
