# Part 1: Introduction to Java HTML Framework

Welcome to the Java HTML Framework (JHF) Getting Started Guide! This comprehensive tutorial will teach you everything you need to build modern, server-side rendered web applications using pure Java.

## What is JHF?

The Java HTML Framework is a lightweight, domain-specific framework for building server-side rendered web applications with minimal JavaScript. Instead of wrestling with complex frontend build tools, component transpilers, or learning multiple languages, you build your entire UI using **fluent, type-safe Java APIs**.

### Core Philosophy

JHF is built on these fundamental principles:

1. **Server-First Rendering**: All HTML generation happens on the server using Java. No complex frontend build process required.

2. **Java-Native Development**: Build entire UIs using fluent, type-safe Java APIs. If you know Java, you can build web UIs.

3. **Minimal JavaScript**: Use HTMX sparingly for dynamic updates only where needed (more on this later).

4. **Simplicity Over Abstraction**: Avoid over-engineering. Keep patterns simple and maintainable.

5. **Composable Architecture**: Build complex UIs from simple, reusable primitives using composition.

### Target Audience

JHF is designed for:
- **Java developers with limited web experience** - You don't need to be a web expert
- **Teams maintaining data-heavy applications** - Research portals, forums, content management
- **Projects requiring minimal client-side JavaScript** - Server-side rendering is a feature, not a limitation
- **Applications with strong type safety requirements** - Leverage Java's type system for UI

### Use Cases

JHF excels at:
- Research portals with heavy data visualization
- Community platforms (forums, discussions, journals)
- Content management systems with user-generated content
- Data-heavy applications requiring minimal client-side JavaScript
- Internal tools and dashboards
- Any application where server-side rendering is preferred

## Why JHF?

### Traditional Web Development Challenges

Building web applications traditionally requires:
- Learning HTML, CSS, and JavaScript (3 different languages)
- Setting up complex build pipelines (webpack, npm, etc.)
- Managing frontend frameworks (React, Vue, Angular)
- Synchronizing backend and frontend code
- Dealing with API contracts and data serialization

### The JHF Approach

JHF simplifies this dramatically:

```java
// Traditional approach: Separate HTML template, controller, and JavaScript
// - users.html template file
// - UserController.java backend
// - users.js frontend logic
// - API endpoint for data
// - JSON serialization/deserialization

// JHF approach: Everything in type-safe Java
Page page = new Page();
page.addRow(new Row()
    .addColumn(new Column(12)
        .addModule(DataModule.create(User.class)
            .withData(userService.findAll())
            .withColumn("Name", User::getName)
            .withColumn("Email", User::getEmail)
            .withColumn("Role", User::getRole))));
return page.render();
```

### Key Benefits

1. **No Build Process**: No webpack, no npm, no transpiling. Just Java compilation.

2. **Type Safety**: Catch errors at compile time, not runtime in a browser.

3. **Single Language**: Write UI, business logic, and data access all in Java.

4. **Familiar Tools**: Use your existing Java IDE, debugger, and testing tools.

5. **Composable Components**: Build complex UIs from simple, reusable parts.

6. **Progressive Enhancement**: Start with server-side rendering, add dynamics with HTMX.

7. **Easy Testing**: Test UI rendering logic with standard JUnit tests.

## Installation & Setup

### Prerequisites

- Java 17 or higher
- Maven or Gradle
- Spring Boot application (JHF integrates seamlessly)
- Your favorite Java IDE (IntelliJ IDEA, Eclipse, VS Code)

### Maven Dependency

Add JHF to your Spring Boot project's `pom.xml`:

```xml
<dependency>
    <groupId>io.mindspice</groupId>
    <artifactId>java-html-framework</artifactId>
    <version>1.0.0</version>
</dependency>
```

That's it! JHF has minimal dependencies:
- Spring Boot Starter Web (you likely already have this)
- HTMX (for dynamic features, loaded via WebJars)
- CommonMark (for Markdown rendering)
- OWASP Encoder (for security)

### Project Structure

Your typical Spring Boot project structure remains unchanged:

```
my-app/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/myapp/
│   │   │       ├── controller/     # Spring controllers
│   │   │       ├── pages/          # JHF page classes (NEW)
│   │   │       ├── service/        # Business logic
│   │   │       └── model/          # Domain models
│   │   └── resources/
│   │       ├── static/
│   │       │   └── css/            # Custom CSS (optional)
│   │       └── application.properties
│   └── test/
│       └── java/                    # JUnit tests
└── pom.xml
```

### IDE Setup (Optional)

For the best experience:
- **IntelliJ IDEA**: Enable Spring support, Java 17+ syntax
- **Eclipse**: Install Spring Tools Suite (STS)
- **VS Code**: Install Java Extension Pack and Spring Boot Extensions

No special web development plugins required!

## Your First JHF Application

Let's build a simple "Hello World" page to see JHF in action. We'll create a page that displays a welcome message with some styling.

### Understanding Web Pages (Web Basics)

Before we write code, a quick primer: A web page is just HTML (HyperText Markup Language) sent from a server to your browser. The browser reads this HTML and displays it visually.

HTML is made up of **elements** (also called "tags") like:
- `<h1>Hello World</h1>` - A heading
- `<p>This is a paragraph.</p>` - A paragraph of text
- `<div>...</div>` - A container (division) for other elements

JHF generates these HTML elements using Java code instead of writing HTML by hand.

### Step 1: Create a Page Class

Create a new class `WelcomePage.java` in your `pages` package:

