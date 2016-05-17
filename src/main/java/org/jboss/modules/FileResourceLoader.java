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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.AccessControlContext;
import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class FileResourceLoader extends PathResourceLoader {

    FileResourceLoader(final String rootName, final File root, final AccessControlContext context) {
        super(rootName, root.toPath(), context);
    }

    public Collection<String> getPaths() {
        new ArrayList<String>();
        final Path indexFile = root.resolveSibling(root.getFileName() + ".index");
        if (ResourceLoaders.USE_INDEXES) {
            // First check for an index file
            if (Files.exists(indexFile)) {
                try {
                    return Files.readAllLines(indexFile);
                } catch (IOException e) {
                    // Do nothing
                }
            }
        }

        // Manually build index
        final Collection<String> index = super.getPaths();

        if (ResourceLoaders.WRITE_INDEXES) {
            // Now try to write it
            try {
                Files.write(indexFile, index);
            } catch (Exception e) {
                // well, we tried...
                try {
                    Files.deleteIfExists(indexFile);
                } catch (IOException ignored) {
                    // Do nothing
                }
            }
        }
        return index;
    }
}
