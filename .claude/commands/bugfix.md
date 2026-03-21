---
description: "Fix a bug with structured workflow: analyze → fix → regression test → verify. Pass a bug description."
---

# Bug Fix: $ARGUMENTS

## Step 0: Context

```bash
echo "=== RECENT COMMITS ==="
git log --oneline -10
echo ""
echo "=== RECENT CHANGES ==="
git diff --name-only HEAD~3 -- '*.kt' | head -20
echo ""
echo "=== ERROR LOGS (if any) ==="
grep -r "Exception\|Error\|FAILED" app/build/reports/ 2>/dev/null | tail -10
```

## Step 1: Analysis

Delegate to the **analyst** sub-agent:

> Analyze the bug: "$ARGUMENTS"
> 1. Search the codebase for related code (grep for error messages, class names, keywords)
> 2. Identify the root cause — which file, which function, what goes wrong
> 3. List candidate fixes with pros/cons
> 4. Recommend the best fix approach

## Step 2: Fix

Delegate to the **coder** sub-agent:

> Fix the bug: "$ARGUMENTS"
> Root cause: [from analyst]
> Recommended fix: [from analyst]
> 1. Implement the fix
> 2. Ensure error handling is complete (especially PartialResult)
> 3. Run `./gradlew assembleDebug` to verify build

## Step 3: Regression Test

Delegate to the **tester** sub-agent:

> Write regression tests for: "$ARGUMENTS"
> 1. Write a test that would have caught the original bug
> 2. Write a test that verifies the fix works
> 3. Run `./gradlew test` — all tests must pass

## Step 4: Verify

Delegate to the **verifier** sub-agent:

> Verify build + tests + lint after bug fix.

## Summary

```markdown
## 🐛 Bug Fixed: $ARGUMENTS

### Root Cause
[What was wrong]

### Fix
[What was changed, which files]

### Regression Tests
[X tests added, all passing]

### Verification
Build: ✅ | Tests: ✅ | Lint: ✅
```

Ask: **"Run `/commit` to commit this fix?"**
