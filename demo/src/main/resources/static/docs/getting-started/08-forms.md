# Part 8: Forms

Forms are essential for user input in web applications. JHF provides a comprehensive set of form components with built-in validation, CSRF protection, and responsive design. This guide covers everything you need to build professional forms.

## Form Components Overview

JHF provides seven form component types:

1. **TextInput** - Single-line text input (10 factory methods for different types)
2. **TextArea** - Multi-line text input
3. **Select** - Dropdown selection
4. **Checkbox** - Single checkbox
5. **RadioGroup** - Grouped radio buttons
6. **Button** - Action buttons (submit, reset, button)
7. **Form** - Container with CSRF protection

## TextInput - Single-Line Input

The most versatile form component with 10 specialized factory methods.

### Factory Methods

```java
// Regular text input
TextInput.create("username");

// Email input (with HTML5 validation)
TextInput.email("email");

// Password input (masked)
TextInput.password("password");

// Numeric input
TextInput.number("age");

// Date picker
TextInput.date("birthdate");

// Search input
TextInput.search("query");

// Telephone input
TextInput.tel("phone");

// URL input
TextInput.url("website");

// Time picker
TextInput.time("alarm");

// DateTime picker
TextInput.datetime("appointment");
```

### HTML5 Input Types

Each factory method generates the appropriate HTML5 input type:

| Method | HTML Type | Browser Behavior |
|--------|-----------|------------------|
| `create()` | `text` | Standard text input |
| `email()` | `email` | Email validation, @ keyboard on mobile |
| `password()` | `password` | Masked characters |
| `number()` | `number` | Numeric keyboard on mobile, up/down arrows |
| `date()` | `date` | Native date picker |
| `search()` | `search` | Search-style input with clear button |
| `tel()` | `tel` | Phone keyboard on mobile |
| `url()` | `url` | URL validation, .com keyboard on mobile |
| `time()` | `time` | Native time picker |
| `datetime()` | `datetime-local` | Native date+time picker |

### Configuration Methods

All TextInput types support these methods:

```java
TextInput.email("email")
    .withPlaceholder("Enter your email")  // Placeholder text
    .withValue("user@example.com")        // Default value
    .withMaxWidth("400px")                // Width constraint
    .withMinWidth("200px")                // Minimum width
    .withWidth("300px")                   // Exact width
    .withClass("custom-input")            // CSS class
    .withId("email-input")                // HTML id attribute
    .required()                           // Required field
    .disabled()                           // Disabled state
    .readonly();                          // Read-only state
```

### Examples

**Email input**:
```java
TextInput.email("email")
    .withPlaceholder("you@example.com")
    .withMaxWidth("400px")
    .required();
```

**Password input**:
```java
TextInput.password("password")
    .withPlaceholder("Enter password")
    .withMaxWidth("300px")
    .required();
```

**Date picker**:
```java
TextInput.date("birthdate")
    .withPlaceholder("YYYY-MM-DD")
    .withMaxWidth("200px");
```

**Number input with constraints**:
```java
TextInput.number("age")
    .withPlaceholder("18")
    .withMaxWidth("100px")
    .withAttribute("min", "0")
    .withAttribute("max", "120");
```

## TextArea - Multi-Line Input

For longer text input like messages, descriptions, or comments.

### Basic Usage

```java
TextArea.create("message")
    .withRows(5)                          // Number of visible rows
    .withPlaceholder("Enter your message")
    .withMaxWidth("600px")
    .required();
```

### Configuration

```java
TextArea.create("description")
    .withRows(10)                   // Visible rows (default: 3)
    .withPlaceholder("Description")
    .withValue("Default text")
    .withMaxWidth("100%")
    .withClass("form-textarea")
    .required();
```

### Example: Comment Box

```java
TextArea.create("comment")
    .withRows(4)
    .withPlaceholder("Share your thoughts...")
    .withMaxWidth("600px")
    .withClass("comment-box");
```

## Select - Dropdown Selection

For choosing from a list of options.

### Basic Usage

```java
Select.create("country")
    .addOption("us", "United States")
    .addOption("uk", "United Kingdom")
    .addOption("ca", "Canada")
    .addOption("au", "Australia")
    .withMaxWidth("300px")
    .required();
```

### Configuration

