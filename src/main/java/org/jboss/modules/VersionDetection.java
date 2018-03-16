/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
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

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * A utility class to assist with detecting the version of a resource root or collection of resource roots.
 */
public final class VersionDetection {
    private VersionDetection() {}

    /**
     * Attempt to guess the version of a resource loader.
     *
     * @param resourceLoader the resource loader to check (must not be {@code null})
     * @return the version, or {@code null} if no version could be determined
     * @throws IOException if necessary resource(s) failed to load
     */
    public static Version detectVersion(ResourceLoader resourceLoader) throws IOException {
        final Resource resource = resourceLoader.getResource("META-INF/MANIFEST.MF");
        if (resource != null) {
            Manifest manifest;
            try (InputStream is = resource.openStream()) {
                manifest = new Manifest(is);
            }
            final Attributes mainAttributes = manifest.getMainAttributes();
            final String versionString = mainAttributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
            if (versionString != null) try {
                return Version.parse(versionString);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return null;
    }
}
