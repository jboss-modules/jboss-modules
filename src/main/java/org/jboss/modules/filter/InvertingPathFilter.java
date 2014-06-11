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

/**
 * A path filter which simply inverts the result of another path filter.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class InvertingPathFilter implements PathFilter {
    private final PathFilter delegate;

    /**
     * Construct a new instance.
     *
     * @param delegate the filter to delegate to
     */
    InvertingPathFilter(final PathFilter delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate is null");
        }
        this.delegate = delegate;
    }

    /** {@inheritDoc} */
    public boolean accept(final String path) {
        return ! delegate.accept(path);
    }

    PathFilter getDelegate() {
        return delegate;
    }

    public int hashCode() {
        return 47 + delegate.hashCode();
    }

    public boolean equals(final Object obj) {
        return obj instanceof InvertingPathFilter && equals((InvertingPathFilter) obj);
    }

    public boolean equals(final InvertingPathFilter obj) {
        return obj != null && obj.delegate.equals(delegate);
    }

    public String toString() {
        return "not " + delegate.toString();
    }
}
