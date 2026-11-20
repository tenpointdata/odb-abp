# Engineering guide — odb-abp

Upstream fork of OrientDB; identical to upstream develop at the review snapshot.

## Fork and upstream ownership

- Reviewed source: `a13262f53bb373e48f0057a15f408432804b7dcb` on `develop`.
- Upstream: [orientechnologies/orientdb](https://github.com/orientechnologies/orientdb).
- At the review snapshot, the fork had 0 fork-only and 0 upstream-only commits relative to `a13262f53bb373e48f0057a15f408432804b7dcb`. [Read the pinned comparison context](https://github.com/tenpointdata/odb-abp/compare/develop...orientechnologies:develop). These counts are historical evidence, not a claim about today's latest release.
- Preserve upstream license/contribution links, but add fork role, exact imported SHA, nonpublishing validation path, and accepted patch/upgrade policy. Distinguish Java 17 build requirement from stale contributor text.

## Setup and native verification

pom.xml targets Java 17; CI validates JDK 17/21/24/25; CONTRIBUTING still says JDK 8 and needs reconciliation. Studio requires yarn/webpack; existing scripts do not provide a standalone typecheck/test command.

| Command | Owning source | Review execution |
| --- | --- | --- |
| mvn -B package --file pom.xml | .github/workflows/tests.yml | Not run |
| mvn -B package -P all --file pom.xml | .github/workflows/tests.yml | Not run |
| yarn build (from studio) | studio/package.json | Not run |

Native toolchains and inherited workflow enablement remain unchanged. Source-rule inspection in this review was local only; the PR does not distribute additional inspection code or introduce validation dependencies. Full inventory debt remains open. Native type/contract tests, targeted source-size review and the linked remediation issues remain the validation path; missing or incomplete checks are not a pass.

## Architecture and trust boundaries

Java multi-module database engine (core/client/server/security/distributed) with legacy Angular/TypeScript/JavaScript Studio.

Reviewed inherited CI credentials/publication conditions and security-scanning workflow definitions. No credential values or deployment state inspected. Snapshot publishing is combined with non-PR validation; issue #1 tracks an explicit fork-safe release boundary.

## Operations and release evidence

This review did not deploy, pair nodes, install engines, publish packages, change credentials, run live data queries or enable repository Actions. Preserve existing workflow restrictions. A release needs its own immutable source/artifact receipt, passed native contracts, real acceptance gates and rollback target. Upstream release text is not proof that this fork shipped that release.

## Roadmap

Use [the dated review](REVIEW-2026-11-02.md) for the consolidated issue map and inspected paths. GitHub issues own current state. Record fork patches, upstream imports and intentional divergences with their compatibility evidence instead of duplicating inconsistent checklists.
