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

package org.jboss.modules;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class IOUtils {

    private IOUtils() {
        // forbidden instantiation
    }

    static void copy(final InputStream is, final OutputStream os) throws IOException {
        final byte[] buffer = new byte[1024];
        int len;
        while ((len = is.read(buffer)) > 0) {
            os.write(buffer, 0, len);
        }
    }

    static void copyAndClose(final InputStream is, final OutputStream os) throws IOException {
        try {
            copy(is, os);
        } finally {
            safeClose(is);
            safeClose(os);
        }
    }

    static void safeClose(final Closeable c) {
        if (c == null) return;
        try {
            c.close();
        } catch (final Throwable t) {
            // ignored
        }
    }

}
