---
description: "Import Figma design and produce UI implementation spec. Pass a TASK-ID (reads URL from docs/tasks.md) or a direct Figma URL."
---

# Figma Import: $ARGUMENTS

## Resolve Figma URL

```bash
INPUT="$ARGUMENTS"
if [[ "$INPUT" =~ ^TASK- ]]; then
    echo "=== RESOLVING FROM TASK REGISTRY ==="
    FIGMA_URL=$(sed -n "/### ${INPUT}:/,/^### TASK-/p" docs/tasks.md | grep "Figma:" | sed 's/.*Figma: //')
    TASK_NAME=$(sed -n "/### ${INPUT}:/p" docs/tasks.md | sed 's/### [^:]*: //')
    echo "Task: $TASK_NAME"
    echo "Figma URL: ${FIGMA_URL:-NOT FOUND}"
    echo ""
    echo "=== SRS REFERENCE ==="
    SRS_REF=$(sed -n "/### ${INPUT}:/,/^### TASK-/p" docs/tasks.md | grep "SRS:" | sed 's/.*SRS: //')
    echo "$SRS_REF"
elif [[ "$INPUT" =~ figma\.com ]]; then
    echo "=== DIRECT URL ==="
    FIGMA_URL="$INPUT"
    echo "Figma URL: $FIGMA_URL"
else
    echo "ERROR: Pass a TASK-ID (e.g., TASK-006) or a Figma URL"
    exit 1
fi
echo ""
echo "=== EXISTING COMPONENTS ==="
find app/src/main/java -path "*/components/*.kt" -type f | sort
echo ""
echo "=== EXISTING THEME ==="
find app/src/main/java -path "*/theme/*.kt" -type f | sort
```

If no Figma URL found, STOP and ask user to add the URL to `docs/tasks.md`.

## Import Design

Delegate to the **designer** sub-agent:

> Import the Figma design and produce a UI implementation spec.
> 
> Figma URL: [resolved URL]
> Context: [task name and SRS reference if from task registry]
> 
> 1. Use Figma MCP tools to fetch the design
> 2. Extract design tokens (colors, typography, spacing)
> 3. Map layout to Jetpack Compose structure
> 4. Check existing components for reuse
> 5. Produce a complete UI implementation spec

## Output

Present the UI spec and ask: **"Ready to implement? Run `/feature $ARGUMENTS` for full workflow, or delegate to coder for implementation only."**
