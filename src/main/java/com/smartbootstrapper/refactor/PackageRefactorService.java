package com.smartbootstrapper.refactor;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.github.javaparser.printer.DefaultPrettyPrinter;
import com.smartbootstrapper.exception.RefactorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Handles package name refactoring for Java source files using JavaParser.
 */
public class PackageRefactorService {

    private static final Logger logger = LoggerFactory.getLogger(PackageRefactorService.class);

    private final JavaParser javaParser;
    private final DefaultPrettyPrinter printer;

    public PackageRefactorService() {
        // Configure JavaParser to support modern Java features (records, pattern matching, switch expressions)
        ParserConfiguration config = new ParserConfiguration();
        config.setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE);
        this.javaParser = new JavaParser(config);
        this.printer = new DefaultPrettyPrinter();
    }

    /**
     * Refactors package names in a Java source file.
     *
     * @param content    The Java source code content
     * @param oldPackage The old package name to replace
     * @param newPackage The new package name
     * @return The refactored source code
     * @throws RefactorException if parsing or refactoring fails
     */
    public String refactorJavaFile(String content, String oldPackage, String newPackage) {
        return refactorJavaFile(content, oldPackage, newPackage, "unknown");
    }

    /**
     * Refactors package names in a Java source file.
     *
     * @param content    The Java source code content
     * @param oldPackage The old package name to replace
     * @param newPackage The new package name
     * @param fileName   The file name for error messages
     * @return The refactored source code
     * @throws RefactorException if parsing or refactoring fails
     */
    public String refactorJavaFile(String content, String oldPackage, String newPackage, String fileName) {
        logger.debug("Refactoring Java file: {} ({} -> {})", fileName, oldPackage, newPackage);

        if (oldPackage.equals(newPackage)) {
            logger.debug("Old and new packages are the same, no refactoring needed");
            return content;
        }

        ParseResult<CompilationUnit> parseResult = javaParser.parse(content);

        if (!parseResult.isSuccessful()) {
            String errorMessage = parseResult.getProblems().stream()
                    .map(p -> p.getMessage())
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("Unknown parse error");

            throw new RefactorException(
                    "Failed to parse Java file: " + errorMessage,
                    fileName
            );
        }

        CompilationUnit compilationUnit = parseResult.getResult()
                .orElseThrow(() -> new RefactorException("Parse result is empty", fileName));

        boolean modified = false;

        // Refactor package declaration
        Optional<PackageDeclaration> packageDecl = compilationUnit.getPackageDeclaration();
        if (packageDecl.isPresent()) {
            String currentPackage = packageDecl.get().getNameAsString();
            String refactoredPackage = refactorPackageName(currentPackage, oldPackage, newPackage);
            if (!currentPackage.equals(refactoredPackage)) {
                packageDecl.get().setName(refactoredPackage);
                modified = true;
                logger.debug("Refactored package declaration: {} -> {}", currentPackage, refactoredPackage);
            }
        }

        // Refactor import statements
        for (ImportDeclaration importDecl : compilationUnit.getImports()) {
            String importName = importDecl.getNameAsString();
            String refactoredImport = refactorPackageName(importName, oldPackage, newPackage);
            if (!importName.equals(refactoredImport)) {
                importDecl.setName(refactoredImport);
                modified = true;
                logger.debug("Refactored import: {} -> {}", importName, refactoredImport);
            }
        }

        // Refactor fully qualified class references in the code
        compilationUnit.findAll(Name.class).forEach(name -> {
            String qualifiedName = name.asString();
            if (qualifiedName.startsWith(oldPackage + ".")) {
                String refactoredName = refactorPackageName(qualifiedName, oldPackage, newPackage);
                if (!qualifiedName.equals(refactoredName)) {
                    // Replace the name by rebuilding it
                    Name newName = parseName(refactoredName);
                    name.getParentNode().ifPresent(parent -> {
                        if (parent instanceof NodeWithName) {
                            ((NodeWithName<?>) parent).setName(refactoredName);
                        }
                    });
                    logger.debug("Refactored qualified name: {} -> {}", qualifiedName, refactoredName);
                }
            }
        });

        if (modified) {
            logger.info("Successfully refactored Java file: {}", fileName);
        } else {
            logger.debug("No package references found to refactor in: {}", fileName);
        }

        return printer.print(compilationUnit);
    }

    /**
     * Refactors a package name by replacing the old package prefix with the new one.
     *
     * @param originalName The original package or fully qualified name
     * @param oldPackage   The old package prefix to replace
     * @param newPackage   The new package prefix
     * @return The refactored name
     */
    public String refactorPackageName(String originalName, String oldPackage, String newPackage) {
        if (originalName == null || originalName.isEmpty()) {
            return originalName;
        }

        // Handle exact match
        if (originalName.equals(oldPackage)) {
            return newPackage;
        }

        // Handle prefix match (e.g., com.old.package.Class -> com.new.package.Class)
        if (originalName.startsWith(oldPackage + ".")) {
            return newPackage + originalName.substring(oldPackage.length());
        }

        return originalName;
    }

    /**
     * Validates that a Java file can be parsed.
     *
     * @param content  The Java source code content
     * @param fileName The file name for error messages
     * @return true if the file can be parsed, false otherwise
     */
    public boolean canParse(String content, String fileName) {
        try {
            ParseResult<CompilationUnit> parseResult = javaParser.parse(content);
            return parseResult.isSuccessful();
        } catch (Exception e) {
            logger.debug("Failed to parse Java file {}: {}", fileName, e.getMessage());
            return false;
        }
    }

    /**
     * Gets parse errors for a Java file.
     *
     * @param content  The Java source code content
     * @param fileName The file name for error messages
     * @return Error message if parsing fails, empty string if successful
     */
    public String getParseErrors(String content, String fileName) {
        try {
            ParseResult<CompilationUnit> parseResult = javaParser.parse(content);
            if (parseResult.isSuccessful()) {
                return "";
            }
            return parseResult.getProblems().stream()
                    .map(p -> String.format("Line %d: %s",
                            p.getLocation().map(l -> l.getBegin().getRange()
                                    .map(r -> r.begin.line).orElse(-1)).orElse(-1),
                            p.getMessage()))
                    .reduce((a, b) -> a + "\n" + b)
                    .orElse("Unknown parse error");
        } catch (Exception e) {
            return "Parse exception: " + e.getMessage();
        }
    }

    private Name parseName(String name) {
        String[] parts = name.split("\\.");
        Name result = new Name(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            result = new Name(result, parts[i]);
        }
        return result;
    }
}
