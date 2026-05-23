# Implementation Plan: Beta Security & Flexibility Fixes

This plan addresses two specific findings from the milestone code review for the `simplypages/` module.

## Plan Overview
- **Total Phases**: 1
- **Agents Involved**: `security_engineer`
- **Estimated Effort**: Low

## Execution Strategy

| Phase | Objective | Agent | Mode |
|-------|-----------|-------|------|
| 1 | Security & Flexibility Fixes | security_engineer | Sequential |

---

## Phase 1: Security & Flexibility Fixes (Major/Minor)

### Objective
Address CSS injection in `HtmlTag.addStyle` and hardcoded CSRF header in `Form`.

### Agent: `security_engineer`

### Files to Modify
- `simplypages/src/main/java/io/mindspice/simplypages/core/HtmlTag.java`:
    - Update `addStyle(String property, String value)` to validate/escape `value` using `org.owasp.encoder.Encode.forCssString()`.
- `simplypages/src/main/java/io/mindspice/simplypages/components/forms/Form.java`:
    - Add configurable `csrfHeaderName` field (defaulting to "X-CSRF-TOKEN").
    - Update `withHxPostCsrf` to use the configured header name.

### Validation Criteria
- Run `io.mindspice.simplypages.security.CssInjectionTest`.
- Add test case for custom CSRF header names in `FormTest`.
