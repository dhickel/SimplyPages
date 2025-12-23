# Part 12: Security Best Practices

Security is critical for web applications. This comprehensive guide covers essential security practices when building with JHF, from XSS prevention to HTTPS deployment.

## Security Mindset

### Core Principles

1. **Never Trust User Input** - All user input is potentially malicious
2. **Defense in Depth** - Use multiple layers of security
3. **Principle of Least Privilege** - Grant minimal necessary permissions
4. **Validate Server-Side Always** - Client-side validation is convenience, not security
5. **Security is Ongoing** - Regular updates, audits, and testing

### Threat Model

Common web application threats:
- **XSS (Cross-Site Scripting)** - Injecting malicious scripts
- **CSRF (Cross-Site Request Forgery)** - Unauthorized actions
- **SQL Injection** - Malicious database queries
- **Path Traversal** - Unauthorized file access
- **Insecure Deserialization** - Exploiting object deserialization
- **Broken Authentication** - Weak login/session management
- **Sensitive Data Exposure** - Unencrypted data transmission

## XSS Prevention (Cross-Site Scripting)

### What is XSS?

**Attack**: Attacker injects malicious JavaScript into your page

**Example Attack**:
```
User enters username: <script>alert('XSS!')</script>
Your page displays: <h1>Welcome <script>alert('XSS!')</script></h1>
Result: Script executes in victim's browser
```

**Impact**:
- Session hijacking (steal cookies)
- Data theft (read page content)
- Defacement (modify page)
- Phishing (fake login forms)

### JHF's Built-In Protection

#### 1. OWASP Encoder (Attribute Escaping)

JHF uses OWASP Encoder for all HTML attributes:

```java
component.withAttribute("title", userInput);
```

Internally:
```java
import org.owasp.encoder.Encode;

String escaped = Encode.forHtmlAttribute(userInput);
```

**Example**:
```java
// User input: "><script>alert('XSS')</script>
component.withAttribute("data-value", userInput);

// Generated HTML (safe):
// data-value="&quot;&gt;&lt;script&gt;alert(&#39;XSS&#39;)&lt;/script&gt;"
```

#### 2. Markdown Escaping

The `Markdown` component escapes HTML by default:

```java
Markdown markdown = new Markdown(userContent);
```

Uses CommonMark with `escapeHtml(true)`:
```java
HtmlRenderer.builder()
    .escapeHtml(true)  // Escape all HTML
    .build();
```

**Example**:
```java
// User markdown: Hello <script>alert('XSS')</script>
Markdown.create(userContent).render();

// Output: Hello &lt;script&gt;alert('XSS')&lt;/script&gt;
```

### Best Practices for XSS Prevention

#### 1. Never Use `withInnerText()` with Unescaped User Input

```java
// VULNERABLE - Don't do this with user input
Paragraph.create()
    .withInnerText(userInput);  // If userInput contains <script>, it renders!
```

**Fix**: JHF's `withInnerText()` should escape automatically, but verify or use explicit escaping:

```java
import org.springframework.web.util.HtmlUtils;

// Safe - explicitly escape
Paragraph.create()
    .withInnerText(HtmlUtils.htmlEscape(userInput));
```

#### 2. Be Careful with Raw HTML

If you must render user-provided HTML (rare), sanitize it:

```java
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

// Sanitize HTML
String cleanHtml = Jsoup.clean(userHtml, Safelist.basic());
```

#### 3. Use Markdown for Rich Content

For user-generated rich content, use Markdown (not raw HTML):

```java
// Good - Markdown is safe
Markdown.create(userMarkdown);

// Avoid - Raw HTML is dangerous
new Div().withInnerHtml(userHtml);  // Don't do this
```

## CSRF Prevention (Cross-Site Request Forgery)

### What is CSRF?

**Attack**: Attacker tricks user into making unwanted requests

**Example Attack**:
```html
<!-- Attacker's website -->
<img src="https://yourbank.com/transfer?to=attacker&amount=1000">
```

When victim (logged into yourbank.com) loads attacker's page, the request executes using victim's session.

**Impact**:
- Unauthorized fund transfers
- Account changes
- Data deletion
- Privilege escalation

### JHF's CSRF Protection

#### Spring Security Integration

Spring Security automatically enables CSRF protection. JHF integrates seamlessly:

