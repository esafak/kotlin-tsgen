package buildsrc.config

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.File

class PnpmAuditTest :
  FunSpec({
    // The shape `pnpm audit --json` printed for follow-redirects 1.16.1.
    val audit =
      """
      {
        "advisories": {
          "1244003": {
            "findings": [
              {
                "version": "1.16.1",
                "paths": [".>webpack-dev-server>http-proxy>follow-redirects"]
              }
            ],
            "id": 1244003,
            "title": "follow-redirects: Cross-realm sensitiveHeaders arrays are discarded",
            "module_name": "follow-redirects",
            "vulnerable_versions": ">=1.16.0 <=1.16.1",
            "patched_versions": ">=1.16.2",
            "severity": "high",
            "github_advisory_id": "GHSA-mrwp-f3q8-3g9p",
            "url": "https://github.com/advisories/GHSA-mrwp-f3q8-3g9p"
          }
        },
        "metadata": { "totalDependencies": 1218 }
      }
      """.trimIndent()

    test("reads an advisory with its locked version and the patched range") {
      val result = PnpmAudit.parse(audit)
      result.checkedPackages shouldBe 1218
      val finding = result.findings.single()
      finding.id shouldBe "GHSA-mrwp-f3q8-3g9p"
      finding.packageName shouldBe "follow-redirects"
      finding.versions shouldBe listOf("1.16.1")
      finding.patched shouldBe ">=1.16.2"
      finding.severity shouldBe "high"
    }

    test("keeps only the severities the audit fails on") {
      val moderate = audit.replace("\"severity\": \"high\"", "\"severity\": \"moderate\"")
      PnpmAudit.parse(moderate).findings shouldBe emptyList()
    }

    test("ignores the text mise prints around the JSON") {
      PnpmAudit.parse("mise warning: something\n$audit\n").findings.size shouldBe 1
    }

    test("fails on output that carries no audit JSON") {
      val error = runCatching { PnpmAudit.parse("ERR_PNPM_AUDIT no lockfile") }.exceptionOrNull()
      (error?.message ?: "") shouldContain "No audit JSON in the pnpm output"
    }

    test("fails on a payload without advisories instead of passing") {
      val error =
        runCatching { PnpmAudit.parse("""{"metadata":{"totalDependencies":1}}""") }
          .exceptionOrNull()
      (error?.message ?: "") shouldContain "No advisories in the pnpm audit output"
    }

    test("fails on an advisory entry that is not an object") {
      val error =
        runCatching { PnpmAudit.parse("""{"advisories":{"1244003":null}}""") }
          .exceptionOrNull()
      (error?.message ?: "") shouldContain "advisory 1244003"
    }

    test("fails on an advisory without a severity") {
      val payload = """{"advisories":{"1":{"module_name":"follow-redirects"}}}"""
      val error = runCatching { PnpmAudit.parse(payload) }.exceptionOrNull()
      (error?.message ?: "") shouldContain "without a severity"
    }

    test("fails on an advisory with an unknown severity") {
      val payload =
        """{"advisories":{"1":{"severity":"urgent","module_name":"x","id":1}}}"""
      val error = runCatching { PnpmAudit.parse(payload) }.exceptionOrNull()
      (error?.message ?: "") shouldContain "unknown severity"
    }

    test("fails on an advisory without a package name") {
      val payload = """{"advisories":{"1":{"severity":"high","id":1}}}"""
      val error = runCatching { PnpmAudit.parse(payload) }.exceptionOrNull()
      (error?.message ?: "") shouldContain "without a package name"
    }

    test("fails on an advisory without an identifier") {
      val payload =
        """{"advisories":{"1":{"severity":"high","module_name":"x"}}}"""
      val error = runCatching { PnpmAudit.parse(payload) }.exceptionOrNull()
      (error?.message ?: "") shouldContain "without an identifier"
    }

    test("fails when the audit exited non-zero without a blocking advisory") {
      val error = runCatching { PnpmAudit.requireReported(1, emptyList(), "ERR_PNPM no registry") }
        .exceptionOrNull()
      (error?.message ?: "") shouldContain "without reporting a blocking advisory"
      (error?.message ?: "") shouldContain "ERR_PNPM no registry"
    }

    test("allows a clean exit and a reported advisory") {
      PnpmAudit.requireReported(0, emptyList())
      PnpmAudit.requireReported(1, listOf(AuditFinding("GHSA-1", "high", "x", listOf("1"), "", "")))
    }

    test("reads the ignored advisories with the reason above each one") {
      val ignored =
        PnpmAudit.ignoredIn(
          """
          audit:
            ignore:
              # GHSA-aaaa-bbbb-cccc: the patched release needs node 26.
              # Remove when the site moves to node 26.
              - GHSA-aaaa-bbbb-cccc
              # GHSA-dddd-eeee-ffff: no patched release exists.
              - 'GHSA-dddd-eeee-ffff'
            reporter:
              ignored-looking-list:
                - GHSA-1111-2222-3333

          overrides:
            # GHSA-1111-2222-3333 is an override, not an ignore.
            'glob@<14': 14.0.3
            ignored-looking-list:
              - GHSA-1111-2222-3333
          """.trimIndent(),
        )
      ignored shouldBe
        listOf(
          AuditAcceptance(
            "GHSA-aaaa-bbbb-cccc",
            "the patched release needs node 26. Remove when the site moves to node 26.",
          ),
          AuditAcceptance("GHSA-dddd-eeee-ffff", "no patched release exists."),
        )
    }

    test("reads an indentless ignore sequence and stops at its sibling section") {
      PnpmAudit.ignoredIn(
        """
        audit:
          ignore:
          - GHSA-eeee-ffff-aaaa
          reporter:
            ignored-looking-list:
              - GHSA-1111-2222-3333
        """.trimIndent(),
      ) shouldBe listOf(AuditAcceptance("GHSA-eeee-ffff-aaaa", null))
    }

    test("keeps an ignored advisory even when its reason is missing") {
      PnpmAudit.ignoredIn(
        """
        audit:
          ignore:
            - GHSA-aaaa-bbbb-cccc
        """.trimIndent(),
      ) shouldBe listOf(AuditAcceptance("GHSA-aaaa-bbbb-cccc", null))
    }

    test("reads quoted and unquoted override keys") {
      val pins =
        PnpmAudit.pinsIn(
          """
          audit:
            ignore:
              - GHSA-vfj7-8cjw-p6xm

          overrides:
            # a comment
            'follow-redirects@<1.16.1': '>=1.16.1'
            tinypool@<2.1.2: 2.2.0
            '@docusaurus/theme@<1.0.0': 2.0.0
          """.trimIndent(),
          "pnpm-workspace.yaml",
        )
      pins["follow-redirects"] shouldBe AuditPin("pnpm-workspace.yaml", 7, ">=1.16.1")
      pins["tinypool"] shouldBe AuditPin("pnpm-workspace.yaml", 8, "2.2.0")
      pins["@docusaurus/theme"] shouldBe AuditPin("pnpm-workspace.yaml", 9, "2.0.0")
      pins.size shouldBe 3
    }

    test("reads the overrides of the repository workspace file") {
      val pins = PnpmAudit.pinsIn(File("../site/pnpm-workspace.yaml").readText(), "workspace")
      pins.keys.contains("follow-redirects") shouldBe true
      (pins.getValue("follow-redirects").line > 0) shouldBe true
    }
  })
