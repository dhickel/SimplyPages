# Part 11: Spring Integration

JHF integrates seamlessly with Spring Boot, providing clean separation between HTTP concerns (controllers) and rendering logic (page classes). This guide covers integration patterns, assuming you have intermediate Spring Boot knowledge.

## JHF + Spring Boot Overview

### Clean Separation of Concerns

**JHF Philosophy**: Keep rendering logic separate from HTTP layer

```
┌─────────────────┐
│   Controller    │  ← HTTP concerns (routing, headers, status)
│   (@Controller) │
└────────┬────────┘
         │ calls
         ▼
┌─────────────────┐
│   Page Class    │  ← Content generation (JHF rendering)
│   (@Component)  │
└────────┬────────┘
         │ uses
         ▼
┌─────────────────┐
│  JHF Framework  │  ← Pure rendering library
│  (Components)   │
└─────────────────┘
```

**Benefits**:
- **Testability**: Test rendering logic without HTTP layer
- **Reusability**: Use same page class in multiple contexts
- **Clarity**: Each class has single responsibility

### JHF is HTTP-Agnostic

JHF has no Spring dependency. It's a pure rendering library that generates HTML strings. Spring provides the HTTP layer.

## Controller Pattern

### Basic Controller

```java
package com.example.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HomeController {

    @GetMapping("/")
    @ResponseBody  // Return HTML string directly
    public String home() {
        return Page.create()
            .addComponents(Header.h1("Welcome"))
            .render();  // Returns HTML string
    }
}
```

**Key points**:
- `@Controller` - Spring MVC controller
- `@GetMapping` - Route mapping
- `@ResponseBody` - Return HTML string (not view name)
- `.render()` - Generate HTML

### Controller with Page Class

**Better approach** - Separate rendering logic:

```java
// Page class
package com.example.pages;

import io.mindspice.jhf.components.*;
import io.mindspice.jhf.layout.Page;
import org.springframework.stereotype.Component;

@Component
public class HomePage {

    public String buildPage() {
        return Page.create()
            .addRow(row -> row
                .withChild(Column.create().withWidth(12)
                    .withChild(Header.h1("Welcome to Our Site"))
                    .withChild(Paragraph.create()
                        .withInnerText("This is the home page."))))
            .render();
    }
}

// Controller
package com.example.controller;

import com.example.pages.HomePage;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HomeController {
    private final HomePage homePage;

    public HomeController(HomePage homePage) {
        this.homePage = homePage;
    }

    @GetMapping("/")
    @ResponseBody
    public String home() {
        return homePage.buildPage();
    }
}
```

## Dependency Injection

### Constructor Injection (Recommended)

Spring automatically injects dependencies via constructor:

```java
@Controller
public class ProductController {
    private final ProductListPage productListPage;
    private final ProductDetailPage productDetailPage;
    private final ProductService productService;
    private final ShellService shellService;

    // Constructor injection (recommended)
    public ProductController(ProductListPage productListPage,
                            ProductDetailPage productDetailPage,
                            ProductService productService,
                            ShellService shellService) {
        this.productListPage = productListPage;
        this.productDetailPage = productDetailPage;
        this.productService = productService;
        this.shellService = shellService;
    }

    @GetMapping("/products")
    @ResponseBody
    public String listProducts() {
        String pageContent = productListPage.buildPage();
        return shellService.buildShell("Products", pageContent);
    }
}
```

### Page Class with Dependencies

Page classes can inject services:

```java
@Component
public class ProductListPage {
    private final ProductService productService;

    public ProductListPage(ProductService productService) {
        this.productService = productService;
    }

    public String buildPage() {
        List<Product> products = productService.findAll();

        return Page.create()
            .addRow(row -> row
                .withChild(Column.create().withWidth(12)
                    .withChild(buildProductGrid(products))))
            .render();
    }

    private Component buildProductGrid(List<Product> products) {
        CardGrid grid = new CardGrid();
        for (Product product : products) {
            grid.withChild(buildProductCard(product));
        }
        return grid;
    }

    private Component buildProductCard(Product product) {
        return Card.create()
            .withChild(Image.create(product.getImageUrl(), product.getName()))
            .withChild(Header.h3(product.getName()))
            .withChild(Paragraph.create().withInnerText("$" + product.getPrice()))
            .withChild(Link.create("/product/" + product.getId(), "View Details"));
    }
}
```

## Request Parameters

### Path Variables

```java
@GetMapping("/product/{id}")
@ResponseBody
public String productDetail(@PathVariable Long id) {
    String pageContent = productDetailPage.buildPage(id);
    return shellService.buildShell("Product Detail", pageContent);
}

// In page class
public String buildPage(Long productId) {
    Product product = productService.findById(productId);
    return Page.create()
        .addComponents(Header.h1(product.getName()))
        .render();
}
```

