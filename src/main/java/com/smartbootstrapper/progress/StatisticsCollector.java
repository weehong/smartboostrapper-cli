package com.smartbootstrapper.progress;

import com.smartbootstrapper.Constants;
import com.smartbootstrapper.model.BootstrapResult;
import com.smartbootstrapper.model.HarvestResult;
import com.smartbootstrapper.model.RefactorResult;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Collects and tracks statistics during the bootstrap operation.
 */
public class StatisticsCollector {

    private Instant startTime;
    private Instant endTime;

    private final AtomicInteger filesDownloaded = new AtomicInteger(0);
    private final AtomicInteger filesHarvested = new AtomicInteger(0);
    private final AtomicInteger filesRefactored = new AtomicInteger(0);
    private final AtomicInteger filesWritten = new AtomicInteger(0);
    private final AtomicInteger errorsEncountered = new AtomicInteger(0);

    private HarvestResult harvestResult;
    private RefactorResult refactorResult;

    /**
     * Starts the timer.
     */
    public void start() {
        this.startTime = Instant.now();
    }

    /**
     * Stops the timer.
     */
    public void stop() {
        this.endTime = Instant.now();
    }

    /**
     * Records downloaded files count.
     */
    public void recordFilesDownloaded(int count) {
        filesDownloaded.addAndGet(count);
    }

    /**
     * Records a harvested file.
     */
    public void recordFileHarvested() {
        filesHarvested.incrementAndGet();
    }

    /**
     * Records a refactored file.
     */
    public void recordFileRefactored() {
        filesRefactored.incrementAndGet();
    }

    /**
     * Records a written file.
     */
    public void recordFileWritten() {
        filesWritten.incrementAndGet();
    }

    /**
     * Records an error.
     */
    public void recordError() {
        errorsEncountered.incrementAndGet();
    }

    /**
     * Sets the harvest result.
     */
    public void setHarvestResult(HarvestResult result) {
        this.harvestResult = result;
    }

    /**
     * Sets the refactor result.
     */
    public void setRefactorResult(RefactorResult result) {
        this.refactorResult = result;
    }

    /**
     * Adds a harvest result (for multi-manifest support).
     * Accumulates statistics from multiple harvests.
     */
    public void addHarvestResult(HarvestResult result) {
        if (this.harvestResult == null) {
            this.harvestResult = result;
        } else {
            // Combine results
            this.harvestResult = HarvestResult.combine(this.harvestResult, result);
        }
    }

    /**
     * Adds a refactor result (for multi-manifest support).
     * Accumulates statistics from multiple refactors.
     */
    public void addRefactorResult(RefactorResult result) {
        if (this.refactorResult == null) {
            this.refactorResult = result;
        } else {
            // Combine results
            this.refactorResult = RefactorResult.combine(this.refactorResult, result);
        }
    }

    /**
     * Gets the elapsed duration.
     */
    public Duration getDuration() {
        if (startTime == null) {
            return Duration.ZERO;
        }
        Instant end = endTime != null ? endTime : Instant.now();
        return Duration.between(startTime, end);
    }

    /**
     * Generates a formatted summary string.
     */
    public String generateSummary() {
        StringBuilder sb = new StringBuilder();

        sb.append("\n");
        sb.append(Constants.ANSI_BOLD + "Statistics:" + Constants.ANSI_RESET).append("\n");

        if (harvestResult != null) {
            sb.append(formatBullet("Files harvested: " + harvestResult.getSuccessfulFiles()));
            if (harvestResult.getFailedFiles() > 0) {
                sb.append(formatBullet("  Failed: " + harvestResult.getFailedFiles()));
            }
        }

        if (refactorResult != null) {
            sb.append(formatBullet("Files refactored: " + refactorResult.getTotalRefactored()));
            sb.append(formatBullet("  Java: " + refactorResult.getJavaFilesRefactored()));
            sb.append(formatBullet("  Properties: " + refactorResult.getPropertiesFilesRefactored()));
            sb.append(formatBullet("  XML: " + refactorResult.getXmlFilesRefactored()));
            sb.append(formatBullet("  Skipped: " + refactorResult.getFilesSkipped()));
        }

        sb.append(formatBullet("Files written: " + filesWritten.get()));

        if (errorsEncountered.get() > 0) {
            sb.append(formatBullet("Errors: " + errorsEncountered.get()));
        }

        sb.append(formatBullet("Duration: " + formatDuration(getDuration())));

        return sb.toString();
    }

    /**
     * Generates a brief one-line summary.
     */
    public String generateBriefSummary() {
        int total = filesWritten.get();
        int refactored = refactorResult != null ? refactorResult.getTotalRefactored() : 0;
        String duration = formatDuration(getDuration());

        return String.format("%d files processed, %d refactored in %s", total, refactored, duration);
    }

    /**
     * Creates a BootstrapResult from collected statistics.
     */
    public BootstrapResult toBootstrapResult(boolean success, String targetDirectory, String errorMessage) {
        if (success) {
            return BootstrapResult.success(targetDirectory, harvestResult, refactorResult, getDuration());
        } else {
            return BootstrapResult.failure(errorMessage, getDuration());
        }
    }

    private String formatBullet(String text) {
        return "  " + Constants.SYMBOL_BULLET + " " + text + "\n";
    }

    private String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        if (seconds < 60) {
            return seconds + " seconds";
        }
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        return String.format("%d min %d sec", minutes, remainingSeconds);
    }

    // Getters for statistics

    public int getFilesDownloaded() {
        return filesDownloaded.get();
    }

    public int getFilesHarvested() {
        return filesHarvested.get();
    }

    public int getFilesRefactored() {
        return filesRefactored.get();
    }

    public int getFilesWritten() {
        return filesWritten.get();
    }

    public int getErrorsEncountered() {
        return errorsEncountered.get();
    }

    public HarvestResult getHarvestResult() {
        return harvestResult;
    }

    public RefactorResult getRefactorResult() {
        return refactorResult;
    }
}
