/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
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
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Java NIO Path-based Resource
 *
 * @author <a href="mailto:bsideup@gmail.com">Sergei Egorov</a>
 */
class PathResource implements Resource {

    private final Path path;

    PathResource(Path path) {
        this.path = path;
    }

    @Override
    public String getName() {
        return path.toString();
    }

    @Override
    public URL getURL() {
        try {
            return path.toUri().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream openStream() throws IOException {
        return Files.newInputStream(path);
    }

    @Override
    public long getSize() {
        try {
            return Files.size(path);
        } catch (IOException e) {
            return 0;
        }
    }
}