### Query Parameters

```java
@GetMapping("/search")
@ResponseBody
public String search(@RequestParam String q,
                    @RequestParam(required = false) String category) {
    String pageContent = searchResultsPage.buildPage(q, category);
    return shellService.buildShell("Search Results", pageContent);
}

// In page class
public String buildPage(String query, String category) {
    List<Result> results = searchService.search(query, category);
    return Page.create()
        .addComponents(Header.h1("Search: " + query))
        .addComponents(buildResults(results))
        .render();
}
```

### Request Body (Forms)

```java
@PostMapping("/contact")
public String submitContact(@ModelAttribute ContactRequest request) {
    contactService.send(request);
    return "redirect:/contact/success";
}

// Model class
public class ContactRequest {
    private String name;
    private String email;
    private String message;

    // Getters and setters
}
```

## HTMX Endpoint Pattern

### Detecting HTMX Requests

Use `HX-Request` header to differentiate:

```java
@GetMapping("/products")
@ResponseBody
public String products(@RequestHeader(value = "HX-Request", required = false) String hxRequest) {
    if (hxRequest != null) {
        // HTMX request - return HTML fragment
        return productListPage.buildFragment();
    } else {
        // Normal request - return full page with shell
        String pageContent = productListPage.buildPage();
        return shellService.buildShell("Products", pageContent);
    }
}
```

### HTMX-Only Endpoints

For endpoints that should only handle HTMX requests:

```java
@PostMapping("/api/load-more")
@ResponseBody
public String loadMore(@RequestParam int page,
                      @RequestHeader(value = "HX-Request", required = false) String hxRequest) {
    // Verify HTMX request
    if (hxRequest == null) {
        throw new AccessDeniedException("Direct access not allowed");
    }

    // Return HTML fragment
    List<Item> items = itemService.getPage(page);
    return buildItemsFragment(items).render();
}
```

### Returning Different Content Types

```java
@GetMapping("/api/stats")
@ResponseBody
public ResponseEntity<String> stats(@RequestHeader(value = "HX-Request", required = false) String hxRequest) {
    if (hxRequest != null) {
        // HTMX - return HTML
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .body(buildStatsHtml().render());
    } else {
        // API call - return JSON
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(statsService.getJsonStats());
    }
}
```

## Form Submission

### GET Form (Search)

No CSRF token needed for GET requests:

```java
// Page with search form
public String buildPage() {
    Form searchForm = Form.create()
        .withAction("/search")
        .withMethod(Form.Method.GET)  // GET method
        .addField("", TextInput.search("q")
            .withPlaceholder("Search...")
            .required())
        .addField("", Button.submit("Search"));

    return Page.create().addComponents(searchForm).render();
}

// Controller
@GetMapping("/search")
@ResponseBody
public String search(@RequestParam String q) {
    List<Result> results = searchService.search(q);
    String pageContent = searchResultsPage.buildPage(q, results);
    return shellService.buildShell("Search Results", pageContent);
}
```

### POST Form (with CSRF)

CSRF token required for POST requests:

```java
// Page with contact form
public String buildPage(CsrfToken csrfToken) {
    Form contactForm = Form.create()
        .withAction("/contact")
        .withMethod(Form.Method.POST)
        .withCsrfToken(csrfToken.getToken())  // CRITICAL

        .addField("Name", TextInput.create("name").required())
        .addField("Email", TextInput.email("email").required())
        .addField("Message", TextArea.create("message").required())
        .addField("", Button.submit("Send"));

    return Page.create().addComponents(contactForm).render();
}

// Controller - GET (show form)
@GetMapping("/contact")
@ResponseBody
public String contactForm(CsrfToken csrfToken) {
    String pageContent = contactPage.buildPage(csrfToken);
    return shellService.buildShell("Contact Us", pageContent);
}

// Controller - POST (handle submission)
@PostMapping("/contact")
public String submitContact(@Valid @ModelAttribute ContactRequest request,
                           BindingResult result) {
    if (result.hasErrors()) {
        // Show errors
        return "redirect:/contact?error=true";
    }

    contactService.send(request);
    return "redirect:/contact/success";
}
```

## CSRF Integration

### Getting CSRF Token in Controller

Spring Security automatically provides CSRF tokens:

```java
@GetMapping("/form")
@ResponseBody
public String showForm(CsrfToken csrfToken) {
    // csrfToken automatically injected by Spring Security
    String pageContent = formPage.buildPage(csrfToken.getToken());
    return shellService.buildShell("Form", pageContent);
}
```

### Including CSRF in Forms

```java
Form.create()
    .withCsrfToken(csrfToken.getToken())  // Adds hidden input
    .withAction("/submit")
    .withMethod(Form.Method.POST)
    ...
```

