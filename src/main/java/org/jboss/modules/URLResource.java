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
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.AccessControlContext;
import java.security.PrivilegedAction;

import static java.security.AccessController.doPrivileged;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class URLResource implements Resource {
    private final URL url;
    private final AccessControlContext context;

    public URLResource(final URL url, final AccessControlContext context) {
        this.url = url;
        this.context = context;
    }

    public String getName() {
        return url.getPath();
    }

    public URL getURL() {
        return url;
    }

    public InputStream openStream() throws IOException {
        return url.openStream();
    }

    public boolean isDirectory() {
        final File file = new File(url.getFile());
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            return doPrivileged(new PrivilegedAction<Boolean>() {
                public Boolean run() {
                    return file.isDirectory();
                }
            }, context).booleanValue();
        } else {
            return file.isDirectory();
        }
    }

    public long getSize() {
        return 0L;
    }
}
