package com.smartbootstrapper.progress;

import com.smartbootstrapper.Constants;

import java.io.PrintWriter;

/**
 * Manages multi-phase progress display for CLI output.
 */
public class ProgressTracker {

    private final PrintWriter writer;
    private final boolean useColors;
    private int currentPhase;
    private int totalPhases;
    private String currentPhaseDescription;
    private int subTaskCount;

    public ProgressTracker(PrintWriter writer) {
        this(writer, true);
    }

    public ProgressTracker(PrintWriter writer, boolean useColors) {
        this.writer = writer;
        this.useColors = useColors;
        this.currentPhase = 0;
        this.totalPhases = 0;
        this.subTaskCount = 0;
    }

    /**
     * Initializes the progress tracker with total number of phases.
     *
     * @param totalPhases Total number of phases
     */
    public void initialize(int totalPhases) {
        this.totalPhases = totalPhases;
        this.currentPhase = 0;
    }

    /**
     * Starts a new phase.
     *
     * @param phaseNumber   The phase number (1-indexed)
     * @param description   Description of the phase
     */
    public void startPhase(int phaseNumber, String description) {
        startPhase(phaseNumber, this.totalPhases, description);
    }

    /**
     * Starts a new phase with explicit total phases.
     *
     * @param phaseNumber   The phase number (1-indexed)
     * @param totalPhases   Total number of phases
     * @param description   Description of the phase
     */
    public void startPhase(int phaseNumber, int totalPhases, String description) {
        this.currentPhase = phaseNumber;
        this.totalPhases = totalPhases;
        this.currentPhaseDescription = description;
        this.subTaskCount = 0;

        writer.println();
        writer.println(formatPhaseHeader(phaseNumber, totalPhases, description));
        writer.flush();
    }

    /**
     * Reports a completed sub-task within the current phase.
     *
     * @param description Description of the completed sub-task
     */
    public void reportSubTask(String description) {
        subTaskCount++;
        writer.println(formatSubTask(true, description));
        writer.flush();
    }

    /**
     * Reports an error within the current phase.
     *
     * @param description Description of the error
     */
    public void reportError(String description) {
        writer.println(formatSubTask(false, description));
        writer.flush();
    }

    /**
     * Reports progress with a custom message.
     *
     * @param message The message to display
     */
    public void reportProgress(String message) {
        writer.println("  " + message);
        writer.flush();
    }

    /**
     * Reports phase completion.
     */
    public void completePhase() {
        writer.println(formatPhaseComplete());
        writer.flush();
    }

    /**
     * Reports overall completion.
     *
     * @param success Whether the operation was successful
     * @param summary Summary message
     */
    public void reportCompletion(boolean success, String summary) {
        writer.println();
        if (success) {
            writer.println(colorGreen(bold(Constants.SYMBOL_CHECK + " " + summary)));
        } else {
            writer.println(colorRed(bold(Constants.SYMBOL_CROSS + " " + summary)));
        }
        writer.flush();
    }

    /**
     * Reports rollback in progress.
     */
    public void reportRollbackStart() {
        writer.println();
        writer.println(colorYellow(bold("Rolling back changes...")));
        writer.flush();
    }

    /**
     * Reports rollback progress.
     *
     * @param message Progress message
     */
    public void reportRollbackProgress(String message) {
        writer.println("  " + message);
        writer.flush();
    }

    /**
     * Reports rollback completion.
     *
     * @param success Whether rollback was successful
     */
    public void reportRollbackComplete(boolean success) {
        if (success) {
            writer.println(colorGreen("  " + Constants.SYMBOL_CHECK + " Rollback completed successfully"));
        } else {
            writer.println(colorRed("  " + Constants.SYMBOL_CROSS + " Rollback completed with errors"));
        }
        writer.flush();
    }

    private String formatPhaseHeader(int phase, int total, String description) {
        String header = String.format("[%d/%d] %s", phase, total, description);
        return bold(header);
    }

    private String formatSubTask(boolean success, String description) {
        String symbol = success ? Constants.SYMBOL_CHECK : Constants.SYMBOL_CROSS;
        String colored = success ? colorGreen(symbol) : colorRed(symbol);
        return "  " + colored + " " + description;
    }

    private String formatPhaseComplete() {
        return colorGreen("  " + Constants.SYMBOL_ARROW + " Phase complete (" + subTaskCount + " tasks)");
    }

    // Color helper methods

    private String bold(String text) {
        return useColors ? Constants.ANSI_BOLD + text + Constants.ANSI_RESET : text;
    }

    private String colorRed(String text) {
        return useColors ? Constants.ANSI_RED + text + Constants.ANSI_RESET : text;
    }

    private String colorGreen(String text) {
        return useColors ? Constants.ANSI_GREEN + text + Constants.ANSI_RESET : text;
    }

    private String colorYellow(String text) {
        return useColors ? Constants.ANSI_YELLOW + text + Constants.ANSI_RESET : text;
    }

    /**
     * Gets the current phase number.
     */
    public int getCurrentPhase() {
        return currentPhase;
    }

    /**
     * Gets the current phase description.
     */
    public String getCurrentPhaseDescription() {
        return currentPhaseDescription;
    }

    /**
     * Gets the sub-task count for the current phase.
     */
    public int getSubTaskCount() {
        return subTaskCount;
    }
}
