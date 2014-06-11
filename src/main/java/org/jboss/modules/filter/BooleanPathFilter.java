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
final class BooleanPathFilter implements PathFilter {

    private final boolean result;

    private BooleanPathFilter(final boolean result) {
        this.result = result;
    }

    public boolean accept(final String path) {
        return result;
    }

    static final BooleanPathFilter TRUE = new BooleanPathFilter(true);
    static final BooleanPathFilter FALSE = new BooleanPathFilter(false);

    public int hashCode() {
        return Boolean.valueOf(result).hashCode();
    }

    public boolean equals(final Object obj) {
        return obj == this;
    }

    public String toString() {
        return result ? "Accept" : "Reject";
    }

    boolean getResult() {
        return result;
    }
}