```java
Select.create("role")
    .addOption("", "-- Select Role --")  // Placeholder option
    .addOption("user", "User")
    .addOption("admin", "Administrator")
    .addOption("moderator", "Moderator")
    .withClass("form-select")
    .required();
```

### Selected Option

```java
Select.create("status")
    .addOption("active", "Active", true)   // Selected by default
    .addOption("inactive", "Inactive")
    .addOption("pending", "Pending");
```

### Example: Dynamic Options

```java
Select categorySelect = Select.create("category")
    .withMaxWidth("250px");

for (Category category : categories) {
    categorySelect.addOption(
        String.valueOf(category.getId()),
        category.getName()
    );
}
```

## Checkbox - Single Checkbox

For boolean choices (yes/no, agree/disagree, enable/disable).

### Basic Usage

```java
Checkbox.create("agree", "I agree to the terms and conditions")
    .required();
```

### Configuration

```java
Checkbox.create("newsletter", "Subscribe to newsletter")
    .withChecked(true)              // Checked by default
    .withClass("custom-checkbox");
```

### Example: Multiple Checkboxes

```java
Div preferences = new Div().withClass("preferences");

preferences.withChild(Checkbox.create("email_notifications", "Email Notifications"));
preferences.withChild(Checkbox.create("sms_notifications", "SMS Notifications"));
preferences.withChild(Checkbox.create("push_notifications", "Push Notifications"));
```

## RadioGroup - Grouped Radio Buttons

For choosing one option from multiple choices.

### Basic Usage

```java
RadioGroup.create("size")
    .addOption("small", "Small")
    .addOption("medium", "Medium")
    .addOption("large", "Large")
    .withSelected("medium");  // Default selection
```

### Configuration

```java
RadioGroup.create("payment_method")
    .addOption("credit_card", "Credit Card")
    .addOption("paypal", "PayPal")
    .addOption("bank_transfer", "Bank Transfer")
    .withSelected("credit_card")
    .withClass("payment-options");
```

### Example: Survey Question

```java
RadioGroup.create("satisfaction")
    .addOption("very_satisfied", "Very Satisfied")
    .addOption("satisfied", "Satisfied")
    .addOption("neutral", "Neutral")
    .addOption("dissatisfied", "Dissatisfied")
    .addOption("very_dissatisfied", "Very Dissatisfied")
    .required();
```

## Button - Action Buttons

For form submission and actions.

### Factory Methods

```java
// Submit button (submits form)
Button.submit("Submit");

// Reset button (clears form)
Button.reset("Clear");

// Regular button (for JavaScript/HTMX actions)
Button.create("Click Me");
```

### Configuration

```java
Button.submit("Register")
    .withClass("btn btn-primary")
    .withMinWidth("120px");

Button.create("Cancel")
    .withClass("btn btn-secondary")
    .withAttribute("onclick", "history.back()");
```

### Styling Variants

```java
Button.submit("Save").withClass("btn btn-primary");   // Blue
Button.create("Delete").withClass("btn btn-danger");  // Red
Button.create("Edit").withClass("btn btn-warning");   // Yellow
Button.create("Info").withClass("btn btn-info");      // Cyan
```

## Form - Container with CSRF Protection

The `Form` component wraps inputs and provides structure, CSRF protection, and HTMX support.

### Basic Form

```java
Form.create()
    .withAction("/submit")
    .withMethod(Form.Method.POST)
    .addField("Username", TextInput.create("username").required())
    .addField("Email", TextInput.email("email").required())
    .addField("", Button.submit("Submit"));
```

### addField() Method

The `addField(label, component)` method creates a structured form field:

```java
form.addField("Email", TextInput.email("email"));
```

Generates:
```html
<div class="form-field">
    <label class="form-label">Email</label>
    <input type="email" name="email" class="form-input">
</div>
```

**Empty label** for buttons:
```java
form.addField("", Button.submit("Submit"));
```

### CSRF Protection

Spring Security automatically enables CSRF protection. Include the token in forms:

```java
@GetMapping("/register")
@ResponseBody
public String registerPage(CsrfToken csrfToken) {
    Form form = Form.create()
        .withAction("/register")
        .withMethod(Form.Method.POST)
        .withCsrfToken(csrfToken.getToken())  // CRITICAL: Include CSRF token
        .addField("Username", TextInput.create("username").required())
        .addField("Email", TextInput.email("email").required())
        .addField("Password", TextInput.password("password").required())
        .addField("", Button.submit("Register"));

    return Page.create()
        .addComponents(form)
        .render();
}
```

