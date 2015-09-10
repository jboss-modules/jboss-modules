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
import java.io.InputStream;
import java.net.URL;

/**
 * A single resource from a {@link ResourceLoader}.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public interface Resource {
    /**
     * Get the relative resource name.
     *
     * @return the name
     */
    String getName();

    /**
     * Get the complete URL of this resource.
     *
     * @return the URL
     */
    URL getURL();

    /**
     * Returns true if this is a directory entry.
     *
     * @return true if this is a directory entry.
     */
    boolean isDirectory();

    /**
     * Open an input stream to this resource.
     *
     * @return the stream
     * @throws java.io.IOException if an I/O error occurs
     */
    InputStream openStream() throws IOException;

    /**
     * Get the size of the resource, if known.
     *
     * @return the size, or 0L if unknown
     */
    long getSize();
}
