package com.smartbootstrapper.cli;

import com.smartbootstrapper.Constants;
import com.smartbootstrapper.model.ProjectConfiguration;
import com.smartbootstrapper.validation.InputValidator;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Attributes;
import org.jline.utils.InfoCmp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Interactive prompter for collecting user configuration.
 * Uses JLine3 for rich terminal interactions.
 */
public class InteractivePrompter {

    private static final Logger logger = LoggerFactory.getLogger(InteractivePrompter.class);

    private final Terminal terminal;
    private final LineReader reader;
    private final PrintWriter writer;
    private final InputValidator inputValidator;
    private final boolean useColors;
    private final boolean isDumbTerminal;

    public InteractivePrompter() throws IOException {
        this(true);
    }

    public InteractivePrompter(boolean useColors) throws IOException {
        this.terminal = TerminalBuilder.builder()
                .system(true)
                .build();
        this.isDumbTerminal = terminal.getType().equals(Terminal.TYPE_DUMB)
                || terminal.getType().equals(Terminal.TYPE_DUMB_COLOR);
        this.reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();
        this.writer = terminal.writer();
        this.inputValidator = new InputValidator();
        this.useColors = useColors;

        if (isDumbTerminal) {
            logger.debug("Running with dumb terminal, using fallback I/O for confirmations");
        }
    }

    /**
     * Prompts for all configuration values and returns a ProjectConfiguration.
     *
     * @param targetDirectory The target directory for the project
     * @return Completed ProjectConfiguration
     */
    public ProjectConfiguration promptForConfiguration(String targetDirectory) {
        return promptForConfiguration(targetDirectory, null);
    }

    /**
     * Prompts for all configuration values with optional pre-populated defaults from an existing project.
     *
     * @param targetDirectory The target directory for the project
     * @param configReader    Optional reader for pre-populating defaults from an existing project
     * @return Completed ProjectConfiguration
     */
    public ProjectConfiguration promptForConfiguration(String targetDirectory, ProjectConfigurationReader configReader) {
        printHeader("Project Configuration");

        if (configReader != null) {
            writer.println();
            writer.println(colorCyan("  Pre-populating defaults from: " + configReader.getProjectPath()));
        }
        writer.println();

        // Get defaults from config reader if available
        String defaultGroupId = configReader != null ? configReader.getGroupId().orElse(null) : null;
        String defaultArtifactId = configReader != null ? configReader.getArtifactId().orElse(null) : null;
        String defaultProjectName = configReader != null ? configReader.getProjectName().orElse(null) : null;
        String defaultVersion = configReader != null ? configReader.getVersion().orElse(null) : null;
        String defaultSpringBootVersion = configReader != null ? configReader.getSpringBootVersion().orElse(null) : null;
        String defaultJavaVersion = configReader != null ? configReader.getJavaVersion().orElse(null) : null;
        String defaultOldPackage = configReader != null ? configReader.inferBasePackage().orElse(null) : null;

        String groupId = promptGroupId(defaultGroupId);
        String artifactId = promptArtifactId(defaultArtifactId);
        String projectName = promptProjectName(artifactId, defaultProjectName);
        String version = promptVersion(defaultVersion);
        String springBootVersion = promptSpringBootVersion(defaultSpringBootVersion);
        String javaVersion = promptJavaVersion(defaultJavaVersion);
        List<String> dependencies = promptDependencies();
        String oldPackage = promptOldPackage(defaultOldPackage);
        String newPackage = promptNewPackage(groupId, artifactId);

        return ProjectConfiguration.builder()
                .groupId(groupId)
                .artifactId(artifactId)
                .projectName(projectName)
                .version(version)
                .springBootVersion(springBootVersion)
                .javaVersion(javaVersion)
                .dependencies(dependencies)
                .oldPackage(oldPackage)
                .newPackage(newPackage)
                .targetDirectory(targetDirectory)
                .build();
    }

    /**
     * Prompts for Group ID with validation.
     */
    public String promptGroupId() {
        return promptGroupId(null);
    }

    /**
     * Prompts for Group ID with validation and optional default.
     */
    public String promptGroupId(String defaultValue) {
        return promptWithValidation(
                "Group ID",
                "e.g., com.example, org.mycompany",
                input -> inputValidator.validateGroupId(input),
                defaultValue
        );
    }

    /**
     * Prompts for Artifact ID with validation.
     */
    public String promptArtifactId() {
        return promptArtifactId(null);
    }