**1. Include CSRF Token in Forms**:
```java
@GetMapping("/form")
@ResponseBody
public String showForm(CsrfToken csrfToken) {
    Form form = Form.create()
        .withAction("/submit")
        .withMethod(Form.Method.POST)
        .withCsrfToken(csrfToken.getToken())  // CRITICAL
        .addField("Email", TextInput.email("email"))
        .addField("", Button.submit("Submit"));

    return Page.create().addComponents(form).render();
}
```

**Generated HTML**:
```html
<form action="/submit" method="POST">
    <input type="hidden" name="_csrf" value="abc123...">
    <!-- Other fields -->
</form>
```

**2. Spring Security Validates Automatically**:
```java
@PostMapping("/submit")
public String submit(@RequestParam String email) {
    // Spring Security validates CSRF token before this runs
    // If invalid token, request is rejected with 403 Forbidden
    return "success";
}
```

#### HTMX CSRF Integration

HTMX automatically includes CSRF token from hidden input:

```java
Form.create()
    .withAttribute("hx-post", "/api/submit")
    .withCsrfToken(csrfToken.getToken())
    .addField("Email", TextInput.email("email"))
    .addField("", Button.submit("Submit"));
```

HTMX sends token in `X-CSRF-TOKEN` header. Spring Security validates.

### Best Practices for CSRF Prevention

#### 1. Always Include CSRF Tokens in POST Forms

```java
// Good
Form.create()
    .withCsrfToken(csrfToken.getToken())
    ...

// VULNERABLE - Missing CSRF token
Form.create()
    .withAction("/submit")
    .withMethod(Form.Method.POST)
    // Missing: .withCsrfToken(...)
```

#### 2. Don't Disable CSRF Protection

```java
// DON'T DO THIS
@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf().disable();  // NEVER disable CSRF
        return http.build();
    }
}
```

#### 3. Use GET for Read-Only Operations

```java
// Good - GET for reading (no CSRF needed)
@GetMapping("/products")
public String listProducts() { ... }

// Avoid - POST for reading (requires CSRF)
@PostMapping("/products")  // Wrong HTTP method
public String listProducts() { ... }
```

## Server-Side Validation

### Why Client-Side Validation is Not Enough

**Problem**: Users can bypass client-side validation

**Example**:
```java
// Client-side validation (HTML5)
TextInput.email("email").required();
```

Generates:
```html
<input type="email" name="email" required>
```

**Attacker bypasses**:
- Disable JavaScript
- Edit HTML in browser DevTools
- Send direct HTTP request with `curl`

### Always Validate Server-Side

#### Spring Validation Annotations

```java
// Domain model with validation
public class RegistrationRequest {
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 20, message = "Username must be 3-20 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(regexp = ".*[A-Z].*", message = "Password must contain uppercase letter")
    @Pattern(regexp = ".*[0-9].*", message = "Password must contain number")
    private String password;

    // Getters and setters
}
```

#### Controller Validation

```java
@PostMapping("/register")
@ResponseBody
public ResponseEntity<String> register(
        @Valid @ModelAttribute RegistrationRequest request,
        BindingResult result,
        CsrfToken csrfToken) {

    // Check for validation errors
    if (result.hasErrors()) {
        List<String> errors = result.getAllErrors().stream()
            .map(ObjectError::getDefaultMessage)
            .toList();

        String errorPage = registrationPage.buildPage(errors, csrfToken.getToken());

        return ResponseEntity
            .badRequest()  // 400 status
            .body(errorPage);
    }

    // Validation passed - process registration
    userService.register(request);

    return ResponseEntity.ok(
        Alert.success("Registration successful!").render()
    );
}
```

### Best Practices for Validation

#### 1. Validate All Input

```java
// Good - validate everything
@PostMapping("/submit")
public String submit(@Valid @ModelAttribute Request request) { ... }

// VULNERABLE - no validation
@PostMapping("/submit")
public String submit(@ModelAttribute Request request) {
    // Missing @Valid - no validation!
}
```

#### 2. Use Whitelist Validation

```java
// Good - whitelist allowed values
@Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username contains invalid characters")
private String username;

// Avoid - blacklist forbidden values (incomplete)
// Don't try to block bad characters, allow only good ones
```

#### 3. Validate Business Logic

```java
@PostMapping("/transfer")
public String transfer(@Valid @ModelAttribute TransferRequest request) {
    // Validate form fields (@Valid above)

    // ALSO validate business logic
    if (request.getAmount().compareTo(account.getBalance()) > 0) {
        throw new InsufficientFundsException();
    }

    if (!request.getToAccount().equals(user.getAuthorizedAccounts())) {
        throw new UnauthorizedAccountException();
    }

    // Process transfer
    transferService.execute(request);
    return "success";
}
```