**Why CSRF protection?**
- Prevents Cross-Site Request Forgery attacks
- Ensures requests originated from your application
- Spring Security validates tokens automatically

### Complete Form Example

```java
Form contactForm = Form.create()
    .withAction("/contact")
    .withMethod(Form.Method.POST)
    .withCsrfToken(csrfToken.getToken())

    // Name field
    .addField("Name", TextInput.create("name")
        .withPlaceholder("Your name")
        .withMaxWidth("400px")
        .required())

    // Email field
    .addField("Email", TextInput.email("email")
        .withPlaceholder("your@email.com")
        .withMaxWidth("400px")
        .required())

    // Subject dropdown
    .addField("Subject", Select.create("subject")
        .addOption("general", "General Inquiry")
        .addOption("support", "Support Request")
        .addOption("feedback", "Feedback")
        .withMaxWidth("300px")
        .required())

    // Message textarea
    .addField("Message", TextArea.create("message")
        .withRows(6)
        .withPlaceholder("Your message...")
        .withMaxWidth("600px")
        .required())

    // Submit button
    .addField("", Button.submit("Send Message")
        .withClass("btn btn-primary")
        .withMinWidth("150px"));
```

## Form Validation

Use both HTML5 client-side validation and server-side validation.

### HTML5 Validation (Client-Side)

Built-in browser validation:

```java
TextInput.email("email")
    .required()                              // Cannot be empty
    .withAttribute("minlength", "3")         // Minimum length
    .withAttribute("maxlength", "50")        // Maximum length
    .withAttribute("pattern", "[a-zA-Z]+");  // Regex pattern

TextInput.number("age")
    .withAttribute("min", "18")              // Minimum value
    .withAttribute("max", "100");            // Maximum value
```

**Validation attributes**:
- `required` - Field must be filled
- `minlength` / `maxlength` - Length constraints
- `min` / `max` - Numeric constraints
- `pattern` - Regex pattern matching

### Server-Side Validation (Critical!)

**Never trust client-side validation alone.** Users can bypass it.

#### Spring Validation Example

```java
// Domain model with validation annotations
public class RegistrationRequest {
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 20, message = "Username must be 3-20 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    // Getters and setters
}

// Controller with validation
@PostMapping("/register")
public String register(@Valid @ModelAttribute RegistrationRequest request,
                       BindingResult result,
                       Model model) {
    if (result.hasErrors()) {
        // Validation failed - show errors
        model.addAttribute("errors", result.getAllErrors());
        return "registration-form";  // Return form with errors
    }

    // Validation passed - process registration
    userService.register(request);
    return "redirect:/login";
}
```

### Displaying Validation Errors

```java
public String buildForm(List<String> errors) {
    Form form = Form.create()
        .withAction("/register")
        .withMethod(Form.Method.POST);

    // Show validation errors
    if (errors != null && !errors.isEmpty()) {
        Div errorBox = new Div().withClass("alert alert-danger");
        for (String error : errors) {
            errorBox.withChild(Paragraph.create().withInnerText(error));
        }
        form.withChild(errorBox);
    }

    form.addField("Username", TextInput.create("username").required())
        .addField("Email", TextInput.email("email").required())
        .addField("", Button.submit("Register"));

    return form.render();
}
```

## Width Constraints for Forms

Form inputs should have appropriate widths for better UX.

### Why Constrain Widths?

**Poor UX** (no width constraints):
```java
TextInput.create("zip_code");  // Full width (bad for 5-digit zip code)
```

**Good UX** (appropriate width):
```java
TextInput.create("zip_code").withMaxWidth("120px");  // Sized for 5 digits
```

### Width Guidelines

| Field Type | Recommended Width | Example |
|------------|-------------------|---------|
| Username | 300px | `withMaxWidth("300px")` |
| Email | 400px | `withMaxWidth("400px")` |
| Password | 300px | `withMaxWidth("300px")` |
| Phone | 200px | `withMaxWidth("200px")` |
| Zip Code | 100px | `withMaxWidth("100px")` |
| Age | 80px | `withMaxWidth("80px")` |
| Full Name | 400px | `withMaxWidth("400px")` |
| City | 250px | `withMaxWidth("250px")` |
| Search | 500px | `withMaxWidth("500px")` |
| Message | 600px | `withMaxWidth("600px")` |

