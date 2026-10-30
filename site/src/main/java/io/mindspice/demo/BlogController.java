package io.mindspice.demo;

import io.mindspice.simplypages.core.Component;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class BlogController {

    private final BlogContentService blogContentService;

    public BlogController(BlogContentService blogContentService) {
        this.blogContentService = blogContentService;
    }

    @GetMapping(value = {"/blog", "/blog/**"}, produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String blog(
        HttpServletRequest request,
        HttpServletResponse response,
        @RequestHeader(value = "HX-Request", required = false) String hxRequest
    ) {
        addHxVary(response);
        String requestPath = request.getRequestURI();
        String pageParam = request.getParameter("page");

        Component resolved = blogContentService.resolveRequest(requestPath, pageParam).orElse(null);
        if (resolved == null) {
            response.setStatus(404);
            return SiteShell.render("Publication entry not found", "/demos/features/static-content",
                SiteShell.intro("Publication entry not found", "No publication entry exists at: " + requestPath + ". Choose an entry from the generated index.")
                    + io.mindspice.simplypages.components.navigation.Link.create("/blog", "Publication index").render());
        }

        String renderedContent = new io.mindspice.simplypages.components.Div().withClass("sp-site-local-nav")
            .withChild(io.mindspice.simplypages.components.navigation.Link.create("/demos/features/static-content", "Content pipeline example"))
            .withChild(io.mindspice.simplypages.components.navigation.Link.create("/blog", "Publication index"))
            .render() + resolved.render();
        if ("true".equalsIgnoreCase(hxRequest)) {
            return renderedContent;
        }

        return SiteShell.render("Markdown publication", "/demos/features/static-content", renderedContent);
    }

    private void addHxVary(HttpServletResponse response) {
        for (String header : response.getHeaders("Vary")) {
            for (String token : header.split(",")) {
                if ("HX-Request".equalsIgnoreCase(token.trim())) {
                    return;
                }
            }
        }
        response.addHeader("Vary", "HX-Request");
    }

}
