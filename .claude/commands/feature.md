---
name: feature
description: "Develop a feature end-to-end. Pass a TASK-ID (e.g., TASK-006) to auto-load SRS + Figma context. Orchestrates: context → analysis → parallel test writing → design → implement → independent QC loop until DONE."
allowed-tools: Bash(git *), Bash(./gradlew *), Bash(sed *), Bash(grep *), Bash(head *), Bash(tail *), Bash(cat *), Bash(echo *), Read, Write, Edit, Glob, Grep, MultiEdit, Task
---

# Feature Development: $ARGUMENTS

## Execution Rules

You MUST follow these rules throughout the entire flow. Violation of any rule is a failure condition.

1. **Execute every step in order.** Do NOT skip or merge steps.
2. **Morph MCP first.** Use `warpgrep_codebase_search` as the FIRST search tool. Use `edit_file` for ALL file edits. NEVER use `replace_file_content`.
3. **Each step has a CHECKPOINT.** Verify all items before moving to the next step.
4. **Sub-agent fallback.** If a sub-agent file (`.claude/agents/{name}.md`) does not exist, you MUST still perform that step's work yourself. The step is NOT optional.
5. **SRS is mandatory.** Every feature MUST cross-reference the SRS. Do NOT implement based on Figma alone.
6. **Completion means QC-passed.** A feature is DONE only when: every SRS requirement is implemented, independent review has ZERO critical AND ZERO warning issues, all unit tests pass (count > 0), build succeeds, and lint has zero new errors. You MUST NOT stop, summarize, or ask to commit until ALL conditions are met.
7. **Tests are specification-driven.** Unit tests MUST be derived from SRS requirements, NOT from the implementation code. Test cases are written BEFORE or IN PARALLEL with implementation, never after.
8. **Reviews must be independent.** Code review MUST be performed by a Task agent that receives ONLY the diff and review checklist — NOT the implementation rationale. The reviewer must NOT have access to design decisions or "why" the code was written that way.

---

## Step 0: Load Task Context

Read the task registry and extract context for this feature:

1. Use `Read` tool to open `docs/tasks.md`. Find the section `### $ARGUMENTS:` and read until the next `### TASK-` heading. Extract: **feature name**, **SRS section reference**, **Figma URL** (if any), **branch name**, **dependencies**, and **status**.

2. **Check dependencies.** For each dependency listed (comma-separated), find its section in `docs/tasks.md` and check its Status. If ANY dependency is NOT `done`, STOP immediately and inform the user: "Dependency [DEP-ID] is [status], cannot proceed."

3. **Check git state.** Run:
   - `git branch --show-current` — to confirm current branch
   - `git status --short` — to check for uncommitted changes

4. **CRITICAL — Read SRS NOW.** Open `docs/SRS.md` and read the EXACT section referenced by the SRS field in the task entry. This is a top-level requirement, not delegated. Extract every functional requirement, validation rule, format specification, and quality requirement. You will need them for implementation, testing, and review.

**CHECKPOINT Step 0:**
- [ ] Task entry parsed (feature name, SRS ref, Figma URL, branch, deps)
- [ ] Dependencies verified — all `done` (or STOPPED if not)
- [ ] SRS section has been READ and key requirements are in context
- [ ] If any item is missing, DO NOT proceed

---

## Step 1: Setup Branch + Update Status

Create or checkout the feature branch (extracted from task entry). Then edit `docs/tasks.md` — change this task's Status from `planned` to `in-progress`.

**CHECKPOINT Step 1:**
- [ ] On correct feature branch
- [ ] Task status updated to `in-progress` in docs/tasks.md

---

## Step 2: Analysis + Spawn Test Writer

Use the **analyst** sub-agent if `.claude/agents/analyst.md` exists. If it does NOT exist, perform this analysis yourself.

Analysis scope:

1. Review the SRS requirements extracted in Step 0 (already in context).
2. Use `warpgrep_codebase_search` to find existing related code — screens, models, navigation routes, string resources.
3. Identify affected files, data models needed, error scenarios, and reusable components.
4. Produce a structured analysis summary with numbered requirements:

```
ANALYSIS: $ARGUMENTS
━━━━━━━━━━━━━━━━━━━
Requirements (from SRS):
  - [REQ-01] [description]
  - [REQ-02] [description]
  - ...

Affected Files:
  - [files to create]
  - [files to modify]

Reusable Components:
  - [existing components to reuse]

Data Models:
  - [models to create or extend]

Error Scenarios:
  - [list error cases to handle]

Ambiguities:
  - [any conflicts between SRS and Figma, or unclear requirements]
```