### Responsive Widths

Use `withMaxWidth()` instead of `withWidth()` for responsive behavior:

```java
// Good - responsive (shrinks on mobile)
TextInput.email("email").withMaxWidth("400px");

// Avoid - fixed width (doesn't shrink on mobile)
TextInput.email("email").withWidth("400px");
```

### Example: Well-Sized Form

```java
Form.create()
    .addField("Username", TextInput.create("username")
        .withMaxWidth("300px")
        .required())

    .addField("Email", TextInput.email("email")
        .withMaxWidth("400px")
        .required())

    .addField("Phone", TextInput.tel("phone")
        .withMaxWidth("200px"))

    .addField("Zip Code", TextInput.create("zip")
        .withMaxWidth("100px")
        .withAttribute("maxlength", "5"))

    .addField("Age", TextInput.number("age")
        .withMaxWidth("80px")
        .withAttribute("min", "0")
        .withAttribute("max", "120"))

    .addField("", Button.submit("Submit")
        .withMinWidth("120px"));
```

## Complete Form Examples

### Example 1: Contact Form

```java
public String buildContactForm(CsrfToken csrfToken) {
    return Form.create()
        .withAction("/contact")
        .withMethod(Form.Method.POST)
        .withCsrfToken(csrfToken.getToken())

        .addField("Name", TextInput.create("name")
            .withPlaceholder("John Doe")
            .withMaxWidth("400px")
            .required())

        .addField("Email", TextInput.email("email")
            .withPlaceholder("john@example.com")
            .withMaxWidth("400px")
            .required())

        .addField("Subject", Select.create("subject")
            .addOption("", "-- Select Subject --")
            .addOption("general", "General Inquiry")
            .addOption("support", "Technical Support")
            .addOption("billing", "Billing Question")
            .addOption("other", "Other")
            .withMaxWidth("300px")
            .required())

        .addField("Message", TextArea.create("message")
            .withRows(8)
            .withPlaceholder("How can we help you?")
            .withMaxWidth("600px")
            .required())

        .addField("", Button.submit("Send Message")
            .withClass("btn btn-primary")
            .withMinWidth("150px"))

        .render();
}
```

### Example 2: Registration Form

```java
public String buildRegistrationForm(CsrfToken csrfToken) {
    return Form.create()
        .withAction("/register")
        .withMethod(Form.Method.POST)
        .withCsrfToken(csrfToken.getToken())

        .addField("Username", TextInput.create("username")
            .withPlaceholder("Choose a username")
            .withMaxWidth("300px")
            .withAttribute("minlength", "3")
            .withAttribute("maxlength", "20")
            .required())

        .addField("Email", TextInput.email("email")
            .withPlaceholder("your@email.com")
            .withMaxWidth("400px")
            .required())

        .addField("Password", TextInput.password("password")
            .withPlaceholder("At least 8 characters")
            .withMaxWidth("300px")
            .withAttribute("minlength", "8")
            .required())

        .addField("Confirm Password", TextInput.password("confirm_password")
            .withPlaceholder("Re-enter password")
            .withMaxWidth("300px")
            .required())

        .addField("", Checkbox.create("agree_terms",
            "I agree to the Terms of Service and Privacy Policy")
            .required())

        .addField("", Button.submit("Create Account")
            .withClass("btn btn-primary btn-block")
            .withMinWidth("200px"))

        .render();
}
```

### Example 3: Profile Edit Form

```java
public String buildProfileForm(User user, CsrfToken csrfToken) {
    return Form.create()
        .withAction("/profile/update")
        .withMethod(Form.Method.POST)
        .withCsrfToken(csrfToken.getToken())

        .addField("Full Name", TextInput.create("full_name")
            .withValue(user.getFullName())
            .withMaxWidth("400px")
            .required())

        .addField("Email", TextInput.email("email")
            .withValue(user.getEmail())
            .withMaxWidth("400px")
            .required())

        .addField("Phone", TextInput.tel("phone")
            .withValue(user.getPhone())
            .withMaxWidth("200px"))

        .addField("Bio", TextArea.create("bio")
            .withValue(user.getBio())
            .withRows(5)
            .withMaxWidth("600px"))

        .addField("Birth Date", TextInput.date("birth_date")
            .withValue(user.getBirthDate())
            .withMaxWidth("200px"))

        .addField("Country", Select.create("country")
            .addOption("us", "United States", "us".equals(user.getCountry()))
            .addOption("uk", "United Kingdom", "uk".equals(user.getCountry()))
            .addOption("ca", "Canada", "ca".equals(user.getCountry()))
            .withMaxWidth("300px"))

        .addField("", Button.submit("Save Changes")
            .withClass("btn btn-primary")
            .withMinWidth("150px"))

        .render();
}
```