Generates:
```html
<form action="/submit" method="POST">
    <input type="hidden" name="_csrf" value="abc123...">
    <!-- Other fields -->
</form>
```

### CSRF with HTMX

HTMX automatically includes CSRF token from hidden input:

```java
Form.create()
    .withAttribute("hx-post", "/api/submit")
    .withCsrfToken(csrfToken.getToken())  // HTMX will include in headers
    ...
```

Spring Security validates automatically.

## Error Handling

### Custom Error Pages

```java
@ControllerAdvice
public class ErrorController {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseBody
    public ResponseEntity<String> handleNotFound(ResourceNotFoundException ex) {
        String errorPage = Page.create()
            .addComponents(
                Header.h1("404 - Not Found"),
                Paragraph.create().withInnerText(ex.getMessage()),
                Link.create("/", "Go Home")
            )
            .render();

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(errorPage);
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseBody
    public ResponseEntity<String> handleAccessDenied(AccessDeniedException ex) {
        String errorPage = Page.create()
            .addComponents(
                Header.h1("403 - Access Denied"),
                Paragraph.create().withInnerText("You don't have permission to access this resource."),
                Link.create("/", "Go Home")
            )
            .render();

        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(errorPage);
    }
}
```

### Validation Errors

```java
@PostMapping("/register")
@ResponseBody
public String register(@Valid @ModelAttribute RegistrationRequest request,
                      BindingResult result,
                      CsrfToken csrfToken) {
    if (result.hasErrors()) {
        // Convert errors to list
        List<String> errors = result.getAllErrors().stream()
            .map(ObjectError::getDefaultMessage)
            .toList();

        // Show form with errors
        String pageContent = registrationPage.buildPage(errors, csrfToken.getToken());
        return shellService.buildShell("Registration", pageContent);
    }

    userService.register(request);
    return "redirect:/login";
}

// In page class
public String buildPage(List<String> errors, String csrfToken) {
    Page page = Page.create();

    // Show errors if present
    if (errors != null && !errors.isEmpty()) {
        Div errorBox = new Div().withClass("alert alert-danger");
        for (String error : errors) {
            errorBox.withChild(Paragraph.create().withInnerText(error));
        }
        page.addComponents(errorBox);
    }

    // Build form
    page.addComponents(buildRegistrationForm(csrfToken));

    return page.render();
}
```

## Static Resources

### Default Static Resource Mapping

Spring Boot automatically serves files from `/src/main/resources/static/`:

```
static/
├── css/
│   ├── framework.css     →  /css/framework.css
│   └── custom.css        →  /css/custom.css
├── js/
│   └── app.js            →  /js/app.js
├── images/
│   └── logo.png          →  /images/logo.png
└── downloads/
    └── guide.pdf         →  /downloads/guide.pdf
```

### Referencing Static Resources in Components

```java
// Images
Image.create("/images/logo.png", "Logo");

// Links to downloads
Link.create("/downloads/guide.pdf", "Download Guide");

// CSS (in ShellBuilder)
ShellBuilder.create()
    .withAdditionalCSS("/css/custom.css")
    ...
```

## Complete Application Example

### Project Structure

```
src/main/java/com/example/
├── Application.java                    # Spring Boot main class
├── controller/
│   ├── HomeController.java            # Home page routes
│   ├── ProductController.java         # Product routes
│   └── ApiController.java             # HTMX API endpoints
├── pages/
│   ├── HomePage.java                  # Home page rendering
│   ├── ProductListPage.java           # Product list rendering
│   └── ProductDetailPage.java         # Product detail rendering
├── service/
│   ├── ProductService.java            # Business logic
│   └── ShellService.java              # Shell generation
├── model/
│   └── Product.java                   # Domain model
└── config/
    └── SecurityConfig.java            # Spring Security config
```

### Example: Complete Product Catalog

**Product Model**:
```java
public class Product {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private String imageUrl;

    // Constructor, getters, setters
}
```

**ProductService**:
```java
@Service
public class ProductService {
    private final Map<Long, Product> products = new ConcurrentHashMap<>();

    public ProductService() {
        // Sample data
        products.put(1L, new Product(1L, "Laptop", "High-performance laptop",
            new BigDecimal("999.99"), "/images/laptop.jpg"));
        products.put(2L, new Product(2L, "Mouse", "Wireless mouse",
            new BigDecimal("29.99"), "/images/mouse.jpg"));
    }

    public List<Product> findAll() {
        return new ArrayList<>(products.values());
    }

    public Product findById(Long id) {
        return products.get(id);
    }
}
```

