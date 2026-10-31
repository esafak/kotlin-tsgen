package buildsrc.config

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.io.File

class AuditReportTest :
  FunSpec({
    val pin = AuditPin("buildSrc/src/main/kotlin/buildsrc/config/Kmm.kt", 29, "1.16.1")
    val finding =
      AuditFinding(
        id = "GHSA-mrwp-f3q8-3g9p",
        severity = "high",
        packageName = "follow-redirects",
        versions = listOf("1.16.1"),
        title = "Cross-realm sensitiveHeaders arrays are discarded",
        url = "https://github.com/advisories/GHSA-mrwp-f3q8-3g9p",
        pin = pin,
      )
    val outcome =
      AuditOutcome(
        kind = "yarn",
        lockFile = "gradle/kotlin-js-store/yarn.lock",
        checkedPackages = 187,
        findings = listOf(finding),
        accepted = listOf(finding.copy(id = "GHSA-vfj7-8cjw-p6xm")),
        acceptedReasons = mapOf("GHSA-vfj7-8cjw-p6xm" to "braces 3.0.3 is the latest release"),
      )

    test("the summary names the advisory, the locked version and the pin") {
      val summary = AuditReport.markdown(outcome)
      summary shouldContain "1 unaccepted advisory"
      summary shouldContain "GHSA-mrwp-f3q8-3g9p"
      summary shouldContain "follow-redirects"
      summary shouldContain "Kmm.kt:29 (1.16.1)"
      summary shouldContain "187 packages checked"
      summary shouldContain "braces 3.0.3 is the latest release"
    }

    test("the summary of a clean audit has no table") {
      val summary = AuditReport.markdown(
        outcome.copy(findings = emptyList(), accepted = emptyList()),
      )
      summary shouldContain "advisory audit: clean"
      summary shouldNotContain "| Advisory |"
    }

    test("an unpinned advisory points at the lockfile") {
      val summary = AuditReport.markdown(outcome.copy(findings = listOf(finding.copy(pin = null))))
      summary shouldContain "no pin; transitive"
    }

    test("the annotation is an error on the pin line") {
      AuditReport.commands(outcome) shouldBe
        listOf(
          "::error file=buildSrc/src/main/kotlin/buildsrc/config/Kmm.kt,line=29," +
            "title=GHSA-mrwp-f3q8-3g9p::follow-redirects 1.16.1 is high: " +
            "GHSA-mrwp-f3q8-3g9p Cross-realm sensitiveHeaders arrays are discarded",
        )
    }

    test("an unpinned annotation points at the lockfile without a line") {
      AuditReport.commands(outcome.copy(findings = listOf(finding.copy(pin = null))))
        .single() shouldContain
        "::error file=gradle/kotlin-js-store/yarn.lock,title=GHSA-mrwp-f3q8-3g9p::"
    }

    test("a title with a newline cannot break the annotation") {
      val commands = AuditReport.commands(
        outcome.copy(findings = listOf(finding.copy(title = "a\nb"))),
      )
      commands.single() shouldContain "a%0Ab"
      (commands.single().lines().size) shouldBe 1
    }

    test("the report carries the pin, the versions and the accepted list") {
      val json = AuditReport.json(outcome)

      @Suppress("UNCHECKED_CAST")
      val report = groovy.json.JsonSlurper().parseText(json) as Map<String, Any?>
      report["lockFile"] shouldBe "gradle/kotlin-js-store/yarn.lock"
      report["checkedPackages"] shouldBe 187
      @Suppress("UNCHECKED_CAST")
      val unaccepted = report["unaccepted"] as List<Map<String, Any?>>
      val reported = unaccepted.single()
      reported["id"] shouldBe "GHSA-mrwp-f3q8-3g9p"
      reported["versions"] shouldBe listOf("1.16.1")
      reported["pin"] shouldBe
        mapOf(
          "file" to "buildSrc/src/main/kotlin/buildsrc/config/Kmm.kt",
          "line" to 29,
          "version" to "1.16.1",
        )
      @Suppress("UNCHECKED_CAST")
      val accepted = report["accepted"] as List<Map<String, Any?>>
      accepted.single()["id"] shouldBe "GHSA-vfj7-8cjw-p6xm"
      // The JSON has to carry the reason, or the artifact cannot explain an
      // acceptance without the build log next to it.
      accepted.single()["reason"] shouldBe "braces 3.0.3 is the latest release"
    }

    test("the summary and the report carry the advisories the tool skips") {
      val skipped =
        outcome.copy(
          findings = emptyList(),
          accepted = emptyList(),
          ignored = listOf(AuditAcceptance("GHSA-vfj7-8cjw-p6xm", "braces 3.0.3 is the latest")),
        )
      val summary = AuditReport.markdown(skipped)
      summary shouldContain "Skipped by `audit.ignore`"
      summary shouldContain "GHSA-vfj7-8cjw-p6xm — braces 3.0.3 is the latest"

      @Suppress("UNCHECKED_CAST")
      val report = groovy.json.JsonSlurper().parseText(
        AuditReport.json(skipped),
      ) as Map<String, Any?>

      @Suppress("UNCHECKED_CAST")
      val ignored = report["ignored"] as List<Map<String, Any?>>
      ignored.single() shouldBe
        mapOf("id" to "GHSA-vfj7-8cjw-p6xm", "reason" to "braces 3.0.3 is the latest")
    }

    test("the message says which pin holds the advisory") {
      val message = AuditReport.message(outcome, File("build/reports/yarn-audit.json"))
      message shouldContain "The Kotlin/JS dependencies have advisories that are not accepted:"
      message shouldContain "GHSA-mrwp-f3q8-3g9p high follow-redirects"
      message shouldContain "pinned at buildSrc/src/main/kotlin/buildsrc/config/Kmm.kt:29"
      message shouldContain "build/reports/yarn-audit.json"
    }

    test("workflow command properties escape colons and commas") {
      AuditReport.escapeProperty("a:b,c%d\n") shouldBe "a%3Ab%2Cc%25d%0A"
      AuditReport.escapeData("a:b,c%d\n") shouldBe "a:b,c%25d%0A"
    }

    test("the repository pins parse into annotations the diff can show") {
      val source = File("../buildSrc/src/main/kotlin/buildsrc/config/Kmm.kt").readText()
      val pins = YarnAudit.pinsIn(source, "buildSrc/src/main/kotlin/buildsrc/config/Kmm.kt")
      val pinned =
        AuditOutcome(
          kind = "yarn",
          lockFile = "gradle/kotlin-js-store/yarn.lock",
          checkedPackages = 1,
          findings =
          listOf(
            finding.copy(
              packageName = "follow-redirects",
              pin = pins.getValue("follow-redirects"),
            ),
          ),
        )
      AuditReport.commands(pinned).single() shouldContain
        "file=buildSrc/src/main/kotlin/buildsrc/config/Kmm.kt,line="
      pins.getValue("follow-redirects").version?.isNotBlank() shouldBe true
    }
  })
