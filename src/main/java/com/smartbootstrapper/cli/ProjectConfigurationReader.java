package com.smartbootstrapper.cli;

import com.smartbootstrapper.exception.SmartBootstrapperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Reads project configuration from an existing Maven project (pom.xml).
 * Used to pre-populate configuration defaults in interactive mode.
 */
public class ProjectConfigurationReader {

    private static final Logger logger = LoggerFactory.getLogger(ProjectConfigurationReader.class);

    private final Path projectPath;
    private Document pomDocument;

    public ProjectConfigurationReader(Path projectPath) {
        this.projectPath = projectPath;
    }

    /**
     * Reads and parses the pom.xml from the project path.
     *
     * @throws SmartBootstrapperException if the pom.xml cannot be read or parsed
     */
    public void load() {
        Path pomPath = projectPath.resolve("pom.xml");

        if (!Files.exists(pomPath)) {
            throw new SmartBootstrapperException(
                    "No pom.xml found in project path: " + projectPath,
                    "Ensure the path points to a Maven project root"
            );
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Security: disable external entities
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            pomDocument = builder.parse(pomPath.toFile());
            pomDocument.getDocumentElement().normalize();

            logger.debug("Successfully loaded pom.xml from {}", pomPath);
        } catch (Exception e) {
            throw new SmartBootstrapperException(
                    "Failed to parse pom.xml: " + e.getMessage(),
                    pomPath.toString(),
                    e
            );
        }
    }

    /**
     * Gets the groupId from the pom.xml.
     */
    public Optional<String> getGroupId() {
        return getDirectChildText("groupId")
                .or(() -> getParentChildText("groupId"));
    }

    /**
     * Gets the artifactId from the pom.xml.
     */
    public Optional<String> getArtifactId() {
        return getDirectChildText("artifactId");
    }

    /**
     * Gets the version from the pom.xml.
     */
    public Optional<String> getVersion() {
        return getDirectChildText("version")
                .or(() -> getParentChildText("version"));
    }

    /**
     * Gets the project name from the pom.xml.
     */
    public Optional<String> getProjectName() {
        return getDirectChildText("name");
    }

    /**
     * Gets the Java version from the pom.xml properties.
     */
    public Optional<String> getJavaVersion() {
        return getPropertyValue("java.version")
                .or(() -> getPropertyValue("maven.compiler.source"))
                .or(() -> getPropertyValue("maven.compiler.target"));
    }

    /**
     * Gets the Spring Boot version from the pom.xml.
     * Checks parent version if spring-boot-starter-parent is used,
     * or looks for spring-boot.version property.
     */
    public Optional<String> getSpringBootVersion() {
        // Check parent for spring-boot-starter-parent
        Optional<String> parentArtifactId = getParentChildText("artifactId");
        if (parentArtifactId.isPresent() && parentArtifactId.get().equals("spring-boot-starter-parent")) {
            return getParentChildText("version");
        }

        // Check for spring-boot.version property
        return getPropertyValue("spring-boot.version");
    }

    /**
     * Infers the base package from the project's source directory structure.
     * Looks for the main Application class or the first package found.
     */
    public Optional<String> inferBasePackage() {
        Path srcMainJava = projectPath.resolve("src/main/java");

        if (!Files.exists(srcMainJava)) {
            return Optional.empty();
        }

        try {
            // Walk the source tree to find the deepest common package
            return Files.walk(srcMainJava)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .map(p -> {
                        Path relative = srcMainJava.relativize(p.getParent());
                        return relative.toString().replace(File.separatorChar, '.');
                    })
                    .filter(pkg -> !pkg.isEmpty())
                    .findFirst();
        } catch (Exception e) {
            logger.debug("Failed to infer base package: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Gets the project path.
     */
    public Path getProjectPath() {
        return projectPath;
    }

    private Optional<String> getDirectChildText(String tagName) {
        if (pomDocument == null) {
            return Optional.empty();
        }

        Element root = pomDocument.getDocumentElement();
        NodeList nodeList = root.getElementsByTagName(tagName);

        for (int i = 0; i < nodeList.getLength(); i++) {
            Element element = (Element) nodeList.item(i);
            // Only get direct children of the root element
            if (element.getParentNode().equals(root)) {
                String text = element.getTextContent().trim();
                if (!text.isEmpty() && !text.startsWith("${")) {
                    return Optional.of(text);
                }
            }
        }

        return Optional.empty();
    }

    private Optional<String> getParentChildText(String tagName) {
        if (pomDocument == null) {
            return Optional.empty();
        }

        Element root = pomDocument.getDocumentElement();
        NodeList parentList = root.getElementsByTagName("parent");

        if (parentList.getLength() > 0) {
            Element parent = (Element) parentList.item(0);
            NodeList childList = parent.getElementsByTagName(tagName);

            if (childList.getLength() > 0) {
                String text = childList.item(0).getTextContent().trim();
                if (!text.isEmpty() && !text.startsWith("${")) {
                    return Optional.of(text);
                }
            }
        }

        return Optional.empty();
    }

    private Optional<String> getPropertyValue(String propertyName) {
        if (pomDocument == null) {
            return Optional.empty();
        }

        Element root = pomDocument.getDocumentElement();
        NodeList propertiesList = root.getElementsByTagName("properties");

        if (propertiesList.getLength() > 0) {
            Element properties = (Element) propertiesList.item(0);
            NodeList propertyList = properties.getElementsByTagName(propertyName);

            if (propertyList.getLength() > 0) {
                String text = propertyList.item(0).getTextContent().trim();
                if (!text.isEmpty()) {
                    return Optional.of(text);
                }
            }
        }

        return Optional.empty();
    }
}
