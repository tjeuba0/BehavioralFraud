---
description: "Security audit of the codebase. Optionally pass a scope (e.g., 'network', 'storage', 'auth'). Checks for vulnerabilities, hardcoded secrets, insecure patterns."
---

# Security Audit: $ARGUMENTS

## Automated Scans

```bash
SCOPE="${ARGUMENTS:-all}"
echo "=== SECURITY SCAN: $SCOPE ==="
echo ""
echo "--- Hardcoded Secrets ---"
grep -rn "password\|secret\|api_key\|token\|Bearer\|private_key" app/src/main/java/ --include="*.kt" | grep -v "test\|Test\|mock\|Mock" | head -20
echo ""
echo "--- HTTP (non-HTTPS) URLs ---"
grep -rn "http://" app/src/main/java/ --include="*.kt" | grep -v "https://" | head -10
echo ""
echo "--- Logging Sensitive Data ---"
grep -rn "Log\.\|println\|Timber" app/src/main/java/ --include="*.kt" | grep -i "token\|nonce\|password\|key" | head -10
echo ""
echo "--- ProGuard Rules ---"
cat app/proguard-rules.pro 2>/dev/null | head -20
echo ""
echo "--- Network Security Config ---"
cat app/src/main/res/xml/network_security_config.xml 2>/dev/null || echo "No network security config found"
echo ""
echo "--- Permissions ---"
grep -E "uses-permission" app/src/main/AndroidManifest.xml | head -10
echo ""
echo "--- Exported Components ---"
grep -E "exported" app/src/main/AndroidManifest.xml | head -10
```

## Manual Review

Delegate to the **reviewer** sub-agent (security focus):

> Perform a security-focused review of the Máy Sạch codebase.
> Scope: ${ARGUMENTS:-entire codebase}
> 
> Check for:
> 1. Hardcoded secrets or API keys
> 2. Insecure network communication
> 3. Sensitive data in logs
> 4. Missing ProGuard rules for Retrofit/Moshi models
> 5. Improper certificate validation
> 6. Exported components without proper protection
> 7. Insecure data storage (SharedPreferences without encryption)
> 8. Missing input validation
> 9. Integrity token handling security
> 10. Nonce replay attack prevention
> 
> Produce a security report with severity levels.

## Output Format

```markdown
## 🔒 Security Audit Report

### Critical Vulnerabilities
[Must fix immediately]

### High Risk
[Should fix before release]

### Medium Risk
[Fix in next sprint]

### Low Risk / Informational
[Nice to have]

### Recommendations
[Best practices to adopt]
```
