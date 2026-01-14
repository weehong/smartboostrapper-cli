package com.smartbootstrapper.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contains statistics and results from the code refactoring operation.
 */
public class RefactorResult {

    private final int totalFiles;
    private final int javaFilesRefactored;
    private final int propertiesFilesRefactored;
    private final int xmlFilesRefactored;
    private final int filesSkipped;
    private final List<String> refactoredFiles;
    private final List<RefactorError> errors;
    private final Map<String, byte[]> refactoredFilesWithContent;

    private RefactorResult(Builder builder) {
        this.totalFiles = builder.totalFiles;
        this.javaFilesRefactored = builder.javaFilesRefactored;
        this.propertiesFilesRefactored = builder.propertiesFilesRefactored;
        this.xmlFilesRefactored = builder.xmlFilesRefactored;
        this.filesSkipped = builder.filesSkipped;
        this.refactoredFiles = Collections.unmodifiableList(new ArrayList<>(builder.refactoredFiles));
        this.errors = Collections.unmodifiableList(new ArrayList<>(builder.errors));
        this.refactoredFilesWithContent = Collections.unmodifiableMap(new HashMap<>(builder.refactoredFilesWithContent));
    }

    public int getTotalFiles() {
        return totalFiles;
    }

    public int getJavaFilesRefactored() {
        return javaFilesRefactored;
    }

    public int getPropertiesFilesRefactored() {
        return propertiesFilesRefactored;
    }

    public int getXmlFilesRefactored() {
        return xmlFilesRefactored;
    }

    public int getFilesSkipped() {
        return filesSkipped;
    }

    public int getTotalRefactored() {
        return javaFilesRefactored + propertiesFilesRefactored + xmlFilesRefactored;
    }

    public List<String> getRefactoredFiles() {
        return refactoredFiles;
    }

    /**
     * Returns the refactored files with their content and transformed paths.
     * The map keys are the transformed destination paths.
     */
    public Map<String, byte[]> getRefactoredFilesWithContent() {
        return refactoredFilesWithContent;
    }

    /**
     * Returns true if this result contains refactored files with content.
     */
    public boolean hasRefactoredFilesWithContent() {
        return !refactoredFilesWithContent.isEmpty();
    }

    public List<RefactorError> getErrors() {
        return errors;
    }

    public boolean isSuccess() {
        return errors.isEmpty();
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Combines two refactor results into one (for multi-manifest support).
     */
    public static RefactorResult combine(RefactorResult a, RefactorResult b) {
        Builder builder = builder()
                .totalFiles(a.totalFiles + b.totalFiles);

        // Accumulate counts
        for (int i = 0; i < a.javaFilesRefactored; i++) builder.incrementJavaFiles();
        for (int i = 0; i < b.javaFilesRefactored; i++) builder.incrementJavaFiles();
        for (int i = 0; i < a.propertiesFilesRefactored; i++) builder.incrementPropertiesFiles();
        for (int i = 0; i < b.propertiesFilesRefactored; i++) builder.incrementPropertiesFiles();
        for (int i = 0; i < a.xmlFilesRefactored; i++) builder.incrementXmlFiles();
        for (int i = 0; i < b.xmlFilesRefactored; i++) builder.incrementXmlFiles();
        for (int i = 0; i < a.filesSkipped; i++) builder.incrementSkipped();
        for (int i = 0; i < b.filesSkipped; i++) builder.incrementSkipped();

        // Combine file lists
        a.refactoredFiles.forEach(builder::addRefactoredFile);
        b.refactoredFiles.forEach(builder::addRefactoredFile);

        // Combine file content
        Map<String, byte[]> combinedContent = new HashMap<>(a.refactoredFilesWithContent);
        combinedContent.putAll(b.refactoredFilesWithContent);
        builder.refactoredFiles(combinedContent);

        // Combine errors
        a.errors.forEach(e -> builder.addError(e.getFile(), e.getMessage()));
        b.errors.forEach(e -> builder.addError(e.getFile(), e.getMessage()));

        return builder.build();
    }

    public static class Builder {
        private int totalFiles;
        private int javaFilesRefactored;
        private int propertiesFilesRefactored;
        private int xmlFilesRefactored;
        private int filesSkipped;
        private final List<String> refactoredFiles = new ArrayList<>();
        private final List<RefactorError> errors = new ArrayList<>();
        private Map<String, byte[]> refactoredFilesWithContent = new HashMap<>();

        public Builder totalFiles(int totalFiles) {
            this.totalFiles = totalFiles;
            return this;
        }

        public Builder incrementJavaFiles() {
            this.javaFilesRefactored++;
            return this;
        }

        public Builder incrementPropertiesFiles() {
            this.propertiesFilesRefactored++;
            return this;
        }

        public Builder incrementXmlFiles() {
            this.xmlFilesRefactored++;
            return this;
        }

        public Builder incrementSkipped() {
            this.filesSkipped++;
            return this;
        }

        public Builder addRefactoredFile(String path) {
            this.refactoredFiles.add(path);
            return this;
        }

        /**
         * Sets the refactored files with their content and transformed paths.
         */
        public Builder refactoredFiles(Map<String, byte[]> files) {
            this.refactoredFilesWithContent = new HashMap<>(files);
            return this;
        }

        public Builder addError(String file, String message) {
            this.errors.add(new RefactorError(file, message));
            return this;
        }

        public Builder addError(String file, String message, int line) {
            this.errors.add(new RefactorError(file, message, line));
            return this;
        }

        public RefactorResult build() {
            return new RefactorResult(this);
        }
    }

    /**
     * Represents an error that occurred during refactoring.
     */
    public static class RefactorError {
        private final String file;
        private final String message;
        private final Integer line;

        public RefactorError(String file, String message) {
            this(file, message, null);
        }

        public RefactorError(String file, String message, Integer line) {
            this.file = file;
            this.message = message;
            this.line = line;
        }

        public String getFile() {
            return file;
        }

        public String getMessage() {
            return message;
        }

        public Integer getLine() {
            return line;
        }

        @Override
        public String toString() {
            if (line != null) {
                return String.format("%s:%d - %s", file, line, message);
            }
            return String.format("%s - %s", file, message);
        }
    }

    @Override
    public String toString() {
        return String.format("RefactorResult{total=%d, java=%d, properties=%d, xml=%d, skipped=%d, errors=%d}",
                totalFiles, javaFilesRefactored, propertiesFilesRefactored, xmlFilesRefactored, filesSkipped, errors.size());
    }
}
