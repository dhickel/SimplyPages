package io.mindspice.demo;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class DocumentationExamplesTest {
    static final List<String> TASKS = List.of("build-a-markdown-blog", "build-a-forum",
        "build-user-editable-pages", "add-chat", "build-a-dashboard", "handle-form-submissions");
    static final List<String> GUIDES = Stream.concat(
        Stream.of("tutorials/first-page.md", "tutorials/dynamic-page.md",
            "how-to/serve-pages-and-fragments.md", "operations/03-writing-tests-for-components-and-modules.md"),
        TASKS.stream().map(name -> "how-to/" + name + ".md")
    ).toList();
    private static final Pattern JAVA = Pattern.compile("(?m)^```java[\\t ]*\\R([\\s\\S]*?)^```[\\t ]*$");
    private static final Pattern PACKAGE = Pattern.compile("(?m)^package\\s+([\\w.]+)\\s*;");
    private static final Pattern TYPE = Pattern.compile("public\\s+(?:final\\s+)?(?:class|record)\\s+(\\w+)");

    @TempDir Path directory;

    @TestFactory
    Stream<DynamicTest> completeListingsUseTheCurrentPublicApi() {
        return GUIDES.stream().map(guide -> DynamicTest.dynamicTest(guide, () -> {
            try (Compilation compiled = compile(guide, directory.resolve(guide.replace('/', '_')))) {
                assertEquals(1, compiled.classes().size(), "Keep complete example coverage explicit: " + guide);
            }
        }));
    }

    static Compilation compile(String guide, Path directory) throws IOException {
        var compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "Documentation checks require a JDK, not a JRE");
        Path sources = Files.createDirectories(directory.resolve("source"));
        Path classes = Files.createDirectories(directory.resolve("classes"));
        List<Path> sourceFiles = new ArrayList<>();
        List<String> names = new ArrayList<>();
        var blocks = JAVA.matcher(Files.readString(Path.of("../docs", guide)));
        while (blocks.find()) {
            String code = blocks.group(1);
            var pack = PACKAGE.matcher(code);
            var type = TYPE.matcher(code);
            if (!pack.find() || !type.find()) continue;
            String name = pack.group(1) + "." + type.group(1);
            assertFalse(names.contains(name), "Duplicate complete listing: " + guide + " / " + name);
            Path source = sources.resolve(name.replace('.', '/') + ".java");
            Files.createDirectories(source.getParent());
            Files.writeString(source, code);
            sourceFiles.add(source);
            names.add(name);
        }
        assertFalse(sourceFiles.isEmpty(), "Provide a complete imported Java example: " + guide);
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        try (var files = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            boolean success = compiler.getTask(null, files, diagnostics,
                List.of("--release", "21", "-proc:none", "-classpath", System.getProperty("java.class.path"),
                    "-d", classes.toString()), null,
                files.getJavaFileObjectsFromPaths(sourceFiles)).call();
            assertTrue(success, () -> "Noncompiling documentation: " + guide + "\n" + diagnostics.getDiagnostics());
        }
        return new Compilation(new URLClassLoader(new java.net.URL[]{classes.toUri().toURL()},
            DocumentationExamplesTest.class.getClassLoader()), List.copyOf(names));
    }

    record Compilation(URLClassLoader loader, List<String> classes) implements AutoCloseable {
        Class<?> type(String simpleName) throws ClassNotFoundException {
            return Class.forName(classes.stream().filter(n -> n.endsWith("." + simpleName)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Missing example: " + simpleName)), true, loader);
        }

        @Override public void close() throws IOException { loader.close(); }
    }
}
