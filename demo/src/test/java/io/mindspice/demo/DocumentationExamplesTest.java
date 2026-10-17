package io.mindspice.demo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import javax.tools.ToolProvider;
import java.nio.file.*;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

class DocumentationExamplesTest {
    @TempDir Path output;
    @Test void completeJavaExamplesCompileAgainstTheCurrentApi() throws Exception {
        var compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "Documentation checks require a JDK");
        var blockPattern = Pattern.compile("```java\\s*\\n(.*?)\\n```", Pattern.DOTALL);
        var classPattern = Pattern.compile("public final class (\\w+)");
        int checked = 0;
        for (String guide : new String[] {"tutorials/first-page.md", "tutorials/dynamic-page.md",
                "how-to/serve-pages-and-fragments.md", "operations/03-writing-tests-for-components-and-modules.md"}) {
            var blocks = blockPattern.matcher(Files.readString(Path.of("../docs", guide)));
            while (blocks.find()) {
                String source = blocks.group(1);
                var name = classPattern.matcher(source);
                if (!name.find() || !source.startsWith("package example;")) continue;
                Path file = output.resolve(name.group(1) + ".java");
                Files.writeString(file, source);
                assertEquals(0, compiler.run(null, null, null, "-classpath", System.getProperty("java.class.path"),
                    "-d", output.toString(), file.toString()), "Example no longer compiles: " + guide);
                checked++;
            }
        }
        assertEquals(4, checked, "Keep complete example coverage explicit");
    }
}
