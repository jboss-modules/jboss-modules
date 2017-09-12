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

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.security.AccessControlContext;
import java.util.HashMap;

/**
 * @author Tomaz Cerar
 */
final class JarResourceLoader extends PathResourceLoader implements IterableResourceLoader {
    private static final String INDEX_FILE = "META-INF/PATHS.LIST";

    private static Path getJarPath(Path root) throws IOException {
        URI p = URI.create("jar:" + root.toUri());
        FileSystem fs = FileSystems.newFileSystem(p, new HashMap<>());
        return fs.getRootDirectories().iterator().next();
    }

    JarResourceLoader(final String rootName, final Path root, final AccessControlContext context) throws IOException {
        super(rootName, getJarPath(root), context);
    }

    @Override
    public void close() {
        try {
            root.getFileSystem().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
