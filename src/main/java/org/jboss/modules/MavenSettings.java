package org.jboss.modules;

import java.io.File;
import java.nio.file.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Tomaz Cerar (c) 2014 Red Hat Inc.
 */
final class MavenSettings {
    private Path localRepository;
    private final List<String> remoteRepositories = new LinkedList<>();
    private final Map<String, Profile> profiles = new HashMap<>();
    private final List<String> activeProfileNames = new LinkedList<>();

    MavenSettings() {
        configureDefaults();
    }

    void configureDefaults() {
        String localRepositoryPath = System.getProperty("local.maven.repo.path");
        if (localRepositoryPath != null) {
            System.out.println("Please use 'maven.repo.local' instead of 'local.maven.repo.path'");
            localRepository = java.nio.file.Paths.get(localRepositoryPath.split(File.pathSeparator)[0]);
        }

        localRepositoryPath = System.getProperty("maven.repo.local");
        if (localRepositoryPath != null) {
            localRepository = java.nio.file.Paths.get(localRepositoryPath);
        }
        String remoteRepository = System.getProperty("remote.maven.repo");
        if (remoteRepository != null) {
            if (!remoteRepository.endsWith("/")) {
                remoteRepository += "/";
            }
            remoteRepositories.add(remoteRepository);
        }
    }

    public void setLocalRepository(Path localRepository) {
        this.localRepository = localRepository;
    }

    public Path getLocalRepository() {
        return localRepository;
    }

    public List<String> getRemoteRepositories() {
        return remoteRepositories;
    }

    public void addProfile(Profile profile) {
        this.profiles.put(profile.getId(), profile);
    }

    public void addActiveProfile(String profileName) {
        activeProfileNames.add(profileName);
    }

    void resolveActiveSettings() {
        for (String name : activeProfileNames) {
            Profile p = profiles.get(name);
            if (p != null) {
                remoteRepositories.addAll(p.getRepositories());
            }
        }
    }


    static final class Profile {
        private String id;
        final List<String> repositories = new LinkedList<>();

        Profile() {

        }

        public void setId(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public void addRepository(String url) {
            repositories.add(url);
        }

        public List<String> getRepositories() {
            return repositories;
        }
    }
}
