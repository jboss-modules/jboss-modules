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

import static org.jboss.modules.maven.MavenArtifactUtil.doIo;

import java.io.File;
import java.io.IOException;
import java.security.AccessControlContext;
import java.security.AccessController;

final class DefaultMavenResolver implements MavenResolver {
    private final AccessControlContext context;

    DefaultMavenResolver() {
        context = AccessController.getContext();
    }

    public File resolveArtifact(final ArtifactCoordinates coordinates, final String packaging) throws IOException {
        return doIo(() -> MavenArtifactUtil.resolveArtifact(coordinates, packaging), context);
    }
}
