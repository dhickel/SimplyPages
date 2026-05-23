# Implementation Plan: Beta Security & Flexibility Fixes

This plan specifies the technical implementation for two critical findings identified during the beta-readiness review of the `simplypages` module.

## Plan Overview
- **Total Phases**: 2
- **Agents Involved**: `security_engineer` (Phase 1), `tester` (Phase 2)
- **Estimated Effort**: Low (2-3 hours)
- **Primary Goal**: Neutralize CSS injection vectors and provide configurable CSRF protection.

---

## 1. Architecture Analysis & Risk Assessment

### Issue 1: CSS Injection in `HtmlTag.addStyle`
- **Root Cause**: The current implementation of `addStyle` concatenates raw strings into the `style` attribute. While the attribute itself is HTML-escaped during render, the *content* of the style attribute can still execute CSS-based attacks (e.g., `background-image: url(...)` for data exfiltration).
- **Remediation Strategy**: 
    1. Validate CSS property names against a strict alphanumeric/hyphen regex.
    2. Escape CSS values using `org.owasp.encoder.Encode.forCssString()`.
- **Risk**: Low. Modern CSS values are robust to standard escaping, but we must ensure complex values (like calc or multi-part shadows) remain functional.

### Issue 2: Hardcoded CSRF Header in `Form`
- **Root Cause**: `Form.withHxPostCsrf` hardcodes the `X-CSRF-TOKEN` header. This prevents interoperability with backends requiring `X-XSRF-TOKEN` or custom headers.
- **Remediation Strategy**: Introduce a configurable `csrfHeaderName` field with a fluent setter.
- **Risk**: Zero. This is a backward-compatible enhancement.

---

## 2. Execution Strategy

| Phase | Objective | Agent | Mode |
|-------|-----------|-------|------|
| 1 | Implementation | security_engineer | Sequential |
| 2 | Validation | tester | Sequential |

---

## 3. Phase Details

### Phase 1: Core Implementation

#### Objective
Apply security hardening and flexibility enhancements to `HtmlTag` and `Form`.

#### Agent: `security_engineer`

#### Files to Modify
- **`simplypages/src/main/java/io/mindspice/simplypages/core/HtmlTag.java`**:
    - **Logic Change**: Update `addStyle(String property, String value)`.
    - **Implementation Detail**:
        ```java
        // 1. Validate property name
        if (!property.matches("^[a-zA-Z0-9-]+$")) {
            throw new IllegalArgumentException("Invalid CSS property name: " + property);
        }
        // 2. Escape value
        String escapedValue = org.owasp.encoder.Encode.forCssString(value);
        // 3. Append to style attribute...
        ```

- **`simplypages/src/main/java/io/mindspice/simplypages/components/forms/Form.java`**:
    - **Fields**: Add `private String csrfHeaderName = "X-CSRF-TOKEN";`.
    - **Methods**:
        - Add `public Form withCsrfHeaderName(String name)`.
        - Update `withHxPostCsrf(String url, String csrfToken)` to use `this.csrfHeaderName`.

---

### Phase 2: Validation & Regression

#### Objective
Verify that security fixes neutralize injection and flexibility works as intended.

#### Agent: `tester`

#### Files to Create
- **`simplypages/src/test/java/io/mindspice/simplypages/security/AddStyleInjectionTest.java`**:
    - Test Case 1: Attempt to inject via property name (e.g., `color: blue; } body { display: none`). Should throw `IllegalArgumentException`.
    - Test Case 2: Attempt to inject via value (e.g., `url(javascript:alert(1))`). Verify the output is escaped correctly.

#### Files to Modify
- **`simplypages/src/test/java/io/mindspice/simplypages/components/forms/FormTest.java`**:
    - Add test for `withCsrfHeaderName` verifying the `hx-headers` attribute contains the custom key.

#### Validation Criteria
- `mvn test -Dtest=AddStyleInjectionTest,FormTest` passes.
- No regressions in `simplypages/src/test/java/io/mindspice/simplypages/security/CssInjectionTest.java`.

---

## 4. Cost Summary (Estimated)

| Phase | Agent | Model | Est. Input | Est. Output | Est. Cost |
|-------|-------|-------|-----------|------------|----------|
| 1 | security_engineer | Pro | 10K | 2K | $0.18 |
| 2 | tester | Pro | 8K | 3K | $0.20 |
| **Total** | | | **18K** | **5K** | **$0.38** |

---

## 5. Execution Profile
- **Total phases**: 2
- **Parallelizable phases**: 0
- **Sequential-only phases**: 2
- **Note**: This is a high-integrity security update. Sequential execution is mandatory to ensure validation follows implementation immediately.
