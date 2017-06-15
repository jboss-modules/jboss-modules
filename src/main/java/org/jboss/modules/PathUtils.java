/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.modules;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.modules.filter.PathFilter;

/**
 * General helpful path utility methods.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class PathUtils {

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
                            JDKPaths.processDirectory0(paths, file);
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
                return i == 0 ? path : path.substring(i);
            }
        }
        return "";
    }

    /**
     * Canonicalize the given path.  Removes all {@code .} and {@code ..} segments from the path.
     *
     * @param path the relative or absolute possibly non-canonical path
     * @return the canonical path
     */
    public static String canonicalize(String path) {
        final int length = path.length();
        // 0 - start
        // 1 - got one .
        // 2 - got two .
        // 3 - got /
        int state = 0;
        if (length == 0) {
            return path;
        }
        final char[] targetBuf = new char[length];
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
                    final int newE = e > 0 ? path.lastIndexOf('/', e - 1) : -1;
                    final int segmentLength = e - newE - 1;
                    if (skip > 0) {
                        skip--;
                    } else {
                        if (state == 3) {
                            targetBuf[a--] = '/';
                        }
                        path.getChars(newE + 1, e, targetBuf, (a -= segmentLength) + 1);
                    }
                    state = 0;
                    i = newE + 1;
                    e = newE;
                    break;
                }
            }
        }
        if (state == 3) {
            targetBuf[a--] = '/';
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