### Example 4: Search Form

```java
public String buildSearchForm() {
    return Form.create()
        .withAction("/search")
        .withMethod(Form.Method.GET)  // GET for search (no CSRF needed)
        .withClass("search-form")

        .addField("", TextInput.search("q")
            .withPlaceholder("Search...")
            .withMaxWidth("500px")
            .required())

        .addField("", Select.create("category")
            .addOption("", "All Categories")
            .addOption("products", "Products")
            .addOption("articles", "Articles")
            .addOption("users", "Users")
            .withMaxWidth("200px"))

        .addField("", Button.submit("Search")
            .withClass("btn btn-primary"))

        .render();
}
```

## HTMX Integration

Forms work seamlessly with HTMX for dynamic submission.

### HTMX Form Submission

```java
Form.create()
    .withHxPost("/api/contact")           // HTMX POST request
    .withHxTarget("#result")              // Where to put response
    .withHxSwap("innerHTML")              // How to swap content
    .withCsrfToken(csrfToken.getToken())  // Include CSRF

    .addField("Email", TextInput.email("email").required())
    .addField("Message", TextArea.create("message").required())
    .addField("", Button.submit("Send"));
```

### Spring Controller for HTMX

```java
@PostMapping("/api/contact")
@ResponseBody
public String handleContact(@RequestParam String email,
                           @RequestParam String message,
                           @RequestHeader("HX-Request") String hxRequest) {
    // Verify HTMX request
    if (hxRequest == null) {
        return Alert.error("Invalid request").render();
    }

    // Process form
    contactService.send(email, message);

    // Return HTML fragment (not full page)
    return Alert.success("Message sent successfully!").render();
}
```

See Part 9 for comprehensive HTMX guide.

## Best Practices

### 1. Always Use Server-Side Validation

```java
// Client-side (HTML5) - convenience only
input.required().withAttribute("minlength", "3");

// Server-side (Spring) - CRITICAL security
@Valid @ModelAttribute RegistrationRequest request
```

### 2. Include CSRF Tokens

```java
// Always include in POST forms
form.withCsrfToken(csrfToken.getToken());
```

### 3. Constrain Input Widths

```java
// Good UX
TextInput.email("email").withMaxWidth("400px");

// Poor UX
TextInput.email("email");  // Full width, looks awkward
```

### 4. Use Appropriate Input Types

```java
// Good - HTML5 type benefits
TextInput.email("email");    // Email keyboard on mobile
TextInput.tel("phone");      // Phone keyboard
TextInput.date("birthdate"); // Native date picker

// Avoid - generic text input
TextInput.create("email");   // No validation or mobile optimization
```

### 5. Provide Clear Placeholders

```java
// Good
TextInput.email("email").withPlaceholder("you@example.com");

// Avoid
TextInput.email("email").withPlaceholder("Email");  // Not helpful
```

### 6. Use Required Validation

```java
// Mark required fields
input.required();
```

## Key Takeaways

1. **10 TextInput Types**: create, email, password, number, date, search, tel, url, time, datetime
2. **Form Components**: TextInput, TextArea, Select, Checkbox, RadioGroup, Button, Form
3. **CSRF Protection**: Always include `withCsrfToken()` in POST forms
4. **Validation**: HTML5 (client) + Spring Validation (server)
5. **Width Constraints**: Use `withMaxWidth()` for better UX
6. **HTML5 Benefits**: Native validation, mobile keyboards, date pickers
7. **HTMX Ready**: Forms support HTMX attributes for dynamic submission
8. **Security First**: Never trust client-side validation alone

---

**Previous**: [Part 7: CSS and Styling](07-css-and-styling.md)

**Next**: [Part 9: HTMX and Dynamic Features](09-htmx-dynamic-features.md)

**Table of Contents**: [Getting Started Guide](README.md)
