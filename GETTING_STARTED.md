# Getting Started with JHF (Java HTML Framework)

This guide provides a comprehensive overview of using JHF to build server-side rendered web applications. It covers everything from basic configuration to advanced dynamic features.

## Table of Contents
1. [Introduction](#introduction)
2. [Best Practices & Gotchas](#best-practices--gotchas)
3. [Configuring the Shell](#configuring-the-shell)
4. [Styling & CSS](#styling--css)
5. [Custom Components, Modules, and Pages](#custom-components-modules-and-pages)
6. [Dynamic Aspects (HTMX)](#dynamic-aspects-htmx)
7. [Simple Spring Implementation](#simple-spring-implementation)
8. [Security Practices](#security-practices)

---

## Introduction

JHF is a domain-specific framework designed for building data-heavy applications with minimal JavaScript. It operates on a "Server-First" philosophy, where all rendering happens on the server, and the client receives ready-to-display HTML. It leverages modern standards and libraries like HTMX to provide interactivity without the complexity of a full Single Page Application (SPA) framework.

## Best Practices & Gotchas

### Best Practices
*   **Use the Builder Pattern**: Most components and layouts are designed to be created using fluent builder APIs (e.g., `ShellBuilder.create()...`, `Page.builder()...`). This ensures type safety and readability.
*   **Leverage Modules**: Instead of manually assembling every page from primitive components (`Div`, `Button`), use `Module`s (like `FormModule`, `DataModule`) to group related functionality. This promotes consistency and reusability.
*   **Stateless Components**: Treat components as stateless rendering instructions. A new component tree is typically built for every request. Do not try to store request-specific state in static component instances.
*   **HTMX for Interactivity**: Use HTMX attributes (`hx-get`, `hx-post`, `hx-target`) for dynamic updates instead of writing custom JavaScript.
*   **Type Safety**: Use Java's type system. If a component expects a `List<String>`, passing it ensures you won't have runtime errors related to data structure mismatches.

### Gotchas
*   **Server-Side Rendering Only**: Remember that `render()` returns a `String` of HTML. You cannot manipulate the DOM directly from Java code after the response is sent. All changes require a new server request (or HTMX partial update).
*   **Thread Safety**: `Module` instances are **not thread-safe**. Always create new module instances for each request. Do not share module instances across multiple threads or requests, as they may hold state during the rendering process.
*   **CSS Class Overwriting**: When using `withClass()`, generally it appends to existing classes. However, be mindful of how specific components handle class attributes to avoid accidentally removing required framework classes.
*   **HTMX Targeting**: When using `hx-target`, ensure the target ID exists in the DOM. If the ID is missing, the update will fail silently or behave unexpectedly.

---

## Configuring the Shell

The "Shell" is the outer layout of your application, including the HTML head, body, top navigation (banner), side navigation, and the main content area. JHF provides a `ShellBuilder` to configure this easily.

### Using `ShellBuilder`

You typically configure the shell once (or per layout type) and reuse the logic.

```java
import io.mindspice.jhf.builders.ShellBuilder;
import io.mindspice.jhf.components.navigation.SideNav;
// ... other imports

public String createApplicationShell() {
    return ShellBuilder.create()
        .withPageTitle("My Application")
        .withTopBanner(
            // Configure your top banner/header here
            createTopBanner()
        )
        .withSideNav(
            // Configure your side navigation here
            createSideNav(),
            true // Make it collapsible
        )
        .withContentTarget("content-area") // The ID where main content will be injected
        .withHtmx(true) // Enable HTMX (default is true)
        .build(); // Returns the full HTML string
}
```

*   **`withContentTarget(String targetId)`**: This is crucial. It defines the `id` of the `div` where your pages will be loaded dynamically via HTMX. The default is usually `"content-area"`.
*   **`withHtmx(boolean include)`**: Includes the HTMX library automatically. Keep this `true` for dynamic features.

---

## Styling & CSS

JHF comes with a built-in CSS framework designed to work out-of-the-box.

### Internal CSS (`framework.css`)

The core styles are located in `framework.css` (typically served from `/css/framework.css`). This file defines:
*   **Reset & Base Styles**: Normalizes browser defaults.
*   **Layout System**: Grid, containers, and shell structure (sidebar, content area).
*   **Component Styles**: Default looks for buttons, forms, tables, cards, alerts, etc.
*   **Utility Classes**: Helpers for spacing (`m-1`, `p-2`), text alignment (`text-center`), and display (`d-none`).

**Key Selectors to Know:**
*   `.main-container`: The grid wrapper for sidebar + content.
*   `.main-sidebar`: The side navigation area.
*   `.content-wrapper`: The container for your page content.
*   `.btn-*`, `.alert-*`, `.card`: Component classes.

### Customizing Styles

You have two main ways to customize the look and feel:

1.  **Overriding CSS**:
    Create your own CSS file (e.g., `custom.css`) and include it in the shell configuration.

    ```java
    ShellBuilder.create()
        // ... other config
        .withCustomCss("/css/custom.css") // Path to your custom CSS file
        .build();
    ```

    In `custom.css`, you can override framework classes:
    ```css
    /* Override primary button color */
    .btn-primary {
        background-color: #ff5722;
    }
    ```

2.  **Custom Classes on Components**:
    Almost every component has a `withClass("my-class")` method. This appends your class to the component's class list.

    ```java
    Button.create("Click Me")
        .withClass("my-custom-button");
    ```

    Then define `.my-custom-button` in your CSS.

---

## Custom Components, Modules, and Pages

### Custom Components

If the built-in library (Buttons, Cards, Inputs) isn't enough, you can create your own.
A component is simply a class that implements the `Component` interface (or extends `HtmlTag`).

**Simple Component Example:**

```java
public class UserBadge extends HtmlTag {
    public UserBadge(String username, String role) {
        super("div"); // The root tag is a <div>
        this.withClass("user-badge");

        // Add children
        this.withChild(new Span(username).withClass("username"));
        this.withChild(new Span(role).withClass("role-tag"));
    }

    // Static factory for convenience
    public static UserBadge create(String username, String role) {
        return new UserBadge(username, role);
    }
}
```

### Modules

Modules are higher-level compositions. While a `Component` might be a button, a `Module` is a "User Registration Form" or a "Sales Data Table".
Extend `Module` to create reusable sections.

```java
public class UserProfileModule extends Module {
    private User user;

    private UserProfileModule() {
        super("div"); // Root element
        this.withClass("user-profile-module");
    }

    public static UserProfileModule create() {
        return new UserProfileModule();
    }

    public UserProfileModule withUser(User user) {
        this.user = user;
        return this;
    }

    @Override
    protected void buildContent() {
        // This method is called when .render() is called
        if (user != null) {
            this.withChild(new Header.H3(user.getName()));
            this.withChild(new Paragraph("Email: " + user.getEmail()));
        } else {
            this.withChild(new Alert.Warning("No user selected"));
        }
    }
}
```

### Pages

A `Page` in JHF is a layout wrapper that organizes your components/modules into a grid or structure.

```java
Page.builder()
    .addComponents(new Header.H1("Welcome"))
    .addRow(row -> row
        .withChild(new Column().withWidth(8).withChild(new MainContentModule()))
        .withChild(new Column().withWidth(4).withChild(new SidebarModule()))
    )
    .build();
```

---

## Dynamic Aspects (HTMX)

JHF uses [HTMX](https://htmx.org/) to add interactivity. HTMX allows you to access AJAX, CSS Transitions, WebSockets and Server Sent Events directly in HTML, using attributes.

### How it works (Simplified)
Instead of writing JavaScript to fetch data and update the DOM, you add attributes to your HTML elements (via Java methods).
When a user interacts (clicks, types), HTMX sends a request to the server, gets back **HTML**, and swaps it into the page.

### Example: Building a Simple Forum

Let's build a forum post list where clicking "Load More" appends new posts without refreshing the page.

**1. The "Load More" Button (Frontend)**
In your Java component for the button:

```java
Button.create("Load More")
    .withAttribute("hx-get", "/forum/posts?page=2") // 1. When clicked, GET this URL
    .withAttribute("hx-target", "#post-list")       // 2. Put the result in the element with id="post-list"
    .withAttribute("hx-swap", "beforeend");         // 3. Append the result (don't replace everything)
```

**2. The Controller (Backend)**
Spring controller handling the request:

```java
@GetMapping("/forum/posts")
@ResponseBody
public String getMorePosts(@RequestParam int page) {
    // 1. Fetch data for page
    List<Post> posts = postService.getPosts(page);

    // 2. Build HTML fragments (just the new posts, not a full page)
    PostList list = new PostList();
    posts.forEach(p -> list.addPost(new ForumPost(p)));

    // 3. Return HTML string
    return list.render();
}
```

### Forms with HTMX

Submitting a form without a full page reload:

```java
Form.create()
    .withHxPost("/forum/submit-post")   // Send POST to this URL
    .withHxTarget("#forum-container")   // Update this container
    .addField("Subject", TextInput.create("subject"))
    .addField("Message", TextArea.create("message"))
    .addField("", Button.submit("Post"));
```

**What happens behind the scenes:**
1.  User fills form and clicks submit.
2.  Browser intercepts the submit.
3.  HTMX sends the form data via AJAX POST to `/forum/submit-post`.
4.  Server processes data and returns the updated forum list HTML (or a success message).
5.  Browser replaces the content of `#forum-container` with the new HTML.

---

## Simple Spring Implementation

Here is how you wire it all together in a Spring Boot Controller.

**One Page Endpoint (The "Home" Page)**

```java
@Controller
public class HomeController {

    // 1. serve the full Shell (only loaded once)
    @GetMapping("/")
    @ResponseBody
    public String index() {
        return ShellBuilder.create()
            .withPageTitle("My App")
            .withContentTarget("content-area")
            .withHtmx(true)
            .build(); // Returns full <html>...</html>
    }

    // 2. Serve the content (loaded via HTMX or initially)
    @GetMapping("/home")
    @ResponseBody
    public String homeContent() {
        // Create the page content
        Page page = Page.builder()
            .addComponents(Header.H1("Welcome to JHF"))
            .addComponents(new Paragraph("This is loaded dynamically!"))
            .build();

        return page.render(); // Returns just the <div>...</div> content
    }
}
```

*   **`@ResponseBody`**: Essential because we are returning raw HTML strings, not resolving to a template file (like Thymeleaf).
*   **The Flow**:
    1.  User visits `/`. Server returns the Shell.
    2.  The Shell includes `<div id="content-area" hx-get="/home" hx-trigger="load"></div>`.
    3.  Browser loads Shell, sees `hx-trigger="load"`, and immediately requests `/home`.
    4.  Server returns the home content HTML.
    5.  HTMX inserts it into `content-area`.

---

## Security Practices

When building web applications, security is paramount. Since you are handling raw HTML rendering, be aware of the following:

1.  **XSS (Cross-Site Scripting)**:
    *   **The Risk**: If you take user input (like a forum post title) and render it directly into HTML, a malicious user could input `<script>alert('hacked')</script>`.
    *   **JHF Protection**: By default, standard JHF components (like `Paragraph`, `Header`, `TextNode`) should escape special characters.
    *   **Your Responsibility**: When creating custom components or using `innerHTML`, **always sanitize user input**. Ensure that data stored in your database is safe or sanitized before rendering.

2.  **CSRF (Cross-Site Request Forgery)**:
    *   **The Risk**: Attackers tricking users into performing actions (like submitting a form) without their consent.
    *   **Spring Security**: If using Spring Security, ensure CSRF protection is configured. You may need to include the CSRF token in your JHF forms or HTMX headers.
    *   **HTMX & CSRF**: You can configure HTMX to include the CSRF token in headers globally.

3.  **Input Validation**:
    *   Always validate data on the **server side** (in your Controller), even if you have frontend validation. Frontend checks can be bypassed.

4.  **Authorization**:
    *   Ensure every endpoint (`/home`, `/admin/users`) checks if the current user has permission to view that data. Just hiding the "Admin" button isn't enough; the endpoint itself must be secured.

5.  **Dependencies**:
    *   Keep JHF and underlying libraries (Spring Boot, HTMX) up to date to patch known vulnerabilities.
