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

package org.jboss.modules.filter;

import java.io.File;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ChildPathFilter implements PathFilter {

    private final String prefix;

    ChildPathFilter(final String path) {
        prefix = path.charAt(path.length() - 1) == File.separatorChar ? path : path + File.separatorChar;
    }

    public boolean accept(final String path) {
        return path.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    public boolean equals(final Object obj) {
        return obj instanceof ChildPathFilter && equals((ChildPathFilter) obj);
    }

    public boolean equals(final ChildPathFilter obj) {
        return obj != null && obj.prefix.equals(prefix);
    }

    public String toString() {
        return "children of \"" + prefix + "\"";
    }

    public int hashCode() {
        return prefix.hashCode();
    }
}
