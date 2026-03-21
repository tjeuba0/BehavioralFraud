---
description: "Review code changes on the current branch. Optionally pass specific file paths to review."
---

# Code Review: $ARGUMENTS

## Load Changed Files

```bash
echo "=== CURRENT BRANCH ==="
git branch --show-current
echo ""
echo "=== CHANGED FILES (vs main) ==="
git diff --name-only main -- '*.kt' 2>/dev/null || git diff --name-only HEAD~5 -- '*.kt'
echo ""
echo "=== DIFF STATS ==="
git diff --stat main 2>/dev/null || git diff --stat HEAD~5
```

## Review

Delegate to the **reviewer** sub-agent:

> Review the following code changes:
> - Branch: [current branch]
> - Files: [changed .kt files, or $ARGUMENTS if specific files given]
> 
> Check all 5 criteria: correctness, architecture, security, performance, code style.
> Pay special attention to PartialResult handling and error states.
> Produce a structured review report with findings and verdict.

## Post-Review

If critical issues found, ask: **"Should I fix these issues? I'll delegate to the coder agent."**

If approved, ask: **"Ready to commit? Run `/commit` to create a PR."**
