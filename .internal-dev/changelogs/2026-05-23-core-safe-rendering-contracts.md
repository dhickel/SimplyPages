# Date

2026-05-23

# Change Summary

Implemented the first remediation domain unit for core safe rendering contracts. Core rendering now validates HTML tag names and attribute names before output, exact-matches style property replacement instead of regex substring replacement, and clears dynamic inner text slots when trusted HTML is explicitly set.

# Files

- `simplypages/src/main/java/io/mindspice/simplypages/core/Attribute.java`
- `simplypages/src/main/java/io/mindspice/simplypages/core/HtmlTag.java`
- `simplypages/src/test/java/io/mindspice/simplypages/core/HtmlTagTest.java`
- `docs/security/01-security-boundaries-and-safe-rendering.md`
- `docs/core/04-rendering-pipeline-high-and-low-level.md`

# Behavioral Impact

Valid tag and attribute names continue rendering normally. Invalid tag/attribute names now fail fast with `IllegalArgumentException`. Existing valid style output is preserved except exact-property replacement now avoids corrupting substring properties such as `width` inside `max-width`.

# Risks

- Consumers passing invalid custom tag or attribute names will now fail earlier.
- The style parser remains intentionally small and declaration-oriented; broad CSS parsing is still out of scope.

# Follow-up Items

- Continue with public URL/style contract adoption in the next domain PR.
- Add broader component/module adoption of these core safety paths.
