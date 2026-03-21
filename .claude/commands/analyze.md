---
description: "Analyze a new document (requirement, API spec, design doc) and propose tasks to add to the registry. Pass the file path."
---

# Analyze Document: $ARGUMENTS

## Load Document

```bash
echo "=== DOCUMENT ==="
if [ -f "$ARGUMENTS" ]; then
    wc -l "$ARGUMENTS"
    echo ""
    head -50 "$ARGUMENTS"
else
    echo "File not found: $ARGUMENTS"
    echo "Available docs:"
    find docs/ -type f | sort
fi
echo ""
echo "=== CURRENT TASKS ==="
grep -E "^### TASK-" docs/tasks.md
echo ""
echo "=== CURRENT SRS SECTIONS ==="
grep -E "^### |^## " docs/SRS.md | head -30
```

## Analysis

Delegate to the **analyst** sub-agent:

> Analyze the document at: $ARGUMENTS
> 
> 1. Summarize the document's key requirements
> 2. Cross-reference with existing SRS (`docs/SRS.md`) — what's new vs already covered?
> 3. Cross-reference with existing tasks (`docs/tasks.md`) — what's already planned?
> 4. Identify new features/changes needed
> 5. Propose new task entries for `docs/tasks.md`
> 6. Identify if SRS needs updating

## Output

```markdown
## Document Analysis: [filename]

### Summary
[What this document describes]

### New Requirements (not in current SRS)
| # | Requirement | Priority | Complexity |
|---|-------------|----------|------------|

### Overlapping Requirements (already in SRS)
| # | Requirement | SRS Section | Differences |
|---|-------------|-------------|-------------|

### Proposed New Tasks
| Task ID | Feature | SRS Section | Figma | Dependencies |
|---------|---------|-------------|-------|--------------|
| TASK-XXX | [name] | [new section] | [TBD] | [deps] |

### SRS Updates Needed
[What sections to add/modify in docs/SRS.md]

### Recommended Next Steps
1. [action items in order]
```

Ask: **"Should I add these tasks to `docs/tasks.md` and update `docs/SRS.md`?"**
