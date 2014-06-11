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
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class EqualsPathFilter implements PathFilter {

    private final String path;

    EqualsPathFilter(final String path) {
        if (path == null) {
            throw new IllegalArgumentException("path is null");
        }
        this.path = path;
    }

    public boolean accept(final String path) {
        return path.equals(this.path);
    }

    public boolean equals(final Object obj) {
        return obj instanceof EqualsPathFilter && equals((EqualsPathFilter) obj);
    }

    public boolean equals(final EqualsPathFilter obj) {
        return obj != null && obj.path.equals(path);
    }

    public String toString() {
        return "equals \"" + path + "\"";
    }

    public int hashCode() {
        return path.hashCode() + 7;
    }
}
