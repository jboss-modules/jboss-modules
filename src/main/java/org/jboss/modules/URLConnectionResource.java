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
import java.net.URL;
import java.net.URLConnection;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class URLConnectionResource implements Resource {
    private final URLConnection connection;

    URLConnectionResource(final URL url) throws IOException {
        this(url.openConnection());
    }

    URLConnectionResource(final URLConnection connection) {
        this.connection = connection;
    }

    public String getName() {
        return getURL().getPath();
    }

    public URL getURL() {
        return connection.getURL();
    }

    public InputStream openStream() throws IOException {
        return connection.getInputStream();
    }

    public long getSize() {
        final long len = connection.getContentLengthLong();
        return len == -1 ? 0 : len;
    }
}
