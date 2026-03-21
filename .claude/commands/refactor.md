---
description: "Refactor a component to improve code quality without changing behavior. Pass the target file or component name."
---

# Refactor: $ARGUMENTS

## Load Context

```bash
echo "=== TARGET FILES ==="
find app/src/main/java -name "*$ARGUMENTS*" -type f | head -10
echo ""
echo "=== RELATED TESTS ==="
find app/src/test/java -name "*$ARGUMENTS*" -type f | head -10
echo ""
echo "=== USAGES ==="
grep -rl "$ARGUMENTS" app/src/main/java/ --include="*.kt" | head -20
```

## Analysis

Delegate to the **analyst** sub-agent:

> Analyze $ARGUMENTS for refactoring opportunities:
> 1. Code smells (long functions, god classes, deep nesting)
> 2. Duplication with other files
> 3. Architecture violations
> 4. Missing error handling
> 5. Propose specific refactoring steps

## Refactor

Delegate to the **coder** sub-agent:

> Refactor $ARGUMENTS based on the analysis.
> Rules:
> - Do NOT change external behavior
> - Ensure all existing tests still pass
> - Run `./gradlew assembleDebug` after changes

## Verify

Delegate to the **verifier** sub-agent:

> Verify build + tests after refactoring $ARGUMENTS.
> All existing tests MUST still pass — behavior must not change.

## Summary

Present: what changed, why, before/after comparison, test results.
