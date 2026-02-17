# Java HTML Framework - Getting Started Guide

Welcome to the comprehensive Getting Started Guide for the Java HTML Framework (JHF)! This guide teaches you everything you need to build modern, server-side rendered web applications using pure Java.

## About This Guide

This documentation is designed for **Java developers with minimal to no web development experience**. We teach web concepts alongside JHF framework usage, providing complete working examples throughout.

### What You'll Learn

- **Framework Fundamentals**: Components, modules, pages, and rendering
- **Building UIs**: Custom components, modules, and responsive layouts
- **Dynamic Features**: Forms, HTMX integration, and interactive elements
- **Spring Integration**: Controller patterns and HTTP layer integration
- **Security**: Comprehensive security best practices
- **Real-World Application**: Build a complete forum from scratch

### Prerequisites

- Java 25 or higher
- Basic Java knowledge (classes, interfaces, generics)
- Spring Boot experience (intermediate level)
- Maven or Gradle
- Your favorite Java IDE

No web development experience required - we'll teach you what you need to know!

## Documentation Structure

This guide is organized into 12 sequential parts. We recommend reading them in order, especially if you're new to web development.

### Part 1: Introduction
**[01-introduction.md](01-introduction.md)**

- What is JHF and why use it?
- Installation and project setup
- Your first "Hello World" application
- Framework philosophy and benefits
- Quick start example with Spring Boot

*Start here if you're new to JHF*

---

### Part 2: Core Concepts
**[02-core-concepts.md](02-core-concepts.md)**

- Component hierarchy (Component interface, HtmlTag base class)
- Components vs Modules - when to use each
- Page structure and PageBuilder pattern
- Rendering flow and lifecycle
- Best practices for clean code
- **Common gotchas**: re-rendering bug, class attribute handling, sizing decisions

*Essential reading for understanding the framework architecture*

---

### Part 3: Building Custom Components
**[03-custom-components.md](03-custom-components.md)**

- When to create custom components
- Extending HtmlTag with properties and methods
- Factory method pattern for instantiation
- Fluent API design principles
- **Complete example**: Custom Notification component
- Enum-based styling and composition patterns
- Testing custom components

*Learn to extend JHF with your own reusable components*

---

### Part 4: Building Custom Modules
**[04-custom-modules.md](04-custom-modules.md)**

- Modules vs components (high-level vs low-level)
- Extending Module base class
- `buildContent()` lazy building pattern
- Module composition strategies
- **Complete examples**: ProfileCard and ActivityFeed modules
- HTMX integration for dynamic modules
- Testing modules

*Master building complex, reusable UI sections*

---

### Part 5: Pages and Layouts
**[05-pages-and-layouts.md](05-pages-and-layouts.md)**

- Page structure and PageBuilder pattern
- **12-column responsive grid system** (primary layout mechanism)
- Column width options: `withWidth(1-12)`, `auto()`, `fill()`
- Row auto-wrapping behavior
- Responsive breakpoints (mobile, tablet, desktop)
- **Grid vs CSS width** - when to use each
- Complete layout examples (dashboard, blog, e-commerce)

*Learn to build professional responsive layouts*

---

### Part 6: Shell Configuration
**[06-shell-configuration.md](06-shell-configuration.md)**

- Application shell architecture
- **ShellBuilder**: Complete page structure generation
- **BannerBuilder**: Header branding
- **AccountBarBuilder**: Secondary navigation
- **SideNavBuilder**: Primary navigation sidebar
- Collapsible sidebar (desktop/mobile)
- Spring service integration pattern
- User-specific shell customization

*Build consistent application shells with navigation*

---

### Part 7: CSS and Styling
**[07-css-and-styling.md](07-css-and-styling.md)**

- **Comprehensive CSS fundamentals** for Java developers
  - CSS syntax, selectors, and specificity
  - Box model and `box-sizing: border-box`
  - Responsive design with media queries
  - Mobile-first approach
- **framework.css structure** (1,540 lines)
  - Grid system, typography, components
  - Color palette and spacing scale
- **Utility classes** reference
- **Customization approaches**
  - Custom stylesheets
  - Inline styles
  - Custom CSS classes
- **Complete dark theme example**

*Master CSS for styling and customization*

---

### Part 8: Forms
**[08-forms.md](08-forms.md)**

- **Form components**: TextInput, TextArea, Select, Checkbox, RadioGroup, Button, Form
- **10 TextInput types**: email, password, date, number, search, tel, url, time, datetime
- Width constraints for better UX (`withMaxWidth`, `withMinWidth`)
- **HTML5 validation** (client-side convenience)
- **Server-side validation** (critical security - Spring Validation)
- **CSRF protection** with Spring Security
- Complete form examples (contact, registration, profile edit)
- HTMX form submission

*Build professional, secure forms*

---

### Part 9: HTMX and Dynamic Features
**[09-htmx-dynamic-features.md](09-htmx-dynamic-features.md)**

- **What is HTMX?** (for beginners) - HTML attributes for dynamic interactions
- How HTMX works (AJAX requests returning HTML fragments)
- When to use HTMX (and when not to)
- **Core attributes**: `hx-get`, `hx-post`, `hx-target`, `hx-swap`, `hx-trigger`
- **Common patterns**:
  - Load more (pagination)
  - Auto-refresh (polling)
  - Lazy loading (on scroll)
  - Form submission with inline validation
  - Inline editing
  - Filtering/sorting
- Spring endpoint pattern for HTMX
- Security considerations (verify `HX-Request` header, CSRF)

*Add dynamic behavior without writing JavaScript*

---

### Part 10: Building a Forum Tutorial
**[10-forum-tutorial.md](10-forum-tutorial.md)**