**ProductListPage**:
```java
@Component
public class ProductListPage {
    private final ProductService productService;

    public ProductListPage(ProductService productService) {
        this.productService = productService;
    }

    public String buildPage() {
        List<Product> products = productService.findAll();

        return Page.create()
            .addRow(row -> row
                .withChild(Column.create().withWidth(12)
                    .withChild(Header.h1("Our Products"))))
            .addRow(row -> row
                .withChild(Column.create().withWidth(12)
                    .withChild(buildProductGrid(products))))
            .render();
    }

    private Component buildProductGrid(List<Product> products) {
        Row row = new Row();

        for (Product product : products) {
            row.withChild(Column.create().withWidth(4)
                .withChild(buildProductCard(product)));
        }

        return row;
    }

    private Component buildProductCard(Product product) {
        return Card.create()
            .withChild(Image.create(product.getImageUrl(), product.getName()))
            .withChild(Header.h3(product.getName()))
            .withChild(Paragraph.create().withInnerText("$" + product.getPrice()))
            .withChild(Link.create("/product/" + product.getId(), "View Details")
                .withClass("btn btn-primary"));
    }
}
```

**ProductController**:
```java
@Controller
public class ProductController {
    private final ProductListPage productListPage;
    private final ProductDetailPage productDetailPage;
    private final ShellService shellService;

    public ProductController(ProductListPage productListPage,
                            ProductDetailPage productDetailPage,
                            ShellService shellService) {
        this.productListPage = productListPage;
        this.productDetailPage = productDetailPage;
        this.shellService = shellService;
    }

    @GetMapping("/products")
    @ResponseBody
    public String listProducts() {
        String pageContent = productListPage.buildPage();
        return shellService.buildShell("Products", pageContent);
    }

    @GetMapping("/product/{id}")
    @ResponseBody
    public String productDetail(@PathVariable Long id) {
        String pageContent = productDetailPage.buildPage(id);
        return shellService.buildShell("Product Detail", pageContent);
    }
}
```

## Best Practices

### 1. Keep Controllers Thin

Controllers should only handle HTTP concerns:

```java
// Good - thin controller
@GetMapping("/products")
@ResponseBody
public String products() {
    String pageContent = productListPage.buildPage();
    return shellService.buildShell("Products", pageContent);
}

// Avoid - fat controller with rendering logic
@GetMapping("/products")
@ResponseBody
public String products() {
    List<Product> products = productService.findAll();
    // Lots of rendering code here... (BAD)
    return Page.create().addComponents(...).render();
}
```

### 2. Use Page Classes for Rendering

```java
// Good - separate page class
@Component
public class ProductListPage {
    public String buildPage() {
        // All rendering logic here
    }
}

// Avoid - rendering in controller
@Controller
public class ProductController {
    @GetMapping("/products")
    @ResponseBody
    public String products() {
        return Page.create()...  // Rendering in controller (BAD)
    }
}
```

### 3. Inject Dependencies via Constructor

```java
// Good - constructor injection
public class MyController {
    private final MyPage myPage;

    public MyController(MyPage myPage) {
        this.myPage = myPage;
    }
}

// Avoid - field injection
public class MyController {
    @Autowired  // Avoid
    private MyPage myPage;
}
```

### 4. Separate HTMX Endpoints

```java
// Good - separate API endpoints for HTMX
@GetMapping("/products")         // Full page
@GetMapping("/api/products")     // HTMX fragment

// Avoid - same endpoint for both
@GetMapping("/products")  // Handles both? Confusing
```

### 5. Return Appropriate Status Codes

```java
// Good - proper status codes
@PostMapping("/api/submit")
@ResponseBody
public ResponseEntity<String> submit(@Valid @ModelAttribute Request request) {
    if (isValid(request)) {
        return ResponseEntity.ok(successHtml);  // 200
    } else {
        return ResponseEntity.badRequest().body(errorHtml);  // 400
    }
}

// Avoid - always 200
@PostMapping("/api/submit")
@ResponseBody
public String submit(@Valid @ModelAttribute Request request) {
    return errorHtml;  // Returns 200 even on error (BAD)
}
```

## Key Takeaways

1. **Separation of Concerns**: Controllers handle HTTP, page classes handle rendering
2. **Constructor Injection**: Inject page classes and services into controllers
3. **HTMX Detection**: Check `HX-Request` header for HTMX requests
4. **CSRF Tokens**: Always include in POST forms (Spring validates automatically)
5. **Error Handling**: Use `@ControllerAdvice` for custom error pages
6. **Static Resources**: Served automatically from `/static/` directory
7. **Thin Controllers**: Keep HTTP logic minimal, delegate to page classes
8. **Testing**: Page classes are easily testable without HTTP layer

---

**Previous**: [Part 10: Building a Forum Tutorial](10-forum-tutorial.md)

**Next**: [Part 12: Security Best Practices](12-security.md)

**Table of Contents**: [Getting Started Guide](README.md)