## SQL Injection Prevention

### What is SQL Injection?

**Attack**: Attacker injects malicious SQL code

**Vulnerable Code** (Don't do this):
```java
// VULNERABLE - String concatenation
String sql = "SELECT * FROM users WHERE username = '" + userInput + "'";
```

**Attack**:
```
User input: admin' OR '1'='1
Generated SQL: SELECT * FROM users WHERE username = 'admin' OR '1'='1'
Result: Returns all users (bypassed authentication)
```

### JHF + JPA: Safe by Default

JPA uses parameterized queries automatically:

```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Safe - JPA uses parameterized query
    Optional<User> findByUsername(String username);

    // Safe - named parameter
    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmail(@Param("email") String email);
}
```

JPA generates:
```sql
SELECT * FROM users WHERE username = ?
```

Then binds parameter safely (no injection possible).

### Never Concatenate User Input in Queries

```java
// VULNERABLE - Don't do this
@Query(value = "SELECT * FROM users WHERE username = '" + username + "'",
       nativeQuery = true)
List<User> findByUsernameBad(String username);

// Good - Use named parameters
@Query(value = "SELECT * FROM users WHERE username = :username",
       nativeQuery = true)
List<User> findByUsernameGood(@Param("username") String username);
```

## HTMX Security

### 1. Verify HX-Request Header

Prevent direct browser access to HTMX endpoints:

```java
@PostMapping("/api/internal")
@ResponseBody
public String internalEndpoint(
        @RequestHeader(value = "HX-Request", required = false) String hxRequest) {

    // Verify HTMX request
    if (hxRequest == null) {
        throw new AccessDeniedException("Direct access not allowed");
    }

    // Process HTMX request
    return buildFragment().render();
}
```

### 2. CSRF Tokens on All HTMX POST Requests

```java
// Always include CSRF token
Form.create()
    .withAttribute("hx-post", "/api/submit")
    .withCsrfToken(csrfToken.getToken())  // CRITICAL
    ...
```

### 3. Authorization Checks

```java
@PostMapping("/api/admin/action")
@PreAuthorize("hasRole('ADMIN')")  // Require ADMIN role
@ResponseBody
public String adminAction(@RequestHeader("HX-Request") String hxRequest) {
    // Verify HTMX
    if (hxRequest == null) {
        throw new AccessDeniedException("Direct access not allowed");
    }

    // Admin action
    return Alert.success("Action completed").render();
}
```

## Authorization

### Authentication vs Authorization

- **Authentication**: Who are you? (Login)
- **Authorization**: What can you do? (Permissions)

### Spring Security Integration

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // Enable @PreAuthorize
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/home", "/public/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/user/**").hasAnyRole("USER", "ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .permitAll()
            )
            .logout(logout -> logout
                .permitAll()
            );

        return http.build();
    }
}
```

### Method-Level Security

```java
@Controller
public class AdminController {

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/users")
    @ResponseBody
    public String manageUsers() {
        // Only accessible to ADMIN role
        return adminPage.buildPage();
    }

    @PreAuthorize("#username == authentication.name or hasRole('ADMIN')")
    @GetMapping("/user/{username}/profile")
    @ResponseBody
    public String viewProfile(@PathVariable String username) {
        // Accessible to user themselves or ADMIN
        return profilePage.buildPage(username);
    }
}
```

## File Upload Security

### Validate File Type (Whitelist)

```java
@PostMapping("/upload")
public String upload(@RequestParam("file") MultipartFile file) {
    // Validate file type (whitelist)
    String contentType = file.getContentType();
    List<String> allowedTypes = List.of("image/jpeg", "image/png", "image/gif");

    if (!allowedTypes.contains(contentType)) {
        throw new InvalidFileTypeException("Only JPEG, PNG, and GIF allowed");
    }

    // Validate file size (10MB max)
    if (file.getSize() > 10 * 1024 * 1024) {
        throw new FileTooLargeException("File must be under 10MB");
    }

    // Save file
    fileService.save(file);
    return "success";
}
```

### Don't Trust File Extension

```java
// VULNERABLE - File extension can be faked
String filename = file.getOriginalFilename();
if (filename.endsWith(".jpg")) {  // Attacker can name virus.exe as virus.jpg
    // UNSAFE
}

// Good - Check actual content type
String contentType = file.getContentType();
if ("image/jpeg".equals(contentType)) {
    // Safer (but still verify content)
}
```

### Store Outside Web Root

```java
// VULNERABLE - Stored in /static (publicly accessible)
File uploadDir = new File("src/main/resources/static/uploads");

// Good - Stored outside web root
File uploadDir = new File("/var/app/uploads");  // Not web-accessible

// Serve via controlled endpoint
@GetMapping("/uploads/{filename}")
public ResponseEntity<Resource> downloadFile(@PathVariable String filename) {
    // Validate user has permission
    // Prevent path traversal
    // Serve file
}
```

### Prevent Path Traversal

```java
@GetMapping("/file/{filename}")
public ResponseEntity<Resource> download(@PathVariable String filename) {
    // VULNERABLE - Path traversal attack
    // Attacker sends: filename=../../etc/passwd
    File file = new File("/var/uploads/" + filename);

    // Good - Validate filename
    if (filename.contains("..") || filename.contains("/")) {
        throw new SecurityException("Invalid filename");
    }

    File file = new File("/var/uploads/" + filename);

    // Better - Use whitelist or UUID filenames
    if (!filename.matches("^[a-zA-Z0-9-_]+\\.(jpg|png|pdf)$")) {
        throw new SecurityException("Invalid filename");
    }
}
```

## Password Security

### Never Store Plaintext Passwords

```java
// VULNERABLE - plaintext password
user.setPassword(password);  // DON'T DO THIS

// Good - hash password with bcrypt
user.setPassword(passwordEncoder.encode(password));
```

### Use BCrypt (Spring Security Default)

```java
@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();  // Industry standard
    }
}

// Usage
@Service
public class UserService {
    private final PasswordEncoder passwordEncoder;

    public void registerUser(String username, String password) {
        String hashedPassword = passwordEncoder.encode(password);
        user.setPassword(hashedPassword);
        userRepository.save(user);
    }

    public boolean authenticate(String username, String password) {
        User user = userRepository.findByUsername(username);
        return passwordEncoder.matches(password, user.getPassword());
    }
}
```

### Password Strength Requirements

```java
@Pattern(regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9])(?=.*[@#$%^&+=]).{8,}$",
         message = "Password must be at least 8 characters with uppercase, lowercase, number, and special character")
private String password;
```

## Session Security

### HttpOnly Cookies (Prevent XSS Cookie Theft)

```java
// Spring Security sets HttpOnly automatically
server.servlet.session.cookie.http-only=true
```

Cookies with `HttpOnly` flag cannot be accessed by JavaScript:
```javascript
document.cookie;  // Cannot read HttpOnly cookies (prevents XSS cookie theft)
```

### Secure Flag (HTTPS Only)

```java
// In production, require HTTPS for cookies
server.servlet.session.cookie.secure=true
```

### Session Timeout

```java
// application.properties
server.servlet.session.timeout=30m  // 30 minutes
```

## HTTPS in Production

### Why HTTPS is Critical

- **Encryption**: Prevents eavesdropping on data transmission
- **Integrity**: Prevents tampering with requests/responses
- **Authentication**: Verifies server identity
- **Required**: For modern features (geolocation, camera, service workers)

### Enable HTTPS in Spring Boot

```java
// application.properties (production)
server.port=8443
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=changeit
server.ssl.key-store-type=PKCS12
```

### Free SSL Certificates

Use Let's Encrypt for free SSL certificates:
```bash
sudo certbot certonly --standalone -d yourdomain.com
```

### Redirect HTTP to HTTPS

```java
@Configuration
public class HttpsRedirectConfig {
    @Bean
    public ServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory() {
            @Override
            protected void postProcessContext(Context context) {
                SecurityConstraint securityConstraint = new SecurityConstraint();
                securityConstraint.setUserConstraint("CONFIDENTIAL");
                SecurityCollection collection = new SecurityCollection();
                collection.addPattern("/*");
                securityConstraint.addCollection(collection);
                context.addConstraint(securityConstraint);
            }
        };

        tomcat.addAdditionalTomcatConnectors(redirectConnector());
        return tomcat;
    }

    private Connector redirectConnector() {
        Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
        connector.setScheme("http");
        connector.setPort(8080);
        connector.setSecure(false);
        connector.setRedirectPort(8443);
        return connector;
    }
}
```

## Security Headers

### Set Security Headers

```java
@Configuration
public class SecurityHeadersConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.headers(headers -> headers
            .contentSecurityPolicy(csp -> csp
                .policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'"))
            .frameOptions(frame -> frame.sameOrigin())
            .xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
            .contentTypeOptions(Customizer.withDefaults())
            .httpStrictTransportSecurity(hsts -> hsts
                .includeSubDomains(true)
                .maxAgeInSeconds(31536000))
        );

        return http.build();
    }
}
```

### Important Headers

- **X-Content-Type-Options: nosniff** - Prevent MIME sniffing
- **X-Frame-Options: SAMEORIGIN** - Prevent clickjacking
- **X-XSS-Protection: 1; mode=block** - Enable XSS filter
- **Strict-Transport-Security: max-age=31536000** - Force HTTPS
- **Content-Security-Policy** - Control resource loading

## Security Checklist

### Pre-Deployment Checklist

- [ ] **Input Validation**: All user input validated server-side
- [ ] **Output Encoding**: All output escaped (OWASP Encoder, HtmlUtils)
- [ ] **CSRF Protection**: CSRF tokens on all POST forms
- [ ] **SQL Injection**: Using JPA with parameterized queries
- [ ] **Password Security**: Passwords hashed with BCrypt
- [ ] **Authorization**: Proper role checks on protected endpoints
- [ ] **HTTPS**: SSL/TLS enabled in production
- [ ] **Security Headers**: CSP, HSTS, X-Frame-Options set
- [ ] **Session Security**: HttpOnly, Secure flags on cookies
- [ ] **File Uploads**: Type validation, size limits, safe storage
- [ ] **Error Messages**: Don't leak sensitive info in errors
- [ ] **Dependencies**: All dependencies up to date
- [ ] **Secrets**: No secrets in source code (use environment variables)
- [ ] **Logging**: No sensitive data in logs

### Testing Checklist

- [ ] Try SQL injection in all inputs
- [ ] Try XSS in all text fields
- [ ] Test CSRF by crafting malicious requests
- [ ] Test authorization (access other users' resources)
- [ ] Test file upload with malicious files
- [ ] Test session expiration
- [ ] Run automated security scanner (OWASP ZAP)

## Common Vulnerabilities to Avoid

### 1. XSS (Script Injection)

```java
// VULNERABLE
Paragraph.create().withInnerText(userInput);  // If not escaped

// Good
Paragraph.create().withInnerText(HtmlUtils.htmlEscape(userInput));
```

### 2. CSRF (Unauthorized Actions)

```java
// VULNERABLE
Form.create().withAction("/submit");  // Missing CSRF token

// Good
Form.create().withCsrfToken(csrfToken.getToken());
```

### 3. SQL Injection

```java
// VULNERABLE
@Query("SELECT u FROM User WHERE name = '" + name + "'")

// Good
@Query("SELECT u FROM User WHERE name = :name")
List<User> findByName(@Param("name") String name);
```

### 4. Path Traversal

```java
// VULNERABLE
File file = new File("/uploads/" + userFilename);

// Good
if (userFilename.contains("..")) throw new SecurityException();
```

### 5. Insecure Direct Object Reference

```java
// VULNERABLE - Anyone can access any user's data
@GetMapping("/user/{id}/data")
public String getData(@PathVariable Long id) {
    return userService.getData(id);  // No authorization check!
}

// Good - Check authorization
@GetMapping("/user/{id}/data")
public String getData(@PathVariable Long id, Principal principal) {
    User user = userService.findById(id);
    if (!user.getUsername().equals(principal.getName())) {
        throw new AccessDeniedException();
    }
    return userService.getData(id);
}
```

## Resources

### Official Documentation

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)
- [OWASP Cheat Sheet Series](https://cheatsheetseries.owasp.org/)

### Security Tools

- **OWASP ZAP**: Web application security scanner
- **Burp Suite**: Security testing toolkit
- **SonarQube**: Static code analysis
- **Dependabot**: Dependency vulnerability alerts (GitHub)

## Key Takeaways

1. **Never Trust User Input**: Validate and sanitize everything
2. **XSS Protection**: Use OWASP Encoder, HtmlUtils, escape output
3. **CSRF Protection**: Always include tokens in POST forms
4. **Server-Side Validation**: Client-side is convenience, not security
5. **SQL Injection**: Use JPA with parameterized queries
6. **HTMX Security**: Verify HX-Request header, include CSRF tokens
7. **Authorization**: Check permissions, not just authentication
8. **HTTPS**: Required for production (encryption, integrity)
9. **Security Headers**: CSP, HSTS, X-Frame-Options
10. **Regular Updates**: Keep dependencies current, monitor vulnerabilities

---

**Previous**: [Part 11: Spring Integration](11-spring-integration.md)

**Table of Contents**: [Getting Started Guide](README.md)
