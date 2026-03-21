---
description: "Write and run unit tests for a specific component (ViewModel, UseCase, Repository). Pass the component name or file path."
---

# Test: $ARGUMENTS

## Load Context

```bash
echo "=== FIND TARGET ==="
find app/src/main/java -name "*$ARGUMENTS*" -type f | head -10
echo ""
echo "=== EXISTING TESTS ==="
find app/src/test/java -name "*$ARGUMENTS*" -type f | head -10
echo ""
echo "=== TEST FRAMEWORK ==="
grep -E "testImplementation|androidTestImplementation" app/build.gradle.kts | head -10
```

## Generate Tests

Delegate to the **tester** sub-agent:

> Write unit tests for: $ARGUMENTS
> 
> Target files: [found in context above]
> Existing tests: [found in context above]
> 
> Cover: happy path, error path, timeout/PartialResult, edge cases.
> Use MockK for mocking, JUnit 5 for assertions, Turbine for Flow testing.
> Run `./gradlew test` and report results.

## Results

Present the test report and ask: **"All tests passing. Run `/commit` to commit?"**
