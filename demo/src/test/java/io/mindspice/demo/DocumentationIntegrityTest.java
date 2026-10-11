package io.mindspice.demo;

import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DocumentationIntegrityTest {
    @Test void localMarkdownLinksAndAnchorsResolve() throws Exception {
        Path root = Path.of("../docs").toAbsolutePath().normalize();
        var service = new DocumentationService();
        var failures = new ArrayList<String>();
        try (var files = Files.walk(root)) {
            var sources = new ArrayList<>(files.filter(p -> p.toString().endsWith(".md")).toList());
            sources.add(root.getParent().resolve("README.md"));
            for (Path source : sources) {
                var document = Parser.builder().build().parse(Files.readString(source));
                document.accept(new AbstractVisitor() {
                    @Override public void visit(Link link) {
                        try {
                            URI uri = URI.create(link.getDestination());
                            if (uri.isAbsolute() || uri.getRawAuthority() != null || uri.getPath().startsWith("/")) return;
                            Path target = uri.getPath().isEmpty() ? source : source.getParent().resolve(uri.getPath()).normalize();
                            if (!Files.isRegularFile(target)) {
                                failures.add(source + " -> " + link.getDestination());
                            } else if (uri.getFragment() != null && !uri.getFragment().isEmpty() && target.toString().endsWith(".md")) {
                                String html = service.renderMarkdown(root.relativize(target).toString(), Files.readString(target));
                                if (Jsoup.parse(html).getElementById(uri.getFragment()) == null) {
                                    failures.add(source + " -> missing anchor " + link.getDestination());
                                }
                            }
                        } catch (Exception failure) {
                            failures.add(source + " -> " + link.getDestination() + ": " + failure);
                        }
                    }
                });
            }
        }
        assertTrue(failures.isEmpty(), String.join("\n", failures));
    }

    @Test void everyGuideIsDiscoverableFromTheIndex() throws Exception {
        Path root = Path.of("../docs");
        String index = Files.readString(root.resolve("INDEX.md"));
        try (var files = Files.walk(root)) {
            for (Path path : files.filter(p -> p.toString().endsWith(".md")).toList()) {
                String relative = root.relativize(path).toString();
                if (relative.equals("INDEX.md") || relative.equals("README.md")) continue;
                assertTrue(index.contains("(" + relative + ")"), "Missing index link: " + relative);
            }
        }
    }
}
