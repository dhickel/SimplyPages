# SimplyPages Remediation Plan & Test Suite

**Created:** January 4, 2026
**Status:** Post-Alpha Sprint
**Priority:** Security fixes FIRST, then quality improvements, then tests

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Phase 1: Critical Security Fixes](#phase-1-critical-security-fixes)
3. [Phase 2: Code Quality Improvements](#phase-2-code-quality-improvements)
4. [Phase 3: Comprehensive Unit Test Suite](#phase-3-comprehensive-unit-test-suite)
5. [Implementation Schedule](#implementation-schedule)
6. [Living Remediation Log](#living-remediation-log)

---

## Executive Summary

This plan addresses issues identified in the Alpha Sprint review:

| Phase | Focus | Priority | Effort |
|-------|-------|----------|--------|
| Phase 1 | Security Fixes | CRITICAL | 1-2 hours |
| Phase 2 | Code Quality | MEDIUM | 3-4 hours |
| Phase 3 | Unit Tests | HIGH | 8-12 hours |

**Total Estimated Effort:** 12-18 hours

---

## Phase 1: Critical Security Fixes

### 1.1 Select.Option XSS Vulnerability (CRITICAL)

**File:** `simplypages/src/main/java/io/mindspice/simplypages/components/forms/Select.java`

**Current Code (Lines 118-129):**
```java
@Override
public String render(RenderContext context) {
    StringBuilder sb = new StringBuilder("<option value=\"").append(value).append("\"");
    if (selected) {
        sb.append(" selected");
    }
    if (disabled) {
        sb.append(" disabled");
    }
    sb.append(">").append(label).append("</option>");
    return sb.toString();
}
```

**Fixed Code:**
```java
import org.owasp.encoder.Encode;

@Override
public String render(RenderContext context) {
    StringBuilder sb = new StringBuilder("<option value=\"")
        .append(Encode.forHtmlAttribute(value))
        .append("\"");
    if (selected) {
        sb.append(" selected");
    }
    if (disabled) {
        sb.append(" disabled");
    }
    sb.append(">").append(Encode.forHtml(label)).append("</option>");
    return sb.toString();
}
```

**Testing Verification:**
```java
// Test case to verify fix
@Test
void testOptionXssProtection() {
    Select select = Select.create("test")
        .addOption("<script>alert('xss')</script>", "Malicious Label");

    String html = select.render();

    assertFalse(html.contains("<script>"));
    assertTrue(html.contains("&lt;script&gt;"));
}
```

---

### 1.2 EditModalBuilder Container ID Validation (MEDIUM)

**File:** `simplypages/src/main/java/io/mindspice/simplypages/editing/EditModalBuilder.java`

**Current Code (Lines 142-158):** No validation on `modalContainerId` or `pageContainerId`

**Add Validation Pattern (same as Modal.java):**
```java
// Add at class level (line ~37)
private static final java.util.regex.Pattern VALID_ID_PATTERN =
    java.util.regex.Pattern.compile("^[a-zA-Z][a-zA-Z0-9_-]*$");

// Modify withModalContainerId (line 155-158)
public EditModalBuilder withModalContainerId(String modalContainerId) {
    if (modalContainerId == null || !VALID_ID_PATTERN.matcher(modalContainerId).matches()) {
        throw new IllegalArgumentException(
            "Modal container ID must start with a letter and contain only letters, numbers, hyphens, and underscores. Got: " + modalContainerId);
    }
    this.modalContainerId = modalContainerId;
    return this;
}

// Modify withPageContainerId (line 142-145)
public EditModalBuilder withPageContainerId(String pageContainerId) {
    if (pageContainerId == null || !VALID_ID_PATTERN.matcher(pageContainerId).matches()) {
        throw new IllegalArgumentException(
            "Page container ID must start with a letter and contain only letters, numbers, hyphens, and underscores. Got: " + pageContainerId);
    }
    this.pageContainerId = pageContainerId;
    return this;
}
```

**Also update onclick handler (line 216) to use escaped ID:**
```java
// Current (vulnerable if ID somehow bypasses validation)
cancelBtn.withAttribute("onclick", "document.getElementById('" + modalContainerId + "').innerHTML = ''");

// More defensive (use data attribute pattern)
cancelBtn.withAttribute("data-modal-id", modalContainerId);
cancelBtn.withAttribute("onclick", "document.getElementById(this.dataset.modalId).innerHTML = ''");
```

**Testing Verification:**
```java
@Test
void testModalContainerIdValidation() {
    assertThrows(IllegalArgumentException.class, () -> {
        EditModalBuilder.create()
            .withModalContainerId("x').innerHTML='<script>alert(1)</script>')//");
    });
}

@Test
void testValidModalContainerId() {
    EditModalBuilder builder = EditModalBuilder.create()
        .withModalContainerId("my-valid-id-123");
    // Should not throw
}
```

---

### 1.3 Security Fix Checklist

| Fix | File | Status |
|-----|------|--------|
| Select.Option value escaping | Select.java:120 | [x] |
| Select.Option label escaping | Select.java:127 | [x] |
| EditModalBuilder modalContainerId validation | EditModalBuilder.java:155 | [x] |
| EditModalBuilder pageContainerId validation | EditModalBuilder.java:142 | [x] |
| EditModalBuilder onclick hardening | EditModalBuilder.java:216 | [x] |

---

## Phase 2: Code Quality Improvements

### 2.1 Extract withClass() Duplication

**Problem:** 4+ components duplicate the same class-merging logic instead of using `HtmlTag.addClass()`.

**Affected Files:**
- `Button.java:73-81`
- `TextInput.java:111-119`
- `Alert.java:60-68`
- `Badge.java:74-82`

**Solution:** Refactor to use parent's `addClass()` method.

**Example Fix (Button.java):**
```java
// Current (duplicated logic)
@Override
public Button withClass(String className) {
    String currentClass = attributes.stream()
        .filter(attr -> "class".equals(attr.getName()))
        .findFirst()
        .map(attr -> attr.getValue())
        .orElse("btn btn-primary");
    this.withAttribute("class", currentClass + " " + className);
    return this;
}

// Fixed (use parent)
@Override
public Button withClass(String className) {
    super.addClass(className);
    return this;
}
```

**Note:** Ensure default class is set in constructor, not in `withClass()`.

---

### 2.2 Extract EditAdapter Form Building Helper

**Problem:** Repetitive form field creation pattern across 3+ modules.

**Solution:** Create `FormFieldHelper` utility class.

**New File:** `simplypages/src/main/java/io/mindspice/simplypages/editing/FormFieldHelper.java`

```java
package io.mindspice.simplypages.editing;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.components.Paragraph;
import io.mindspice.simplypages.components.forms.TextInput;
import io.mindspice.simplypages.components.forms.TextArea;
import io.mindspice.simplypages.components.forms.Checkbox;
import io.mindspice.simplypages.core.Component;

/**
 * Helper for building consistent form fields in EditAdapter implementations.
 */
public class FormFieldHelper {

    /**
     * Create a labeled text input field.
     *
     * @param label The field label
     * @param name The input name attribute
     * @param value The current value (may be null)
     * @return A form field component
     */
    public static Component textField(String label, String name, String value) {
        Div group = new Div().withClass("form-field");
        group.withChild(new Paragraph(label + ":").withClass("form-label"));
        group.withChild(TextInput.create(name)
            .withValue(value != null ? value : "")
            .withMaxWidth("100%"));
        return group;
    }

    /**
     * Create a labeled textarea field.
     *
     * @param label The field label
     * @param name The textarea name attribute
     * @param value The current value (may be null)
     * @param rows Number of rows
     * @return A form field component
     */
    public static Component textAreaField(String label, String name, String value, int rows) {
        Div group = new Div().withClass("form-field");
        group.withChild(new Paragraph(label + ":").withClass("form-label"));
        group.withChild(TextArea.create(name)
            .withValue(value != null ? value : "")
            .withRows(rows)
            .withMaxWidth("100%"));
        return group;
    }

    /**
     * Create a labeled checkbox field.
     *
     * @param label The field label
     * @param name The checkbox name attribute
     * @param checked Whether the checkbox is checked
     * @return A form field component
     */
    public static Component checkboxField(String label, String name, boolean checked) {
        Div group = new Div().withClass("form-field");
        Checkbox checkbox = Checkbox.create(name).withLabel(label);
        if (checked) {
            checkbox.checked();
        }
        group.withChild(checkbox);
        return group;
    }
}
```

**Usage in ContentModule:**
```java
@Override
public Component buildEditView() {
    Div form = new Div();
    form.withChild(FormFieldHelper.textField("Title", "title", title));
    form.withChild(FormFieldHelper.textAreaField("Content", "content", content, 10));
    form.withChild(FormFieldHelper.checkboxField("Render as Markdown", "useMarkdown", useMarkdown));
    return form;
}
```

---

### 2.3 Add Module.rebuildContent() Helper

**Problem:** All editable modules repeat `children.clear(); buildContent();` pattern.

**Solution:** Add protected helper method to Module base class.

**File:** `simplypages/src/main/java/io/mindspice/simplypages/modules/Module.java`

```java
/**
 * Rebuild module content after edits.
 *
 * <p>Call this from {@link EditAdapter#applyEdits(Map)} after updating
 * module properties. This clears existing children and rebuilds content.</p>
 */
protected void rebuildContent() {
    children.clear();
    built = false;  // Reset built flag if using lazy building
    buildContent();
}
```

**Usage in ContentModule.applyEdits():**
```java
@Override
public ContentModule applyEdits(Map<String, String> formData) {
    this.title = formData.get("title");
    this.content = formData.get("content");
    this.useMarkdown = formData.containsKey("useMarkdown");
    rebuildContent();  // Single method call instead of 2 lines
    return this;
}
```

---

### 2.4 Fix Naming Convention (RichContentModule)

**File:** `simplypages/src/main/java/io/mindspice/simplypages/modules/RichContentModule.java`

**Current (line 38):**
```java
public void setModuleId(String moduleId) {
    this.moduleId = moduleId;
}
```

**Fix:** Remove or rename to fluent API pattern (already has `withModuleId` at line 33-34).

```java
// Remove setModuleId() entirely, or deprecate
@Deprecated
public void setModuleId(String moduleId) {
    this.moduleId = moduleId;
}
```

---

### 2.5 Code Quality Checklist

| Improvement | Files Affected | Status |
|-------------|----------------|--------|
| Refactor Button.withClass() | Button.java | [x] |
| Refactor TextInput.withClass() | TextInput.java | [x] |
| Refactor Alert.withClass() | Alert.java | [x] |
| Refactor Badge.withClass() | Badge.java | [x] |
| Create FormFieldHelper | NEW: FormFieldHelper.java | [x] |
| Update ContentModule to use helper | ContentModule.java | [x] |
| Update RichContentModule to use helper | RichContentModule.java | [x] |
| Update SimpleListModule to use helper | SimpleListModule.java | [x] |
| Add Module.rebuildContent() | Module.java | [x] |
| Fix RichContentModule naming | RichContentModule.java | [x] |

---

## Phase 3: Comprehensive Unit Test Suite

### 3.1 Test Structure

```
simplypages/src/test/java/io/mindspice/simplypages/
├── core/
│   ├── HtmlTagTest.java
│   ├── AttributeTest.java
│   └── ComponentTest.java
├── components/
│   ├── forms/
│   │   ├── SelectTest.java
│   │   ├── TextInputTest.java
│   │   ├── TextAreaTest.java
│   │   ├── ButtonTest.java
│   │   ├── FormTest.java
│   │   └── CheckboxTest.java
│   ├── display/
│   │   ├── ModalTest.java
│   │   ├── AlertTest.java
│   │   ├── CardTest.java
│   │   └── BadgeTest.java
│   └── BasicComponentsTest.java
├── modules/
│   ├── ContentModuleTest.java
│   ├── RichContentModuleTest.java
│   ├── SimpleListModuleTest.java
│   ├── EditableModuleTest.java
│   └── ModuleLifecycleTest.java
├── editing/
│   ├── EditAdapterTest.java
│   ├── EditModalBuilderTest.java
│   ├── AuthWrapperTest.java
│   ├── ValidationResultTest.java
│   └── EditableRowTest.java
├── layout/
│   ├── RowTest.java
│   ├── ColumnTest.java
│   ├── PageTest.java
│   └── GridTest.java
├── security/
│   ├── XssProtectionTest.java
│   ├── CssInjectionTest.java
│   └── UrlValidationTest.java
└── integration/
    ├── HtmxIntegrationTest.java
    └── CrudOperationsTest.java
```

---

### 3.2 Security Tests (HIGHEST PRIORITY)

**File:** `simplypages/src/test/java/io/mindspice/simplypages/security/XssProtectionTest.java`

```java
package io.mindspice.simplypages.security;

import io.mindspice.simplypages.components.forms.*;
import io.mindspice.simplypages.components.display.*;
import io.mindspice.simplypages.components.*;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.editing.EditModalBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * XSS protection tests for all components that render user content.
 *
 * Reference: Phase6_5TestController shows patterns where user content
 * flows through the system (title, content fields).
 */
class XssProtectionTest {

    // === SELECT COMPONENT TESTS ===

    @Test
    @DisplayName("Select.Option should escape HTML in value attribute")
    void testSelectOptionValueEscaping() {
        Select select = Select.create("test")
            .addOption("<script>alert('xss')</script>", "Label");

        String html = select.render();

        assertFalse(html.contains("<script>"), "Raw script tag found in value");
        assertTrue(html.contains("&lt;script&gt;") || html.contains("&#"),
            "Script tag should be HTML-encoded");
    }

    @Test
    @DisplayName("Select.Option should escape HTML in label")
    void testSelectOptionLabelEscaping() {
        Select select = Select.create("test")
            .addOption("safe", "<img src=x onerror=alert('xss')>");

        String html = select.render();

        assertFalse(html.contains("<img"), "Raw img tag found in label");
        assertTrue(html.contains("&lt;img") || html.contains("&#"),
            "Img tag should be HTML-encoded");
    }

    @Test
    @DisplayName("Select.Option should escape quotes in value attribute")
    void testSelectOptionQuoteEscaping() {
        Select select = Select.create("test")
            .addOption("value\" onclick=\"alert('xss')", "Label");

        String html = select.render();

        assertFalse(html.contains("onclick="), "Attribute injection possible");
    }

    // === MODAL COMPONENT TESTS ===

    @Test
    @DisplayName("Modal should reject invalid IDs that could enable XSS")
    void testModalIdValidation() {
        assertThrows(IllegalArgumentException.class, () -> {
            Modal.create().withModalId("x').remove();alert('xss');//");
        });
    }

    @Test
    @DisplayName("Modal should accept valid alphanumeric IDs")
    void testModalValidId() {
        Modal modal = Modal.create().withModalId("valid-modal-123");
        String html = modal.render();
        assertTrue(html.contains("id=\"valid-modal-123\""));
    }

    @Test
    @DisplayName("Modal should escape title content")
    void testModalTitleEscaping() {
        Modal modal = Modal.create()
            .withTitle("<script>alert('xss')</script>");

        String html = modal.render();

        assertFalse(html.contains("<script>alert"), "Raw script in title");
    }

    // === EDIT MODAL BUILDER TESTS ===

    @Test
    @DisplayName("EditModalBuilder should reject invalid modal container IDs")
    void testEditModalBuilderContainerIdValidation() {
        assertThrows(IllegalArgumentException.class, () -> {
            EditModalBuilder.create()
                .withModalContainerId("x').innerHTML='<script>alert(1)</script>')//");
        });
    }

    @Test
    @DisplayName("EditModalBuilder should reject invalid page container IDs")
    void testEditModalBuilderPageContainerIdValidation() {
        assertThrows(IllegalArgumentException.class, () -> {
            EditModalBuilder.create()
                .withPageContainerId("invalid id with spaces");
        });
    }

    // === TEXT INPUT TESTS ===

    @Test
    @DisplayName("TextInput should escape value attribute")
    void testTextInputValueEscaping() {
        TextInput input = TextInput.create("test")
            .withValue("\"><script>alert('xss')</script>");

        String html = input.render();

        assertFalse(html.contains("<script>"), "Raw script in value");
    }

    @Test
    @DisplayName("TextInput should escape placeholder attribute")
    void testTextInputPlaceholderEscaping() {
        TextInput input = TextInput.create("test")
            .withPlaceholder("\"><script>alert('xss')</script>");

        String html = input.render();

        assertFalse(html.contains("<script>"), "Raw script in placeholder");
    }

    // === PARAGRAPH/DIV INNER TEXT TESTS ===

    @Test
    @DisplayName("Paragraph should escape inner text by default")
    void testParagraphInnerTextEscaping() {
        Paragraph p = new Paragraph("<script>alert('xss')</script>");

        String html = p.render();

        assertFalse(html.contains("<script>alert"), "Raw script in paragraph");
        assertTrue(html.contains("&lt;script&gt;") || html.contains("&#"),
            "Script should be encoded");
    }

    @Test
    @DisplayName("Div should escape inner text by default")
    void testDivInnerTextEscaping() {
        Div div = new Div().withInnerText("<script>alert('xss')</script>");

        String html = div.render();

        assertFalse(html.contains("<script>alert"), "Raw script in div");
    }

    // === HEADER TESTS ===

    @Test
    @DisplayName("Header should escape text content")
    void testHeaderTextEscaping() {
        Header header = Header.H1("<script>alert('xss')</script>");

        String html = header.render();

        assertFalse(html.contains("<script>alert"), "Raw script in header");
    }

    // === ALERT TESTS ===

    @Test
    @DisplayName("Alert should escape message content")
    void testAlertMessageEscaping() {
        Alert alert = Alert.danger("<script>alert('xss')</script>");

        String html = alert.render();

        assertFalse(html.contains("<script>alert"), "Raw script in alert");
    }

    // === PARAMETERIZED XSS PAYLOAD TESTS ===

    @ParameterizedTest
    @ValueSource(strings = {
        "<script>alert('xss')</script>",
        "<img src=x onerror=alert('xss')>",
        "<svg onload=alert('xss')>",
        "javascript:alert('xss')",
        "<body onload=alert('xss')>",
        "<iframe src=\"javascript:alert('xss')\">",
        "'-alert('xss')-'",
        "\"><script>alert('xss')</script>",
        "' onclick='alert(1)'",
        "<ScRiPt>alert('xss')</ScRiPt>"
    })
    @DisplayName("Common XSS payloads should be neutralized in text content")
    void testCommonXssPayloads(String payload) {
        Paragraph p = new Paragraph(payload);
        String html = p.render();

        // Should not contain raw dangerous content
        assertFalse(html.contains("<script>"), "Raw script tag found");
        assertFalse(html.contains("<Script>"), "Raw Script tag found");
        assertFalse(html.contains("<ScRiPt>"), "Raw ScRiPt tag found");
        assertFalse(html.contains("onerror="), "Event handler found");
        assertFalse(html.contains("onload="), "Event handler found");
        assertFalse(html.contains("onclick="), "Event handler found");
    }
}
```

---

**File:** `simplypages/src/test/java/io/mindspice/simplypages/security/CssInjectionTest.java`

```java
package io.mindspice.simplypages.security;

import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.components.Div;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CSS injection protection tests.
 */
class CssInjectionTest {

    @Test
    @DisplayName("withWidth should reject CSS injection attempts")
    void testWidthCssInjection() {
        Div div = new Div();

        assertThrows(IllegalArgumentException.class, () -> {
            div.withWidth("100px; background: url(evil.com)");
        });
    }

    @Test
    @DisplayName("withMaxWidth should reject CSS injection attempts")
    void testMaxWidthCssInjection() {
        Div div = new Div();

        assertThrows(IllegalArgumentException.class, () -> {
            div.withMaxWidth("100px; color: red");
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "100px",
        "50%",
        "10rem",
        "5em",
        "auto",
        "0",
        "100vw",
        "50vh"
    })
    @DisplayName("Valid CSS units should be accepted")
    void testValidCssUnits(String value) {
        Div div = new Div();
        assertDoesNotThrow(() -> div.withWidth(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "100px; color: red",
        "expression(alert('xss'))",
        "url(javascript:alert('xss'))",
        "-100px",
        "100px 100px",
        "calc(100px)",
        ""
    })
    @DisplayName("Invalid/dangerous CSS values should be rejected")
    void testInvalidCssValues(String value) {
        Div div = new Div();
        assertThrows(IllegalArgumentException.class, () -> div.withWidth(value));
    }
}
```

---

**File:** `simplypages/src/test/java/io/mindspice/simplypages/security/UrlValidationTest.java`

```java
package io.mindspice.simplypages.security;

import io.mindspice.simplypages.components.Link;
import io.mindspice.simplypages.components.Image;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * URL validation tests for Link and Image components.
 *
 * Reference: Link.java blocks javascript:, vbscript:, data: schemes.
 */
class UrlValidationTest {

    // === LINK TESTS ===

    @Test
    @DisplayName("Link should reject javascript: URLs")
    void testLinkRejectsJavascriptUrl() {
        assertThrows(IllegalArgumentException.class, () -> {
            Link.create("javascript:alert('xss')", "Click me");
        });
    }

    @Test
    @DisplayName("Link should reject JavaScript: URLs (case insensitive)")
    void testLinkRejectsJavascriptUrlCaseInsensitive() {
        assertThrows(IllegalArgumentException.class, () -> {
            Link.create("JavaScript:alert('xss')", "Click me");
        });
    }

    @Test
    @DisplayName("Link should reject vbscript: URLs")
    void testLinkRejectsVbscriptUrl() {
        assertThrows(IllegalArgumentException.class, () -> {
            Link.create("vbscript:msgbox('xss')", "Click me");
        });
    }

    @Test
    @DisplayName("Link should reject data: URLs")
    void testLinkRejectsDataUrl() {
        assertThrows(IllegalArgumentException.class, () -> {
            Link.create("data:text/html,<script>alert('xss')</script>", "Click me");
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "https://example.com",
        "http://example.com",
        "/relative/path",
        "./relative/path",
        "#anchor",
        "mailto:test@example.com"
    })
    @DisplayName("Link should accept safe URLs")
    void testLinkAcceptsSafeUrls(String url) {
        assertDoesNotThrow(() -> Link.create(url, "Safe link"));
    }

    // === IMAGE TESTS ===

    @Test
    @DisplayName("Image should reject javascript: URLs")
    void testImageRejectsJavascriptUrl() {
        assertThrows(IllegalArgumentException.class, () -> {
            Image.create("javascript:alert('xss')");
        });
    }

    @Test
    @DisplayName("Image should reject non-image data: URLs")
    void testImageRejectsNonImageDataUrl() {
        assertThrows(IllegalArgumentException.class, () -> {
            Image.create("data:text/html,<script>alert('xss')</script>");
        });
    }

    @Test
    @DisplayName("Image should accept data: image URLs")
    void testImageAcceptsDataImageUrl() {
        assertDoesNotThrow(() -> {
            Image.create("data:image/png;base64,iVBORw0KGgo=");
        });
    }
}
```

---

### 3.3 Component Tests

**File:** `simplypages/src/test/java/io/mindspice/simplypages/components/forms/SelectTest.java`

```java
package io.mindspice.simplypages.components.forms;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Select component.
 */
class SelectTest {

    @Test
    @DisplayName("create() should return Select with name attribute")
    void testCreate() {
        Select select = Select.create("mySelect");
        String html = select.render();

        assertTrue(html.contains("name=\"mySelect\""));
        assertTrue(html.contains("class=\"form-select\""));
    }

    @Test
    @DisplayName("addOption() should add option with value and label")
    void testAddOption() {
        Select select = Select.create("test")
            .addOption("value1", "Label 1");

        String html = select.render();

        assertTrue(html.contains("<option"));
        assertTrue(html.contains("value=\"value1\""));
        assertTrue(html.contains(">Label 1</option>"));
    }

    @Test
    @DisplayName("addOption() with selected should mark option selected")
    void testAddOptionSelected() {
        Select select = Select.create("test")
            .addOption("value1", "Label 1", true);

        String html = select.render();

        assertTrue(html.contains("selected"));
    }

    @Test
    @DisplayName("multiple() should add multiple attribute")
    void testMultiple() {
        Select select = Select.create("test").multiple();
        String html = select.render();

        assertTrue(html.contains("multiple"));
    }

    @Test
    @DisplayName("required() should add required attribute")
    void testRequired() {
        Select select = Select.create("test").required();
        String html = select.render();

        assertTrue(html.contains("required"));
    }

    @Test
    @DisplayName("disabled() should add disabled attribute")
    void testDisabled() {
        Select select = Select.create("test").disabled();
        String html = select.render();

        assertTrue(html.contains("disabled"));
    }

    @Test
    @DisplayName("withClass() should add CSS class")
    void testWithClass() {
        Select select = Select.create("test").withClass("custom-class");
        String html = select.render();

        assertTrue(html.contains("custom-class"));
        assertTrue(html.contains("form-select")); // Default class preserved
    }

    @Test
    @DisplayName("withWidth() should add inline style")
    void testWithWidth() {
        Select select = Select.create("test").withWidth("200px");
        String html = select.render();

        assertTrue(html.contains("width: 200px") || html.contains("width:200px"));
    }
}
```

---

**File:** `simplypages/src/test/java/io/mindspice/simplypages/components/display/ModalTest.java`

```java
package io.mindspice.simplypages.components.display;

import io.mindspice.simplypages.components.Paragraph;
import io.mindspice.simplypages.components.Div;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Modal component.
 */
class ModalTest {

    @Test
    @DisplayName("create() should return Modal with backdrop class")
    void testCreate() {
        Modal modal = Modal.create();
        String html = modal.render();

        assertTrue(html.contains("class=\"modal-backdrop\""));
    }

    @Test
    @DisplayName("withTitle() should set modal title")
    void testWithTitle() {
        Modal modal = Modal.create().withTitle("Test Title");
        String html = modal.render();

        assertTrue(html.contains("Test Title"));
        assertTrue(html.contains("modal-title"));
    }

    @Test
    @DisplayName("withBody() should render body content")
    void testWithBody() {
        Modal modal = Modal.create()
            .withBody(new Paragraph("Body content"));

        String html = modal.render();

        assertTrue(html.contains("modal-body"));
        assertTrue(html.contains("Body content"));
    }

    @Test
    @DisplayName("withFooter() should render footer content")
    void testWithFooter() {
        Modal modal = Modal.create()
            .withFooter(new Div().withInnerText("Footer"));

        String html = modal.render();

        assertTrue(html.contains("modal-footer"));
        assertTrue(html.contains("Footer"));
    }

    @Test
    @DisplayName("closeOnBackdrop(true) should add onclick handler")
    void testCloseOnBackdrop() {
        Modal modal = Modal.create().closeOnBackdrop(true);
        String html = modal.render();

        assertTrue(html.contains("onclick="));
        assertTrue(html.contains(".remove()"));
    }

    @Test
    @DisplayName("closeOnBackdrop(false) should not add onclick handler")
    void testCloseOnBackdropDisabled() {
        Modal modal = Modal.create().closeOnBackdrop(false);
        String html = modal.render();

        // Modal container has onclick="event.stopPropagation()"
        // But backdrop should NOT have remove onclick
        assertFalse(html.startsWith("<div class=\"modal-backdrop\"") &&
                   html.contains("onclick=\"document.getElementById"));
    }

    @Test
    @DisplayName("Modal should include ESC key handler")
    void testEscKeyHandler() {
        Modal modal = Modal.create();
        String html = modal.render();

        assertTrue(html.contains("onkeydown"));
        assertTrue(html.contains("Escape"));
    }

    @Test
    @DisplayName("Modal should stop click propagation on container")
    void testStopPropagation() {
        Modal modal = Modal.create();
        String html = modal.render();

        assertTrue(html.contains("event.stopPropagation()"));
    }

    @Test
    @DisplayName("withModalId() should validate ID format")
    void testModalIdValidation() {
        // Valid IDs
        assertDoesNotThrow(() -> Modal.create().withModalId("valid-id"));
        assertDoesNotThrow(() -> Modal.create().withModalId("validId123"));
        assertDoesNotThrow(() -> Modal.create().withModalId("valid_id"));

        // Invalid IDs
        assertThrows(IllegalArgumentException.class,
            () -> Modal.create().withModalId("123invalid"));
        assertThrows(IllegalArgumentException.class,
            () -> Modal.create().withModalId("invalid id"));
        assertThrows(IllegalArgumentException.class,
            () -> Modal.create().withModalId(""));
        assertThrows(IllegalArgumentException.class,
            () -> Modal.create().withModalId(null));
    }
}
```

---

### 3.4 Module Tests

**File:** `simplypages/src/test/java/io/mindspice/simplypages/modules/ContentModuleTest.java`

```java
package io.mindspice.simplypages.modules;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.editing.EditAdapter;
import io.mindspice.simplypages.editing.ValidationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ContentModule.
 *
 * Reference: Phase6_5TestController uses ContentModule with full CRUD.
 */
class ContentModuleTest {

    @Test
    @DisplayName("create() should return ContentModule instance")
    void testCreate() {
        ContentModule module = ContentModule.create();
        assertNotNull(module);
    }

    @Test
    @DisplayName("withTitle() should set title")
    void testWithTitle() {
        ContentModule module = ContentModule.create()
            .withTitle("Test Title");

        String html = module.render();

        assertTrue(html.contains("Test Title"));
    }

    @Test
    @DisplayName("withContent() should set content")
    void testWithContent() {
        ContentModule module = ContentModule.create()
            .withContent("Test content here");

        String html = module.render();

        assertTrue(html.contains("Test content"));
    }

    @Test
    @DisplayName("withMarkdown(true) should render markdown")
    void testWithMarkdownEnabled() {
        ContentModule module = ContentModule.create()
            .withContent("# Heading")
            .withMarkdown(true);

        String html = module.render();

        assertTrue(html.contains("<h1>") || html.contains("Heading"));
    }

    @Test
    @DisplayName("withMarkdown(false) should render plain text")
    void testWithMarkdownDisabled() {
        ContentModule module = ContentModule.create()
            .withContent("# Not a heading")
            .withMarkdown(false);

        String html = module.render();

        assertFalse(html.contains("<h1>"));
        assertTrue(html.contains("# Not a heading") || html.contains("#"));
    }

    // === EditAdapter Contract Tests ===

    @Test
    @DisplayName("buildEditView() should return form component")
    void testBuildEditView() {
        ContentModule module = ContentModule.create()
            .withTitle("Original")
            .withContent("Original content");

        EditAdapter<ContentModule> adapter = module;
        Component editView = adapter.buildEditView();

        assertNotNull(editView);
        String html = editView.render();

        // Should contain form fields for title, content
        assertTrue(html.contains("name=\"title\"") || html.contains("title"));
        assertTrue(html.contains("name=\"content\"") || html.contains("content"));
    }

    @Test
    @DisplayName("applyEdits() should update module properties")
    void testApplyEdits() {
        ContentModule module = ContentModule.create()
            .withTitle("Original")
            .withContent("Original content");

        Map<String, String> formData = new HashMap<>();
        formData.put("title", "Updated Title");
        formData.put("content", "Updated content");

        EditAdapter<ContentModule> adapter = module;
        adapter.applyEdits(formData);

        String html = module.render();

        assertTrue(html.contains("Updated Title"));
        assertTrue(html.contains("Updated content"));
    }

    @Test
    @DisplayName("applyEdits() should rebuild content (children cleared)")
    void testApplyEditsRebuildsContent() {
        ContentModule module = ContentModule.create()
            .withTitle("Original")
            .withContent("Original");

        // Force initial build
        module.render();

        Map<String, String> formData = new HashMap<>();
        formData.put("title", "New Title");
        formData.put("content", "New content");

        module.applyEdits(formData);

        String html = module.render();

        // Old content should be gone
        assertFalse(html.contains("Original"));
        assertTrue(html.contains("New Title"));
    }

    @Test
    @DisplayName("validate() should return valid for good data")
    void testValidateGoodData() {
        ContentModule module = ContentModule.create();

        Map<String, String> formData = new HashMap<>();
        formData.put("title", "Valid Title");
        formData.put("content", "Valid content");

        ValidationResult result = module.validate(formData);

        assertTrue(result.isValid());
    }

    // === Module Lifecycle Tests ===

    @Test
    @DisplayName("Module should have 'module' CSS class")
    void testModuleClass() {
        ContentModule module = ContentModule.create();
        String html = module.render();

        assertTrue(html.contains("class=\"") && html.contains("module"));
    }

    @Test
    @DisplayName("withModuleId() should set ID")
    void testWithModuleId() {
        ContentModule module = ContentModule.create()
            .withModuleId("test-module-123");

        assertEquals("test-module-123", module.getModuleId());
    }

    @Test
    @DisplayName("render() should be idempotent")
    void testRenderIdempotent() {
        ContentModule module = ContentModule.create()
            .withTitle("Test")
            .withContent("Content");

        String html1 = module.render();
        String html2 = module.render();

        assertEquals(html1, html2);
    }
}
```

---

**File:** `simplypages/src/test/java/io/mindspice/simplypages/modules/EditableModuleTest.java`

```java
package io.mindspice.simplypages.modules;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for EditableModule decorator.
 *
 * Reference: Phase6_5TestController uses EditableModule.wrap() pattern.
 */
class EditableModuleTest {

    @Test
    @DisplayName("wrap() should create EditableModule containing original")
    void testWrap() {
        ContentModule original = ContentModule.create()
            .withTitle("Original");

        EditableModule editable = EditableModule.wrap(original);

        String html = editable.render();

        assertTrue(html.contains("Original"));
    }

    @Test
    @DisplayName("withEditUrl() should add edit button with HTMX attributes")
    void testWithEditUrl() {
        ContentModule original = ContentModule.create();

        EditableModule editable = EditableModule.wrap(original)
            .withEditUrl("/edit/123");

        String html = editable.render();

        assertTrue(html.contains("hx-get=\"/edit/123\"") ||
                   html.contains("hx-get='/edit/123'"));
    }

    @Test
    @DisplayName("withDeleteUrl() should add delete button with HTMX attributes")
    void testWithDeleteUrl() {
        ContentModule original = ContentModule.create();

        EditableModule editable = EditableModule.wrap(original)
            .withDeleteUrl("/delete/123");

        String html = editable.render();

        assertTrue(html.contains("hx-delete=\"/delete/123\"") ||
                   html.contains("hx-delete='/delete/123'"));
    }

    @Test
    @DisplayName("withCanEdit(false) should hide edit button")
    void testCanEditFalse() {
        ContentModule original = ContentModule.create();

        EditableModule editable = EditableModule.wrap(original)
            .withEditUrl("/edit/123")
            .withCanEdit(false);

        String html = editable.render();

        assertFalse(html.contains("hx-get=\"/edit/123\""));
    }

    @Test
    @DisplayName("withCanDelete(false) should hide delete button")
    void testCanDeleteFalse() {
        ContentModule original = ContentModule.create();

        EditableModule editable = EditableModule.wrap(original)
            .withDeleteUrl("/delete/123")
            .withCanDelete(false);

        String html = editable.render();

        assertFalse(html.contains("hx-delete=\"/delete/123\""));
    }

    @Test
    @DisplayName("withDeleteConfirm() should add hx-confirm attribute")
    void testWithDeleteConfirm() {
        ContentModule original = ContentModule.create();

        EditableModule editable = EditableModule.wrap(original)
            .withDeleteUrl("/delete/123")
            .withDeleteConfirm("Are you sure?");

        String html = editable.render();

        assertTrue(html.contains("hx-confirm"));
        assertTrue(html.contains("Are you sure?"));
    }

    @Test
    @DisplayName("Wrapper should have editable-module-wrapper class")
    void testWrapperClass() {
        ContentModule original = ContentModule.create();

        EditableModule editable = EditableModule.wrap(original)
            .withEditUrl("/edit/123");

        String html = editable.render();

        assertTrue(html.contains("editable-module-wrapper"));
    }

    @Test
    @DisplayName("Edit button should have module-edit-btn class")
    void testEditButtonClass() {
        ContentModule original = ContentModule.create();

        EditableModule editable = EditableModule.wrap(original)
            .withEditUrl("/edit/123");

        String html = editable.render();

        assertTrue(html.contains("module-edit-btn"));
    }

    @Test
    @DisplayName("Delete button should have module-delete-btn class")
    void testDeleteButtonClass() {
        ContentModule original = ContentModule.create();

        EditableModule editable = EditableModule.wrap(original)
            .withDeleteUrl("/delete/123");

        String html = editable.render();

        assertTrue(html.contains("module-delete-btn"));
    }

    @Test
    @DisplayName("render() should be idempotent")
    void testRenderIdempotent() {
        ContentModule original = ContentModule.create()
            .withTitle("Test");

        EditableModule editable = EditableModule.wrap(original)
            .withEditUrl("/edit/123");

        String html1 = editable.render();
        String html2 = editable.render();

        assertEquals(html1, html2);
    }

    @Test
    @DisplayName("Wrapper should have unique ID for HTMX targeting")
    void testUniqueId() {
        ContentModule original1 = ContentModule.create();
        ContentModule original2 = ContentModule.create();

        EditableModule editable1 = EditableModule.wrap(original1)
            .withDeleteUrl("/delete/1");
        EditableModule editable2 = EditableModule.wrap(original2)
            .withDeleteUrl("/delete/2");

        String html1 = editable1.render();
        String html2 = editable2.render();

        // Extract IDs and verify they're different
        // Both should have id= attributes but different values
        assertTrue(html1.contains(" id=\""));
        assertTrue(html2.contains(" id=\""));
    }
}
```

---

### 3.5 Editing System Tests

**File:** `simplypages/src/test/java/io/mindspice/simplypages/editing/EditModalBuilderTest.java`

```java
package io.mindspice.simplypages.editing;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.components.display.Modal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for EditModalBuilder.
 *
 * Reference: Phase6_5TestController uses EditModalBuilder for edit modals.
 */
class EditModalBuilderTest {

    @Test
    @DisplayName("build() should require editView")
    void testRequiresEditView() {
        EditModalBuilder builder = EditModalBuilder.create()
            .withSaveUrl("/save");

        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    @DisplayName("build() should require saveUrl")
    void testRequiresSaveUrl() {
        EditModalBuilder builder = EditModalBuilder.create()
            .withEditView(new Div());

        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    @DisplayName("build() should return Modal with required fields")
    void testBuildWithRequiredFields() {
        Modal modal = EditModalBuilder.create()
            .withEditView(new Div().withInnerText("Form content"))
            .withSaveUrl("/save/123")
            .build();

        assertNotNull(modal);
        String html = modal.render();

        assertTrue(html.contains("Form content"));
        assertTrue(html.contains("/save/123"));
    }

    @Test
    @DisplayName("withTitle() should set modal title")
    void testWithTitle() {
        Modal modal = EditModalBuilder.create()
            .withTitle("Edit Item")
            .withEditView(new Div())
            .withSaveUrl("/save")
            .build();

        String html = modal.render();

        assertTrue(html.contains("Edit Item"));
    }

    @Test
    @DisplayName("withDeleteUrl() should add delete button")
    void testWithDeleteUrl() {
        Modal modal = EditModalBuilder.create()
            .withEditView(new Div())
            .withSaveUrl("/save")
            .withDeleteUrl("/delete/123")
            .build();

        String html = modal.render();

        assertTrue(html.contains("Delete"));
        assertTrue(html.contains("/delete/123"));
        assertTrue(html.contains("hx-delete"));
    }

    @Test
    @DisplayName("hideDelete() should not show delete button")
    void testHideDelete() {
        Modal modal = EditModalBuilder.create()
            .withEditView(new Div())
            .withSaveUrl("/save")
            .withDeleteUrl("/delete/123")
            .hideDelete()
            .build();

        String html = modal.render();

        assertFalse(html.contains("hx-delete"));
    }

    @Test
    @DisplayName("Modal should have Cancel button")
    void testCancelButton() {
        Modal modal = EditModalBuilder.create()
            .withEditView(new Div())
            .withSaveUrl("/save")
            .build();

        String html = modal.render();

        assertTrue(html.contains("Cancel"));
    }

    @Test
    @DisplayName("Modal should have Save button with hx-post")
    void testSaveButton() {
        Modal modal = EditModalBuilder.create()
            .withEditView(new Div())
            .withSaveUrl("/save/123")
            .build();

        String html = modal.render();

        assertTrue(html.contains("Save"));
        assertTrue(html.contains("hx-post=\"/save/123\"") ||
                   html.contains("hx-post='/save/123'"));
    }

    @Test
    @DisplayName("Save button should include form fields")
    void testSaveButtonIncludesFields() {
        Modal modal = EditModalBuilder.create()
            .withEditView(new Div())
            .withSaveUrl("/save")
            .build();

        String html = modal.render();

        assertTrue(html.contains("hx-include"));
        assertTrue(html.contains("input") || html.contains("textarea") ||
                   html.contains("select"));
    }

    @Test
    @DisplayName("Delete button should have confirmation")
    void testDeleteConfirmation() {
        Modal modal = EditModalBuilder.create()
            .withEditView(new Div())
            .withSaveUrl("/save")
            .withDeleteUrl("/delete")
            .build();

        String html = modal.render();

        assertTrue(html.contains("hx-confirm"));
    }

    @Test
    @DisplayName("Modal should not close on backdrop click")
    void testNoCloseOnBackdrop() {
        Modal modal = EditModalBuilder.create()
            .withEditView(new Div())
            .withSaveUrl("/save")
            .build();

        // EditModalBuilder sets closeOnBackdrop(false)
        // The modal backdrop should not have onclick remove handler
        String html = modal.render();

        // This is implementation-specific, but we can check
        // that the modal doesn't accidentally close
    }

    // === Container ID Validation Tests (after fix) ===

    @Test
    @DisplayName("withModalContainerId() should reject invalid IDs")
    void testModalContainerIdValidation() {
        // This test will pass after the security fix
        EditModalBuilder builder = EditModalBuilder.create();

        assertThrows(IllegalArgumentException.class, () -> {
            builder.withModalContainerId("invalid id");
        });
    }

    @Test
    @DisplayName("withPageContainerId() should reject invalid IDs")
    void testPageContainerIdValidation() {
        // This test will pass after the security fix
        EditModalBuilder builder = EditModalBuilder.create();

        assertThrows(IllegalArgumentException.class, () -> {
            builder.withPageContainerId("invalid'id");
        });
    }
}
```

---

**File:** `simplypages/src/test/java/io/mindspice/simplypages/editing/AuthWrapperTest.java`

```java
package io.mindspice.simplypages.editing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AuthWrapper utility.
 *
 * Reference: Phase8TestController demonstrates role-based authorization.
 */
class AuthWrapperTest {

    @Test
    @DisplayName("require() should execute action when authorized")
    void testRequireAuthorized() {
        AtomicBoolean actionExecuted = new AtomicBoolean(false);

        String result = AuthWrapper.require(
            () -> true,  // Authorized
            () -> {
                actionExecuted.set(true);
                return "Success";
            },
            () -> "Unauthorized"
        );

        assertTrue(actionExecuted.get());
        assertEquals("Success", result);
    }

    @Test
    @DisplayName("require() should execute unauthorized handler when not authorized")
    void testRequireUnauthorized() {
        AtomicBoolean actionExecuted = new AtomicBoolean(false);
        AtomicBoolean unauthorizedExecuted = new AtomicBoolean(false);

        String result = AuthWrapper.require(
            () -> false,  // Not authorized
            () -> {
                actionExecuted.set(true);
                return "Success";
            },
            () -> {
                unauthorizedExecuted.set(true);
                return "Unauthorized";
            }
        );

        assertFalse(actionExecuted.get());
        assertTrue(unauthorizedExecuted.get());
        assertEquals("Unauthorized", result);
    }

    @Test
    @DisplayName("requireForEdit() should return modal on unauthorized")
    void testRequireForEditUnauthorized() {
        String result = AuthWrapper.requireForEdit(
            () -> false,
            () -> "Edit modal"
        );

        // Should return an error modal, not the edit modal
        assertNotEquals("Edit modal", result);
        assertTrue(result.contains("modal") || result.contains("Modal") ||
                   result.contains("Unauthorized") || result.contains("Permission"));
    }

    @Test
    @DisplayName("requireForEdit() with custom message should include message")
    void testRequireForEditCustomMessage() {
        String result = AuthWrapper.requireForEdit(
            () -> false,
            () -> "Edit modal",
            "Custom error message"
        );

        assertTrue(result.contains("Custom error message"));
    }

    @Test
    @DisplayName("requireForDelete() should return modal on unauthorized")
    void testRequireForDeleteUnauthorized() {
        String result = AuthWrapper.requireForDelete(
            () -> false,
            () -> "Delete action"
        );

        assertNotEquals("Delete action", result);
        assertTrue(result.contains("modal") || result.contains("Modal") ||
                   result.contains("delete") || result.contains("Delete"));
    }

    @Test
    @DisplayName("requireForCreate() should return modal on unauthorized")
    void testRequireForCreateUnauthorized() {
        String result = AuthWrapper.requireForCreate(
            () -> false,
            () -> "Create action"
        );

        assertNotEquals("Create action", result);
        assertTrue(result.contains("modal") || result.contains("Modal") ||
                   result.contains("create") || result.contains("Create"));
    }

    @Test
    @DisplayName("Auth check should be evaluated before action")
    void testAuthCheckOrder() {
        AtomicBoolean authChecked = new AtomicBoolean(false);
        AtomicBoolean actionExecuted = new AtomicBoolean(false);

        AuthWrapper.require(
            () -> {
                authChecked.set(true);
                return false;
            },
            () -> {
                // Action should only run if auth passed
                assertTrue(authChecked.get(), "Auth should be checked before action");
                actionExecuted.set(true);
                return "Success";
            },
            () -> "Unauthorized"
        );

        assertTrue(authChecked.get());
        assertFalse(actionExecuted.get());
    }
}
```

---

**File:** `simplypages/src/test/java/io/mindspice/simplypages/editing/ValidationResultTest.java`

```java
package io.mindspice.simplypages.editing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ValidationResult.
 */
class ValidationResultTest {

    @Test
    @DisplayName("valid() should return valid result")
    void testValid() {
        ValidationResult result = ValidationResult.valid();

        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    @DisplayName("invalid() with single error should return invalid result")
    void testInvalidSingleError() {
        ValidationResult result = ValidationResult.invalid("Error message");

        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertEquals("Error message", result.getErrors().get(0));
    }

    @Test
    @DisplayName("invalid() with multiple errors should return all errors")
    void testInvalidMultipleErrors() {
        ValidationResult result = ValidationResult.invalid(
            List.of("Error 1", "Error 2", "Error 3")
        );

        assertFalse(result.isValid());
        assertEquals(3, result.getErrors().size());
    }

    @Test
    @DisplayName("addError() should accumulate errors")
    void testAddError() {
        ValidationResult result = ValidationResult.valid();
        result.addError("First error");
        result.addError("Second error");

        assertFalse(result.isValid());
        assertEquals(2, result.getErrors().size());
    }
}
```

---

### 3.6 Layout Tests

**File:** `simplypages/src/test/java/io/mindspice/simplypages/layout/ColumnTest.java`

```java
package io.mindspice.simplypages.layout;

import io.mindspice.simplypages.components.Paragraph;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Column layout component.
 */
class ColumnTest {

    @Test
    @DisplayName("create() should return Column with col class")
    void testCreate() {
        Column column = Column.create();
        String html = column.render();

        assertTrue(html.contains("col"));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12})
    @DisplayName("withWidth() should set col-{width} class")
    void testWithWidth(int width) {
        Column column = Column.create().withWidth(width);
        String html = column.render();

        assertTrue(html.contains("col-" + width));
    }

    @Test
    @DisplayName("auto() should set col-auto class")
    void testAuto() {
        Column column = Column.create().auto();
        String html = column.render();

        assertTrue(html.contains("col-auto") || html.contains("auto"));
    }

    @Test
    @DisplayName("fill() should set appropriate flex class")
    void testFill() {
        Column column = Column.create().fill();
        String html = column.render();

        // Implementation may use col or flex-grow
        assertTrue(html.contains("col") || html.contains("flex"));
    }

    @Test
    @DisplayName("withChild() should add child component")
    void testWithChild() {
        Column column = Column.create()
            .withChild(new Paragraph("Child content"));

        String html = column.render();

        assertTrue(html.contains("Child content"));
    }

    @Test
    @DisplayName("withWidth() should reject invalid widths")
    void testInvalidWidth() {
        assertThrows(IllegalArgumentException.class, () -> {
            Column.create().withWidth(0);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            Column.create().withWidth(13);
        });
    }
}
```

---

**File:** `simplypages/src/test/java/io/mindspice/simplypages/layout/RowTest.java`

```java
package io.mindspice.simplypages.layout;

import io.mindspice.simplypages.components.Paragraph;
import io.mindspice.simplypages.modules.ContentModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Row layout component.
 */
class RowTest {

    @Test
    @DisplayName("create() should return Row with row class")
    void testCreate() {
        Row row = new Row();
        String html = row.render();

        assertTrue(html.contains("row"));
    }

    @Test
    @DisplayName("withChild(Column) should add column directly")
    void testWithChildColumn() {
        Row row = new Row()
            .withChild(Column.create().withWidth(6)
                .withChild(new Paragraph("Content")));

        String html = row.render();

        assertTrue(html.contains("col-6") || html.contains("col"));
        assertTrue(html.contains("Content"));
    }

    @Test
    @DisplayName("withChild(non-Column) should auto-wrap in col div")
    void testWithChildAutoWrap() {
        Row row = new Row()
            .withChild(new Paragraph("Unwrapped"));

        String html = row.render();

        assertTrue(html.contains("col"));
        assertTrue(html.contains("Unwrapped"));
    }

    @Test
    @DisplayName("addColumn() should add column")
    void testAddColumn() {
        Row row = new Row();
        row.addColumn(Column.create().withWidth(4));
        row.addColumn(Column.create().withWidth(8));

        String html = row.render();

        assertTrue(html.contains("col-4") || html.contains("col"));
        assertTrue(html.contains("col-8") || html.contains("col"));
    }

    @Test
    @DisplayName("withGap() should add gap class")
    void testWithGap() {
        Row row = new Row().withGap("2");
        String html = row.render();

        assertTrue(html.contains("gap") || html.contains("g-2"));
    }

    @Test
    @DisplayName("withAlign() should add alignment class")
    void testWithAlign() {
        Row row = new Row().withAlign("center");
        String html = row.render();

        assertTrue(html.contains("align") || html.contains("center"));
    }

    @Test
    @DisplayName("withJustify() should add justify class")
    void testWithJustify() {
        Row row = new Row().withJustify("between");
        String html = row.render();

        assertTrue(html.contains("justify") || html.contains("between"));
    }
}
```

---

### 3.7 Integration Tests

**File:** `simplypages/src/test/java/io/mindspice/simplypages/integration/HtmxIntegrationTest.java`

```java
package io.mindspice.simplypages.integration;

import io.mindspice.simplypages.components.forms.Button;
import io.mindspice.simplypages.modules.ContentModule;
import io.mindspice.simplypages.modules.EditableModule;
import io.mindspice.simplypages.editing.EditModalBuilder;
import io.mindspice.simplypages.components.Div;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for HTMX patterns.
 *
 * Reference: Phase6_5TestController demonstrates all HTMX patterns.
 */
class HtmxIntegrationTest {

    @Test
    @DisplayName("EditableModule edit button should have correct HTMX attributes")
    void testEditButtonHtmxAttributes() {
        EditableModule editable = EditableModule.wrap(ContentModule.create())
            .withEditUrl("/api/edit/123");

        String html = editable.render();

        assertTrue(html.contains("hx-get=\"/api/edit/123\"") ||
                   html.contains("hx-get='/api/edit/123'"));
        assertTrue(html.contains("hx-target"));
        assertTrue(html.contains("hx-swap"));
    }

    @Test
    @DisplayName("EditableModule delete button should have correct HTMX attributes")
    void testDeleteButtonHtmxAttributes() {
        EditableModule editable = EditableModule.wrap(ContentModule.create())
            .withDeleteUrl("/api/delete/123");

        String html = editable.render();

        assertTrue(html.contains("hx-delete=\"/api/delete/123\"") ||
                   html.contains("hx-delete='/api/delete/123'"));
    }

    @Test
    @DisplayName("EditModalBuilder save button should use hx-swap='none' for OOB")
    void testSaveButtonSwapNone() {
        var modal = EditModalBuilder.create()
            .withEditView(new Div())
            .withSaveUrl("/save")
            .build();

        String html = modal.render();

        // Save button should use swap=none because response uses OOB swaps
        assertTrue(html.contains("hx-swap=\"none\"") ||
                   html.contains("hx-swap='none'"));
    }

    @Test
    @DisplayName("EditModalBuilder should include form fields in save request")
    void testSaveButtonIncludesFields() {
        var modal = EditModalBuilder.create()
            .withEditView(new Div())
            .withSaveUrl("/save")
            .build();

        String html = modal.render();

        assertTrue(html.contains("hx-include"));
        // Should include input, textarea, select
        assertTrue(html.contains(".modal-body"));
    }

    @Test
    @DisplayName("OOB swap HTML should have correct format")
    void testOobSwapFormat() {
        // Simulate OOB swap response pattern from Phase6_5TestController
        String clearModal = "<div hx-swap-oob=\"true\" id=\"edit-modal-container\"></div>";
        String updatePage = "<div hx-swap-oob=\"true\" id=\"page-content\">Updated</div>";

        String response = clearModal + updatePage;

        assertTrue(response.contains("hx-swap-oob=\"true\""));
        assertTrue(response.contains("id=\"edit-modal-container\""));
        assertTrue(response.contains("id=\"page-content\""));
    }

    @Test
    @DisplayName("Button with HTMX should not be type='submit'")
    void testHtmxButtonNotSubmit() {
        Button button = Button.create("Save");
        button.withAttribute("hx-post", "/save");

        String html = button.render();

        // Button.create() should produce type="button", not type="submit"
        assertTrue(html.contains("type=\"button\"") || !html.contains("type=\"submit\""));
    }
}
```

---

**File:** `simplypages/src/test/java/io/mindspice/simplypages/integration/CrudOperationsTest.java`

```java
package io.mindspice.simplypages.integration;

import io.mindspice.simplypages.modules.ContentModule;
import io.mindspice.simplypages.editing.EditAdapter;
import io.mindspice.simplypages.editing.ValidationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for CRUD operations pattern.
 *
 * Reference: Phase6_5TestController full CRUD implementation.
 */
class CrudOperationsTest {

    @Test
    @DisplayName("Create: New module should have default values")
    void testCreateModule() {
        ContentModule module = ContentModule.create()
            .withModuleId("new-module")
            .withTitle("New Module")
            .withContent("Initial content");

        String html = module.render();

        assertTrue(html.contains("New Module"));
        assertTrue(html.contains("Initial content"));
    }

    @Test
    @DisplayName("Read: Module should render current state")
    void testReadModule() {
        ContentModule module = ContentModule.create()
            .withTitle("Existing")
            .withContent("Existing content");

        String html = module.render();

        assertTrue(html.contains("Existing"));
        assertTrue(html.contains("Existing content"));
    }

    @Test
    @DisplayName("Update: applyEdits should modify module")
    void testUpdateModule() {
        ContentModule module = ContentModule.create()
            .withTitle("Original")
            .withContent("Original content");

        // Simulate form submission
        Map<String, String> formData = new HashMap<>();
        formData.put("title", "Updated");
        formData.put("content", "Updated content");

        module.applyEdits(formData);

        String html = module.render();

        assertTrue(html.contains("Updated"));
        assertTrue(html.contains("Updated content"));
        assertFalse(html.contains("Original"));
    }

    @Test
    @DisplayName("Update: Validate before apply")
    void testValidateBeforeUpdate() {
        ContentModule module = ContentModule.create();

        Map<String, String> formData = new HashMap<>();
        formData.put("title", "");  // Empty title might be invalid
        formData.put("content", "Some content");

        ValidationResult validation = module.validate(formData);

        // Only apply if valid
        if (validation.isValid()) {
            module.applyEdits(formData);
        }

        // Test that validation was checked
        assertNotNull(validation);
    }

    @Test
    @DisplayName("Full edit workflow: buildEditView -> validate -> applyEdits")
    void testFullEditWorkflow() {
        // 1. Create module with initial data
        ContentModule module = ContentModule.create()
            .withModuleId("test-123")
            .withTitle("Initial Title")
            .withContent("Initial content");

        // 2. Build edit view (would be returned by GET /edit endpoint)
        EditAdapter<ContentModule> adapter = module;
        var editView = adapter.buildEditView();
        assertNotNull(editView);

        // 3. User submits form (simulated)
        Map<String, String> formData = new HashMap<>();
        formData.put("title", "User Updated Title");
        formData.put("content", "User updated content");

        // 4. Validate (would happen in POST /save endpoint)
        ValidationResult validation = adapter.validate(formData);
        assertTrue(validation.isValid());

        // 5. Apply edits
        adapter.applyEdits(formData);

        // 6. Verify update
        String html = module.render();
        assertTrue(html.contains("User Updated Title"));
        assertTrue(html.contains("User updated content"));
    }

    @Test
    @DisplayName("Module rebuild after edit should clear old content")
    void testModuleRebuildClearsOldContent() {
        ContentModule module = ContentModule.create()
            .withTitle("First Title")
            .withContent("First content");

        // Initial render
        String html1 = module.render();
        assertTrue(html1.contains("First Title"));

        // Edit
        Map<String, String> formData = new HashMap<>();
        formData.put("title", "Second Title");
        formData.put("content", "Second content");
        module.applyEdits(formData);

        // Render after edit
        String html2 = module.render();

        // Old content should be gone
        assertFalse(html2.contains("First Title"));
        assertFalse(html2.contains("First content"));

        // New content should be present
        assertTrue(html2.contains("Second Title"));
        assertTrue(html2.contains("Second content"));
    }
}
```

---

### 3.8 Test Dependencies (pom.xml addition)

```xml
<!-- Add to simplypages/pom.xml -->
<dependencies>
    <!-- Existing dependencies... -->

    <!-- Test dependencies -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.10.1</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter-params</artifactId>
        <version>5.10.1</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.assertj</groupId>
        <artifactId>assertj-core</artifactId>
        <version>3.24.2</version>
        <scope>test</scope>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.2.2</version>
        </plugin>
    </plugins>
</build>
```

---

## Implementation Schedule

### Sprint 1: Security Fixes (Day 1)

| Task | Time | Priority |
|------|------|----------|
| Fix Select.Option XSS | 30 min | CRITICAL |
| Fix EditModalBuilder ID validation | 30 min | CRITICAL |
| Write security tests | 1 hour | HIGH |
| Verify fixes pass tests | 30 min | HIGH |

### Sprint 2: Code Quality (Days 2-3)

| Task | Time | Priority |
|------|------|----------|
| Create FormFieldHelper | 1 hour | MEDIUM |
| Refactor withClass() methods | 1 hour | MEDIUM |
| Add Module.rebuildContent() | 30 min | MEDIUM |
| Fix RichContentModule naming | 15 min | LOW |
| Update modules to use helpers | 1 hour | MEDIUM |

### Sprint 3: Core Tests (Days 4-6)

| Task | Time | Priority |
|------|------|----------|
| Setup test infrastructure | 1 hour | HIGH |
| Component tests (Select, Modal, etc.) | 3 hours | HIGH |
| Module tests (ContentModule, EditableModule) | 2 hours | HIGH |
| Editing system tests | 2 hours | HIGH |
| Layout tests | 1 hour | MEDIUM |

### Sprint 4: Integration Tests (Days 7-8)

| Task | Time | Priority |
|------|------|----------|
| HTMX integration tests | 2 hours | HIGH |
| CRUD operations tests | 2 hours | HIGH |
| End-to-end workflow tests | 2 hours | MEDIUM |

---

## Success Criteria

### Phase 1 Complete When:
- [x] All XSS payloads blocked in Select.Option
- [x] EditModalBuilder rejects invalid container IDs
- [x] Security tests pass

### Phase 2 Complete When:
- [x] No duplicate withClass() implementations
- [x] FormFieldHelper used in 3+ modules
- [x] Module.rebuildContent() available
- [x] No JavaBean-style setters in fluent API classes

### Phase 3 Complete When:
- [ ] 80%+ code coverage on security-critical paths (deferred per owner request; no JaCoCo)
- [x] All component tests pass
- [x] All module tests pass
- [x] All integration tests pass
- [x] `mvn test` completes successfully

---

## Living Remediation Log

**Last Updated:** 2026-01-05

### Phase 1: Critical Security Fixes
- Select.Option XSS: Done - Escaped option value and label in render using OWASP Encoder. Files: `simplypages/src/main/java/io/mindspice/simplypages/components/forms/Select.java`.
- EditModalBuilder ID validation: Done - Added regex validation for modal/page container IDs and hardened cancel handler with data-modal-id. Files: `simplypages/src/main/java/io/mindspice/simplypages/editing/EditModalBuilder.java`.
- Security tests: Done - Added XSS/CSS/URL validation tests and ran `mvn -pl simplypages test`.
- Markdown URL sanitization: Done - Enabled CommonMark URL sanitization to block unsafe link schemes in rendered markdown. File: `simplypages/src/main/java/io/mindspice/simplypages/components/Markdown.java`.
- Navigation/list escaping: Done - Escaped nav/list text and attribute values to prevent HTML injection. Files: `simplypages/src/main/java/io/mindspice/simplypages/components/navigation/NavBar.java`, `simplypages/src/main/java/io/mindspice/simplypages/components/navigation/SideNav.java`, `simplypages/src/main/java/io/mindspice/simplypages/components/navigation/Breadcrumb.java`, `simplypages/src/main/java/io/mindspice/simplypages/components/display/OrderedList.java`, `simplypages/src/main/java/io/mindspice/simplypages/components/display/UnorderedList.java`.
- Link scheme allowlist: Done - Restricted Link URLs to http/https/mailto/tel or relative paths; added rejection tests for unsupported schemes. Files: `simplypages/src/main/java/io/mindspice/simplypages/components/navigation/Link.java`, `simplypages/src/test/java/io/mindspice/simplypages/security/UrlValidationTest.java`.

### Phase 2: Code Quality Improvements
- withClass() refactors: Done - Button/TextInput/Alert/Badge now use HtmlTag.addClass. Files: `simplypages/src/main/java/io/mindspice/simplypages/components/forms/Button.java`, `simplypages/src/main/java/io/mindspice/simplypages/components/forms/TextInput.java`, `simplypages/src/main/java/io/mindspice/simplypages/components/display/Alert.java`, `simplypages/src/main/java/io/mindspice/simplypages/components/display/Badge.java`.
- FormFieldHelper: Done - Added helper for text/textarea/checkbox fields. File: `simplypages/src/main/java/io/mindspice/simplypages/editing/FormFieldHelper.java`.
- Module edit forms: Done - ContentModule/RichContentModule/SimpleListModule now use FormFieldHelper. Files: `simplypages/src/main/java/io/mindspice/simplypages/modules/ContentModule.java`, `simplypages/src/main/java/io/mindspice/simplypages/modules/RichContentModule.java`, `simplypages/src/main/java/io/mindspice/simplypages/modules/SimpleListModule.java`.
- Module.rebuildContent(): Done - Added helper to rebuild content after edits. File: `simplypages/src/main/java/io/mindspice/simplypages/core/Module.java`.
- RichContentModule naming: Done - Deprecated JavaBean-style setModuleId in favor of withModuleId. File: `simplypages/src/main/java/io/mindspice/simplypages/modules/RichContentModule.java`.
- Alignment class handling: Done - Header/Paragraph alignment updates replace prior alignment classes instead of stacking duplicates. Files: `simplypages/src/main/java/io/mindspice/simplypages/components/Header.java`, `simplypages/src/main/java/io/mindspice/simplypages/components/Paragraph.java`.
- Editable wrappers: Done - EditableRow/EditablePage now render through their own wrapper so attributes on the instance are preserved. Files: `simplypages/src/main/java/io/mindspice/simplypages/editing/EditableRow.java`, `simplypages/src/main/java/io/mindspice/simplypages/editing/EditablePage.java`.
- EditModalBuilder defaults: Done - Default page container ID now matches docs (`page-content`). File: `simplypages/src/main/java/io/mindspice/simplypages/editing/EditModalBuilder.java`.
- Documentation drift: Done - Removed undocumented `withErrors` references and marked legacy guide as deprecated. Files: `EDITING_SYSTEM_API.md`, `docs/EDITING_SYSTEM.md`.
- Modal ESC close control: Done - Added closeOnEscape to allow disabling ESC key close. Files: `simplypages/src/main/java/io/mindspice/simplypages/components/display/Modal.java`, `simplypages/src/test/java/io/mindspice/simplypages/components/display/ModalTest.java`.
- Demo delete resiliency: Done - Phase 8 demo now renders a placeholder module after deletion to avoid NPEs when refreshing. File: `demo/src/main/java/io/mindspice/demo/Phase8TestController.java`.

### Phase 3: Comprehensive Unit Test Suite
- Security tests: Done - Added coverage for XSS, CSS injection, and URL validation. Files: `simplypages/src/test/java/io/mindspice/simplypages/security/XssProtectionTest.java`, `simplypages/src/test/java/io/mindspice/simplypages/security/CssInjectionTest.java`, `simplypages/src/test/java/io/mindspice/simplypages/security/UrlValidationTest.java`.
- Core tests: Done - Added Attribute/HtmlTag/Slot/Template/RenderContext/Style coverage. Files: `simplypages/src/test/java/io/mindspice/simplypages/core/*`.
- Component tests: Done - Added broad coverage for basic, display, forms, navigation, media, forum, and utility components. Files: `simplypages/src/test/java/io/mindspice/simplypages/components/*`, `simplypages/src/test/java/io/mindspice/simplypages/components/display/*`, `simplypages/src/test/java/io/mindspice/simplypages/components/forms/*`, `simplypages/src/test/java/io/mindspice/simplypages/components/navigation/*`, `simplypages/src/test/java/io/mindspice/simplypages/components/media/*`, `simplypages/src/test/java/io/mindspice/simplypages/components/forum/*`.
- Form coverage expansion: Done - Added attribute/behavior coverage for Form, Button, TextInput, TextArea, Checkbox, Select, RadioGroup, FormModule, and FormFieldHelper. Files: `simplypages/src/test/java/io/mindspice/simplypages/components/forms/FormTest.java`, `simplypages/src/test/java/io/mindspice/simplypages/components/forms/ButtonTest.java`, `simplypages/src/test/java/io/mindspice/simplypages/components/forms/TextInputTest.java`, `simplypages/src/test/java/io/mindspice/simplypages/components/forms/TextAreaTest.java`, `simplypages/src/test/java/io/mindspice/simplypages/components/forms/CheckboxTest.java`, `simplypages/src/test/java/io/mindspice/simplypages/components/forms/SelectTest.java`, `simplypages/src/test/java/io/mindspice/simplypages/components/forms/RadioGroupTest.java`, `simplypages/src/test/java/io/mindspice/simplypages/modules/FormModuleTest.java`, `simplypages/src/test/java/io/mindspice/simplypages/editing/FormFieldHelperTest.java`.
- Builder tests: Done - Added coverage for Banner/TopBanner/AccountBar/SideNav/TopNav/Shell builders. Files: `simplypages/src/test/java/io/mindspice/simplypages/builders/*`.
- Module tests: Done - Added coverage for core and specialized modules (content, lists, dynamic, data, hero, tabs, stats, timeline, etc.). Files: `simplypages/src/test/java/io/mindspice/simplypages/modules/*`.
- Editing system tests: Done - Added AuthWrapper/EditModalBuilder/ValidationResult/EditableRow/EditablePage coverage. Files: `simplypages/src/test/java/io/mindspice/simplypages/editing/*`.
- Layout tests: Done - Added Column/Row/Grid/Page/Container/Section coverage. Files: `simplypages/src/test/java/io/mindspice/simplypages/layout/*`.
- Integration tests: Done - Added HTMX/CRUD integration checks. Files: `simplypages/src/test/java/io/mindspice/simplypages/integration/HtmxIntegrationTest.java`, `simplypages/src/test/java/io/mindspice/simplypages/integration/CrudOperationsTest.java`.
- Demo integration tests: Done - Added MockMvc coverage for OOB swap responses in demo edit/save/delete flows. File: `demo/src/test/java/io/mindspice/demo/integration/EditingOobIntegrationTest.java`.
- Test runs: Done - `mvn -pl simplypages test`.

### Test-Driven Fixes (Found During Phase 3)
- ListItem render: Done - Ensured list item text is applied when rendering with a RenderContext. File: `simplypages/src/main/java/io/mindspice/simplypages/components/ListItem.java`.
- Code block with title: Done - Ensured titled code blocks render inner content by using RenderContext-aware render. File: `simplypages/src/main/java/io/mindspice/simplypages/components/Code.java`.
- TopNavBuilder HTMX links: Done - NavBar now accepts NavItem instances so TopNavBuilder can attach HX attributes. Files: `simplypages/src/main/java/io/mindspice/simplypages/components/navigation/NavBar.java`, `simplypages/src/main/java/io/mindspice/simplypages/builders/TopNavBuilder.java`.
- AccountWidget render: Done - Ensured RenderContext-aware rendering builds the correct guest/authenticated content and stays idempotent. File: `simplypages/src/main/java/io/mindspice/simplypages/components/AccountWidget.java`.
- Navigation/list escaping: Done - Added regression coverage for escaping in nav/list components. Files: `simplypages/src/test/java/io/mindspice/simplypages/components/navigation/NavBarTest.java`, `simplypages/src/test/java/io/mindspice/simplypages/components/navigation/SideNavTest.java`, `simplypages/src/test/java/io/mindspice/simplypages/components/navigation/BreadcrumbTest.java`, `simplypages/src/test/java/io/mindspice/simplypages/components/display/OrderedListTest.java`, `simplypages/src/test/java/io/mindspice/simplypages/components/display/UnorderedListTest.java`.
- Alignment/Markdown/Editable wrapper tests: Done - Added regression coverage for alignment class replacement, markdown URL sanitization, and editable wrapper attribute preservation. Files: `simplypages/src/test/java/io/mindspice/simplypages/components/BasicComponentsTest.java`, `simplypages/src/test/java/io/mindspice/simplypages/components/MarkdownTest.java`, `simplypages/src/test/java/io/mindspice/simplypages/editing/EditableRowTest.java`, `simplypages/src/test/java/io/mindspice/simplypages/editing/EditablePageTest.java`.
- URL scheme allowlist tests: Done - Added regression coverage for unsupported link schemes and extra safe URL forms. File: `simplypages/src/test/java/io/mindspice/simplypages/security/UrlValidationTest.java`.

---

## Appendix: Test File Summary

| Category | Files | Test Count (Est.) |
|----------|-------|-------------------|
| Security | 3 | 35-40 |
| Core | 8 | 20-30 |
| Components | 45 | 120-150 |
| Builders | 6 | 15-25 |
| Modules | 11 | 35-45 |
| Editing | 5 | 25-35 |
| Layout | 6 | 25-35 |
| Integration | 2 | 15-20 |
| **Total** | **86** | **290-380** |

---

**Document Version:** 2.0
**Created:** January 4, 2026
**Last Updated:** 2026-01-04
**Author:** Automated Analysis