Number each requirement as REQ-01, REQ-02, etc. These IDs track compliance throughout the rest of the flow.

If you find ambiguities (SRS says X but Figma shows Y, or a requirement is unclear), ask the user for clarification BEFORE proceeding. Do NOT guess.

### Spawn Parallel Test Writer (Task Agent)

**Immediately after producing the analysis summary**, use the `Task` tool to spawn a parallel agent that writes test cases from the SRS specification. This agent works independently while you continue with Design and Implementation.

Task agent prompt:

> You are a test engineer. Write unit test cases for the following feature based ONLY on the SRS requirements below. Do NOT look at any implementation code. Your tests must verify the SPECIFICATION, not any particular implementation.
>
> Feature: [feature name from task entry]
> SRS Requirements:
> [paste the REQ-01 through REQ-XX list from analysis]
>
> Existing test infrastructure: Check `app/build.gradle.kts` for test dependencies. If JUnit/MockK/Turbine are not present, note which dependencies need to be added.
>
> Write test files to: `app/src/test/java/dev/khoivan/maysach/`
>
> Rules:
> - Test the SPECIFICATION (what the code SHOULD do), not the implementation (what the code DOES do)
> - For each REQ-ID, write at least: 1 happy path test, 1 error/edge case test
> - Use descriptive test names that reference the REQ-ID: `REQ-01 - should produce verification code in format MS-yyyy-XXXX-XXXX`
> - Test boundary values, null/empty inputs, and error conditions from SRS
> - If a requirement involves a format specification (e.g., "MS-yyyy-XXXX-XXXX"), test the format strictly
> - If a requirement involves security (e.g., SHA-256), verify the algorithm properties (deterministic, collision-resistant)
> - Save test files but do NOT run them yet — they will be run after implementation

**CHECKPOINT Step 2:**
- [ ] Analysis summary produced with all sections filled
- [ ] SRS requirements listed explicitly with REQ-IDs
- [ ] Ambiguities resolved (asked user if needed)
- [ ] Test writer Task agent spawned with SRS requirements (runs in parallel)

---

## Step 3: Design

Extract the Figma URL from the task entry. If a Figma URL exists:

Use the **designer** sub-agent if `.claude/agents/designer.md` exists. If it does NOT exist, perform this design work yourself.

Design scope:

1. Fetch the design via Figma MCP tools (`get_design_context` + `get_screenshot`).
2. If the Figma node returns only a section header (not the actual screen), navigate to child nodes using `get_metadata` and find the correct screen node.
3. Extract design tokens, typography, layout structure, color values, and spacing.
4. Use `warpgrep_codebase_search` to check existing components in `ui/components/` for reuse.
5. Map Figma elements to Jetpack Compose components.
6. Cross-reference with SRS requirements from Step 2 — flag any discrepancies.

Produce a UI implementation spec:

```
UI SPEC: $ARGUMENTS
━━━━━━━━━━━━━━━━━━
Screen Structure:
  - [top-level layout]
  - [sections and their components]

Components to Reuse:
  - GlassCard, StatusRow, FloatUpAnimation, etc.

Components to Create:
  - [new components needed]

Design Tokens:
  - Colors: [specific values]
  - Typography: [font weights, sizes]
  - Spacing: [padding, margins]

SRS Cross-Check:
  - [REQ-01] → [mapped to UI element X]
  - [REQ-02] → [mapped to UI element Y]
  - [REQ-03] → NOT in Figma → MUST implement from SRS
```

If NO Figma URL exists, skip Figma fetch and note: "No Figma design — implementing based on SRS + existing screen patterns."

**CHECKPOINT Step 3:**
- [ ] Figma design fetched and understood (or noted as absent)
- [ ] UI spec produced
- [ ] SRS cross-check completed — every REQ-ID mapped to a UI element or flagged as "implement from SRS"

---

## Step 4: Implementation

Use the **coder** sub-agent if `.claude/agents/coder.md` exists. If it does NOT exist, implement the code yourself.

Implementation inputs (you MUST reference all of these):
- SRS requirements from Step 0
- Analysis summary from Step 2 (with REQ-IDs)
- UI spec from Step 3

Before writing any code, READ `CLAUDE.md` at the project root — it is the single source of truth for all coding standards. Every line of code you write MUST comply with its rules. If you intentionally deviate from any rule, add a comment: `// DEVIATION: [rule description] — [reason]`. No deviation is permitted without a comment.

