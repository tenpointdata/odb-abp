# OrientDB ABP handoff

**Last updated:** 2026-11-20, recorded the merged index-drop fixes and verified Java distribution.

**Start here:** `ej handoff brief` (or the MCP tool `get_handoff` with `brief=true`) is the short way in: what to pick up, who is working here, and where each section of this file is. Without those tools, read Current state, Triage and Known issues and gotchas, then the issue you take.

## 1. Handoff policy

Any repository you work in must stay in a state where another agent (Claude, Codex, Gemini, Antigravity, or a human) can pick up the work cold, with no access to the conversation that produced it. Two things make that true, and you maintain both:

1. **`HANDOFF.md` at the repository root.** The single entry point for whoever comes next. It holds: what the project is and where its pieces live (code, data, services, deploy targets); the current state, under a dated stamp; how to set up, run, test and deploy; the open work, each item linked to its GitHub issue; a **triage** list ranking what to pick up next (lanes for agents, a human, and blocked; one issue per line with its size and what done looks like); known issues and gotchas; recent decisions and why; and this handoff policy itself, in full, so the instruction survives the handoff.
2. **GitHub issues for every open piece of work.** Anything unfinished, deferred, newly discovered, or decided against for now gets an issue with enough context to act on without the conversation: what, why, where in the code, and what done looks like. Close issues when the work lands and reference them from the commit or PR. The open-work list in `HANDOFF.md` points at issues; it never carries work that has no issue.

**When to update:** at the end of any session or turn that changed code, state, configuration, or plans, before the final commit or push. Refresh the current-state stamp, add or close issues, and record decisions made. If a repository has no `HANDOFF.md`, create it and the issues as your first act there. If it has no GitHub remote, say so in `HANDOFF.md` and keep an `## Open work` list there until one exists.

**Triage:** when you start, take the top unclaimed item in the agents' lane unless you were given a task, and claim it (`fork_handoff` with `claim=#N`, or `ej handoff fork --claim N`) so no other agent takes it. When you finish, re-rank: drop what you closed, add what you found, move items between lanes when that changed (a human is needed, something blocks it). Keep it short: the next handful, not the whole backlog; the rest stays in Open work.

**Capture, don't chase:** when the engineer voices a side thought mid-task ("oh, also…"), record it with `capture_idea` (the eng-journal MCP tool, or `ej capture`) and stay on the current goal, unless they say to do it now. When you drift from the goal yourself (more than a few tool calls on something the goal does not need), open a hole first with `hole_open` (the detour, the point to return to, a budget of calls), and close it with `hole_close` and its outcome: fixed-in-place, filed (with the issue or card it went to), abandoned (with why) or became-goal. When a hole reaches its budget ("Hole '…' is at its budget"), decide then: extend it (`hole_tick` with `extend`), file it and return, or abandon it.

**Propagation:** every `HANDOFF.md` you create or update contains this policy, so an agent that reads only the repository learns to keep maintaining it. Mirror it in the repository's `AGENTS.md` (read by Codex, Gemini CLI and others) and have the repository's `CLAUDE.md` import that file with `@AGENTS.md`.

Hand-offs move like a Zed Delta thread: **fork** when you start (`ej handoff fork`, or the MCP tool `fork_handoff`), which records your base commit and shows what was merged since and who else is working here; **merge** a delta when you finish (`ej handoff merge`, `merge_handoff`): what changed, what comes next, what you decided, the issues you opened and closed; or **discard** a fork that leaves nothing to hand on. Work that stops unfinished is **handed on** (`ej handoff pass <id> --note …`, `pass_handoff`): the fork stays open with a note saying where it stopped, and the next agent **resumes** it (`ej handoff fork --resume <id>`, `fork_handoff resume=`) instead of starting over. Start from the brief (`ej handoff brief`, `get_handoff brief=true`): what to pick up, who is working here, and this file's sections with their line ranges, so you read the section you need. Before merging, rewrite Current state to what is true now; history belongs in Deltas, not in the stamp. Claim the triage item you take when you fork (`claim=#N`); a merge that closes an issue drops its triage line.

## 2. What this is

This public fork maintains OrientDB for downstream use. The `develop` branch follows the 4.0 development line. The Community distribution is built from the Maven reactor.

## 3. Where things live

