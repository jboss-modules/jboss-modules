/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.module;

public final class Version {
    private final Part initial;

    private static final class Separator {
        private final String literal;
        private final Part nextPart;

        private Separator(final String literal, final Part nextPart) {
            this.literal = literal;
            this.nextPart = nextPart;
        }

        public String getLiteral() {
            return literal;
        }
    }

    private static class Part implements Comparable<Part> {
        private final Separator nextSeparator;
        private final String literal;

        protected Part(final String literal, final Separator nextSeparator) {
            this.nextSeparator = nextSeparator;
            this.literal = literal;
        }

        public Separator getNextSeparator() {
            return nextSeparator;
        }

        public String getLiteral() {
            return literal;
        }

        public int compareTo(final Part o) {
            if (o instanceof NumericPart) {
                return -o.compareTo(this);
            } else {
                return literal.compareTo(o.literal);
            }
        }
    }

    private static final class NumericPart extends Part {
        private final long value;

        private NumericPart(final String literal, final Separator separator) {
            super(literal, separator);
            value = Long.parseLong(literal, 10);
        }

        public long getValue() {
            return value;
        }

        public int compareTo(final Part o) {
            return super.compareTo(o);
        }
    }

    public Version(String original) {
        initial = getPartFor(original, 0);
    }

    private static Part getPartFor(String original, int offs) {
        final int start = offs;
        final int len = original.length();
        while (offs < len) {
            char ch = original.charAt(offs);
            if (Character.isLetterOrDigit(ch)) {
                offs++;
                continue;
            } else {
                return new Part();
            }
        }
    }
}
