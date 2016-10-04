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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitor;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.AccessController;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;

/**
 * A utility class which maintains the set of JDK paths.  Makes certain assumptions about the disposition of the
 * class loader used to load JBoss Modules; thus this class should only be used when booted up via the "-jar" or "-cp"
 * switches.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class JDKPaths {
    static final Set<String> JDK;
    static final boolean isJDK9orAbove;
    private static final String JDK9_CLASS = "java.lang.reflect.Module";

    static {
        Class<?> moduleClass = null;
        try {
            moduleClass = JDKPaths.class.getClassLoader().loadClass(JDK9_CLASS);
        } catch (final Throwable ignored) {}
        isJDK9orAbove = moduleClass != null;
        final Set<String> pathSet;
        final Set<String> jarSet = new FastCopyHashSet<>(1024);
        if (isJDK9orAbove) {
            // TODO: Remove this to the JDK9 supplement
            pathSet = new FastCopyHashSet<>(1024);
            processRuntimeImages(pathSet);
            final String javaClassPath = AccessController.doPrivileged(new PropertyReadAction("java.class.path"));
            processClassPathItem(javaClassPath, jarSet, pathSet);
            pathSet.add("org/jboss/modules");
            pathSet.add("org/jboss/modules/filter");
            pathSet.add("org/jboss/modules/log");
            pathSet.add("org/jboss/modules/management");
            pathSet.add("org/jboss/modules/ref");
        } else {
            pathSet = JDKSpecific.getJDKPaths();
        }
        if (pathSet.size() == 0) throw new IllegalStateException("Something went wrong with system paths set up");
        JDK = Collections.unmodifiableSet(pathSet);
    }

    private JDKPaths() {
    }

    private static void processRuntimeImages(final Set<String> jarSet) {
        try {
            for (final Path root : FileSystems.getFileSystem(new URI("jrt:/")).getRootDirectories()) {
                Files.walkFileTree(root, new JrtFileVisitor(jarSet));
            }
        } catch (final URISyntaxException|IOException e) {
            throw new IllegalStateException("Unable to process java runtime images");
        }
    }

    private static class JrtFileVisitor implements FileVisitor<Path> {

        private final String SLASH = "/";
        private final String PACKAGES = "/packages";
        private final Set<String> jarSet;

        private JrtFileVisitor(final Set<String> jarSet) {
            this.jarSet = jarSet;
        }

        @Override
        public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
            final String d = dir.toString();
            return d.equals(SLASH) || d.startsWith(PACKAGES) ? CONTINUE : SKIP_SUBTREE;
        }

        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
            String f = file.toString();
            String packageName = f.substring(PACKAGES.length() + 1, f.lastIndexOf(SLASH)).replace('.', '/');
            jarSet.add(packageName);
            return CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(final Path file, final IOException exc) throws IOException {
            return CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
            return CONTINUE;
        }
    }

    private static void processClassPathItem(final String classPath, final Set<String> jarSet, final Set<String> pathSet) {
        if (classPath == null) return;
        int s = 0, e;
        do {
            e = classPath.indexOf(File.pathSeparatorChar, s);
            String item = e == -1 ? classPath.substring(s) : classPath.substring(s, e);
            if (! jarSet.contains(item)) {
                final File file = new File(item);
                if (file.isDirectory()) {
                    processDirectory0(pathSet, file);
                } else {
                    try {
                        processJar(pathSet, file);
                    } catch (IOException ex) {
                        // ignore
                    }
                }
            }
            s = e + 1;
        } while (e != -1);
    }

    static void processJar(final Set<String> pathSet, final File file) throws IOException {
        final ZipFile zipFile = new ZipFile(file);
        try {
            final Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                final ZipEntry entry = entries.nextElement();
                final String name = entry.getName();
                final int lastSlash = name.lastIndexOf('/');
                if (lastSlash != -1) {
                    pathSet.add(name.substring(0, lastSlash));
                }
            }
            zipFile.close();
        } finally {
            StreamUtil.safeClose(zipFile);
        }
    }

    static void processDirectory0(final Set<String> pathSet, final File file) {
        for (File entry : file.listFiles()) {
            if (entry.isDirectory()) {
                processDirectory1(pathSet, entry, file.getPath());
            } else {
                final String parent = entry.getParent();
                if (parent != null) pathSet.add(parent);
            }
        }
    }

    private static void processDirectory1(final Set<String> pathSet, final File file, final String pathBase) {
        for (File entry : file.listFiles()) {
            if (entry.isDirectory()) {
                processDirectory1(pathSet, entry, pathBase);
            } else {
                String packagePath = entry.getParent();
                if (packagePath != null) {
                    packagePath = packagePath.substring(pathBase.length()).replace('\\', '/');
                    if(packagePath.startsWith("/")) {
                        packagePath = packagePath.substring(1);
                    }
                    pathSet.add(packagePath);
                }
            }
        }
    }
}
