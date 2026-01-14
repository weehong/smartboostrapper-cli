package com.smartbootstrapper.cli;

import com.smartbootstrapper.git.GitRepositoryService;
import com.smartbootstrapper.orchestrator.ErrorHandler;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * Subcommand to initialize a Git repository.
 */
@Command(
        name = "init",
        description = "Initialize a new Git repository"
)
public class InitCommand implements Callable<Integer> {

    @Parameters(
            index = "0",
            description = "Target directory for the Git repository (defaults to current directory)",
            defaultValue = ".",
            paramLabel = "<directory>"
    )
    private Path targetDirectory;

    @Option(
            names = {"--no-color"},
            description = "Disable colored output"
    )
    private boolean noColor;

    private final PrintWriter out;
    private final PrintWriter err;

    public InitCommand() {
        this(new PrintWriter(System.out, true), new PrintWriter(System.err, true));
    }

    public InitCommand(PrintWriter out, PrintWriter err) {
        this.out = out;
        this.err = err;
    }

    @Override
    public Integer call() {
        boolean useColors = !noColor && System.console() != null;
        ErrorHandler errorHandler = new ErrorHandler(useColors, false);

        try {
            Path absolutePath = targetDirectory.toAbsolutePath();

            // Create directory if it doesn't exist
            if (!Files.exists(absolutePath)) {
                Files.createDirectories(absolutePath);
                out.println("Created directory: " + absolutePath);
            }

            // Initialize git repository
            try (GitRepositoryService ignored = GitRepositoryService.init(absolutePath)) {
                out.println("Initialized empty Git repository in " + absolutePath.resolve(".git"));
            }

            return 0;

        } catch (Exception e) {
            err.println(errorHandler.formatError(e));
            return 1;
        }
    }
}
