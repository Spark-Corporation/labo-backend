package org.sparkcorporation.lims;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Locks the logging contract in {@code logback-spring.xml}: the console is human-readable (a plain
 * pattern encoder, <em>not</em> JSON), while structured JSON goes to a rolling file appender. This
 * parses the production resource directly rather than inspecting the runtime {@code LoggerContext},
 * because {@code logback-test.xml} overrides {@code logback-spring.xml} during the test JVM and the
 * live context would reflect the test config, not the one shipped to production.
 */
class LoggingConfigurationTest {

    private static final String LOGSTASH_ENCODER = "net.logstash.logback.encoder.LogstashEncoder";

    @Test
    void console_appender_is_plain_text_not_json() {
        var root = loadLogbackSpringConfig();
        Element console = singleAppenderOfType(root, "ConsoleAppender");

        Element encoder = firstChildElement(console, "encoder");
        assertThat(encoder).as("console appender must declare an encoder").isNotNull();
        assertThat(attr(encoder, "class"))
                .as("console encoder must not be the JSON LogstashEncoder")
                .doesNotContain("Logstash");
        assertThat(firstChildElement(encoder, "pattern"))
                .as("console encoder must use a human-readable <pattern>")
                .isNotNull();
    }

    @Test
    void json_goes_to_a_rolling_file_appender() {
        var root = loadLogbackSpringConfig();
        Element file = singleAppenderOfType(root, "RollingFileAppender");

        Element encoder = firstChildElement(file, "encoder");
        assertThat(encoder).as("rolling file appender must declare an encoder").isNotNull();
        assertThat(attr(encoder, "class"))
                .as("file encoder must emit JSON via LogstashEncoder")
                .isEqualTo(LOGSTASH_ENCODER);
    }

    @Test
    void root_logger_wires_both_console_and_file() {
        var root = loadLogbackSpringConfig();
        String consoleName = attr(singleAppenderOfType(root, "ConsoleAppender"), "name");
        String fileName = attr(singleAppenderOfType(root, "RollingFileAppender"), "name");

        Element rootLogger = firstChildElement(root, "root");
        assertThat(rootLogger).as("a <root> logger must be declared").isNotNull();
        List<String> refs =
                childElements(rootLogger, "appender-ref").stream()
                        .map(ref -> attr(ref, "ref"))
                        .toList();
        assertThat(refs).containsExactlyInAnyOrder(consoleName, fileName);
    }

    private static Element loadLogbackSpringConfig() {
        try (InputStream in =
                LoggingConfigurationTest.class.getResourceAsStream("/logback-spring.xml")) {
            assertThat(in).as("logback-spring.xml must be on the classpath").isNotNull();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Harden the parser against XXE — this config is trusted, but keep the default secure.
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setExpandEntityReferences(false);
            Document doc = factory.newDocumentBuilder().parse(in);
            return doc.getDocumentElement();
        } catch (Exception e) {
            throw new AssertionError("Failed to parse logback-spring.xml", e);
        }
    }

    /**
     * Returns the single {@code <appender>} whose {@code class} attribute ends with the given type.
     */
    private static Element singleAppenderOfType(Element configRoot, String simpleClassName) {
        List<Element> matches =
                childElements(configRoot, "appender").stream()
                        .filter(a -> attr(a, "class").endsWith(simpleClassName))
                        .toList();
        assertThat(matches)
                .as("expected exactly one %s in logback-spring.xml", simpleClassName)
                .hasSize(1);
        return matches.get(0);
    }

    private static List<Element> childElements(Element parent, String tagName) {
        NodeList all = parent.getElementsByTagName(tagName);
        List<Element> result = new ArrayList<>();
        for (int i = 0; i < all.getLength(); i++) {
            Node node = all.item(i);
            if (node instanceof Element element) {
                result.add(element);
            }
        }
        return result;
    }

    private static Element firstChildElement(Element parent, String tagName) {
        List<Element> children = childElements(parent, tagName);
        return children.isEmpty() ? null : children.get(0);
    }

    private static String attr(Element element, String name) {
        return element.getAttribute(name);
    }
}
