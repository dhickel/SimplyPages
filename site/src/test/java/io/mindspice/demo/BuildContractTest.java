package io.mindspice.demo;

import io.mindspice.simplypages.core.Component;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;
import java.io.DataInputStream;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BuildContractTest {
    private static final List<String> POMS = List.of("pom.xml", "simplypages/pom.xml", "site/pom.xml");
    private static final String RULES = "/project/build/plugins/plugin[artifactId='maven-enforcer-plugin']"
        + "/executions/execution[id='build-tool-contract']/configuration/rules/";

    @Test
    void reactorAndSiteDependencyVersionsMatch() throws Exception {
        String version = value("pom.xml", "/project/version");
        assertFalse(version.isBlank());
        for (String pom : POMS) {
            assertEquals(version, value(pom, "/project/version"), pom);
        }
        assertEquals(version, value("site/pom.xml",
            "/project/dependencies/dependency[groupId='io.mindspice' and artifactId='simplypages']/version"));
    }

    @Test
    void reactorAndIndependentModulesRequireJava21OrNewer() throws Exception {
        for (String pom : POMS) {
            assertEquals("[21,)", value(pom, RULES + "requireJavaVersion/version"), pom);
            assertEquals("[3.10.0,4)", value(pom, RULES + "requireMavenVersion/version"), pom);
        }
    }

    @Test
    void bothModulesCompileForJava21() throws Exception {
        assertEquals("21", value("simplypages/pom.xml", "/project/properties/maven.compiler.release"));
        assertEquals("21", value("site/pom.xml", "/project/properties/java.version"));
        assertEquals("${maven.compiler.release}", value("simplypages/pom.xml",
            "/project/build/plugins/plugin[artifactId='maven-compiler-plugin']/configuration/release"));
        assertEquals("${java.version}", value("site/pom.xml",
            "/project/build/plugins/plugin[artifactId='maven-compiler-plugin']/configuration/release"));
    }

    @Test
    void compiledLibraryAndSiteUseJava21Bytecode() throws Exception {
        for (Class<?> type : List.of(Component.class, DemoApplication.class)) {
            try (var stream = type.getResourceAsStream("/" + type.getName().replace('.', '/') + ".class")) {
                assertNotNull(stream, type.getName());
                var data = new DataInputStream(stream);
                assertEquals(0xCAFEBABE, data.readInt());
                assertEquals(0, data.readUnsignedShort(), "No preview bytecode: " + type.getName());
                assertEquals(65, data.readUnsignedShort(), type.getName());
            }
        }
    }

    private static String value(String pom, String expression) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Document document = factory.newDocumentBuilder().parse(Path.of("..", pom).toFile());
        return XPathFactory.newInstance().newXPath().evaluate(expression, document).trim();
    }
}
