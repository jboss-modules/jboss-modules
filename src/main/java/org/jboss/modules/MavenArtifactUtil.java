/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.modules;

import static org.jboss.modules.xml.ModuleXmlParser.endOfDocument;
import static org.jboss.modules.xml.ModuleXmlParser.unexpectedContent;
import static org.jboss.modules.xml.XmlPullParser.END_DOCUMENT;
import static org.jboss.modules.xml.XmlPullParser.END_TAG;
import static org.jboss.modules.xml.XmlPullParser.FEATURE_PROCESS_NAMESPACES;
import static org.jboss.modules.xml.XmlPullParser.START_TAG;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.modules.xml.MXParser;
import org.jboss.modules.xml.XmlPullParser;
import org.jboss.modules.xml.XmlPullParserException;

/**
 * Helper class to resolve a maven artifact.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author <a href="mailto:tcerar@redhat.com">Tomaz Cerar</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class MavenArtifactUtil {

    private static MavenSettings mavenSettings;
    private static final Object settingLoaderMutex = new Object();
    private static final Pattern snapshotPattern = Pattern.compile("-\\d{8}\\.\\d+-\\d+$");

    static MavenSettings getSettings() throws IOException {
        if (mavenSettings != null) {
            return mavenSettings;
        }
        synchronized (settingLoaderMutex) {
            MavenSettings settings = new MavenSettings();

            Path m2 = java.nio.file.Paths.get(System.getProperty("user.home"), ".m2");
            Path settingsPath = m2.resolve("settings.xml");

            if (Files.notExists(settingsPath)) {
                String mavenHome = System.getenv("M2_HOME");
                if (mavenHome != null) {
                    settingsPath = java.nio.file.Paths.get(mavenHome, "conf", "settings.xml");
                }
            }
            if (Files.exists(settingsPath)) {
                parseSettingsXml(settingsPath, settings);
            }
            if (settings.getLocalRepository() == null) {
                Path repository = m2.resolve("repository");
                settings.setLocalRepository(repository);
            }
            settings.resolveActiveSettings();
            mavenSettings = settings;
            return mavenSettings;
        }
    }

    static MavenSettings parseSettingsXml(Path settings, MavenSettings mavenSettings) throws IOException {
        try {
            final MXParser reader = new MXParser();
            reader.setFeature(FEATURE_PROCESS_NAMESPACES, false);
            InputStream source = Files.newInputStream(settings, StandardOpenOption.READ);
            reader.setInput(source, null);
            int eventType;
            while ((eventType = reader.next()) != END_DOCUMENT) {
                switch (eventType) {
                    case START_TAG: {
                        switch (reader.getName()) {
                            case "settings": {
                                parseSettings(reader, mavenSettings);
                                break;
                            }
                        }
                    }
                    default: {
                        break;
                    }
                }
            }
            return mavenSettings;
        } catch (XmlPullParserException e) {
            throw new IOException("Could not parse maven settings.xml");
        }

    }

    static void parseSettings(final XmlPullParser reader, MavenSettings mavenSettings) throws XmlPullParserException, IOException {
        int eventType;
        while ((eventType = reader.nextTag()) != END_DOCUMENT) {
            switch (eventType) {
                case END_TAG: {
                    return;
                }
                case START_TAG: {

                    switch (reader.getName()) {
                        case "localRepository": {
                            String localRepository = reader.nextText();
                            if (!"".equals(localRepository)) {
                                mavenSettings.setLocalRepository(java.nio.file.Paths.get(localRepository));
                            }
                            break;
                        }
                        case "profiles": {
                            while ((eventType = reader.nextTag()) != END_DOCUMENT) {
                                if (eventType == START_TAG) {
                                    switch (reader.getName()) {
                                        case "profile": {
                                            parseProfile(reader, mavenSettings);
                                            break;
                                        }
                                    }
                                } else {
                                    break;
                                }
                            }
                            break;
                        }
                        case "activeProfiles": {
                            while ((eventType = reader.nextTag()) != END_DOCUMENT) {
                                if (eventType == START_TAG) {
                                    switch (reader.getName()) {
                                        case "activeProfile": {
                                            mavenSettings.addActiveProfile(reader.nextText());
                                            break;
                                        }
                                    }
                                } else {
                                    break;
                                }

                            }
                            break;
                        }
                        default: {
                            skip(reader);

                        }
                    }
                    break;
                }
                default: {
                    throw unexpectedContent(reader);
                }
            }
        }
        throw endOfDocument(reader);
    }

    static void parseProfile(final XmlPullParser reader, MavenSettings mavenSettings) throws XmlPullParserException, IOException {
        int eventType;
        MavenSettings.Profile profile = new MavenSettings.Profile();
        while ((eventType = reader.nextTag()) != END_DOCUMENT) {
            if (eventType == START_TAG) {
                switch (reader.getName()) {
                    case "id": {
                        profile.setId(reader.nextText());
                        break;
                    }
                    case "repositories": {
                        while ((eventType = reader.nextTag()) != END_DOCUMENT) {
                            if (eventType == START_TAG) {
                                switch (reader.getName()) {
                                    case "repository": {
                                        parseRepository(reader, profile);
                                        break;
                                    }
                                }
                            } else {
                                break;
                            }

                        }
                        break;
                    }
                    default: {
                        skip(reader);
                    }
                }
            } else {
                break;
            }
        }
        mavenSettings.addProfile(profile);
    }

    static void parseRepository(final XmlPullParser reader, MavenSettings.Profile profile) throws XmlPullParserException, IOException {
        int eventType;
        while ((eventType = reader.nextTag()) != END_DOCUMENT) {
            if (eventType == START_TAG) {
                switch (reader.getName()) {
                    case "url": {
                        profile.addRepository(reader.nextText());
                        break;
                    }
                    default: {
                        skip(reader);
                    }
                }
            } else {
                break;
            }

        }
    }

    static void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }


    private static final Object artifactLock = new Object();

    /**
     * Try to resolve a Maven JAR artifact.  Calling this method is identical to calling
     * {@code resolveJarArtifact(qualifier, "jar")}.
     *
     * @param qualifier a non-{@code null} string in the form of {@code group:artifact:version[:classifier]}
     * @return the absolute path to the artifact, or {@code null} if none exists
     * @throws IOException if acquiring the artifact path failed for some reason
     */
    public static File resolveJarArtifact(String qualifier) throws IOException {
        return resolveJarArtifact(qualifier, "jar");
    }

    /**
     * Tries to find a maven jar artifact from the system property {@code "maven.repo.local"} This property is a list of
     * platform separated directory names.  If not specified, then it looks in {@code ${user.home}/.m2/repository} by default.
     * <p>
     * If it can't find it in local paths, then will try to download from a remote repository from the system property
     * {@code "remote.maven.repo"}.  There is no default remote repository.  It will download both the pom and jar and put it
     * into the first directory listed in {@code "maven.repo.local"} (or the default directory).  This directory will be
     * created if it doesn't exist.
     * <p>
     * Finally, if you do not want a message to console, then set the system property {@code "maven.download.message"} to
     * {@code "false"}.
     *
     * @param qualifier a non-{@code null} string in the form of {@code group:artifact:version[:classifier]}
     * @param packaging a non-{@code null} string with the exact packaging type desired (e.g. {@code pom}, {@code jar}, etc.)
     * @return the absolute path to the artifact, or {@code null} if none exists
     * @throws IOException if acquiring the artifact path failed for some reason
     */
    public static File resolveJarArtifact(String qualifier, String packaging) throws IOException {
        String[] split = qualifier.split(":");
        if (split.length < 3) {
            throw new IllegalArgumentException("Illegal artifact " + qualifier);
        }
        String groupId = split[0];
        String artifactId = split[1];
        String version = split[2];
        String classifier = "";
        if (split.length >= 4) { classifier = "-" + split[3]; }

        String artifactRelativePath = relativeArtifactPath(File.separatorChar, groupId, artifactId, version);
        String artifactRelativeHttpPath = relativeArtifactPath('/', groupId, artifactId, version);
        final MavenSettings settings = getSettings();
        final Path localRepository = settings.getLocalRepository();

        // serialize artifact lookup because we want to prevent parallel download
        synchronized (artifactLock) {
            String artifactPath = artifactRelativePath + classifier + "." + packaging;
            Path fp = java.nio.file.Paths.get(localRepository.toString(), artifactPath);
            if (Files.exists(fp)) {
                return fp.toFile();
            }

            List<String> remoteRepos = mavenSettings.getRemoteRepositories();
            if (remoteRepos.isEmpty()) {
                return null;
            }

            final File artifactFile = new File(localRepository.toFile(), artifactPath);
            final File pomFile = new File(localRepository.toFile(), artifactRelativePath + ".pom");
            for (String remoteRepository : remoteRepos) {
                try {
                    String remotePomPath = remoteRepository + artifactRelativeHttpPath + ".pom";
                    String remoteArtifactPath = remoteRepository + artifactRelativeHttpPath + classifier + "." + packaging;
                    if (! packaging.equals("pom")) {
                        downloadFile(qualifier + ":pom", remotePomPath, pomFile);
                    }
                    downloadFile(qualifier + ":" + packaging, remoteArtifactPath, artifactFile);
                    if (artifactFile.exists()) { //download successful
                        return artifactFile;
                    }
                } catch (IOException e) {
                    Module.log.trace(e, "Could not download '%s' from '%s' repository", artifactRelativePath, remoteRepository);
                    //
                }
            }
            //could not find it in remote
            Module.log.trace("Could not find in any remote repository");
            return null;
        }
    }

    static String relativeArtifactPath(char separator, String groupId, String artifactId, String version) {
        StringBuilder builder = new StringBuilder(groupId.replace('.', separator));
        builder.append(separator).append(artifactId).append(separator);
        String pathVersion;
        final Matcher versionMatcher = snapshotPattern.matcher(version);
        if (versionMatcher.lookingAt()) {
            // it's really a snapshot
            pathVersion = version.substring(0, versionMatcher.start()) + "-SNAPSHOT";
        } else {
            pathVersion = version;
        }
        builder.append(pathVersion).append(separator).append(artifactId).append('-').append(version);
        return builder.toString();
    }

    static void downloadFile(String artifact, String src, File dest) throws IOException {
        if (dest.exists()){
            return;
        }
        final URL url = new URL(src);
        final URLConnection connection = url.openConnection();
        boolean message = Boolean.getBoolean("maven.download.message");

        try (InputStream bis = connection.getInputStream()){
            dest.getParentFile().mkdirs();
            if (message) { System.out.println("Downloading " + artifact); }
            Files.copy(bis, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * A utility method to create a Maven artifact resource loader for the given artifact name.
     *
     * @param name the artifact name
     * @return the resource loader
     * @throws IOException if the artifact could not be resolved
     */
    public static ResourceLoader createMavenArtifactLoader(final String name) throws IOException {
        File fp = resolveJarArtifact(name);
        if (fp == null) return null;
        JarFile jarFile = new JarFile(fp, true);
        return ResourceLoaders.createJarResourceLoader(name, jarFile);
    }
}
