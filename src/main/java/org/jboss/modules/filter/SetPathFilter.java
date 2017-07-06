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

import java.util.Iterator;
import java.util.Set;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class SetPathFilter implements PathFilter {

    private final Set<String> paths;
    private final int hashCode;

    SetPathFilter(final Set<String> paths) {
        this.paths = paths;
        hashCode = paths.hashCode();
    }

    public boolean accept(final String path) {
        return paths.contains(path);
    }

    public String toString() {
        final StringBuilder b = new StringBuilder();
        b.append("in {");
        Iterator<String> iterator = paths.iterator();
        while (iterator.hasNext()) {
            final String path = iterator.next();
            b.append(path);
            if (iterator.hasNext()) {
                b.append(", ");
            }
        }
        b.append('}');
        return b.toString();
    }

    public boolean equals(final Object obj) {
        return obj == this || obj instanceof SetPathFilter && equals((SetPathFilter) obj);
    }

    public boolean equals(final SetPathFilter obj) {
        return obj != null && obj.paths.equals(paths);
    }

    public int hashCode() {
        return hashCode;
    }
}