- **Step-by-step tutorial** building a complete discussion forum
- Mock data structure (no database required)
- Thread list page with JHF components
- Thread detail page with comments
- **Post comment feature** with HTMX (dynamic submission)
- **Voting feature** with HTMX (upvote/downvote)
- Custom CSS styling
- Complete working code (ready to run)
- Ideas for extending the forum

*Build a real-world application from scratch*

---

### Part 11: Spring Integration
**[11-spring-integration.md](11-spring-integration.md)**

- JHF + Spring Boot architecture
- **Separation of concerns** (Controllers vs Page classes)
- Dependency injection patterns (constructor injection)
- Request parameters (path variables, query params, form data)
- **HTMX endpoint pattern** (detecting `HX-Request` header)
- Form submission (GET and POST)
- **CSRF integration** with Spring Security
- Error handling and custom error pages
- Static resources
- Complete product catalog example
- Best practices (thin controllers, page class reusability)

*Integrate JHF seamlessly with Spring Boot*

---

### Part 12: Security Best Practices
**[12-security.md](12-security.md)**

- **Security mindset** and threat model
- **XSS prevention** (Cross-Site Scripting)
  - OWASP Encoder for attributes
  - Markdown escaping
  - Safe output rendering
- **CSRF prevention** (Cross-Site Request Forgery)
  - Spring Security token integration
  - HTMX CSRF handling
- **Server-side validation** (never trust client)
- **SQL injection prevention** (JPA parameterized queries)
- **HTMX security** (verify headers, authorization)
- **Authorization** (Spring Security `@PreAuthorize`)
- **File upload security** (type validation, safe storage)
- **Password security** (BCrypt hashing)
- **Session security** (HttpOnly, Secure cookies)
- **HTTPS in production** (SSL/TLS configuration)
- **Security headers** (CSP, HSTS, X-Frame-Options)
- **Complete security checklist**

*Protect your application from common vulnerabilities*

---

## Quick Reference

### Key Concepts

**Component**: Basic building block (HTML element)
```java
Header.h1("Title"), Paragraph.create(), Card.create()
```

**Module**: High-level reusable section (combines components)
```java
ContentModule.create(), DataModule.create(), ForumModule.create()
```

**Page**: Complete page container
```java
Page.create().addRow(...).render()
```

**Grid**: 12-column responsive layout system
```java
Row.create()
    .withChild(Column.create().withWidth(8).withChild(main))
    .withChild(Column.create().withWidth(4).withChild(sidebar))
```

**HTMX**: Dynamic interactions without JavaScript
```java
component.withAttribute("hx-get", "/api/data")
         .withAttribute("hx-target", "#result")
         .withAttribute("hx-swap", "innerHTML")
```

### Common Patterns

**Build a Page**:
```java
@Component
public class MyPage {
    public String buildPage() {
        return Page.create()
            .addComponents(Header.h1("Title"))
            .render();
    }
}
```

**Create a Form**:
```java
Form.create()
    .withCsrfToken(csrfToken.getToken())
    .addField("Email", TextInput.email("email").required())
    .addField("", Button.submit("Submit"))
```

**Two-Column Layout**:
```java
Page.create()
    .addRow(row -> row
        .withChild(Column.create().withWidth(8).withChild(content))
        .withChild(Column.create().withWidth(4).withChild(sidebar)))
```

**HTMX Load More**:
```java
Button.create("Load More")
    .withAttribute("hx-get", "/api/more")
    .withAttribute("hx-target", "#list")
    .withAttribute("hx-swap", "beforeend")
```

### File Locations

**Project Structure**:
```
src/main/java/com/example/
├── controller/          # Spring controllers
├── pages/              # JHF page classes
├── service/            # Business logic
└── model/              # Domain models

src/main/resources/
├── static/
│   ├── css/           # CSS stylesheets
│   ├── js/            # JavaScript files
│   └── images/        # Images
└── application.properties
```

**CSS File**: `/src/main/resources/static/css/framework.css` (1,540 lines)

## Next Steps

### 1. Start with Part 1: Introduction
Read [01-introduction.md](01-introduction.md) to understand JHF and build your first application.

### 2. Work Through Parts 2-7
Master the fundamentals: components, modules, layouts, shell, CSS, and forms.

### 3. Build the Forum Tutorial (Part 10)
Apply your knowledge by building a complete working application.

### 4. Study Security (Part 12)
Learn to protect your application before deploying to production.

### 5. Explore the Demo Application
The JHF demo at `http://localhost:8080` showcases all components and patterns.

## Additional Resources

### Framework Documentation

- **This Getting Started Guide** - Comprehensive tutorial
- **Main README.md** - Quick reference and API overview
- **CLAUDE.md** - Detailed framework reference (project root)

### External Resources

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [HTMX Documentation](https://htmx.org/docs/)
- [CommonMark Spec](https://commonmark.org/)
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)

### Getting Help

- Check the documentation first (you're reading it!)
- Review the demo application code
- Search for similar patterns in this guide
- Consult the main CLAUDE.md file for advanced topics

## Philosophy

JHF is built on these principles:

1. **Server-First**: All HTML generation on the server using Java
2. **Java-Native**: Build UIs with fluent, type-safe Java APIs
3. **Minimal JavaScript**: Use HTMX for dynamics, no complex frontend build
4. **Simplicity**: Avoid over-engineering and over-abstraction
5. **Composable**: Build complex UIs from simple primitives
6. **For Java Developers**: No web expertise required to get started

## Contributing

This documentation is designed for the alpha release of JHF. If you find errors, have suggestions, or want to contribute examples, please let us know!

---

**Ready to get started?** Begin with [Part 1: Introduction](01-introduction.md)

**Happy building with JHF!**