```java
package com.example.myapp.pages;

import io.mindspice.simplypages.components.*;
import io.mindspice.simplypages.layout.*;
import org.springframework.stereotype.Component;

@Component
public class WelcomePage {

    public String buildPage() {
        Page page = new Page();

        Row row = new Row();
        Column col = new Column(12);

        col.addModule(ContentModule.create()
            .withTitle("Welcome to JHF!")
            .withContent("You just built your first page using the Java HTML Framework. Everything you see here was generated using pure Java code."));

        row.addColumn(col);
        page.addRow(row);

        return page.render();
    }
}
```

Let's break this down:

1. **new Page()**: Creates a new page container
2. **new Row()**: Creates a horizontal row
3. **new Column(12)**: Creates a full-width column (12 out of 12 columns)
4. **ContentModule.create()**: Creates a module with title and content
5. **render()**: Generates the final HTML string

Notice the **fluent API** pattern: methods return `this` or the parent object, allowing chaining.

### Step 2: Create a Controller

Now create a Spring controller to serve this page at `http://localhost:8080/welcome`:

```java
package com.example.myapp.controller;

import com.example.myapp.pages.WelcomePage;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class WelcomeController {

    private final WelcomePage welcomePage;

    public WelcomeController(WelcomePage welcomePage) {
        this.welcomePage = welcomePage;
    }

    @GetMapping("/welcome")
    @ResponseBody
    public String welcome() {
        return welcomePage.buildPage();
    }
}
```

Key points:

1. **@Controller**: Tells Spring this handles web requests
2. **@GetMapping("/welcome")**: Maps the URL `/welcome` to this method
3. **@ResponseBody**: Returns the HTML string directly (not a template name)
4. **Constructor injection**: Spring automatically injects `WelcomePage`

### Step 3: Run and Test

Start your Spring Boot application:

```bash
./mvnw spring-boot:run
```

Open your browser and navigate to: `http://localhost:8080/welcome`

You should see your content displayed.

### What Just Happened? (Behind the Scenes)

Let's understand the flow:

1. **Browser Request**: Your browser sends an HTTP GET request to `http://localhost:8080/welcome`

2. **Spring Routing**: Spring receives the request and finds the `@GetMapping("/welcome")` method

3. **Method Execution**: Spring calls `welcomePage.buildPage()`

4. **JHF Rendering**:
   - `Page` creates a Page object
   - Components create HTML tag objects
   - `render()` walks through all components and generates HTML string

5. **HTML Response**: Spring returns the HTML string to the browser

6. **Browser Rendering**: Your browser receives the HTML and displays it visually

## Complete Working Example

Here's a slightly more complex example showing multiple components and layout:

```java
package com.example.myapp.pages;

import io.mindspice.simplypages.components.display.*;
import io.mindspice.simplypages.components.navigation.Link;
import io.mindspice.simplypages.layout.*;
import io.mindspice.simplypages.modules.*;
import org.springframework.stereotype.Component;

@Component
public class HomePage {

    public String buildPage() {
        Page page = new Page();

        // Header Row
        Row headerRow = new Row();
        headerRow.addColumn(new Column(12)
            .addModule(HeroModule.create()
                .withTitle("My JHF Application")
                .withDescription("A modern web application built with Java HTML Framework")));
        page.addRow(headerRow);

        // Content Row
        Row contentRow = new Row();

        // Main Content (8/12)
        contentRow.addColumn(new Column(8)
            .addModule(ContentModule.create()
                .withTitle("Getting Started")
                .withContent("JHF makes building web UIs simple for Java developers.")));

        // Sidebar (4/12)
        contentRow.addColumn(new Column(4)
            .addModule(SimpleListModule.create()
                .withTitle("Quick Links")
                .addItem("Home", "/")
                .addItem("About", "/about")
                .addItem("Contact", "/contact")));

        page.addRow(contentRow);

        return page.render();
    }
}
```

This example demonstrates:
- **Multi-row layouts**: Header and content rows
- **Responsive grid**: 8/4 column split (66%/33% on desktop, stacks on mobile)
- **Component composition**: Modules nested inside columns

## Key Concepts Recap

Before moving on, let's recap the key concepts you've learned:

### 1. Components
Building blocks of your UI. Examples: `Button`, `Div`, `Card`, `Link`

### 2. Fluent API
Methods that return `this` or the parent, enabling chaining.

### 3. Layout System
`Page` → `Row` → `Column` → Modules

### 4. Rendering
Call `.render()` on a Page to generate the final HTML string

### 5. Spring Integration
- Page classes generate HTML
- Controllers handle HTTP routing
- Dependency injection connects them

## What's Next?

You've successfully created your first JHF application! In the next parts of this guide, you'll learn:

- **Part 2: Core Concepts** - Deep dive into components, modules, pages, and best practices
- **Part 3: Custom Components** - Build your own reusable components
- **Part 4: Custom Modules** - Create high-level functional modules
- **Part 5: Pages and Layouts** - Master the responsive grid system
- **Part 6: Shell Configuration** - Build complete application shells with navigation
- **Part 7: CSS and Styling** - Comprehensive CSS guide and customization
- **Part 8: Forms** - Build complex forms with validation
- **Part 9: HTMX and Dynamic Features** - Add dynamic interactions without JavaScript
- **Part 10: Building a Forum** - Complete tutorial building a discussion forum
- **Part 11: Spring Integration** - Advanced Spring Boot integration patterns
- **Part 12: Security** - Comprehensive security best practices
- **Part 13: Templates and Dynamic Updates** - High performance dynamic rendering
- **Part 14: Shells and Navigation** - Building application shells
- **Part 15: Building Pages** - In-depth page construction
- **Guides**:
    - [Editable Pages](../guides/01-editable-pages.md)
    - [User Craftable Pages](../guides/02-user-craftable-pages.md)
    - [Communal Pages](../guides/03-communal-pages.md)

---

**Next**: [Part 2: Core Concepts](02-core-concepts.md)

**Table of Contents**: [Getting Started Guide](README.md)
