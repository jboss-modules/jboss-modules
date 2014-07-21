/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.modules;

import static org.jboss.modules.ModuleXmlParser.endOfDocument;
import static org.jboss.modules.ModuleXmlParser.unexpectedContent;
import static org.jboss.modules.xml.XmlPullParser.END_DOCUMENT;
import static org.jboss.modules.xml.XmlPullParser.END_TAG;
import static org.jboss.modules.xml.XmlPullParser.FEATURE_PROCESS_NAMESPACES;
import static org.jboss.modules.xml.XmlPullParser.START_TAG;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import org.jboss.modules.xml.MXParser;
import org.jboss.modules.xml.XmlPullParser;
import org.jboss.modules.xml.XmlPullParserException;

/**
 * Helper class to resolve a maven artifact
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author <a href="mailto:tcerar@redhat.com">Tomaz Cerar</a>
 * @version $Revision: 2 $
 */
class MavenArtifactUtil {

    private static MavenSettings mavenSettings;
    private static final Object settingLoaderMutex = new Object();

    public static MavenSettings getSettings() throws IOException {
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

    private static MavenSettings parseSettingsXml(Path settings, MavenSettings mavenSettings) throws IOException {
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

    private static void parseSettings(final XmlPullParser reader, MavenSettings mavenSettings) throws XmlPullParserException, IOException {
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
                            mavenSettings.setLocalRepository(java.nio.file.Paths.get(localRepository));
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

    private static void parseProfile(final XmlPullParser reader, MavenSettings mavenSettings) throws XmlPullParserException, IOException {
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

    private static void parseRepository(final XmlPullParser reader, MavenSettings.Profile profile) throws XmlPullParserException, IOException {
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

    private static void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
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
     * Tries to find a maven jar artifact from the system property "local.maven.repo.path" This property is a list of
     * platform separated directory names.  If not specified, then it looks in ${user.home}/.m2/repository by default.
     * <p/>
     * If it can't find it in local paths, then will try to download from a remote repository from the system property
     * "remote.maven.repo".  There is no default remote repository.  It will download both the pom and jar and put it
     * into the first directory listed in "local.maven.repo.path" (or the default dir).  This directory will be created
     * if it doesn't exist.
     * <p/>
     * Finally, if you do not want a message to console, then set the system property "maven.download.message" to
     * "false"
     *
     * @param qualifier group:artifact:version[:classifier]
     * @return absolute path to artifact, null if none exists
     * @throws Exception
     */
    public static File resolveJarArtifact(String qualifier) throws IOException {
        String[] split = qualifier.split(":");
        if (split.length < 3) {
            throw new IllegalArgumentException("Illegal artifact " + qualifier);
        }
        String groupId = split[0];
        String artifactId = split[1];
        String version = split[2];
        String classifier = "";
        if (split.length >= 4) { classifier = "-" + split[3]; }

        String artifactRelativePath = relativeArtifactPath(groupId, artifactId, version);
        final MavenSettings settings = getSettings();
        final Path localRepository = settings.getLocalRepository();

        // serialize artifact lookup because we want to prevent parallel download
        synchronized (artifactLock) {
            String jarPath = artifactRelativePath + classifier + ".jar";
            Path fp = java.nio.file.Paths.get(localRepository.toString(), jarPath);
            if (Files.exists(fp)) {
                return fp.toFile();
            }

            List<String> remoteRepos = mavenSettings.getRemoteRepositories();
            if (remoteRepos.isEmpty()) {
                return null;
            }

            File jarFile = new File(localRepository.toFile(), jarPath);
            File pomFile = new File(localRepository.toFile(), artifactRelativePath + ".pom");
            for (String remoteRepository : remoteRepos) {
                String remotePomPath = remoteRepository + relativeArtifactHttpPath(groupId, artifactId, version) + ".pom";
                String remoteJarPath = remoteRepository + relativeArtifactHttpPath(groupId, artifactId, version) + classifier + ".jar";
                downloadFile(qualifier + ":pom", remotePomPath, pomFile);
                downloadFile(qualifier + ":jar", remoteJarPath, jarFile);
                if (jarFile.exists()) { //download successful
                    return jarFile;
                }
            }
            //could not find it in remote
            return null;
        }
    }

    public static String relativeArtifactPath(String groupId, String artifactId, String version) {
        return relativeArtifactPath(File.separatorChar, groupId, artifactId, version);
    }

    public static String relativeArtifactHttpPath(String groupId, String artifactId, String version) {
        return relativeArtifactPath('/', groupId, artifactId, version);
    }

    private static String relativeArtifactPath(char separator, String groupId, String artifactId, String version) {
        StringBuilder builder = new StringBuilder(groupId.replace('.', separator));
        builder.append(separator).append(artifactId).append(separator).append(version).append(separator).append(artifactId).append('-').append(version);
        return builder.toString();
    }

    public static void downloadFile(String artifact, String src, File dest) throws IOException {
        final URL url = new URL(src);
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        boolean message = Boolean.parseBoolean(System.getProperty("maven.download.message", "true"));

        InputStream bis = connection.getInputStream();
        try {
            dest.getParentFile().mkdirs();
            FileOutputStream fos = new FileOutputStream(dest);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            try {
                if (message) { System.out.println("Downloading " + artifact); }
                StreamUtil.copy(bis, bos);
            } finally {
                StreamUtil.safeClose(fos);
            }
        } finally {
            StreamUtil.safeClose(bis);
        }
    }
}
