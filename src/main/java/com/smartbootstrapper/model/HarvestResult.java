package com.smartbootstrapper.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Contains statistics and results from the file harvesting operation.
 */
public class HarvestResult {

    private final int totalFiles;
    private final int successfulFiles;
    private final int failedFiles;
    private final Map<String, byte[]> harvestedFiles;
    private final Map<String, String> errors;

    private HarvestResult(Builder builder) {
        this.totalFiles = builder.totalFiles;
        this.successfulFiles = builder.harvestedFiles.size();
        this.failedFiles = builder.errors.size();
        this.harvestedFiles = Collections.unmodifiableMap(new HashMap<>(builder.harvestedFiles));
        this.errors = Collections.unmodifiableMap(new HashMap<>(builder.errors));
    }

    public int getTotalFiles() {
        return totalFiles;
    }

    public int getSuccessfulFiles() {
        return successfulFiles;
    }

    public int getFailedFiles() {
        return failedFiles;
    }

    public Map<String, byte[]> getHarvestedFiles() {
        return harvestedFiles;
    }

    public Map<String, String> getErrors() {
        return errors;
    }

    public boolean isSuccess() {
        return failedFiles == 0;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Combines two harvest results into one (for multi-manifest support).
     */
    public static HarvestResult combine(HarvestResult a, HarvestResult b) {
        Builder builder = builder()
                .totalFiles(a.totalFiles + b.totalFiles);

        // Combine harvested files
        a.harvestedFiles.forEach(builder::addHarvestedFile);
        b.harvestedFiles.forEach(builder::addHarvestedFile);

        // Combine errors
        a.errors.forEach(builder::addError);
        b.errors.forEach(builder::addError);

        return builder.build();
    }

    public static class Builder {
        private int totalFiles;
        private final Map<String, byte[]> harvestedFiles = new HashMap<>();
        private final Map<String, String> errors = new HashMap<>();

        public Builder totalFiles(int totalFiles) {
            this.totalFiles = totalFiles;
            return this;
        }

        public Builder addHarvestedFile(String destinationPath, byte[] content) {
            harvestedFiles.put(destinationPath, content);
            return this;
        }

        public Builder addError(String destinationPath, String error) {
            errors.put(destinationPath, error);
            return this;
        }

        public HarvestResult build() {
            return new HarvestResult(this);
        }
    }

    @Override
    public String toString() {
        return String.format("HarvestResult{total=%d, successful=%d, failed=%d}",
                totalFiles, successfulFiles, failedFiles);
    }
}
