package com.smartbootstrapper.model;

import java.time.Duration;
import java.time.Instant;

/**
 * Overall result of the bootstrap operation with summary statistics.
 */
public class BootstrapResult {

    private final boolean success;
    private final String targetDirectory;
    private final HarvestResult harvestResult;
    private final RefactorResult refactorResult;
    private final Duration duration;
    private final String errorMessage;

    private BootstrapResult(Builder builder) {
        this.success = builder.success;
        this.targetDirectory = builder.targetDirectory;
        this.harvestResult = builder.harvestResult;
        this.refactorResult = builder.refactorResult;
        this.duration = builder.duration;
        this.errorMessage = builder.errorMessage;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getTargetDirectory() {
        return targetDirectory;
    }

    public HarvestResult getHarvestResult() {
        return harvestResult;
    }

    public RefactorResult getRefactorResult() {
        return refactorResult;
    }

    public Duration getDuration() {
        return duration;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public int getTotalFilesProcessed() {
        int total = 0;
        if (harvestResult != null) {
            total += harvestResult.getSuccessfulFiles();
        }
        return total;
    }

    public int getTotalFilesRefactored() {
        if (refactorResult != null) {
            return refactorResult.getTotalRefactored();
        }
        return 0;
    }

    public String getFormattedDuration() {
        if (duration == null) {
            return "N/A";
        }
        long seconds = duration.getSeconds();
        if (seconds < 60) {
            return seconds + " seconds";
        }
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        return String.format("%d min %d sec", minutes, remainingSeconds);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static BootstrapResult success(String targetDirectory, HarvestResult harvestResult,
                                          RefactorResult refactorResult, Duration duration) {
        return builder()
                .success(true)
                .targetDirectory(targetDirectory)
                .harvestResult(harvestResult)
                .refactorResult(refactorResult)
                .duration(duration)
                .build();
    }

    public static BootstrapResult failure(String errorMessage, Duration duration) {
        return builder()
                .success(false)
                .errorMessage(errorMessage)
                .duration(duration)
                .build();
    }

    public static class Builder {
        private boolean success;
        private String targetDirectory;
        private HarvestResult harvestResult;
        private RefactorResult refactorResult;
        private Duration duration;
        private String errorMessage;
        private Instant startTime;

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder targetDirectory(String targetDirectory) {
            this.targetDirectory = targetDirectory;
            return this;
        }

        public Builder harvestResult(HarvestResult harvestResult) {
            this.harvestResult = harvestResult;
            return this;
        }

        public Builder refactorResult(RefactorResult refactorResult) {
            this.refactorResult = refactorResult;
            return this;
        }

        public Builder duration(Duration duration) {
            this.duration = duration;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder startTimer() {
            this.startTime = Instant.now();
            return this;
        }

        public Builder stopTimer() {
            if (startTime != null) {
                this.duration = Duration.between(startTime, Instant.now());
            }
            return this;
        }

        public BootstrapResult build() {
            return new BootstrapResult(this);
        }
    }

    @Override
    public String toString() {
        if (success) {
            return String.format("BootstrapResult{success=true, target='%s', files=%d, refactored=%d, duration=%s}",
                    targetDirectory, getTotalFilesProcessed(), getTotalFilesRefactored(), getFormattedDuration());
        }
        return String.format("BootstrapResult{success=false, error='%s', duration=%s}",
                errorMessage, getFormattedDuration());
    }
}
