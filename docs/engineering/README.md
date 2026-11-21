# Engineering guide — odb-abp

Upstream fork of OrientDB; identical to upstream develop at the review snapshot.

## Fork and upstream ownership

- Reviewed source: `a13262f53bb373e48f0057a15f408432804b7dcb` on `develop`.
- Upstream: [orientechnologies/orientdb](https://github.com/orientechnologies/orientdb).
- The November 2 review recorded identical upstream and fork tips. Both repositories resolve `a13262f53bb373e48f0057a15f408432804b7dcb` to the same source tree. [Upstream snapshot](https://github.com/orientechnologies/orientdb/commit/a13262f53bb373e48f0057a15f408432804b7dcb) · [Fork snapshot](https://github.com/tenpointdata/odb-abp/commit/a13262f53bb373e48f0057a15f408432804b7dcb)
- Keep upstream licenses and contribution links with their source. Record future fork patches and upstream imports with their exact revision and compatibility evidence. The fork’s release boundary is tracked in [#1](https://github.com/tenpointdata/odb-abp/issues/1).

## Setup and native verification

The current Maven build targets Java 17, and the hosted workflow checks JDK 17, 21 and 25. Studio uses Yarn and webpack. Its package provides build and watch commands. [Contributor setup](../../CONTRIBUTING.md) · [Hosted test workflow](../../.github/workflows/tests.yml)

| Command | Owning source | Review execution |
| --- | --- | --- |
| mvn -B package --file pom.xml | .github/workflows/tests.yml | Not run |
| mvn -B package -P all --file pom.xml | .github/workflows/tests.yml | Not run |
| yarn build (from studio) | studio/package.json | Not run |

### Changed-source standards

List changed files with `git diff --name-only --diff-filter=ACMR <base>...HEAD`. For each changed path in the source scope defined by [AGENTS.md](../../AGENTS.md), count physical lines with `awk 'END { print NR }' "$path"`. Review changed TypeScript and embedded TypeScript for explicit `any` type uses, including annotations, assertions, type arguments and declaration files. Record the base SHA, checked paths, line counts, type uses and any unverified embedded regions. Use the Studio build for applicable Studio changes. [Studio ETL follow-up #2](https://github.com/tenpointdata/odb-abp/issues/2)

## Architecture and trust boundaries

Java multi-module database engine (core/client/server/security/distributed) with legacy Angular/TypeScript/JavaScript Studio.

Reviewed inherited CI credentials/publication conditions and security-scanning workflow definitions. No credential values or deployment state inspected. Snapshot publishing is combined with non-PR validation; issue #1 tracks an explicit fork-safe release boundary.

## Operations and release evidence

This review did not deploy, pair nodes, install engines, publish packages, change credentials, run live data queries or enable repository Actions. Preserve existing workflow restrictions. A release needs its own immutable source/artifact receipt, passed native contracts, real acceptance gates and rollback target. Upstream release text is not proof that this fork shipped that release.

## Roadmap

Use [the dated review](REVIEW-2026-11-02.md) for the consolidated issue map and inspected paths. GitHub issues own current state. Record fork patches, upstream imports and intentional divergences with their compatibility evidence instead of duplicating inconsistent checklists.
