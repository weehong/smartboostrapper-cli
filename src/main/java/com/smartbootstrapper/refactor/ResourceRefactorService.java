package com.smartbootstrapper.refactor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles package name refactoring for non-Java resource files.
 * Uses regex-based replacement while preserving file formatting.
 */
public class ResourceRefactorService {

    private static final Logger logger = LoggerFactory.getLogger(ResourceRefactorService.class);

    /**
     * Refactors package references in a properties file (application.properties, application.yml).
     *
     * @param content    The file content
     * @param oldPackage The old package name to replace
     * @param newPackage The new package name
     * @return The refactored content
     */
    public String refactorPropertiesFile(String content, String oldPackage, String newPackage) {
        if (oldPackage.equals(newPackage)) {
            return content;
        }

        logger.debug("Refactoring properties file ({} -> {})", oldPackage, newPackage);

        String result = content;

        // Replace exact package matches and package prefixes
        // Handles: basePackage=com.old.package, scanPackages=com.old.package.subpkg
        result = replacePackageReferences(result, oldPackage, newPackage);

        // Handle package paths in resource locations (replace . with /)
        String oldPath = oldPackage.replace(".", "/");
        String newPath = newPackage.replace(".", "/");
        result = replacePackageReferences(result, oldPath, newPath);

        return result;
    }

    /**
     * Refactors package references in an XML file (Spring config, pom.xml, etc.).
     *
     * @param content    The file content
     * @param oldPackage The old package name to replace
     * @param newPackage The new package name
     * @return The refactored content
     */
    public String refactorXmlFile(String content, String oldPackage, String newPackage) {
        if (oldPackage.equals(newPackage)) {
            return content;
        }

        logger.debug("Refactoring XML file ({} -> {})", oldPackage, newPackage);

        String result = content;

        // Replace package references in XML attributes and text
        // Handles: base-package="com.old.package", <context:component-scan base-package="..."/>
        result = replacePackageReferences(result, oldPackage, newPackage);

        // Handle package paths (replace . with /)
        String oldPath = oldPackage.replace(".", "/");
        String newPath = newPackage.replace(".", "/");
        result = replacePackageReferences(result, oldPath, newPath);

        return result;
    }

    /**
     * Refactors package references in any text file using pattern matching.
     *
     * @param content    The file content
     * @param oldPackage The old package name to replace
     * @param newPackage The new package name
     * @return The refactored content
     */
    public String refactorGenericFile(String content, String oldPackage, String newPackage) {
        if (oldPackage.equals(newPackage)) {
            return content;
        }

        return replacePackageReferences(content, oldPackage, newPackage);
    }

    /**
     * Replaces package references in content, handling both exact matches
     * and prefix matches (e.g., com.old.package and com.old.package.subpkg).
     */
    private String replacePackageReferences(String content, String oldPackage, String newPackage) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        // Escape special regex characters in the package name
        String escapedOldPackage = Pattern.quote(oldPackage);

        // Pattern to match the old package followed by:
        // - end of line/string
        // - a dot (for subpackages)
        // - a non-word character (but not a dot that continues the package)
        // This ensures we don't accidentally replace partial matches
        // e.g., com.old doesn't match com.older
        Pattern pattern = Pattern.compile(
                escapedOldPackage + "(?=\\.|[^a-zA-Z0-9_.]|$)"
        );

        Matcher matcher = pattern.matcher(content);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            matcher.appendReplacement(result, Matcher.quoteReplacement(newPackage));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Determines if a file should be refactored based on its extension.
     *
     * @param fileName The file name
     * @return true if the file type supports refactoring
     */
    public boolean shouldRefactor(String fileName) {
        if (fileName == null) {
            return false;
        }

        String lowerName = fileName.toLowerCase();
        return lowerName.endsWith(".properties") ||
               lowerName.endsWith(".yml") ||
               lowerName.endsWith(".yaml") ||
               lowerName.endsWith(".xml") ||
               lowerName.endsWith(".json") ||
               lowerName.endsWith(".txt") ||
               lowerName.endsWith(".md") ||
               lowerName.endsWith(".gradle") ||
               lowerName.endsWith(".kts");
    }

    /**
     * Refactors a file based on its type.
     *
     * @param content    The file content
     * @param fileName   The file name (used to determine file type)
     * @param oldPackage The old package name
     * @param newPackage The new package name
     * @return The refactored content
     */
    public String refactorByFileType(String content, String fileName, String oldPackage, String newPackage) {
        if (fileName == null) {
            return refactorGenericFile(content, oldPackage, newPackage);
        }

        String lowerName = fileName.toLowerCase();

        if (lowerName.endsWith(".properties") || lowerName.endsWith(".yml") || lowerName.endsWith(".yaml")) {
            return refactorPropertiesFile(content, oldPackage, newPackage);
        }

        if (lowerName.endsWith(".xml")) {
            return refactorXmlFile(content, oldPackage, newPackage);
        }

        return refactorGenericFile(content, oldPackage, newPackage);
    }
}
