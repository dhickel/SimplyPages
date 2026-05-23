# Context

Components, modules, and builders contain public APIs that create anchors or style attributes without using centralized `Link` validation or `HtmlTag` style hardening.

# Goal

Make public URL/style APIs consistently use shared safety paths while preserving valid output for existing safe inputs.

# In Scope

- Shared URL validation extraction from `components/navigation/Link`.
- Replace raw anchor href paths in `HeroModule`, `NavBar`, `SideNav`, `Breadcrumb`, `Dropdown`, and `AccountWidget`.
- Replace raw style concatenation in `HeroModule`, `BannerBuilder`, `AccountBarBuilder`, and `Divider`.
- Replace hand-built `Form.withHxPostCsrf` JSON with structured encoding.
- Add docs and tests for URL/style/headers safety.

# Out of Scope

- New URL policy design beyond the current `Link` safe schemes.
- Visual changes to navigation, hero, divider, or banners for safe inputs.

# Implementation Steps

1. Extract URL validator.
   - Target: `components/navigation/Link.java` plus new helper if appropriate, for example `core/SafeUrl.java` or `components/navigation/UrlPolicy.java`.
   - Keep current accepted schemes/relative URL behavior.
   - Add package-visible tests proving `Link` behavior remains unchanged.

2. Apply URL validator to raw anchor APIs.
   - `HeroModule`: CTA hrefs validate before rendering.
   - `NavBar`, `SideNav`, `Breadcrumb`, `Dropdown`, `AccountWidget`: validate href/action URLs where they create anchors.
   - Preserve rendered HTML for safe URLs byte-for-byte.

3. Route style APIs through core style helpers.
   - `HeroModule` background image should use trusted URL wrapper only after URL validation or a dedicated safe background-image helper.
   - `BannerBuilder` and `AccountBarBuilder` should use `addStyle`/`addTrustedStyle` instead of raw full-style string assembly.
   - `Divider.withColor` and height handling should preserve both color and height.

4. Encode `hx-headers`.
   - Use a small JSON string escape helper or Jackson if already present.
   - Test quoted/backslash token/header values.

5. Update docs.
   - Reference docs for navigation/components/builders/forms.
   - Security doc section for URL validation reuse and trusted style path.

# Validation

- Targeted tests:
  - `LinkTest`, nav/breadcrumb/dropdown/account widget tests.
  - `HeroModuleTest`, `DividerTest`, `BannerBuilderTest`, `AccountBarBuilderTest`.
  - `FormTest` with quoted/backslash CSRF token/header name.
- `./mvnw -pl simplypages test`
- Before/after HTML checks for representative safe nav/sidebar/hero/banner/divider/form examples.

# Exit Criteria

- Unsafe schemes are rejected consistently across public anchor APIs.
- Safe inputs produce same HTML except documented corrected style/header escaping.
- Raw style assembly no longer bypasses hardened paths.
