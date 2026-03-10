package io.mindspice.demo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentationServiceTest {

    @Test
    @DisplayName("extractDocsRelativePath should normalize jar and file resource URLs")
    void extractDocsRelativePathShouldHandleCommonResourceSchemes() {
        assertEquals(
            "core/01-components-htmltag-and-module-lifecycle.md",
            DocumentationService.extractDocsRelativePath(
                "jar:nested:/opt/apps/simplypages-demo.jar/!BOOT-INF/classes/!/static/docs/core/01-components-htmltag-and-module-lifecycle.md"
            )
        );
        assertEquals(
            "reference/components-and-modules-catalog.md",
            DocumentationService.extractDocsRelativePath(
                "file:/home/demo/target/classes/static/docs/reference/components-and-modules-catalog.md"
            )
        );
        assertEquals(
            "operations/02-testing-and-troubleshooting-playbook.md",
            DocumentationService.extractDocsRelativePath(
                "file:C:\\\\apps\\\\demo\\\\target\\\\classes\\\\static\\\\docs\\\\operations\\\\02-testing-and-troubleshooting-playbook.md"
            )
        );
    }

    @Test
    @DisplayName("docs navigation should render discovered markdown links")
    void docsNavigationShouldIncludeLinks() {
        DocumentationService service = new DocumentationService();
        String rendered = service.getDocsNavigation().render();
        assertTrue(rendered.contains("/docs/"), "Expected docs navigation to include at least one docs link");
        assertTrue(rendered.contains("data-sp-scroll-top=\"target\""), "Expected docs HTMX links to tag target-top scroll reset");
    }

    @Test
    @DisplayName("default docs page content should resolve from available docs sources")
    void defaultDocsContentShouldResolve() {
        DocumentationService service = new DocumentationService();
        String content = service.getDocContent(DocumentationService.DEFAULT_DOC_PATH);
        assertNotNull(content, "Expected default docs content to be discoverable");
        assertTrue(content.contains("SimplyPages") || content.contains("Installation"), "Expected markdown content");
    }
}