Implement EVERY requirement from SRS, not just what is visible in Figma.

After implementation, verify the build succeeds. If it fails, fix the errors and rebuild. Repeat until build succeeds.

**Before moving to QC Loop:** Collect the test files written by the parallel test writer Task agent from Step 2. If the Task agent has not completed yet, wait for it. Integrate the test files into the project. These tests were written from SRS — they validate the specification, not the implementation.

**CHECKPOINT Step 4:**
- [ ] All files created/modified
- [ ] Build succeeds (`./gradlew assembleDebug`)
- [ ] All REQ-IDs from Step 2 implemented (self-check each one)
- [ ] Test files from parallel Task agent collected and integrated

---

## Step 5–7: QC Loop (Independent Review → Fix → Test → Verify)

> **THIS IS A LOOP, NOT A LINEAR SEQUENCE.**
> You MUST repeat this loop until ALL exit criteria are met.
> Do NOT exit early. Do NOT summarize partial results. Do NOT ask to commit.

### 5A: Independent Code Review (Task Agent)

**CRITICAL: The review MUST be independent.** You (the main agent) wrote the code. You MUST NOT review your own code. Use the `Task` tool to spawn a reviewer agent that has NO access to your design decisions or implementation rationale.

Spawn a Task agent with this prompt:

> You are an independent code reviewer. You have NO context about WHY the code was written this way.
>
> BEFORE reviewing, READ these two files:
> 1. `CLAUDE.md` at the project root — this is the ONLY coding standard. No other checklist applies.
> 2. The SRS requirements listed below.
>
> ## Rules for findings:
> - 🔴 Critical: code VIOLATES a specific rule in CLAUDE.md. MUST cite: "Violates rule: [exact rule text from CLAUDE.md]"
> - 🟡 Warning: code does not violate a rule but has a concrete, demonstrable risk (memory leak, race condition, crash, security vulnerability). MUST describe the specific execution path: "happens when [scenario]." Not "might happen."
> - ℹ️ Info: improvement suggestion. Does NOT block merge.
>
> If you cannot point a finding to a specific rule in CLAUDE.md or a concrete execution path → it is ℹ️ Info, NOT 🔴 or 🟡.
>
> ## Code Changes (diff)
> [paste the output of `git diff` for all changed files]
>
> ## SRS Requirements to Verify
> [paste REQ-01 through REQ-XX list from Step 2]
>
> ## Review Scope (in priority order):
>
> ### Priority 1: SRS Compliance
> For each REQ-ID listed above, find the code that implements it. Not found → 🔴.
>
> ### Priority 2: CLAUDE.md Rules
> For each rule in CLAUDE.md applicable to the code in the diff, verify compliance. Violation → 🔴, cite the exact rule text from CLAUDE.md.
>
> ### Priority 3: Concrete Risks
> Memory leak, race condition, crash, or security vulnerability where you can identify a specific execution path. → 🟡 if you can state "happens when [specific scenario]", ℹ️ if only a general concern.
>
> ### Everything else → ℹ️ Info. Does NOT block merge.
>
> ## Output Format
>
> ```
> REVIEW: [Feature Name] (Iteration N)
> ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
> SRS Compliance:
>   - [REQ-01] ✅ | ❌ [detail]
>
> 🔴 Critical (MUST fix):
>   - Violates rule: "[exact rule from CLAUDE.md]" — [file:line] — [fix]
>
> 🟡 Warning (MUST fix):
>   - Risk: "happens when [specific scenario]" — [file:line] — [fix]
>
> ℹ️ Info (optional):
>   - [suggestion]
>
> 🟢 Approved:
>   - [what looks good]
>
> Verdict: [BLOCKED / FIX REQUIRED / APPROVED]
> ```
>
> Verdict rules:
> - BLOCKED = any 🔴 → must fix and re-review
> - FIX REQUIRED = any 🟡 but no 🔴 → must fix and re-review
> - APPROVED = zero 🔴 and zero 🟡

**Why Task agent?** The reviewer agent receives ONLY the diff and checklist. It does NOT see your Step 3 design decisions, Step 2 analysis rationale, or any "why" context. This eliminates confirmation bias — the reviewer judges the code on its own merits, exactly like a human reviewer seeing a PR for the first time.

### 5B: Fix Issues

If verdict is `BLOCKED` or `FIX REQUIRED`:
1. Fix ALL 🔴 critical issues.
2. Fix ALL 🟡 warning issues.
3. Rebuild and verify build succeeds.
4. Go back to 5A — spawn a NEW Task agent for re-review (do NOT reuse the previous one, as it now has context of the previous review).

