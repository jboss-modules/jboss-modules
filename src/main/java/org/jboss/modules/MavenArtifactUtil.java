/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Helper class to resolve a maven artifact
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
class MavenArtifactUtil {

    public static String[] getLocalRepositoryPaths() {
        String localRepositoryPath = System.getProperty("local.maven.repo.path");
        if (localRepositoryPath == null) {
            File m2 = new File(System.getProperty("user.home"), ".m2");
            File repository = new File(m2, "repository");
            String path = repository.getAbsolutePath();
            String[] rtn = { path };
            return rtn;
        } else {
            return localRepositoryPath.split(File.pathSeparator);
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
     *
     * @return absolute path to artifact, null if none exists
     *
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
        if (split.length >= 4) classifier = "-" + split[3];

        String artifactRelativePath = relativeArtifactPath(groupId, artifactId, version);
        String[] localRepositoryPaths = getLocalRepositoryPaths();

        // serialize artifact lookup because we want to prevent parallel download
        synchronized (artifactLock) {
            String jarPath = artifactRelativePath + classifier + ".jar";
            for (String localRepository : localRepositoryPaths) {
                File fp = new File(localRepository, jarPath);
                if (fp.exists()) return fp;
            }

            String remoteRepository = System.getProperty("remote.maven.repo");
            if (remoteRepository == null) return null;
            if (!remoteRepository.endsWith("/")) remoteRepository += "/";

            File jarFile = new File(localRepositoryPaths[0], jarPath);
            File pomFile = new File(localRepositoryPaths[0], artifactRelativePath + ".pom");
            String remotePomPath = remoteRepository + relativeArtifactHttpPath(groupId, artifactId, version) + ".pom";
            String remoteJarPath = remoteRepository + relativeArtifactHttpPath(groupId, artifactId, version) + classifier + ".jar";
            downloadFile(qualifier + ":pom", remotePomPath, pomFile);
            downloadFile(qualifier + ":jar", remoteJarPath, jarFile);
            return jarFile;
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
                if (message) System.out.println("Downloading " + artifact);
                StreamUtil.copy(bis, bos);
            } finally {
                StreamUtil.safeClose(fos);
            }
        } finally {
            StreamUtil.safeClose(bis);
        }
    }
}
