/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
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

import java.io.Serializable;
import java.math.BigInteger;
import java.text.Normalizer;
import java.util.NoSuchElementException;

/**
 * A version for a module.  Versions are series of letters and digits, optionally divided by separator characters, which
 * include {@code .}, {@code -}, {@code +}, and {@code _}.  The transition between letter and digit (or vice-versa) is
 * also considered to be an invisible separator.
 * <p>
 * Versions may be compared, sorted, used as hash keys, and iterated.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class Version implements Comparable<Version>, Serializable {

    private final String version;

    private static final int TOK_INITIAL = 0;
    private static final int TOK_PART_NUMBER = 1;
    private static final int TOK_PART_ALPHA = 2;
    private static final int TOK_SEP = 3;
    private static final int TOK_SEP_EMPTY = 4;

    boolean hasNext(long cookie) {
        return cookieToEndIndex(cookie) < version.length();
    }

    /**
     * Go to the next token.
     *
     * @param cookie the cookie ({@code 0L} indicates start of search)
     * @return the next cookie value in the token sequence
     * @throws IllegalArgumentException if the next token is not a valid token or the string is too long
     */
    long next(long cookie) throws IllegalArgumentException {
        int start, end, token;
        // get old end value
        end = cookieToEndIndex(cookie);
        final String version = this.version;
        final int length = version.length();
        // hasNext() should have been called first on this cookie value
        assert end < length;
        start = end;
        int cp = version.codePointAt(start);
        // examine the previous token
        token = cookieToToken(cookie);
        if (token == TOK_PART_NUMBER && isValidAlphaPart(cp) || token == TOK_PART_ALPHA && isValidNumberPart(cp)) {
            token = TOK_SEP_EMPTY;
            end = start;
        } else if ((token == TOK_INITIAL || token == TOK_SEP || token == TOK_SEP_EMPTY) && isValidAlphaPart(cp)) {
            token = TOK_PART_ALPHA;
            end = length;
            // find end
            for (int i = start; i < length; i = version.offsetByCodePoints(i, 1)) {
                cp = version.codePointAt(i);
                if (! isValidAlphaPart(cp)) {
                    end = i;
                    break;
                }
            }
        } else if ((token == TOK_INITIAL || token == TOK_SEP || token == TOK_SEP_EMPTY) && isValidNumberPart(cp)) {
            token = TOK_PART_NUMBER;
            end = length;
            // find end
            for (int i = start; i < length; i = version.offsetByCodePoints(i, 1)) {
                cp = version.codePointAt(i);
                if (! isValidNumberPart(cp)) {
                    end = i;
                    break;
                }
            }
        } else if ((token == TOK_PART_NUMBER || token == TOK_PART_ALPHA) && isValidSeparator(cp)) {
            token = TOK_SEP;
            end = version.offsetByCodePoints(start, 1);
        } else {
            throw new IllegalArgumentException("Invalid version code point \"" + new String(Character.toChars(cp)) + "\" at offset " + start + " of \"" + version + "\"");
        }
        if (end > (1 << 28) - 1) {
            throw new IllegalArgumentException("Version string is too long");
        }
        assert end >= start;
        return ((long) start) | ((long) end << 28) | ((long) token << 56);
    }

    /**
     * Get the token start index from the cookie.  The start index is stored in the lowest 28 bits (bits 0-27) of the cookie.
     *
     * @param cookie the cookie
     * @return the start character index in the version string
     */
    static int cookieToStartIndex(long cookie) {
        return (int) (cookie & 0xfff_ffff);
    }

    /**
     * Get the token end index from the cookie.  The end index is stored in the second 28 bits (bits 28-55) of the cookie.
     *
     * @param cookie the cookie
     * @return the end character index in the version string
     */
    static int cookieToEndIndex(long cookie) {
        return (int) ((cookie >> 28) & 0xfff_ffff);
    }

    /**
     * Get the token type from the cookie.  The token type is stored in bits 56 and up in the cookie.
     * The value will be one of:
     * <ul>
     *     <li>{@link #TOK_PART_NUMBER}</li>
     *     <li>{@link #TOK_PART_ALPHA}</li>
     *     <li>{@link #TOK_SEP}</li>
     *     <li>{@link #TOK_SEP_EMPTY}</li>
     * </ul>
     *
     * @param cookie the cookie
     * @return the token value
     */
    static int cookieToToken(long cookie) {
        return (int) (cookie >> 56);
    }

    static int sepMagnitude(int codePoint) {
        switch (codePoint) {
            case '.': return 1;
            case '-': return 2;
            case '+': return 3;
            case '_': return 4;
            default: throw new IllegalStateException();
        }
    }

    static int compareSep(int sep1, int sep2) {
        return Integer.signum(sepMagnitude(sep1) - sepMagnitude(sep2));
    }

    static boolean isValidAlphaPart(int codePoint) {
        return Character.isLetter(codePoint);
    }

    static boolean isValidNumberPart(int codePoint) {
        // Use this instead of isDigit(cp) in case a valid digit is not a valid decimal digit
        return Character.digit(codePoint, 10) != -1;
    }

    static boolean isValidSeparator(int codePoint) {
        return codePoint == '.' || codePoint == '-' || codePoint == '+' || codePoint == '_';
    }

    int comparePart(long cookie, Version other, long otherCookie) {
        final int token = cookieToToken(cookie);
        final int otherToken = cookieToToken(otherCookie);
        final int start = cookieToStartIndex(cookie);
        final int otherStart = cookieToStartIndex(cookie);
        final int end = cookieToEndIndex(cookie);
        final int otherEnd = cookieToEndIndex(otherCookie);
        switch (token) {
            case TOK_PART_NUMBER: {
                if (otherToken == TOK_PART_ALPHA) {
                    return 1;
                }
                assert otherToken == TOK_PART_NUMBER;
                // we need the length in digits, not in characters
                final int digits = version.codePointCount(start, end);
                final int otherDigits = other.version.codePointCount(otherStart, otherEnd);
                // if one is shorter, we need to pad it out with invisible zeros for the value comparison
                // otherwise, compare digits naively from left to right
                int res;
                // in this loop, i represents the place with a higher # being more significant
                for (int i = Math.max(digits, otherDigits) - 1; i >= 0; i --) {
                    // if the value has no digit at location 'i', i.e. i > length, then the effective value is 0
                    int a = i >= digits ? 0 : Character.digit(version.codePointBefore(version.offsetByCodePoints(end, -i)), 10);
                    int b = i >= otherDigits ? 0 : Character.digit(other.version.codePointBefore(other.version.offsetByCodePoints(otherEnd, -i)), 10);
                    res = Integer.signum(a - b);
                    if (res != 0) {
                        return res;
                    }
                }
                // equal, so now the shortest wins
                return Integer.signum(digits - otherDigits);
            }
            case TOK_PART_ALPHA: {
                if (otherToken == TOK_PART_NUMBER) {
                    return -1;
                }
                assert otherToken == TOK_PART_ALPHA;
                // compare string naively from left to right
                for (int i = start; i < Math.min(end, otherEnd); i = version.offsetByCodePoints(i, 1)) {
                    int cp = version.codePointAt(i);
                    int ocp = other.version.codePointAt(i);
                    assert isValidAlphaPart(cp) && isValidAlphaPart(ocp);
                    int res = Integer.signum(cp - ocp);
                    if (res != 0) {
                        return res;
                    }
                }
                // identical prefix; fall back to length comparison
                return Integer.signum(end - otherEnd);
            }
            case TOK_SEP: {
                if (otherToken == TOK_SEP_EMPTY) {
                    return 1;
                } else {
                    assert otherToken == TOK_SEP;
                    return compareSep(version.codePointAt(start), other.version.codePointAt(cookieToStartIndex(otherCookie)));
                }
            }
            case TOK_SEP_EMPTY: {
                return otherToken == TOK_SEP_EMPTY ? 0 : -1;
            }
            default: throw new IllegalStateException();
        }
    }

    private Version(String v) {
        if (v == null)
            throw new IllegalArgumentException("Null version string");

        this.version = Normalizer.normalize(v, Normalizer.Form.NFKC);

        // validate
        if (! hasNext(0L)) {
            throw new IllegalArgumentException("Empty version string");
        }
        long cookie = next(0L);
        while (hasNext(cookie)) {
            cookie = next(cookie);
        }

        final int lastToken = cookieToToken(cookie);
        if (lastToken == TOK_SEP || lastToken == TOK_SEP_EMPTY) {
            throw new IllegalArgumentException("Version ends with a separator");
        }
    }

    /**
     * Parses the given string as a version string.
     *
     * @param  v
     *         The string to parse
     *
     * @return The resulting {@code Version}
     *
     * @throws IllegalArgumentException
     *         If {@code v} is {@code null}, an empty string, or cannot be
     *         parsed as a version string
     */
    public static Version parse(String v) {
        return new Version(v);
    }

    /**
     * Construct a new iterator over the parts of this version string.
     *
     * @return the iterator
     */
    public Iterator iterator() {
        return new Iterator();
    }

    /**
     * Compares this module version to another module version. Module
     * versions are compared as described in the class description.
     *
     * @param that
     *        The module version to compare
     *
     * @return A negative integer, zero, or a positive integer as this
     *         module version is less than, equal to, or greater than the
     *         given module version
     */
    @Override
    public int compareTo(Version that) {
        long cookie = 0L;
        long thatCookie = 0L;
        int res;

        while (hasNext(cookie)) {
            cookie = next(cookie);
            if (that.hasNext(thatCookie)) {
                thatCookie = that.next(thatCookie);
                res = comparePart(cookie, that, thatCookie);
                if (res != 0) {
                    return res;
                }
            } else {
                return 1;
            }
        }
        return that.hasNext(thatCookie) ? - 1 : 0;
    }

    /**
     * Tests this module version for equality with the given object.
     *
     * <p> If the given object is not a {@code Version} then this method
     * returns {@code false}. Two module version are equal if their
     * corresponding components are equal. </p>
     *
     * <p> This method satisfies the general contract of the {@link
     * java.lang.Object#equals(Object) Object.equals} method. </p>
     *
     * @param   ob
     *          the object to which this object is to be compared
     *
     * @return  {@code true} if, and only if, the given object is a module
     *          reference that is equal to this module reference
     */
    @Override
    public boolean equals(Object ob) {
        if (!(ob instanceof Version))
            return false;
        return compareTo((Version)ob) == 0;
    }

    /**
     * Computes a hash code for this module version.
     *
     * <p> The hash code is based upon the components of the version and
     * satisfies the general contract of the {@link Object#hashCode
     * Object.hashCode} method. </p>
     *
     * @return The hash-code value for this module version
     */
    @Override
    public int hashCode() {
        return version.hashCode();
    }

    /**
     * Returns the normalized string representation of this version.
     *
     * @return The normalized string.
     */
    @Override
    public String toString() {
        return version;
    }

    /**
     * An iterator over the parts of a version.
     */
    public class Iterator {
        long cookie = 0L;

        Iterator() {
        }

        /**
         * Determine whether another token exists in this version.
         *
         * @return {@code true} if more tokens remain, {@code false} otherwise
         */
        public boolean hasNext() {
            return Version.this.hasNext(cookie);
        }

        /**
         * Move to the next token.
         *
         * @throws NoSuchElementException if there are no more tokens to iterate
         */
        public void next() {
            final long cookie = this.cookie;
            if (! Version.this.hasNext(cookie)) {
                throw new NoSuchElementException();
            }
            this.cookie = Version.this.next(cookie);
        }

        /**
         * Get the length of the current token.  If there is no current token, zero is returned.
         *
         * @return the length of the current token
         */
        public int length() {
            final long cookie = this.cookie;
            return cookieToEndIndex(cookie) - cookieToStartIndex(cookie);
        }

        /**
         * Determine if the current token is some kind of separator (a character or a zero-length alphabetical-to-numeric
         * or numeric-to-alphabetical transition).
         *
         * @return {@code true} if the token is a separator, {@code false} otherwise
         */
        public boolean isSeparator() {
            final int token = cookieToToken(cookie);
            return token == TOK_SEP_EMPTY || token == TOK_SEP;
        }

        /**
         * Determine if the current token is some kind of part (alphabetical or numeric).
         *
         * @return {@code true} if the token is a separator, {@code false} otherwise
         */
        public boolean isPart() {
            final int token = cookieToToken(cookie);
            return token == TOK_PART_ALPHA || token == TOK_PART_NUMBER;
        }

        /**
         * Determine if the current token is an empty (or zero-length alphabetical-to-numeric
         * or numeric-to-alphabetical) separator.
         *
         * @return {@code true} if the token is an empty separator, {@code false} otherwise
         */
        public boolean isEmptySeparator() {
            return cookieToToken(cookie) == TOK_SEP_EMPTY;
        }

        /**
         * Determine if the current token is a non-empty separator.
         *
         * @return {@code true} if the token is a non-empty separator, {@code false} otherwise
         */
        public boolean isNonEmptySeparator() {
            return cookieToToken(cookie) == TOK_SEP;
        }

        /**
         * Get the code point of the current separator.  If the iterator is not positioned on a non-empty separator
         * (i.e. {@link #isNonEmptySeparator()} returns {@code false}), then an exception is thrown.
         *
         * @return the code point of the current separator
         * @throws IllegalStateException if the current token is not a non-empty separator
         */
        public int getSeparatorCodePoint() {
            final long cookie = this.cookie;
            if (cookieToToken(cookie) != TOK_SEP) {
                throw new IllegalStateException();
            }
            return version.codePointAt(cookieToStartIndex(cookie));
        }

        /**
         * Determine if the current token is an alphabetical part.
         *
         * @return {@code true} if the token is an alphabetical part, {@code false} otherwise
         */
        public boolean isAlphaPart() {
            return cookieToToken(cookie) == TOK_PART_ALPHA;
        }

        /**
         * Determine if the current token is a numeric part.
         *
         * @return {@code true} if the token is a numeric part, {@code false} otherwise
         */
        public boolean isNumberPart() {
            return cookieToToken(cookie) == TOK_PART_NUMBER;
        }

        /**
         * Get the current alphabetical part.  If the iterator is not positioned on an alphabetical part (i.e.
         * {@link #isAlphaPart()} returns {@code false}), then an exception is thrown.
         *
         * @return the current alphabetical part
         * @throws IllegalStateException if the current token is not an alphabetical part
         */
        public String getAlphaPart() {
            final long cookie = this.cookie;
            if (cookieToToken(cookie) != TOK_PART_ALPHA) {
                throw new IllegalStateException();
            }
            return version.substring(cookieToStartIndex(cookie), cookieToEndIndex(cookie));
        }

        /**
         * Get the current numeric part, as a {@code String}.  If the iterator is not positioned on a numeric
         * part (i.e. {@link #isNumberPart()} returns {@code false}), then an exception is thrown.
         *
         * @return the current numeric part as a {@code String}
         * @throws IllegalStateException if the current token is not a numeric part
         */
        public String getNumberPartAsString() {
            final long cookie = this.cookie;
            if (cookieToToken(cookie) != TOK_PART_NUMBER) {
                throw new IllegalStateException();
            }
            return version.substring(cookieToStartIndex(cookie), cookieToEndIndex(cookie));
        }

        /**
         * Get the current numeric part, as a {@code long}.  If the iterator is not positioned on a numeric
         * part (i.e. {@link #isNumberPart()} returns {@code false}), then an exception is thrown.  If the value
         * overflows the maximum value for a {@code long}, then only the low-order 64 bits of the version number
         * value are returned.
         *
         * @return the current numeric part as a {@code long}
         * @throws IllegalStateException if the current token is not a numeric part
         */
        public long getNumberPartAsLong() {
            final long cookie = this.cookie;
            if (cookieToToken(cookie) != TOK_PART_NUMBER) {
                throw new IllegalStateException();
            }
            long total = 0L;
            final int start = cookieToStartIndex(cookie);
            final int end = cookieToEndIndex(cookie);
            for (int i = start; i < end; i = version.offsetByCodePoints(i, 1)) {
                total = total * 10 + Character.digit(version.codePointAt(i), 10);
            }
            return total;
        }

        /**
         * Get the current numeric part, as an {@code int}.  If the iterator is not positioned on a numeric
         * part (i.e. {@link #isNumberPart()} returns {@code false}), then an exception is thrown.  If the value
         * overflows the maximum value for an {@code int}, then only the low-order 32 bits of the version number
         * value are returned.
         *
         * @return the current numeric part as an {@code int}
         * @throws IllegalStateException if the current token is not a numeric part
         */
        public int getNumberPartAsInt() {
            final long cookie = this.cookie;
            if (cookieToToken(cookie) != TOK_PART_NUMBER) {
                throw new IllegalStateException();
            }
            int total = 0;
            final int start = cookieToStartIndex(cookie);
            final int end = cookieToEndIndex(cookie);
            for (int i = start; i < end; i = version.offsetByCodePoints(i, 1)) {
                total = total * 10 + Character.digit(version.codePointAt(i), 10);
            }
            return total;
        }

        /**
         * Get the current numeric part, as a {@code BigInteger}.  If the iterator is not positioned on a numeric
         * part (i.e. {@link #isNumberPart()} returns {@code false}), then an exception is thrown.
         *
         * @return the current numeric part as a {@code BigInteger}
         * @throws IllegalStateException if the current token is not a numeric part
         */
        public BigInteger getNumberPartAsBigInteger() {
            final long cookie = this.cookie;
            if (cookieToToken(cookie) != TOK_PART_NUMBER) {
                throw new IllegalStateException();
            }
            return new BigInteger(version.substring(cookieToStartIndex(cookie), cookieToEndIndex(cookie)));
        }
    }

    Object writeReplace() {
        return new Serialized(toString());
    }

    static final class Serialized implements Serializable {
        private static final long serialVersionUID = - 8720461103628158977L;
        private final String string;

        Serialized(final String string) {
            this.string = string;
        }

        Object readResolve() {
            return new Version(string);
        }
    }
}
