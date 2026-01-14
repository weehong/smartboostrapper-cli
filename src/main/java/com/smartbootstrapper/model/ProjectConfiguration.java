package com.smartbootstrapper.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Holds all user-provided configuration for the new Spring Boot project.
 */
public class ProjectConfiguration {

    private final String groupId;
    private final String artifactId;
    private final String projectName;
    private final String version;
    private final String springBootVersion;
    private final String javaVersion;
    private final List<String> dependencies;
    private final String oldPackage;
    private final String newPackage;
    private final String targetDirectory;

    private ProjectConfiguration(Builder builder) {
        this.groupId = Objects.requireNonNull(builder.groupId, "groupId must not be null");
        this.artifactId = Objects.requireNonNull(builder.artifactId, "artifactId must not be null");
        this.projectName = Objects.requireNonNull(builder.projectName, "projectName must not be null");
        this.version = Objects.requireNonNull(builder.version, "version must not be null");
        this.springBootVersion = Objects.requireNonNull(builder.springBootVersion, "springBootVersion must not be null");
        this.javaVersion = Objects.requireNonNull(builder.javaVersion, "javaVersion must not be null");
        this.dependencies = Collections.unmodifiableList(new ArrayList<>(builder.dependencies));
        this.oldPackage = Objects.requireNonNull(builder.oldPackage, "oldPackage must not be null");
        this.newPackage = Objects.requireNonNull(builder.newPackage, "newPackage must not be null");
        this.targetDirectory = Objects.requireNonNull(builder.targetDirectory, "targetDirectory must not be null");
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getVersion() {
        return version;
    }

    public String getSpringBootVersion() {
        return springBootVersion;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public String getOldPackage() {
        return oldPackage;
    }

    public String getNewPackage() {
        return newPackage;
    }

    public String getTargetDirectory() {
        return targetDirectory;
    }

    /**
     * Derives the new package from groupId and artifactId if not explicitly set.
     */
    public String getDerivedPackage() {
        return groupId + "." + artifactId.replace("-", "");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String groupId;
        private String artifactId;
        private String projectName;
        private String version = "0.0.1-SNAPSHOT";
        private String springBootVersion;
        private String javaVersion = "21";
        private List<String> dependencies = new ArrayList<>();
        private String oldPackage;
        private String newPackage;
        private String targetDirectory;

        public Builder groupId(String groupId) {
            this.groupId = groupId;
            return this;
        }

        public Builder artifactId(String artifactId) {
            this.artifactId = artifactId;
            return this;
        }

        public Builder projectName(String projectName) {
            this.projectName = projectName;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder springBootVersion(String springBootVersion) {
            this.springBootVersion = springBootVersion;
            return this;
        }

        public Builder javaVersion(String javaVersion) {
            this.javaVersion = javaVersion;
            return this;
        }

        public Builder dependencies(List<String> dependencies) {
            this.dependencies = new ArrayList<>(dependencies);
            return this;
        }

        public Builder addDependency(String dependency) {
            this.dependencies.add(dependency);
            return this;
        }

        public Builder oldPackage(String oldPackage) {
            this.oldPackage = oldPackage;
            return this;
        }

        public Builder newPackage(String newPackage) {
            this.newPackage = newPackage;
            return this;
        }

        public Builder targetDirectory(String targetDirectory) {
            this.targetDirectory = targetDirectory;
            return this;
        }

        public ProjectConfiguration build() {
            return new ProjectConfiguration(this);
        }
    }

    @Override
    public String toString() {
        return String.format("ProjectConfiguration{groupId='%s', artifactId='%s', name='%s', version='%s', " +
                        "springBoot='%s', java='%s', dependencies=%s, oldPackage='%s', newPackage='%s', target='%s'}",
                groupId, artifactId, projectName, version, springBootVersion, javaVersion,
                dependencies, oldPackage, newPackage, targetDirectory);
    }
}
