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

import org.jboss.modules.filter.PathFilter;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.Normalizer;
import java.util.*;

import static java.lang.Math.max;

/**
 * General helpful path utility methods.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class PathUtils {

    static final int MIN_LENGTH = 256;
    static final ThreadLocal<char[]> CHAR_BUFFER_CACHE = new ThreadLocal<>();

    private PathUtils() {
    }

    /**
     * Filter the paths from {@code source} into {@code target} using {@code filter}.
     *
     * @param source the source paths
     * @param filter the filter to apply
     * @param target the destination for filtered paths
     * @param <T> the collection type
     * @return the {@code target} set
     */
    public static <T extends Collection<? super String>> T filterPaths(Iterable<String> source, PathFilter filter, T target) {
        for (String path : source) {
            if (filter.accept(path)) {
                target.add(path);
            }
        }
        return target;
    }

    /**
     * Attempt to get a set of all paths defined directly by the given class loader.  If the path set cannot be
     * ascertained, {@code null} is returned.
     *
     * @param classLoader the class loader to inspect
     * @return the set, or {@code null} if the paths could not be determined
     */
    public static Set<String> getPathSet(ClassLoader classLoader) {
        if (classLoader == null) {
            return JDKPaths.JDK;
        } else if (classLoader instanceof ModuleClassLoader) {
            final ModuleClassLoader moduleClassLoader = (ModuleClassLoader) classLoader;
            return Collections.unmodifiableSet(moduleClassLoader.getPaths());
        } else if (classLoader instanceof URLClassLoader) {
            // here's where it starts to get ugly...
            final URLClassLoader urlClassLoader = (URLClassLoader) classLoader;
            final URL[] urls = urlClassLoader.getURLs();
            final Set<String> paths = new HashSet<String>();
            for (URL url : urls) {
                final URI uri;
                try {
                    uri = url.toURI();
                } catch (URISyntaxException e) {
                    return null;
                }
                final String scheme = uri.getScheme();
                if ("file".equals(scheme)) {
                    final File file;
                    try {
                        file = new File(uri);
                    } catch (Exception e) {
                        return null;
                    }
                    if (file.exists()) {
                        if (file.isDirectory()) {
                            JDKPaths.processDirectory(paths, file);
                        } else {
                            try {
                                JDKPaths.processJar(paths, file);
                            } catch (IOException e) {
                                return null;
                            }
                        }
                    }
                }
            }
            return Collections.unmodifiableSet(paths);
        } else {
            // ???
            return null;
        }
    }

    /**
     * Relativize the given path.  Removes any leading {@code /} segments from the path.
     *
     * @param path the path to relativize
     * @return the relative path
     */
    public static String relativize(String path) {
        for (int i = 0; i < path.length(); i ++) {
            if (path.charAt(i) != '/' && path.charAt(i) != File.separatorChar) {
                return path.substring(i);
            }
        }
        return "";
    }

    /**
     * Get the file name (last) segment of the given path.
     *
     * @param path the path name
     * @return the file name
     */
    public static String fileNameOfPath(String path) {
        return path.substring(max(path.lastIndexOf('/'), path.lastIndexOf(File.separatorChar)) + 1);
    }

    /**
     * Request a new buffer at or longer than the given length. The minimum length is
     * provided at the class level to make sure that the buffer will always exceed the minimum length.
     *
     * If a size requested is greater than the minimum length it will go to the next power of two up
     * to the maximum integer value. This is intended to prevent an ascending series of paths ("/a/a.txt",
     * "/a/aa.txt", "/a/aaa.txt") from requiring constant/increasing buffer allocations.
     *
     * @param length of the buffer to create
     * @return a created character buffer that may not be empty but that is at least as long as the requeted length
     */
    static char[] getBuffer(final int length) {
        char[] current = CHAR_BUFFER_CACHE.get();
        if(current == null || current.length < length) {
            // for any value less than the minimum length use the minimum length
            // for any value greater subtract one, then shift only the highest bit left one
            // (this is the next greater power of two unless already a power of two where it will be the same)
            final int nextLength = length <= MIN_LENGTH ? MIN_LENGTH : Integer.highestOneBit(length - 1) << 1;
            current = new char[nextLength];
            CHAR_BUFFER_CACHE.set(current);
        }
        return current;
    }

    /**
     * Package private implementation for testing/benchmarking. Provides
     * the same interface as canonicalize without any sort of filtering.
     *
     * @param path the relative or absolute possibly non-canonical path
     * @return the canonical path
     */
    public static String canonicalize(String path) {
        final int length = path.length();
        if (length == 0) {
            return path;
        }

        // 0 - start
        // 1 - got one .
        // 2 - got two .
        // 3 - got /
        int state = 0;

        char[] targetBuf = null;
        // string segment end exclusive
        int e = length;
        // string cursor position
        int i = length;
        // buffer cursor position
        int a = length - 1;
        // number of segments to skip
        int skip = 0;
        loop: while (--i >= 0) {
            char c = path.charAt(i);
            outer: switch (c) {
                case '/': {
                    inner: switch (state) {
                        case 0: state = 3; e = i; break outer;
                        case 1: state = 3; e = i; break outer;
                        case 2: state = 3; e = i; skip ++; break outer;
                        case 3: e = i; break outer;
                        default: throw new IllegalStateException();
                    }
                    // not reached!
                }
                case '.': {
                    inner: switch (state) {
                        case 0: state = 1; break outer;
                        case 1: state = 2; break outer;
                        case 2: break inner; // emit!
                        case 3: state = 1; break outer;
                        default: throw new IllegalStateException();
                    }
                    // fall thru
                }
                default: {
                    if (File.separatorChar != '/' && c == File.separatorChar) {
                        switch (state) {
                            case 0: state = 3; e = i; break outer;
                            case 1: state = 3; e = i; break outer;
                            case 2: state = 3; e = i; skip ++; break outer;
                            case 3: e = i; break outer;
                            default: throw new IllegalStateException();
                        }
                        // not reached!
                    }

                    // look for the next potential boundary point
                    int newE = e > 0 ? path.lastIndexOf('/', e - 1) : -1;

                    if (skip > 0) {
                        skip--;
                    } else {
                        // this is a slight optimization that reduces the number of copies for strings that are
                        // checking paths that need fewer modifications but it can only be done when no skipping
                        // needs to be performed and it leads to an earlier "out" / allows longer strings to get
                        // an early out on the zero-allocation path
                        do {
                            if (newE < 1) {
                                break;
                            }
                            final char peek = path.charAt(newE - 1);
                            if ('/' != peek && '.' != peek) { // if the next character is just a normal character we can continue searching
                                newE = path.lastIndexOf('/', newE - 1);
                            } else { // otherwise we need to break out and do the original logic
                                break;
                            }
                        } while (newE != -1);

                        // create segment length for the region that will be copied, which should be from the segment end
                        // to wherever we found a boundary that cannot be crossed
                        final int segmentLength = e - newE - 1;

                        // at this point we are attempting to copy the entire string so just return which avoids any sort
                        // of allocation for non-modified strings
                        if (segmentLength == length) {
                            return path;
                        }

                        if (segmentLength != 0) {  // not sure if this realistically skips anything
                            if (targetBuf == null) {
                                targetBuf = getBuffer(length);
                            }
                            if (state == 3) {
                                targetBuf[a--] = '/';
                            }
                            path.getChars(newE + 1, e, targetBuf, (a -= segmentLength) + 1);
                        }
                    }
                    state = 0;
                    i = newE + 1;
                    e = newE;
                    break;
                }
            }
        }
        // if we exit on a state of 3 then the path is absolute
        if (state == 3) {
            // if the string is _only_ the absolute character
            // do not allocate a new string
            if (length - a - 1 == 1) {
                return "/";
            }
            // we should not get here because if we do it somehow means we
            // have an empty string but nothing in it that we are going to
            // prepend to (remove?)
            if (targetBuf == null) {
                targetBuf = getBuffer(length);
            }
            // prepend the absolute start and also move the starting position of
            // the buffer to the left by one.
            targetBuf[a--] = '/';
        }
        // if the string has been cut down to a length of 0 then
        // do not allocate anything and return an empty string
        if (length - a - 1 == 0) {
            return "";
        }
        // no change was made so return the original path
        if (targetBuf == null) {
            return path;
        }
        return new String(targetBuf, a + 1, length - a - 1);
    }

    /**
     * Determine whether one path is a child of another.
     *
     * @param parent the parent path
     * @param child the child path
     * @return {@code true} if the child is truly a child of parent
     */
    public static boolean isChild(final String parent, final String child) {
        String cp = canonicalize(parent);
        cp = cp.endsWith("/") ? cp.substring(0, cp.length() - 1) : cp;
        String cc = canonicalize(child);
        if (isRelative(cp) != isRelative(cc)) {
            throw new IllegalArgumentException("Cannot compare relative and absolute paths");
        }
        final int cpl = cp.length();
        return cpl == 0 || cc.length() > cpl + 1 && cc.startsWith(cp) && cc.charAt(cpl) == '/';
    }

    /**
     * Determine whether one path is a direct (or immediate) child of another.
     *
     * @param parent the parent path
     * @param child the child path
     * @return {@code true} if the child is truly a direct child of parent
     */
    public static boolean isDirectChild(final String parent, final String child) {
        String cp = canonicalize(parent);
        cp = cp.endsWith("/") ? cp.substring(0, cp.length() - 1) : cp;
        String cc = canonicalize(child);
        if (isRelative(cp) != isRelative(cc)) {
            throw new IllegalArgumentException("Cannot compare relative and absolute paths");
        }
        final int cpl = cp.length();
        if (cpl == 0) {
            return cc.indexOf('/') < 0;
        } else {
            return cc.length() > cpl + 1 && cc.startsWith(cp) && cc.charAt(cpl) == '/' && cc.indexOf('/', cpl + 1) == -1;
        }
    }

    /**
     * Get the given path name with OS-specific separators replaced with the generic {@code /} separator character.
     *
     * @param original the original string
     * @return the same string with OS-specific separators replaced with {@code /}
     */
    public static String toGenericSeparators(String original) {
        return File.separatorChar == '/' ? original : original.replace(File.separatorChar, '/');
    }

    /**
     * Determine whether a path name is relative.
     *
     * @param path the path name
     * @return {@code true} if it is relative
     */
    public static boolean isRelative(final String path) {
        return path.isEmpty() || !isSeparator(path.charAt(0));
    }

    /**
     * Determine whether the given character is a {@code /} or a platform-specific separator.
     *
     * @param ch the character to test
     * @return {@code true} if it is a separator
     */
    public static boolean isSeparator(final char ch) {
        // the second half of this compare will optimize away on / OSes
        return ch == '/' || File.separatorChar != '/' && ch == File.separatorChar;
    }

    private static boolean isAllowedPunct(final int cp) {
        return cp == '_' || cp == '$' || cp == '%' || cp == '^'
            || cp == '&' || cp == '(' || cp == ')' || cp == '-'
            || cp == '+' || cp == '=' || cp == ';' || cp == '['
            || cp == ']' || cp == '{' || cp == '}' || cp == '<'
            || cp == '>' || cp == ',' || cp == '"' || cp == '\'';
    }

    /**
     * Convert a "basic" module name to a relative path specification.
     *
     * @param moduleName the basic module name
     * @return the path specification, or {@code null} if the name is not valid
     */
    public static String basicModuleNameToPath(final String moduleName) {
        final String normalized = Normalizer.normalize(moduleName, Normalizer.Form.NFKC);
        final int length = normalized.length();
        StringBuilder builder = new StringBuilder(length + 5);
        boolean slot = false; // are we in the slot part?
        boolean sep = false; // is a '.' or ':' (to '/') separator allowed here?
        boolean dot = false; // is a '/' (to '.') separator allowed here?
        boolean esc = false; // was a '\\' previously given?
        for (int i = 0; i < length; i = normalized.offsetByCodePoints(i, 1)) {
            int cp = normalized.codePointAt(i);
            if (Character.isLetterOrDigit(cp) || isAllowedPunct(cp)) {
                builder.appendCodePoint(cp);
                esc = false;
                sep = true;
                dot = true;
            } else if (cp == '\\') {
                if (esc) return null;
                esc = true;
            } else if (cp == ':') {
                if (esc) {
                    builder.append(':');
                    esc = false;
                    sep = true;
                    dot = true;
                } else if (slot) {
                    builder.append(':');
                    esc = false;
                    sep = true;
                    dot = true;
                } else if (! sep) {
                    return null;
                } else {
                    builder.append('/');
                    slot = true;
                    sep = false;
                    dot = false;
                }
            } else if (cp == '.') {
                if (slot) {
                    if (! dot) {
                        return null;
                    }
                    builder.append('.');
                    sep = true;
                    esc = false;
                } else {
                    if (! sep) {
                        return null;
                    } else {
                        builder.append('/');
                        sep = false;
                        dot = false;
                        esc = false;
                    }
                }
            } else if (cp == '/') {
                if (! dot) {
                    return null;
                }
                builder.append('.');
                sep = true;
                dot = false;
                esc = false;
            } else {
                return null;
            }
        }
        if (! dot || ! sep || esc) {
            // invalid end
            return null;
        }
        if (! slot) {
            builder.append("/main");
        }
        return builder.toString();
    }

    /**
     * Takes a list of maps an returns an immutable representation of the same map, with all duplicate lists
     * coalesced into a single object, and with all lists replaced by immutable array lists that has a backing array
     * that is the correct size for the number of contents.
     *
     * This can result in a significant memory saving for some use cases
     *
     */
    static <T> Map<String, List<T>> deduplicateLists(Map<String, List<T>> allPaths) {
        if (allPaths == null) {
            return null;
        } else if (allPaths.size() == 0) {
            return Collections.emptyMap();
        } else {
            Map<String, List<T>> newPaths = new HashMap<>();
            Map<List<T>, List<T>> dedup = new HashMap<>();
            for (Map.Entry<String, List<T>> e : allPaths.entrySet()) {
                List<T> l = dedup.get(e.getValue());
                if (l == null) {
                    dedup.put(e.getValue(), l = Collections.unmodifiableList(new ArrayList<>(e.getValue())));
                }
                newPaths.put(e.getKey(), l);
            }
            return Collections.unmodifiableMap(newPaths);
        }
    }
}
