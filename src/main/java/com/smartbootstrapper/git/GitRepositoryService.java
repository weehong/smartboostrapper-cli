package com.smartbootstrapper.git;

import com.smartbootstrapper.Constants;
import com.smartbootstrapper.exception.GitException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * Service for Git repository operations using JGit.
 */
public class GitRepositoryService implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(GitRepositoryService.class);

    private final Repository repository;
    private final String repositoryPath;

    private GitRepositoryService(Repository repository, String repositoryPath) {
        this.repository = repository;
        this.repositoryPath = repositoryPath;
    }

    /**
     * Opens a Git repository at the given path.
     *
     * @param repositoryPath Path to the Git repository (can be the .git directory or the working directory)
     * @return GitRepositoryService instance
     * @throws GitException if the repository cannot be opened
     */
    public static GitRepositoryService open(String repositoryPath) {
        return open(Path.of(repositoryPath));
    }

    /**
     * Opens a Git repository at the given path.
     *
     * @param repositoryPath Path to the Git repository
     * @return GitRepositoryService instance
     * @throws GitException if the repository cannot be opened
     */
    public static GitRepositoryService open(Path repositoryPath) {
        logger.debug("Opening Git repository at: {}", repositoryPath);

        File gitDir = repositoryPath.toFile();
        if (!gitDir.exists()) {
            throw new GitException(
                    String.format(Constants.ERROR_GIT_REPO_NOT_FOUND, repositoryPath),
                    repositoryPath.toString()
            );
        }

        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            Repository repository;

            // Check if path is a .git directory or a working directory
            if (gitDir.getName().equals(".git") && gitDir.isDirectory()) {
                repository = builder.setGitDir(gitDir)
                        .readEnvironment()
                        .build();
            } else {
                // Explicitly set the .git directory to avoid findGitDir() traversing up
                // parent directories and finding the wrong repository
                File explicitGitDir = new File(gitDir, ".git");
                if (!explicitGitDir.exists() || !explicitGitDir.isDirectory()) {
                    throw new GitException(
                            "No .git directory found in: " + repositoryPath,
                            repositoryPath.toString()
                    );
                }
                repository = builder.setGitDir(explicitGitDir)
                        .setWorkTree(gitDir)
                        .readEnvironment()
                        .build();
            }

            if (repository.getObjectDatabase() == null || !repository.getObjectDatabase().exists()) {
                repository.close();
                throw new GitException(
                        "Not a valid Git repository: " + repositoryPath,
                        repositoryPath.toString()
                );
            }

            logger.info("Successfully opened Git repository: {}", repository.getDirectory());
            return new GitRepositoryService(repository, repositoryPath.toString());

        } catch (IOException e) {
            throw new GitException(
                    "Failed to open Git repository: " + e.getMessage(),
                    repositoryPath.toString(),
                    e
            );
        }
    }

    /**
     * Initializes a new Git repository at the given path.
     *
     * @param targetDirectory Path where the Git repository should be initialized
     * @return GitRepositoryService instance for the newly created repository
     * @throws GitException if the repository cannot be initialized
     */
    public static GitRepositoryService init(Path targetDirectory) {
        logger.debug("Initializing Git repository at: {}", targetDirectory);

        try {
            Git git = Git.init()
                    .setDirectory(targetDirectory.toFile())
                    .call();

            Repository repository = git.getRepository();
            logger.info("Successfully initialized Git repository: {}", repository.getDirectory());
            return new GitRepositoryService(repository, targetDirectory.toString());

        } catch (GitAPIException e) {
            throw new GitException(
                    "Failed to initialize Git repository: " + e.getMessage(),
                    targetDirectory.toString(),
                    e
            );
        }
    }

    /**
     * Stages all files in the working directory.
     *
     * @throws GitException if staging fails
     */
    public void addAll() {
        logger.debug("Staging all files in repository: {}", repositoryPath);

        try (Git git = new Git(repository)) {
            git.add()
                    .addFilepattern(".")
                    .call();
            logger.info("Successfully staged all files");
        } catch (GitAPIException e) {
            throw new GitException(
                    "Failed to stage files: " + e.getMessage(),
                    repositoryPath,
                    e
            );
        }
    }

    /**
     * Creates a commit with the given message.
     *
     * @param message The commit message
     * @return The commit hash of the created commit
     * @throws GitException if the commit fails
     */
    public String commit(String message) {
        logger.debug("Creating commit in repository: {}", repositoryPath);

        try (Git git = new Git(repository)) {
            RevCommit commit = git.commit()
                    .setMessage(message)
                    .call();
            String commitHash = commit.getId().getName();
            logger.info("Successfully created commit: {}", commitHash);
            return commitHash;
        } catch (GitAPIException e) {
            throw new GitException(
                    "Failed to create commit: " + e.getMessage(),
                    repositoryPath,
                    e
            );
        }
    }

    /**
     * Checks if a commit exists in the repository.
     *
     * @param commitHash The commit hash to check (full or abbreviated)
     * @return true if the commit exists, false otherwise
     */
    public boolean commitExists(String commitHash) {
        try {
            ObjectId objectId = repository.resolve(commitHash);
            if (objectId == null) {
                return false;
            }

            try (RevWalk revWalk = new RevWalk(repository)) {
                revWalk.parseCommit(objectId);
                return true;
            }
        } catch (Exception e) {
            logger.debug("Commit not found or not a commit: {}", commitHash);
            return false;
        }
    }

    /**
     * Checks if a file exists at a specific commit.
     *
     * @param commitHash The commit hash
     * @param filePath   The file path relative to repository root
     * @return true if the file exists at the commit, false otherwise
     */
    public boolean fileExistsAtCommit(String commitHash, String filePath) {
        try {
            ObjectId objectId = repository.resolve(commitHash);
            if (objectId == null) {
                return false;
            }

            try (RevWalk revWalk = new RevWalk(repository)) {
                RevCommit commit = revWalk.parseCommit(objectId);
                RevTree tree = commit.getTree();

                try (TreeWalk treeWalk = new TreeWalk(repository)) {
                    treeWalk.addTree(tree);
                    treeWalk.setRecursive(true);
                    treeWalk.setFilter(PathFilter.create(filePath));
                    return treeWalk.next();
                }
            }
        } catch (Exception e) {
            logger.debug("Error checking file existence: {} at {}", filePath, commitHash, e);
            return false;
        }
    }

    /**
     * Retrieves file content as a string from a specific commit.
     *
     * @param commitHash The commit hash
     * @param filePath   The file path relative to repository root
     * @return The file content as a string
     * @throws GitException if the commit or file is not found
     */
    public String getFileContent(String commitHash, String filePath) {
        byte[] bytes = getFileBytes(commitHash, filePath);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Retrieves file content as bytes from a specific commit.
     *
     * @param commitHash The commit hash
     * @param filePath   The file path relative to repository root
     * @return The file content as bytes
     * @throws GitException if the commit or file is not found
     */
    public byte[] getFileBytes(String commitHash, String filePath) {
        logger.debug("Getting file {} at commit {}", filePath, commitHash);

        try {
            ObjectId objectId = repository.resolve(commitHash);
            if (objectId == null) {
                throw new GitException(
                        String.format(Constants.ERROR_GIT_COMMIT_NOT_FOUND, commitHash),
                        repositoryPath,
                        commitHash,
                        filePath
                );
            }

            try (RevWalk revWalk = new RevWalk(repository)) {
                RevCommit commit = revWalk.parseCommit(objectId);
                RevTree tree = commit.getTree();

                try (TreeWalk treeWalk = new TreeWalk(repository)) {
                    treeWalk.addTree(tree);
                    treeWalk.setRecursive(true);
                    treeWalk.setFilter(PathFilter.create(filePath));

                    if (!treeWalk.next()) {
                        throw new GitException(
                                String.format(Constants.ERROR_GIT_FILE_NOT_FOUND,
                                        commitHash.substring(0, Math.min(7, commitHash.length())), filePath),
                                repositoryPath,
                                commitHash,
                                filePath
                        );
                    }

                    ObjectId blobId = treeWalk.getObjectId(0);
                    ObjectLoader loader = repository.open(blobId);

                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    loader.copyTo(outputStream);
                    return outputStream.toByteArray();
                }
            }
        } catch (GitException e) {
            throw e;
        } catch (IOException e) {
            throw new GitException(
                    "Failed to read file from Git: " + e.getMessage(),
                    repositoryPath,
                    commitHash,
                    filePath,
                    e
            );
        }
    }

    /**
     * Gets the full commit hash from an abbreviated hash.
     *
     * @param abbreviatedHash The abbreviated commit hash
     * @return The full commit hash
     * @throws GitException if the commit is not found
     */
    public String resolveCommitHash(String abbreviatedHash) {
        try {
            ObjectId objectId = repository.resolve(abbreviatedHash);
            if (objectId == null) {
                throw new GitException(
                        String.format(Constants.ERROR_GIT_COMMIT_NOT_FOUND, abbreviatedHash),
                        repositoryPath
                );
            }
            return objectId.getName();
        } catch (IOException e) {
            throw new GitException(
                    "Failed to resolve commit hash: " + e.getMessage(),
                    repositoryPath,
                    e
            );
        }
    }

    /**
     * Returns the repository path.
     */
    public String getRepositoryPath() {
        return repositoryPath;
    }

    /**
     * Returns the underlying JGit repository.
     * Use with caution.
     */
    public Repository getRepository() {
        return repository;
    }

    @Override
    public void close() {
        if (repository != null) {
            repository.close();
            logger.debug("Closed Git repository: {}", repositoryPath);
        }
    }
}
