---
description: "Create a detailed implementation plan for a task WITHOUT writing code. Use in Plan mode (shift+tab x2) to review before executing with /feature."
---

# Implementation Plan: $ARGUMENTS

## Load Task Context

```bash
TASK_ID="$ARGUMENTS"
echo "=== TASK ENTRY ==="
sed -n "/### ${TASK_ID}:/,/^### TASK-/p" docs/tasks.md | head -25
echo ""
echo "=== SRS REFERENCE ==="
SRS_REF=$(sed -n "/### ${TASK_ID}:/,/^### TASK-/p" docs/tasks.md | grep "SRS:" | sed 's/.*SRS: //')
echo "$SRS_REF"
echo ""
echo "=== FIGMA URL ==="
FIGMA=$(sed -n "/### ${TASK_ID}:/,/^### TASK-/p" docs/tasks.md | grep "Figma:" | sed 's/.*Figma: //')
echo "${FIGMA:-No Figma URL}"
echo ""
echo "=== EXISTING SCREENS ==="
find app/src/main/java -name "*Screen.kt" -type f | sort
echo ""
echo "=== EXISTING COMPONENTS ==="
find app/src/main/java -path "*/components/*.kt" -type f | sort
```

## Instructions

You are in **PLAN MODE**. Do NOT write any code. Instead:

1. Read the task entry from `docs/tasks.md`
2. Read the relevant SRS section
3. If Figma URL exists, describe what design data to fetch
4. Search codebase for related existing code
5. Produce a detailed plan:

```markdown
## Plan: [Task Name]

### Overview
[What this feature does and why]

### SRS Requirements
[Key requirements extracted from SRS section]

### Design Reference
[Figma URL and what to extract, or "No design — follow existing patterns"]

### Files to Create
| File | Purpose | Layer |
|------|---------|-------|

### Files to Modify
| File | Change | Reason |
|------|--------|--------|

### Data Flow
User action → Event → ViewModel → UseCase → Repository → API → Response → State → UI update

### Error Handling Plan
| Error Type | Detection | UI Response |
|------------|-----------|-------------|
| Timeout | SocketTimeoutException | PartialResult screen |
| Network | IOException | Error dialog + retry |
| API error | errorCode != "1" | Error message display |

### Implementation Steps (in order)
1. [Step with specific file and what to do]
2. ...

### Risks & Edge Cases
[What could go wrong]

### Estimated Effort
[S/M/L/XL with rationale]
```

After presenting the plan, ask: **"Approve this plan? If yes, run `/feature $ARGUMENTS` to execute."**