    /**
     * Prompts for Artifact ID with validation and optional default.
     */
    public String promptArtifactId(String defaultValue) {
        return promptWithValidation(
                "Artifact ID",
                "e.g., my-app, user-service",
                input -> inputValidator.validateArtifactId(input),
                defaultValue
        );
    }

    /**
     * Prompts for Project Name.
     */
    public String promptProjectName(String artifactId) {
        return promptProjectName(artifactId, null);
    }

    /**
     * Prompts for Project Name with optional default.
     */
    public String promptProjectName(String artifactId, String defaultValue) {
        String defaultName = defaultValue != null ? defaultValue : formatArtifactAsName(artifactId);
        return promptWithValidation(
                "Project Name",
                "e.g., My Application",
                input -> inputValidator.validateProjectName(input),
                defaultName
        );
    }

    /**
     * Prompts for Version.
     */
    public String promptVersion() {
        return promptVersion(null);
    }

    /**
     * Prompts for Version with optional default.
     */
    public String promptVersion(String defaultValue) {
        return promptWithValidation(
                "Version",
                "e.g., 1.0.0, 0.0.1-SNAPSHOT",
                input -> inputValidator.validateVersion(input),
                defaultValue != null ? defaultValue : Constants.DEFAULT_PROJECT_VERSION
        );
    }

    /**
     * Prompts for Spring Boot Version.
     */
    public String promptSpringBootVersion() {
        return promptSpringBootVersion(null);
    }

    /**
     * Prompts for Spring Boot Version with optional default.
     */
    public String promptSpringBootVersion(String defaultValue) {
        LineReader versionReader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(new StringsCompleter(Constants.SUPPORTED_SPRING_BOOT_VERSIONS))
                .build();

        return promptWithValidation(
                "Spring Boot Version",
                "Supported: " + String.join(", ", Constants.SUPPORTED_SPRING_BOOT_VERSIONS),
                input -> inputValidator.validateSpringBootVersion(input),
                defaultValue != null ? defaultValue : Constants.DEFAULT_SPRING_BOOT_VERSION,
                versionReader
        );
    }

    /**
     * Prompts for Java Version.
     */
    public String promptJavaVersion() {
        return promptJavaVersion(null);
    }

    /**
     * Prompts for Java Version with optional default.
     */
    public String promptJavaVersion(String defaultValue) {
        LineReader versionReader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(new StringsCompleter(Constants.SUPPORTED_JAVA_VERSIONS))
                .build();

        return promptWithValidation(
                "Java Version",
                "Supported: " + String.join(", ", Constants.SUPPORTED_JAVA_VERSIONS),
                input -> inputValidator.validateJavaVersion(input),
                defaultValue != null ? defaultValue : Constants.DEFAULT_JAVA_VERSION,
                versionReader
        );
    }

