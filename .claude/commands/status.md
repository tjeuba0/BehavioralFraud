---
description: "Show project overview: all tasks with status, git activity, build health, and test results."
---

# Project Status

```bash
echo "========================================="
echo "MÁY SẠCH — PROJECT STATUS"
echo "========================================="
echo ""

echo "=== TASK OVERVIEW ==="
echo "ID | Feature | Status"
echo "---|---------|-------"
grep -E "^### TASK-" docs/tasks.md | while read line; do
    TASK_ID=$(echo "$line" | sed 's/### \(TASK-[0-9]*\):.*/\1/')
    TASK_NAME=$(echo "$line" | sed 's/### TASK-[0-9]*: //')
    STATUS=$(sed -n "/### ${TASK_ID}:/,/^### TASK-/p" docs/tasks.md | grep "Status:" | sed 's/.*Status: //')
    echo "$TASK_ID | $TASK_NAME | $STATUS"
done
echo ""

echo "=== GIT STATUS ==="
echo "Branch: $(git branch --show-current)"
echo "Uncommitted changes: $(git status --short | wc -l)"
echo ""
echo "Recent commits:"
git log --oneline -5
echo ""

echo "=== BUILD HEALTH ==="
if [ -f gradlew ]; then
    ./gradlew assembleDebug --quiet 2>&1 | tail -3
else
    echo "No Gradle wrapper found"
fi
echo ""

echo "=== TEST HEALTH ==="
if [ -f gradlew ]; then
    ./gradlew test --quiet 2>&1 | tail -5
else
    echo "No Gradle wrapper found"
fi
echo ""

echo "=== CODE STATS ==="
echo "Kotlin files: $(find app/src/main/java -name '*.kt' | wc -l)"
echo "Test files: $(find app/src/test/java -name '*.kt' 2>/dev/null | wc -l)"
echo "Lines of code: $(find app/src/main/java -name '*.kt' -exec cat {} + | wc -l)"
```

Present the results in a clean, readable format. Highlight:
- Tasks that are `in-progress` (currently being worked on)
- Tasks that are `planned` (next up)
- Any build or test failures
- Uncommitted changes that need attention