`core/` contains storage and SQL. `server/` and `distributed/` implement service and cluster operation. `studio/` contains the web UI. `packages/community/` assembles the runnable Community distribution. `.github/workflows/` defines GitHub Actions validation. [PR #4](https://github.com/tenpointdata/odb-abp/pull/4) contains the operations candidate and upgrade evidence.

## 4. Current state

[PR #8](https://github.com/tenpointdata/odb-abp/pull/8) merged into `develop` at `bffac2b2`. The landed tree matches the synthetic merge that passed hosted Java 17, 21 and 25, CodeQL and Dependency Review. The [Community distribution](https://github.com/tenpointdata/odb-abp/actions/runs/44856959376/artifacts/14689436490) matched GitHub's SHA-256, the embedded source commit, both inner archive checksums and format checks.

[PR #9](https://github.com/tenpointdata/odb-abp/pull/9) merged into `develop` at `f0940435`. It keeps the simulated monotonic clock used by the distributed ping test and batches null-key index removals through the same path as ordinary keys. With 2,500 indexed nulls the in-memory fixture recorded 2,502 commits before the fix and four after. With 1,000 ordinary keys and 1,500 indexed nulls it recorded 1,503 before and four after. The ordinary-key case also recorded four. The test checks indexed null membership before the drop and checks both a lower and upper bound on commits.

The focused topology, ping and index-drop suites passed all 16 tests on JDK 21 with Java 17-compatible changed classes over the verified distribution libraries. The landed tree `07da853fd64c32b25aeb34fa2c75bd823aad0612` matches test merge `082b1512`. [Hosted Java 17, 21 and 25](https://github.com/tenpointdata/odb-abp/actions/runs/44863255170), CodeQL and Dependency Review passed. The [Community artifact](https://github.com/tenpointdata/odb-abp/actions/runs/44863255170/artifacts/14691693562) matched the GitHub SHA-256, embedded source commit and both inner checksums. The TAR.GZ and ZIP passed integrity checks.

The [fork engineering guide](docs/engineering/README.md) and its November 2 source review document the earlier immutable snapshot. [PR #3](https://github.com/tenpointdata/odb-abp/pull/3) carries that snapshot and the fork standards. [PR #12](https://github.com/tenpointdata/odb-abp/pull/12) confirms the landed #8/#9 state and hosted checks. The fork still has no owned release destination under [#1](https://github.com/tenpointdata/odb-abp/issues/1).

## 5. Triage

**Agents can pick up:**
- [#10](https://github.com/tenpointdata/odb-abp/issues/10) — M — Qualify hosted Java 25 server lifecycle and fixture cleanup with the full Java matrix.
- [#5](https://github.com/tenpointdata/odb-abp/issues/5) — S — Run Studio build, typecheck and ETL smoke test on its development branch.

**Blocked or waiting:**
- [#2](https://github.com/tenpointdata/odb-abp/issues/2) — M — Studio change acceptance waits on #5.

## 6. Set up, run, test, deploy

Use JDK 17 or later and Maven 3.7 or later. Run `mvn -B package -P all --file pom.xml` for the credential-free package and test path. Java CI runs the standard reactor on JDK 21 and 25 and the distribution reactor on JDK 17. The distribution job uploads the Community tarball, zip, `SHA256SUMS` and `SOURCE_COMMIT` together as an Actions artifact retained for 14 days. Verify the SHA and `shasum -a 256 -c SHA256SUMS` before using it. The artifacts are available to people with Actions access to this public repository.

## 7. Open work

- [#1](https://github.com/tenpointdata/odb-abp/issues/1) — An owned release destination remains to be identified if the fork should publish outside GitHub Actions.
- [#10](https://github.com/tenpointdata/odb-abp/issues/10) — Hosted Java 25 server lifecycle and fixture cleanup qualification.
- [#2](https://github.com/tenpointdata/odb-abp/issues/2), [#5](https://github.com/tenpointdata/odb-abp/issues/5), [#6](https://github.com/tenpointdata/odb-abp/issues/6), [#7](https://github.com/tenpointdata/odb-abp/issues/7) — Studio ETL editor qualification and fixes.

## 8. Known issues and gotchas

The merged upgrade audit in PR #4 describes a standalone 3.2.42 compatibility and rollback rehearsal. Cluster qualification and upgrade timing need separate evidence before deployment. The project POM retains upstream Maven coordinates and dependency resolution. [PR #3](https://github.com/tenpointdata/odb-abp/pull/3) contains the draft engineering guide.

## 9. Decisions

- Ordinary Maven validation uses `package` on every event. Snapshots and release publishing require their own explicit destination and identity. The upstream manual release job is scoped to the upstream repository on tags.
- Distribution verification records both archive checksums and the SHA actually checked out, including GitHub's synthetic merge commit on PRs.

## 10. Deltas

<!-- composite:deltas · newest first; Composite rewrites this block on every merge (ej handoff merge / merge_handoff) -->
<!-- /composite:deltas -->

## 11. Updating this document

Refresh Current state, Triage and Open work before finalizing a change. Record decisions and merge the Composite handoff delta. Keep the policy intact.