Do NOT proceed to testing until review verdict is `APPROVED`.

### 5C: Run Specification-Driven Tests

The test files were written in Step 2 (by the parallel Task agent) from SRS requirements — BEFORE the implementation existed. These tests validate the specification, not the implementation.

1. Ensure all test files from the parallel Task agent are in the project.
2. Run the tests. If any test fails, determine the cause:
   - **Implementation bug** (code doesn't match SRS) → fix the implementation code, NOT the test.
   - **Test bug** (test misinterprets SRS) → fix the test, document why.
   - **SRS ambiguity** (requirement is unclear) → ask the user, then fix accordingly.
3. If you created additional code (e.g., utility classes) during implementation that the test writer didn't anticipate, write additional tests for those — but still derive test cases from SRS requirements, not from the implementation.
4. Re-run until ALL tests pass.

**IMPORTANT:** If a specification-driven test fails, the DEFAULT assumption is that the IMPLEMENTATION is wrong, not the test. Only change the test if you can demonstrate that the test misinterprets the SRS.

### 5D: Final Verification

Run all three checks: build, tests, and lint. All three must pass.

### QC Loop Exit Criteria

**ALL of the following must be TRUE to exit this loop:**

- [ ] Independent review verdict: `APPROVED` (zero 🔴, zero 🟡) — reviewed by Task agent, NOT self-reviewed
- [ ] MASVS security categories checked: STORAGE, CRYPTO, PLATFORM, CODE, PRIVACY
- [ ] Every REQ-ID from Step 2: ✅ implemented and verified
- [ ] Build: SUCCESS (`./gradlew assembleDebug`)
- [ ] Tests: ALL PASS (with actual test count > 0, tests derived from SRS)
- [ ] Lint: zero new errors (`./gradlew lint`)

If ANY item above is NOT met, go back to the appropriate sub-step (5A, 5B, or 5C) and fix it.

You are NOT allowed to:
- Self-review your own code (MUST use Task agent)
- Exit with "Tests: PASS (no tests)" — this means you skipped testing
- Exit with review verdict `FIX REQUIRED` — this means warnings remain
- Exit without checking every REQ-ID against the implementation
- Modify tests to match implementation without SRS justification
- Summarize or ask to commit before all exit criteria are met

---

## Step 8: Complete

**Only reach this step after ALL QC Loop Exit Criteria in Step 5–7 are met.**

1. Update `docs/tasks.md` — change status to `done`.
2. Produce final summary:

```markdown
## ✅ Feature Complete: $ARGUMENTS — [Feature Name]

### SRS Compliance (every REQ-ID must appear here)
| REQ-ID | Requirement | Status | Implementation |
|--------|-------------|--------|----------------|
| REQ-01 | [description] | ✅ | [file + how] |
| REQ-02 | [description] | ✅ | [file + how] |

### MASVS Security Summary
| Category | Status | Notes |
|----------|--------|-------|
| STORAGE | ✅ | [detail] |
| CRYPTO | ✅ | [detail] |
| PLATFORM | ✅ | [detail] |
| CODE | ✅ | [detail] |
| PRIVACY | ✅ | [detail] |

### Files Created
| File | Purpose |
|------|---------|
| [path] | [description] |

### Files Modified
| File | Change |
|------|--------|
| [path] | [description] |

### Test Results
- Total: X tests, X passed, 0 failed
- Test origin: Specification-driven (written from SRS before implementation)
- Test files: [list test file paths]

### Review History
- Iteration 1: [BLOCKED — N critical, N warnings] (Task agent)
- Iteration 2: [FIX REQUIRED — 0 critical, N warnings] (Task agent)
- Iteration 3: [APPROVED — 0 critical, 0 warnings] (Task agent)

### Verification
- Build: ✅ PASS
- Tests: ✅ X/X passed
- Lint: ✅ 0 new errors

### QC Exit Criteria
- [x] Independent Review: APPROVED (Task agent, not self-review)
- [x] MASVS Security: All 5 categories checked
- [x] All REQ-IDs: ✅
- [x] Build: PASS
- [x] Tests: ALL PASS (specification-driven, count > 0)
- [x] Lint: 0 new errors

### Next Steps
[Any follow-up items — only for items explicitly OUT OF SCOPE for this task]
```

3. Ask user: **"Ready to commit? Run `/commit` to create a PR."**
