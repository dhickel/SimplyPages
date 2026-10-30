package io.mindspice.demo;

import io.mindspice.simplypages.builders.BannerBuilder;
import io.mindspice.simplypages.builders.ShellBuilder;
import io.mindspice.simplypages.builders.TopNavBuilder;
import io.mindspice.simplypages.components.RawHtml;
import io.mindspice.simplypages.core.Component;
import org.owasp.encoder.Encode;
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
            return "Blog content not found: " + Encode.forHtml(requestPath);
        }

        String renderedContent = resolved.render();
        if ("true".equalsIgnoreCase(hxRequest)) {
            return renderedContent;
        }

        return ShellBuilder.create()
            .withPageTitle("SimplyPages Blog")
            .withTopBanner(BannerBuilder.create()
                .withLayout(BannerBuilder.BannerLayout.HORIZONTAL)
                .withTitle("SimplyPages")
                .withSubtitle("Static content helper smoke test")
                .build())
            .withTopNav(buildGlobalTopNav())
            .withContent(new RawHtml(renderedContent))
            .build();
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

    private Component buildGlobalTopNav() {
        return TopNavBuilder.create()
            .withHtmxNavigation(false)
            .addPrimaryLink("Home", "/home")
            .addPrimaryLink("Demos", "/demos")
            .addPrimaryLink("Javadocs", "/javadocs-view")
            .addPrimaryLink("Forum", "/forum")
            .addPrimaryLink("Chat", "/chat")
            .addPrimaryLink("Blog", "/blog")
            .addPrimaryLink("Docs", "/docs")
            .build();
    }
}
