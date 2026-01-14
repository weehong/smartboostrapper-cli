package com.smartbootstrapper.manifest;

import com.smartbootstrapper.exception.ManifestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses COMMIT.md files to extract commit messages by sequence number.
 *
 * Expected COMMIT.md format:
 * <pre>
 * 1. d3d5969bd815e3b89a66182d7901719a5276e4a5
 *
 * chore(init): bootstrap Spring Boot project
 * - add git configuration (.gitattributes, .gitignore)
 * - add maven wrapper (mvnw, mvnw.cmd) with version 3.3.4
 *
 * ---
 * 2. ef5eb38b3d3385110a8f6802cfed07324699b071
 *
 * ci: add gitlab pipeline and deployment configuration
 * ...
 * </pre>
 */
public class CommitMessageParser {

    private static final Logger logger = LoggerFactory.getLogger(CommitMessageParser.class);

    /**
     * Pattern to match the sequence number line: "1. d3d5969bd815e3b89a66182d7901719a5276e4a5"
     */
    private static final Pattern SEQUENCE_LINE_PATTERN = Pattern.compile("^(\\d+)\\.\\s+([a-fA-F0-9]{7,40})\\s*$");

    /**
     * Delimiter between commit entries.
     */
    private static final String ENTRY_DELIMITER = "---";

    private final Map<Integer, String> commitMessages;
    private final Path sourcePath;

    private CommitMessageParser(Map<Integer, String> commitMessages, Path sourcePath) {
        this.commitMessages = commitMessages;
        this.sourcePath = sourcePath;
    }

    /**
     * Parses a COMMIT.md file and returns a CommitMessageParser instance.
     *
     * @param commitMdPath Path to the COMMIT.md file
     * @return CommitMessageParser instance with parsed commit messages
     * @throws ManifestException if parsing fails
     */
    public static CommitMessageParser parse(Path commitMdPath) {
        logger.debug("Parsing COMMIT.md file: {}", commitMdPath);

        if (!Files.exists(commitMdPath)) {
            throw new ManifestException(
                    "COMMIT.md file not found: " + commitMdPath,
                    commitMdPath.toString()
            );
        }

        try {
            String content = Files.readString(commitMdPath);
            Map<Integer, String> messages = parseContent(content);

            logger.info("Successfully parsed COMMIT.md with {} commit messages", messages.size());
            return new CommitMessageParser(messages, commitMdPath);

        } catch (IOException e) {
            throw new ManifestException(
                    "Failed to read COMMIT.md file: " + e.getMessage(),
                    commitMdPath.toString(),
                    e
            );
        }
    }

    /**
     * Parses the content of a COMMIT.md file.
     *
     * @param content The file content
     * @return Map of sequence number to commit message
     */
    static Map<Integer, String> parseContent(String content) {
        Map<Integer, String> messages = new HashMap<>();

        // Split by entry delimiter
        String[] sections = content.split("\\n" + ENTRY_DELIMITER + "\\s*\\n?");

        for (String section : sections) {
            parseSection(section.trim(), messages);
        }

        return messages;
    }

    /**
     * Parses a single section of COMMIT.md.
     *
     * @param section  The section content
     * @param messages Map to add parsed message to
     */
    private static void parseSection(String section, Map<Integer, String> messages) {
        if (section.isBlank()) {
            return;
        }

        String[] lines = section.split("\\n");
        if (lines.length == 0) {
            return;
        }

        // First non-empty line should be the sequence line
        String firstLine = null;
        int messageStartIndex = 0;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (!line.isEmpty()) {
                firstLine = line;
                messageStartIndex = i + 1;
                break;
            }
        }

        if (firstLine == null) {
            return;
        }

        // Parse the sequence line
        Matcher matcher = SEQUENCE_LINE_PATTERN.matcher(firstLine);
        if (!matcher.matches()) {
            logger.debug("Skipping section - first line doesn't match sequence pattern: {}", firstLine);
            return;
        }

        int sequenceNumber = Integer.parseInt(matcher.group(1));

        // Extract the commit message (everything after the sequence line)
        StringBuilder messageBuilder = new StringBuilder();
        boolean foundMessage = false;

        for (int i = messageStartIndex; i < lines.length; i++) {
            String line = lines[i];

            // Skip initial empty lines
            if (!foundMessage && line.trim().isEmpty()) {
                continue;
            }

            foundMessage = true;
            if (messageBuilder.length() > 0) {
                messageBuilder.append("\n");
            }
            messageBuilder.append(line);
        }

        String message = messageBuilder.toString().trim();
        if (!message.isEmpty()) {
            messages.put(sequenceNumber, message);
            logger.debug("Parsed commit message for sequence {}: {}", sequenceNumber,
                    message.length() > 50 ? message.substring(0, 50) + "..." : message);
        }
    }

    /**
     * Gets the commit message for a given sequence number.
     *
     * @param sequenceNumber The sequence number (1-based)
     * @return Optional containing the commit message, or empty if not found
     */
    public Optional<String> getMessage(int sequenceNumber) {
        return Optional.ofNullable(commitMessages.get(sequenceNumber));
    }

    /**
     * Gets the commit message for a given sequence number, throwing if not found.
     *
     * @param sequenceNumber The sequence number (1-based)
     * @return The commit message
     * @throws ManifestException if the sequence number is not found
     */
    public String getMessageOrThrow(int sequenceNumber) {
        return getMessage(sequenceNumber).orElseThrow(() ->
                new ManifestException(
                        "No commit message found for sequence " + sequenceNumber + " in COMMIT.md",
                        sourcePath.toString()
                )
        );
    }

    /**
     * Returns the number of commit messages parsed.
     */
    public int size() {
        return commitMessages.size();
    }

    /**
     * Checks if a commit message exists for the given sequence number.
     */
    public boolean hasMessage(int sequenceNumber) {
        return commitMessages.containsKey(sequenceNumber);
    }

    /**
     * Returns all parsed commit messages.
     */
    public Map<Integer, String> getAllMessages() {
        return Map.copyOf(commitMessages);
    }
}
