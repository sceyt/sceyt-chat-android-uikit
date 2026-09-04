# Coding Guidelines

These guidelines reduce common coding-agent mistakes: overcomplication, unnecessary refactors, hidden assumptions, and insufficient verification.

They should be combined with any project-specific instructions in this repository.

**Tradeoff:** These rules intentionally bias toward correctness and small, reviewable changes over speed. For trivial tasks, use judgment.

## 1. Think Before Coding

**Understand the task before changing code. Do not hide material assumptions.**

Before implementing:

- Inspect the relevant code and existing patterns first.
- State assumptions when they materially affect the solution.
- If ambiguity could significantly change the implementation, ask for clarification.
- Otherwise, make the most reasonable assumption, state it briefly when useful, and proceed.
- If multiple interpretations are plausible and materially different, surface them.
- If a simpler approach exists, prefer it and say so when relevant.
- Push back when the requested approach creates unnecessary complexity or risk.

Do not invent missing requirements.

## 2. Simplicity First

**Write the minimum code needed to solve the requested problem. Nothing speculative.**

- Do not add features that were not requested.
- Do not introduce abstractions for one-off logic unless they clearly improve the current change.
- Do not add configurability or extensibility for hypothetical future needs.
- Do not add speculative defensive handling for unrealistic scenarios.
- Preserve error handling required by existing project conventions and realistic failure modes.
- Prefer straightforward code over clever code.
- If the implementation becomes much larger than the problem requires, simplify it.

Ask yourself:

> Would a senior engineer reviewing this diff consider it unnecessarily complicated?

If yes, simplify it.

## 3. Surgical Changes

**Touch only what is necessary. Clean up only what your change makes necessary.**

When editing existing code:

- Do not refactor unrelated code.
- Do not reformat unrelated code.
- Do not rewrite comments unless the change requires it.
- Match the surrounding code style and project conventions.
- Preserve existing naming and architecture unless changing them is part of the task.
- If you notice unrelated dead code, bugs, or cleanup opportunities, mention them separately instead of changing them.

When your changes create unused code:

- Remove imports, variables, helpers, or functions that became unused because of your change.
- Do not remove pre-existing unused code unless explicitly asked.

**Every changed line should trace directly to the user's request or be required to keep the resulting code correct.**

## 4. Goal-Driven Execution

**Translate the request into verifiable success criteria and work toward them.**

Examples:

- "Add validation" → define invalid inputs, add or update tests when practical, then make them pass.
- "Fix the bug" → reproduce the failure with a test when practical, implement the fix, then verify the failing scenario.
- "Refactor X" → preserve behavior and ensure relevant tests pass before and after.
- "Improve performance" → identify the measurable bottleneck and verify that the change improves it without breaking behavior.

For multi-step or non-trivial tasks, use a brief plan:

```text
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Keep the plan proportional to the task. Do not create process overhead for trivial changes.

## 5. Verification

**Do not stop at "the code looks right." Verify the change.**

After making changes:

- Run the narrowest relevant test, lint, build, type-check, or static-analysis command available.
- Prefer targeted verification before expensive repository-wide commands.
- Fix failures caused by your changes.
- Do not fix unrelated pre-existing failures unless asked.
- Re-check the final diff for accidental or unrelated edits.
- Confirm that the requested behavior is actually covered.

If verification cannot be run:

- Say exactly what was not verified.
- Say why it could not be verified.
- Do not claim the change is fully validated.

## 6. Respect Existing Project Conventions

Before introducing a new pattern, dependency, helper, abstraction, or architectural layer:

- Search for the existing project convention first.
- Reuse existing utilities when they fit the task.
- Match established APIs and naming.
- Do not add a dependency if the repository already has a reasonable way to solve the problem.
- Avoid broad architectural changes unless the task explicitly requires them.

Project-specific instructions override these general guidelines when they conflict.

## 7. Final Review

Before finishing, check:

- Does the implementation solve exactly the requested problem?
- Is there a simpler solution?
- Did I change anything unrelated?
- Did I preserve existing behavior outside the requested scope?
- Did I verify the relevant behavior?
- Are there assumptions or limitations the user should know about?

These guidelines are working when diffs stay small, behavior is verifiable, unnecessary abstractions disappear, and clarification happens only when ambiguity materially affects the implementation.

## 8. Controlled Architecture Changes

**Make architecture and public-API changes through reviewable file checkpoints.**

- Do not use parallel agents to edit files during architecture or public-API work.
- Before changing a production file, determine why it must change, its current responsibility, and whether the proposed change expands that responsibility. Explain this briefly when it is not obvious from the task or patch.
- Prefer a dedicated component when the new behavior does not fit the existing file's responsibility.
- Change one file per approval patch. Submit the patch directly to the application's Accept/Deny dialog without requesting a separate chat approval first.
- If the user denies a patch, stop all implementation work, ask why it was denied in chat, and wait for the user's response. Do not retry the patch or continue with another file.
- After an accepted patch, review its diff and continue to the next in-scope file without asking for another chat confirmation.
- Group files only for verification when a single-file intermediate state cannot compile; still edit and review those files one at a time.
- If implementation reveals that an unexpected production file must change or that a file's responsibility would materially expand, explain the dependency before submitting its patch.
- Run the narrowest relevant verification after each compilable group of approved file changes.
- Documentation-only and test-only files follow the same one-file review sequence when they define or validate architecture behavior.
