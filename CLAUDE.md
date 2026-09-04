
# CLAUDE.md

Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

## 5. Project Facts

Multi-module Android SDK, Kotlin, Gradle KTS. `rootProject.name = SceytCallSdk`.

| Module | Purpose |
|---|---|
| `:callclient` | Core call client (WebRTC) |
| `:audiorouting` | Audio device discovery + routing for VoIP calls |
| `:toneplayer` | Call tones with audio focus, routing, reactive state |
| `:app` | Demo app |
| `:examples:audiorouting`, `:examples:toneplayer` | Per-module sample apps |

Module docs: `audiorouting/README.md`, `toneplayer/README.md`.

WebRTC is vendored with a local patch (`webrtc-patch/`) that adds the
`AudioRecordDataCallback` mic-buffer hook screen-share device-audio depends on.
Read that README before touching WebRTC or mic-capture code.

## 6. Verify Commands

- Build: `./gradlew assembleDebug`
- Unit tests, one module: `./gradlew :callclient:testDebugUnitTest`
- Unit tests, all: `./gradlew testDebugUnitTest`
- Coverage: jacoco configured in root `build.gradle.kts` for `:callclient`, `:audiorouting`, `:toneplayer`

Never claim "done" without running the relevant test task.

## 7. Public API Rules

This is a published SDK. Public API is a contract with consumers — this overrides §2
where they conflict.

- Breaking a public signature → flag it, don't do it silently.
- New public class/function → explicit visibility, KDoc.
- Java interop matters: don't remove or skip `@JvmOverloads` / `@JvmStatic` as "speculative".
- No new third-party dependency without asking (SDK size, consumer version conflicts).

## 8. Don't Touch

- `local.properties`, `build/` dirs, `gradle/wrapper/`
- Version bumps in `gradle.properties` or module `build.gradle.kts` — only when asked
- `webrtc-patch/` — ask first

---

**These guidelines are working if:** fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.
