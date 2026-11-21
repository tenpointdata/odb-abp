# Agent instructions for tenpointdata/odb-abp

Start with `HANDOFF.md` at the repository root: current state, triage, commands, gotchas.

## Handoff policy

Any repository you work in must stay in a state where another agent (Claude, Codex, Gemini, Antigravity, or a human) can pick up the work cold, with no access to the conversation that produced it. Two things make that true, and you maintain both:

1. **`HANDOFF.md` at the repository root.** The single entry point for whoever comes next. It holds: what the project is and where its pieces live (code, data, services, deploy targets); the current state, under a dated stamp; how to set up, run, test and deploy; the open work, each item linked to its GitHub issue; a **triage** list ranking what to pick up next (lanes for agents, a human, and blocked; one issue per line with its size and what done looks like); known issues and gotchas; recent decisions and why; and this handoff policy itself, in full, so the instruction survives the handoff.
2. **GitHub issues for every open piece of work.** Anything unfinished, deferred, newly discovered, or decided against for now gets an issue with enough context to act on without the conversation: what, why, where in the code, and what done looks like. Close issues when the work lands and reference them from the commit or PR. The open-work list in `HANDOFF.md` points at issues; it never carries work that has no issue.

**When to update:** at the end of any session or turn that changed code, state, configuration, or plans, before the final commit or push. Refresh the current-state stamp, add or close issues, and record decisions made. If a repository has no `HANDOFF.md`, create it and the issues as your first act there. If it has no GitHub remote, say so in `HANDOFF.md` and keep an `## Open work` list there until one exists.

**Triage:** when you start, take the top unclaimed item in the agents' lane unless you were given a task, and claim it (`fork_handoff` with `claim=#N`, or `ej handoff fork --claim N`) so no other agent takes it. When you finish, re-rank: drop what you closed, add what you found, move items between lanes when that changed (a human is needed, something blocks it). Keep it short: the next handful, not the whole backlog; the rest stays in Open work.

**Capture, don't chase:** when the engineer voices a side thought mid-task ("oh, also…"), record it with `capture_idea` (the eng-journal MCP tool, or `ej capture`) and stay on the current goal, unless they say to do it now. When you drift from the goal yourself (more than a few tool calls on something the goal does not need), open a hole first with `hole_open` (the detour, the point to return to, a budget of calls), and close it with `hole_close` and its outcome: fixed-in-place, filed (with the issue or card it went to), abandoned (with why) or became-goal. When a hole reaches its budget ("Hole '…' is at its budget"), decide then: extend it (`hole_tick` with `extend`), file it and return, or abandon it.

**Propagation:** every `HANDOFF.md` you create or update contains this policy, so an agent that reads only the repository learns to keep maintaining it. Mirror it in the repository's `AGENTS.md` (read by Codex, Gemini CLI and others) and have the repository's `CLAUDE.md` import that file with `@AGENTS.md`.

Hand-offs move like a Zed Delta thread: **fork** when you start (`ej handoff fork`, or the MCP tool `fork_handoff`), which records your base commit and shows what was merged since and who else is working here; **merge** a delta when you finish (`ej handoff merge`, `merge_handoff`): what changed, what comes next, what you decided, the issues you opened and closed; or **discard** a fork that leaves nothing to hand on. Work that stops unfinished is **handed on** (`ej handoff pass <id> --note …`, `pass_handoff`): the fork stays open with a note saying where it stopped, and the next agent **resumes** it (`ej handoff fork --resume <id>`, `fork_handoff resume=`) instead of starting over. Start from the brief (`ej handoff brief`, `get_handoff brief=true`): what to pick up, who is working here, and this file's sections with their line ranges, so you read the section you need. Before merging, rewrite Current state to what is true now; history belongs in Deltas, not in the stamp. Claim the triage item you take when you fork (`claim=#N`); a merge that closes an issue drops its triage line.

## Repository conventions

- Stage by explicit path, never `git add -A`: several sessions share checkouts.
- Do not `mvn install` into `~/.m2` from agent sessions.
- Build Studio with `cd studio && yarn install --frozen-lockfile && yarn run build`.

## Repository boundaries

- This is a Tenpointdata fork of [orientechnologies/orientdb](https://github.com/orientechnologies/orientdb). Keep upstream licenses and contribution guidance, and document upstream imports separately from fork-specific changes.
- Read [the fork engineering guide](docs/engineering/README.md) and [source review](docs/engineering/REVIEW-2026-11-02.md) before changing ownership, SDKs, generated interfaces, or release behavior.
- Preserve native build, test, security and compatibility checks. Do not infer current functionality or vulnerability status from a fork name, version label or historical documentation.
- Do not publish packages, change credentials, enable inherited workflows, or deploy as part of a source/documentation review. Current inherited Actions configuration remains unchanged.
- Keep inherited >600-line web source and explicit-any debt visible in canonical issues. A passing check on changed source does not certify the entire upstream corpus.

<!-- tenpointdata-engineering-standards:v1 -->
## Engineering standards

- Keep every TypeScript and web-language code file at or below **600 physical lines**. Split by cohesive responsibility; do not minify, compress, or remove useful context to meet the limit. Fix generators when their checked-in output exceeds the limit.
- Keep code in other languages concise and well organized, with cohesive modules and clear responsibilities.
- Avoid TypeScript `any`, including assertions, generic defaults and declaration files. Reuse canonical domain types and schemas; accept `unknown` at untrusted boundaries and validate/narrow it. Keep strict type checking; do not bypass checks with `@ts-ignore`, `@ts-nocheck` or disabled lint rules.
- Start from the problem, constraints and invariants. Inspect existing libraries, shared types and related project capabilities before building. Extend sound implementations instead of creating parallel ones.
- Keep reusable mechanisms cohesive and independently usable. Keep product rules, UI and external integrations in configuration or thin adapters. Use explicit typed contracts, declarative configuration and composable primitives.
- Preserve the useful capability of shared mechanisms for other consumers. Document assumptions, limits, extension points and compatibility. Use the smallest sound abstraction supported by actual requirements; do not add speculative frameworks or forced cross-project coupling.
- Validate shared contracts, meaningful variations and the affected workflow. For substantial changes, explain what is reusable and what remains project-specific.

## Interaction design

- **Fewer clicks = better design.** Make frequent actions directly available, remove redundant navigation and nonconsequential confirmation steps, apply reversible filters immediately, use sensible defaults and preserve context. Evaluate the complete workflow by interactions required to reach the result.
- Retain confirmations for destructive or consequential actions. Preserve accessibility, clarity and user control.
- Prefer **tables over cards** when users scan, compare or act on multiple items. Choose another layout when it better serves the content, accessibility or task.

## Evidence and documentation

- Preserve existing repository-specific safety, data-access, ownership and workflow rules. Read instructions in the affected directories before changing code.
- Record the source revision, exact checks and results. Distinguish proposed, implemented, locally tested, merged, deployed and accepted behavior; never infer one from another.
- Keep setup, architecture and operational instructions near their owning code. Link the canonical issue for next work instead of maintaining contradictory task lists.
- For a new defect or unresolved design decision, provide evidence, impact, smallest next action and acceptance criteria. Identify hypotheses and untested boundaries explicitly; keep credentials and sensitive data out of issues and logs.
- Before publishing, run applicable source-standards, type, contract and workflow checks using the repository's documented commands. Existing debt remains visible and does not waive the rules for changed code.
<!-- /tenpointdata-engineering-standards:v1 -->