    /**
     * Prompts for Dependencies selection using interactive multi-select.
     * Use arrow keys to navigate, space to select/deselect, Enter to confirm.
     */
    public List<String> promptDependencies() {
        writer.println();
        printHeader("Dependencies Selection");
        writer.println();
        writer.println(colorCyan("  Use " + colorBold("↑/↓") + colorCyan(" to navigate, ") +
                colorBold("Space") + colorCyan(" to select, ") +
                colorBold("Enter") + colorCyan(" to confirm")));
        writer.println();

        // Build flat list of items (categories as headers + dependencies)
        List<DependencyItem> items = new ArrayList<>();
        for (var entry : Constants.DEPENDENCY_CATEGORIES.entrySet()) {
            items.add(new DependencyItem(entry.getKey(), true)); // Category header
            for (String dep : entry.getValue()) {
                items.add(new DependencyItem(dep, false)); // Dependency item
            }
        }

        Set<String> selectedDependencies = new HashSet<>();
        int cursorPosition = 1; // Start at first dependency, not category header
        int viewportStart = 0;
        int viewportHeight = Math.min(20, items.size()); // Show up to 20 items at a time

        // Save terminal attributes and enter raw mode
        Attributes savedAttributes = terminal.enterRawMode();

        // Create key bindings
        KeyMap<String> keyMap = new KeyMap<>();
        // Bind arrow keys using multiple escape sequences for compatibility
        keyMap.bind("up", "\033[A");    // Standard escape sequence
        keyMap.bind("up", "\033OA");    // Alternative escape sequence
        keyMap.bind("down", "\033[B");  // Standard escape sequence
        keyMap.bind("down", "\033OB");  // Alternative escape sequence
        // Also try terminal capabilities as fallback
        String keyUp = KeyMap.key(terminal, InfoCmp.Capability.key_up);
        String keyDown = KeyMap.key(terminal, InfoCmp.Capability.key_down);
        if (keyUp != null) keyMap.bind("up", keyUp);
        if (keyDown != null) keyMap.bind("down", keyDown);
        keyMap.bind("space", " ");
        keyMap.bind("enter", "\r");
        keyMap.bind("enter", "\n");
        keyMap.bind("quit", "q");
        keyMap.bind("quit", "Q");

        BindingReader bindingReader = new BindingReader(terminal.reader());

        try {
            boolean done = false;
            while (!done) {
                // Clear screen and redraw
                clearScreen();
                printHeader("Dependencies Selection");
                writer.println();
                writer.println(colorCyan("  ↑/↓ Navigate  |  Space Select  |  Enter Confirm  |  q Quit"));
                writer.println(colorYellow("  Selected: " + selectedDependencies.size() + " dependencies"));
                writer.println();

                // Adjust viewport to keep cursor visible
                if (cursorPosition < viewportStart) {
                    viewportStart = cursorPosition;
                } else if (cursorPosition >= viewportStart + viewportHeight) {
                    viewportStart = cursorPosition - viewportHeight + 1;
                }

                // Display items within viewport
                int viewportEnd = Math.min(viewportStart + viewportHeight, items.size());
                for (int i = viewportStart; i < viewportEnd; i++) {
                    DependencyItem item = items.get(i);
                    boolean isCursor = (i == cursorPosition);

                    if (item.isCategory) {
                        // Category header
                        String line = (isCursor ? colorCyan(Constants.SYMBOL_POINTER + " ") : "  ") +
                                colorBold(colorYellow(item.name));
                        writer.println(line);
                    } else {
                        // Dependency item
                        boolean isSelected = selectedDependencies.contains(item.name);
                        String radio = isSelected ?
                                colorGreen(Constants.SYMBOL_RADIO_SELECTED) :
                                Constants.SYMBOL_RADIO_EMPTY;
                        String pointer = isCursor ? colorCyan(Constants.SYMBOL_POINTER + " ") : "  ";
                        String depName = isCursor ? colorCyan(item.name) : item.name;
                        if (isSelected) {
                            depName = colorGreen(item.name);
                        }
                        writer.println("    " + pointer + radio + " " + depName);
                    }
                }

                // Show scroll indicators
                writer.println();
                if (viewportStart > 0) {
                    writer.println(colorCyan("  ↑ More above..."));
                }
                if (viewportEnd < items.size()) {
                    writer.println(colorCyan("  ↓ More below... (" + (items.size() - viewportEnd) + " items)"));
                }

                writer.flush();

                // Read key input
                String binding = bindingReader.readBinding(keyMap);
                if (binding == null) {
                    continue;
                }

                switch (binding) {
                    case "up":
                        cursorPosition = Math.max(0, cursorPosition - 1);
                        // Skip category headers when moving up
                        while (cursorPosition > 0 && items.get(cursorPosition).isCategory) {
                            cursorPosition--;
                        }
                        if (cursorPosition == 0 && items.get(0).isCategory && items.size() > 1) {
                            cursorPosition = 1;
                        }
                        break;
                    case "down":
                        cursorPosition = Math.min(items.size() - 1, cursorPosition + 1);
                        // Skip category headers when moving down
                        while (cursorPosition < items.size() - 1 && items.get(cursorPosition).isCategory) {
                            cursorPosition++;
                        }
                        break;
                    case "space":
                        DependencyItem currentItem = items.get(cursorPosition);
                        if (!currentItem.isCategory) {
                            if (selectedDependencies.contains(currentItem.name)) {
                                selectedDependencies.remove(currentItem.name);
                            } else {
                                selectedDependencies.add(currentItem.name);
                            }
                        }
                        break;
                    case "enter":
                        done = true;
                        break;
                    case "quit":
                        done = true;
                        break;
                }
            }
        } finally {
            // Restore terminal attributes
            terminal.setAttributes(savedAttributes);
        }

        // Clear and show final result
        clearScreen();
        writer.println();
        if (selectedDependencies.isEmpty()) {
            printValidation(true, "No dependencies selected");
        } else {
            List<String> sortedDeps = new ArrayList<>(selectedDependencies);
            sortedDeps.sort(String::compareTo);
            printValidation(true, "Selected " + sortedDeps.size() + " dependencies: " + String.join(", ", sortedDeps));
        }

        return new ArrayList<>(selectedDependencies);
    }

    /**
     * Clears the terminal screen.
     */
    private void clearScreen() {
        writer.print("\033[H\033[2J");
        writer.flush();
    }

