# Agent instructions for tenpointdata/odb-abp

Start with `HANDOFF.md` at the repository root: current state, triage, commands, gotchas.

## Handoff policy

Any repository you work in must stay in a state where another agent (Claude, Codex, Gemini, Antigravity, or a human) can pick up the work cold, with no access to the conversation that produced it. Two things make that true, and you maintain both:

1. **`HANDOFF.md` at the repository root.** The single entry point for whoever comes next. It holds: what the project is and where its pieces live; the current state, under a dated stamp; how to set up, run, test and deploy; the open work, each item linked to its GitHub issue; a **triage** list ranking what to pick up next (lanes for agents, a human, and blocked); known issues and gotchas; recent decisions and why; and this handoff policy itself, in full.
2. **GitHub issues for every open piece of work.** Anything unfinished, deferred, newly discovered, or decided against for now gets an issue with enough context to act on: what, why, where in the code, and what done looks like. Close issues when the work lands and reference them from the commit or PR. The open-work list in `HANDOFF.md` never carries work that has no issue.

**When to update:** at the end of any session or turn that changed code, state, configuration, or plans, before the final commit or push: refresh the current-state stamp, add or close issues, record decisions.

**Triage:** when you start, take the top unclaimed item in the agents' lane unless you were given a task, and claim it (`fork_handoff claim=#N` or `ej handoff fork --claim N`). When you finish, re-rank.

**Fork and merge:** fork when you start (`fork_handoff` / `ej handoff fork`), merge a delta when you finish (`merge_handoff` / `ej handoff merge`), hand on unfinished work (`pass_handoff` / `ej handoff pass`), or discard a fork that leaves nothing. Rewrite Current state before merging; history belongs in Deltas.

**Propagation:** every `HANDOFF.md` contains this policy; mirror it here in `AGENTS.md`, and have `CLAUDE.md` import this file with `@AGENTS.md`.

## Repository conventions

- Stage by explicit path, never `git add -A`: several sessions share checkouts.
- Do not `mvn install` into `~/.m2` from agent sessions.
- Studio ETL unit tests: `cd studio && npm run test:unit` (Node >= 22.18).
