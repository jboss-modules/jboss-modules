/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jboss.modules.filter.PathFilter;
import org.jboss.modules.filter.PathFilters;

/**
 * A module finder which locates module specifications which are stored in a local module
 * repository on the filesystem, which uses {@code module.xml} descriptors.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class LocalModuleFinder implements ModuleFinder {

    private static final File[] NO_FILES = new File[0];
    private final File[] repoRoots;
    private final PathFilter pathFilter;

    public LocalModuleFinder(final File[] repoRoots, final PathFilter pathFilter) {
        this.repoRoots = repoRoots;
        this.pathFilter = pathFilter;
    }

    public LocalModuleFinder(final File[] repoRoots) {
        this(repoRoots, PathFilters.acceptAll());
    }

    /**
     * Construct a new instance, using the {@code module.path} system property or the {@code JAVA_MODULEPATH} environment variable
     * to get the list of module repository roots.
     * <p>
     * This is equivalent to a call to {@link LocalModuleFinder#LocalModuleFinder(boolean) LocalModuleFinder(true)}.
     * </p>
     */
    public LocalModuleFinder() {
        this(true);
    }

    /**
     * Construct a new instance, using the {@code module.path} system property or the {@code JAVA_MODULEPATH} environment variable
     * to get the list of module repository roots.
     *
     * @param supportLayersAndAddOns {@code true} if the identified module repository roots should be checked for
     *                               an internal structure of child "layer" and "add-on" directories that may also
     *                               be treated as module roots lower in precedence than the parent root. Any "layers"
     *                               subdirectories whose names are specified in a {@code layers.conf} file found in
     *                               the module repository root will be added in the precedence of order specified
     *                               in the {@code layers.conf} file; all "add-on" subdirectories will be added at
     *                               a lower precedence than all "layers" and with no guaranteed precedence order
     *                               between them. If {@code false} no check for "layer" and "add-on" directories
     *                               will be performed.
     *
     */
    public LocalModuleFinder(boolean supportLayersAndAddOns) {
        final String modulePathFile = System.getProperty("module.path.file");
        File[] basicRoots;
        if (modulePathFile != null) {
            basicRoots = getFileList(modulePathFile);
        } else {
            final String modulePath = System.getProperty("module.path", System.getenv("JAVA_MODULEPATH"));
            if (modulePath == null) {
                //noinspection ZeroLengthArrayAllocation
                basicRoots = NO_FILES;
            } else {
                basicRoots = getFiles(modulePath);
            }
        }
        repoRoots = supportLayersAndAddOns ? LayeredModulePathFactory.resolveLayeredModulePath(basicRoots) : basicRoots;
        pathFilter = PathFilters.acceptAll();
    }

    static File[] getFileList(final String modulePathFile) {
        final ArrayList<File> files = new ArrayList<File>();
        try {
            final FileInputStream inputStream = new FileInputStream(modulePathFile);
            try {
                final InputStreamReader reader = new InputStreamReader(inputStream, "UTF-8");
                try {
                    final BufferedReader bufferedReader = new BufferedReader(reader);
                    try {
                        String line;
                        int i;
                        while ((line = bufferedReader.readLine()) != null) {
                            i = line.indexOf('#');
                            final String trimmed = i == -1 ? line.trim() : line.substring(0, i).trim();
                            if (! trimmed.isEmpty()) {
                                expandWildCard(new File(trimmed), files);
                            }
                        }
                        bufferedReader.close();
                        reader.close();
                        inputStream.close();
                        return files.isEmpty() ? NO_FILES : files.toArray(new File[files.size()]);
                    } finally {
                        safeClose(bufferedReader);
                    }
                } finally {
                    safeClose(reader);
                }
            } finally {
                safeClose(inputStream);
            }
        } catch (IOException e) {
            return NO_FILES;
        }
    }

    private static final void safeClose(Closeable c) {
        if (c != null) try {
            c.close();
        } catch (Throwable ignored) {}
    }

    static void expandWildCard(File wildcard, ArrayList<File> target) {
        final String absolutePath = PathUtils.canonicalize(wildcard.getAbsolutePath());
        expandWildCard(absolutePath, target);
    }

    private static final String QUOTED_SEPARATOR = File.separatorChar == '\\' ? "\\\\" : File.separator;
    private static final Pattern GLOB_PATTERN = Pattern.compile("\\*|\\?|[^*?]+");
    private static final String NOT_SEPARATOR_PATTERN = "[^" + QUOTED_SEPARATOR + "]";

    private static final class PatternFileFilter implements FileFilter {
        private final Pattern pattern;

        private PatternFileFilter(String glob) {
            final StringBuilder patternBuilder = new StringBuilder();
            final Matcher matcher = GLOB_PATTERN.matcher(glob);
            String matched;
            while (matcher.find()) {
                matched = matcher.group();
                if (matched.equals("*")) {
                    patternBuilder.append(NOT_SEPARATOR_PATTERN).append('*');
                } else if (matched.equals("?")) {
                    patternBuilder.append(NOT_SEPARATOR_PATTERN);
                } else {
                    patternBuilder.append(Pattern.quote(matched));
                }
            }
            pattern = Pattern.compile(patternBuilder.toString());
        }

        public boolean accept(final File file) {
            return pattern.matcher(file.getName()).matches();
        }
    }

    private static void expandWildCard(final File parentFile, final ArrayDeque<File> segments, final ArrayList<File> target) {
        if (parentFile == null) {
            // it's a root and cannot be expanded
            if (segments.isEmpty()) {
                // last segment
                return;
            } else {
                final File segment = segments.removeFirst();
                try {
                    expandWildCard(segment, segments, target);
                } finally {
                    segments.addFirst(segment);
                }
            }
        } else {
            if (segments.isEmpty()) {
                // last segment
                target.add(parentFile);
                return;
            } else {
                final File segment = segments.removeFirst();
                try {
                    final String name = segment.getName();
                    if (name.indexOf('?') != -1 || name.indexOf('*') != -1) {
                        // wildcard
                        final File[] files = parentFile.listFiles(new PatternFileFilter(name));
                        Arrays.sort(files);
                        if (segments.isEmpty()) {
                            // last segment
                            for (File file : files) {
                                if (file.isDirectory()) {
                                    target.add(file);
                                }
                            }
                        } else {
                            for (File file : files) {
                                if (file.isDirectory()) {
                                    expandWildCard(file, segments, target);
                                }
                            }
                        }
                    } else {
                        final File file = new File(parentFile, name);
                        if (segments.isEmpty()) {
                            if (file.isDirectory()) target.add(file);
                        } else {
                            expandWildCard(file, segments, target);
                        }
                    }
                } finally {
                    segments.addFirst(segment);
                }
            }
        }
    }

    private static void expandWildCard(final String absolutePath, final ArrayList<File> target) {
        final File absoluteFile = new File(absolutePath);
        final ArrayDeque<File> segments = new ArrayDeque<File>();
        for (File f = absoluteFile; f != null; f = f.getParentFile()) {
            segments.addFirst(f);
        }
        expandWildCard(null, segments, target);
    }

    private static File[] getFiles(String modulePath) {
        final ArrayList<File> files = new ArrayList<File>();
        int i = modulePath.indexOf(File.pathSeparatorChar);
        if (i == -1) {
            expandWildCard(modulePath, files);
        } else {
            int s = 0;
            do {
                expandWildCard(modulePath.substring(s, i), files);
                s = i + 1;
                i = modulePath.indexOf(File.pathSeparatorChar, s);
            } while (i != -1);
            expandWildCard(modulePath.substring(s), files);
        }
        return files.isEmpty() ? NO_FILES : files.toArray(new File[files.size()]);
    }

    static String toPathString(ModuleIdentifier moduleIdentifier) {
        final StringBuilder builder = new StringBuilder(40);
        builder.append(moduleIdentifier.getName().replace('.', File.separatorChar));
        builder.append(File.separatorChar).append(moduleIdentifier.getSlot());
        builder.append(File.separatorChar);
        return builder.toString();
    }

    public ModuleSpec findModule(final ModuleIdentifier identifier, final ModuleLoader delegateLoader) throws ModuleLoadException {
        final String child = toPathString(identifier);
        if (pathFilter.accept(child)) {
            for (File root : repoRoots) {
                final File file = new File(root, child);
                final File moduleXml = new File(file, "module.xml");
                if (moduleXml.exists()) {
                    final ModuleSpec spec = ModuleXmlParser.parseModuleXml(identifier, file, moduleXml);
                    if (spec == null) break;
                    return spec;
                }
            }
        }
        return null;
    }

    public String toString() {
        final StringBuilder b = new StringBuilder();
        b.append("local module finder @").append(Integer.toHexString(hashCode())).append(" (roots: ");
        final int repoRootsLength = repoRoots.length;
        for (int i = 0; i < repoRootsLength; i++) {
            final File root = repoRoots[i];
            b.append(root);
            if (i != repoRootsLength - 1) {
                b.append(',');
            }
        }
        b.append(')');
        return b.toString();
    }
}