    /**
     * Helper class for dependency items in the multi-select list.
     */
    private static class DependencyItem {
        final String name;
        final boolean isCategory;

        DependencyItem(String name, boolean isCategory) {
            this.name = name;
            this.isCategory = isCategory;
        }
    }

    /**
     * Prompts for the old package name (to refactor from).
     */
    public String promptOldPackage() {
        return promptOldPackage(null);
    }

    /**
     * Prompts for the old package name (to refactor from) with optional default.
     */
    public String promptOldPackage(String defaultValue) {
        return promptWithValidation(
                "Old Package Name (to refactor from)",
                "e.g., com.old.package",
                input -> inputValidator.validatePackageName(input),
                defaultValue
        );
    }

    /**
     * Prompts for the new package name (to refactor to).
     */
    public String promptNewPackage(String groupId, String artifactId) {
        String suggested = groupId + "." + artifactId.replace("-", "");
        return promptWithValidation(
                "New Package Name (to refactor to)",
                "e.g., " + suggested,
                input -> inputValidator.validatePackageName(input),
                suggested
        );
    }

    /**
     * Prompts for confirmation.
     *
     * @param message The confirmation message
     * @return true if user confirms, false otherwise
     */
    public boolean promptConfirmation(String message) {
        writer.println();
        String prompt = message.isEmpty() ? " (y/n): " : message + " (y/n): ";
        writer.print(colorBold(prompt));
        writer.flush();

        String response;
        if (isDumbTerminal) {
            // Use standard Java I/O for dumb terminals
            try {
                BufferedReader stdinReader = new BufferedReader(new InputStreamReader(System.in));
                response = stdinReader.readLine();
                if (response == null) {
                    return false;
                }
                response = response.trim().toLowerCase();
            } catch (IOException e) {
                logger.warn("Failed to read confirmation input: {}", e.getMessage());
                return false;
            }
        } else {
            response = reader.readLine("").trim().toLowerCase();
        }
        return response.equals("y") || response.equals("yes");
    }

    private String promptWithValidation(String label, String hint,
                                         java.util.function.Function<String, InputValidator.ValidationResult> validator,
                                         String defaultValue) {
        return promptWithValidation(label, hint, validator, defaultValue, reader);
    }

    private String promptWithValidation(String label, String hint,
                                         java.util.function.Function<String, InputValidator.ValidationResult> validator,
                                         String defaultValue, LineReader lineReader) {
        writer.println();
        printLabel(label);
        if (hint != null && !hint.isEmpty()) {
            writer.println(colorCyan("  " + hint));
        }

        while (true) {
            String prompt = "  > ";
            if (defaultValue != null) {
                prompt = "  [" + defaultValue + "] > ";
            }

            String input = lineReader.readLine(colorBold(prompt)).trim();

            if (input.isEmpty() && defaultValue != null) {
                input = defaultValue;
            }

            InputValidator.ValidationResult result = validator.apply(input);

            if (result.valid()) {
                printValidation(true, input);
                return input;
            } else {
                printValidation(false, result.message());
                if (result.suggestion() != null) {
                    writer.println(colorYellow("  " + Constants.SYMBOL_ARROW + " " + result.suggestion()));
                }
            }
        }
    }

    private void printHeader(String text) {
        writer.println();
        writer.println(colorBold("=== " + text + " ==="));
    }

    private void printLabel(String label) {
        writer.println(colorBold("  " + label + ":"));
    }

    private void printValidation(boolean success, String message) {
        String symbol = success ? Constants.SYMBOL_CHECK : Constants.SYMBOL_CROSS;
        String color = success ? colorGreen(symbol) : colorRed(symbol);
        writer.println("  " + color + " " + message);
    }

    private String formatArtifactAsName(String artifactId) {
        if (artifactId == null || artifactId.isEmpty()) {
            return "My Application";
        }

        StringBuilder sb = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : artifactId.toCharArray()) {
            if (c == '-' || c == '_') {
                sb.append(' ');
                capitalizeNext = true;
            } else if (capitalizeNext) {
                sb.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    // Color helper methods

    private String colorBold(String text) {
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

    private String colorCyan(String text) {
        return useColors ? Constants.ANSI_CYAN + text + Constants.ANSI_RESET : text;
    }

    /**
     * Closes the terminal.
     */
    public void close() {
        try {
            terminal.close();
        } catch (IOException e) {
            logger.warn("Failed to close terminal: {}", e.getMessage());
        }
    }
}
