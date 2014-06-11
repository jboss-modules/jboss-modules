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

import java.util.Arrays;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class MultiplePathFilter implements PathFilter {
    private final PathFilter[] filters;
    private final boolean[] includeFlag;
    private final boolean defaultVal;
    private final int hashCode;

    MultiplePathFilter(final PathFilter[] filters, final boolean[] includeFlag, final boolean defaultVal) {
        this.filters = filters;
        this.includeFlag = includeFlag;
        this.defaultVal = defaultVal;
        hashCode = Boolean.valueOf(defaultVal).hashCode() * 13 + (Arrays.hashCode(includeFlag) * 13 + (Arrays.hashCode(filters)));
    }

    public boolean accept(final String path) {
        final int len = filters.length;
        for (int i = 0; i < len; i++) {
            if (filters[i].accept(path)) return includeFlag[i];
        }
        return defaultVal;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("multi-path filter {");
        int len = filters.length;
        for (int i = 0; i < len; i++) {
            final PathFilter filter = filters[i];
            final boolean include = includeFlag[i];
            builder.append(include ? "include " : "exclude ");
            builder.append(filter);
            builder.append(", ");
        }
        builder.append("default ").append(defaultVal ? "accept" : "reject");
        builder.append('}');
        return builder.toString();
    }

    public int hashCode() {
        return hashCode;
    }

    public boolean equals(Object other) {
        return other instanceof MultiplePathFilter && equals((MultiplePathFilter)other);
    }

    public boolean equals(MultiplePathFilter other) {
        return this == other || other != null && Arrays.equals(filters, other.filters) && Arrays.equals(includeFlag, other.includeFlag) && defaultVal == other.defaultVal;
    }
}
