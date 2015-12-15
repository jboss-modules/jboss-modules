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

package org.jboss.modules.maven;

import java.io.File;
import java.io.IOException;

/**
 * A resolution strategy for Maven artifacts.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface MavenResolver {

    /**
     * Try to resolve a Maven JAR artifact.  Calling this method is identical to calling
     * {@code resolveJarArtifact(qualifier, "jar")}.
     *
     * @param coordinates the non-{@code null} Maven coordinates object
     * @return the absolute path to the artifact, or {@code null} if none exists
     * @throws IOException if acquiring the artifact path failed for some reason
     */
    default File resolveJarArtifact(final ArtifactCoordinates coordinates) throws IOException {
        return resolveArtifact(coordinates, "jar");
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
     * @param coordinates the non-{@code null} Maven coordinates object
     * @param packaging a non-{@code null} string with the exact packaging type desired (e.g. {@code pom}, {@code jar}, etc.)
     * @return the absolute path to the artifact, or {@code null} if none exists
     * @throws IOException if acquiring the artifact path failed for some reason
     */
    File resolveArtifact(final ArtifactCoordinates coordinates, final String packaging) throws IOException;

    /**
     * Create a Maven artifact resolver using the default strategy.  The permissions of the class calling this method
     * are captured and used for filesystem and network accesses.  The default strategy uses the following system
     * properties:
     * <ul>
     *     <li>{@code maven.repo.local} - a list of directory names using the platform separator which reflect local
     *     Maven repository roots</li>
     *     <li>{@code remote.maven.repo} - a comma-separated list of URIs which refer to remote Maven repositories,
     *     from which artifacts can be downloaded</li>
     *     <li>{@code maven.download.message} - a boolean system property which controls the logging of messages to
     *     the console</li>
     * </ul>
     *
     * @return the maven resolver strategy (not {@code null})
     */
    static MavenResolver createDefaultResolver() {
        return new DefaultMavenResolver();
    }
}
