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
 * PathFilter implementation that aggregates multiple other filters.
 *
 * @author John E. Bailey
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class AggregatePathFilter implements PathFilter {
    private final PathFilter[] delegates;
    private final boolean any;
    private final int hashCode;

    /**
     * Construct a new instance.
     *
     * @param any {@code true} if this is an "any" filter, {@code false} if this an "all" filter
     * @param delegates the delegate filter list
     */
    AggregatePathFilter(final boolean any, final PathFilter... delegates) {
        this.any = any;
        this.delegates = delegates;
        hashCode = Boolean.valueOf(any).hashCode() ^ Arrays.hashCode(delegates);
    }

    /** {@inheritDoc} */
    @Override
    public boolean accept(String path) {
        for (PathFilter filter : delegates) {
            if (filter.accept(path) == any) {
                return any;
            }
        }
        return ! any;
    }


    public int hashCode() {
        return hashCode;
    }

    public boolean equals(final Object obj) {
        return obj instanceof AggregatePathFilter && equals((AggregatePathFilter) obj);
    }

    public boolean equals(final AggregatePathFilter obj) {
        return obj != null && obj.any == any && Arrays.equals(obj.delegates, delegates);
    }

    public String toString() {
        final StringBuilder b = new StringBuilder();
        b.append(any ? "Any " : "All ").append("of (");
        for (int idx = 0; idx < delegates.length; idx++) {
            final PathFilter delegate = delegates[idx];
            b.append(delegate);
            if (idx < delegates.length - 1) {
                b.append(',');
            }
        }
        b.append(')');
        return b.toString();
    }
}
