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

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;
import static java.security.AccessController.doPrivileged;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.module.ModuleDescriptor;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import java.util.jar.JarFile;

/**
 * JDK-specific classes which are replaced for different JDK major versions.  This one is for Java 9 only.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class JDKSpecific {

    // === private fields and data ===

    static final Set<String> MODULES_PACKAGES = new HashSet<>(Arrays.asList(
        "org/jboss/modules",
        "org/jboss/modules/filter",
        "org/jboss/modules/log",
        "org/jboss/modules/management",
        "org/jboss/modules/ref"
    ));

    static final StackWalker STACK_WALKER = doPrivileged((PrivilegedAction<StackWalker>) () -> StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE));

    static final ClassLoader SYSTEM_CLASS_LOADER = doPrivileged((PrivilegedAction<ClassLoader>) ClassLoader::getSystemClassLoader);
    static final ClassLoader OUR_CLASS_LOADER = JDKSpecific.class.getClassLoader();

    // === the actual JDK-specific API ===

    static JarFile getJarFile(final File name, final boolean verify) throws IOException {
        return new JarFile(name, verify, JarFile.OPEN_READ, JarFile.runtimeVersion());
    }

    static Class<?> getCallingUserClass() {
        return STACK_WALKER.walk(stream -> stream.skip(1)
                .filter(s -> !s.getDeclaringClass().equals(org.jboss.modules.Module.class))
                .findFirst().get().getDeclaringClass());
    }

    static Class<?> getCallingClass() {
        return STACK_WALKER.walk(JDKSpecific::processFrame);
    }

    static boolean isParallelCapable(ConcurrentClassLoader cl) {
        return cl.isRegisteredAsParallelCapable();
    }

    static Package getPackage(ClassLoader cl, String packageName) {
        return cl.getDefinedPackage(packageName);
    }

    static Set<String> getJDKPaths() {
        Set<String> pathSet = new FastCopyHashSet<>(1024);
        processRuntimeImages(pathSet);
        // TODO: Remove this stuff once jboss-modules is itself a module
        final String javaClassPath = AccessController.doPrivileged(new PropertyReadAction("java.class.path"));
        JDKPaths.processClassPathItem(javaClassPath, new FastCopyHashSet<>(1024), pathSet);
        pathSet.addAll(MODULES_PACKAGES);
        return pathSet;
    }

    static LocalLoader getSystemLocalLoader() {
        return new LocalLoader() {
            public Class<?> loadClassLocal(final String name, final boolean resolve) {
                try {
                    return Class.forName(name, resolve, getPlatformClassLoader());
                } catch (ClassNotFoundException ignored) {
                    try {
                        return Class.forName(name, resolve, OUR_CLASS_LOADER);
                    } catch (ClassNotFoundException e) {
                        return null;
                    }
                }
            }

            public Package loadPackageLocal(final String name) {
                final Package pkg = getPackage(getPlatformClassLoader(), name);
                return pkg != null ? pkg : getPackage(OUR_CLASS_LOADER, name);
            }

            public List<Resource> loadResourceLocal(final String name) {
                final Enumeration<URL> urls;
                try {
                    urls = getSystemResources(name);
                } catch (IOException e) {
                    return Collections.emptyList();
                }
                final List<Resource> list = new ArrayList<Resource>();
                while (urls.hasMoreElements()) {
                    final URL url = urls.nextElement();
                    URLConnection connection = null;
                    try {
                        connection = doPrivileged(new GetURLConnectionAction(url));
                    } catch (PrivilegedActionException e) {
                        try {
                            throw e.getException();
                        } catch (IOException e2) {
                            // omit from list
                        } catch (RuntimeException re) {
                            throw re;
                        } catch (Exception e2) {
                            throw new UndeclaredThrowableException(e2);
                        }
                    }
                    list.add(new URLConnectionResource(connection));
                }
                return list;
            }
        };
    }

    static ClassLoader getPlatformClassLoader() {
        return SYSTEM_CLASS_LOADER;
    }

    static URL getSystemResource(final String name) {
        final URL resource = getPlatformClassLoader().getResource(name);
        return resource != null ? resource : OUR_CLASS_LOADER != null ? OUR_CLASS_LOADER.getResource(name) : ClassLoader.getSystemResource(name);
    }

    static Enumeration<URL> getSystemResources(final String name) throws IOException {
        final Enumeration<URL> resources = getPlatformClassLoader().getResources(name);
        return resources != null && resources.hasMoreElements() ? resources : OUR_CLASS_LOADER.getResources(name);
    }

    static InputStream getSystemResourceAsStream(final String name) {
        final InputStream stream = getPlatformClassLoader().getResourceAsStream(name);
        return stream != null ? stream : OUR_CLASS_LOADER != null ? OUR_CLASS_LOADER.getResourceAsStream(name) : ClassLoader.getSystemResourceAsStream(name);
    }

    static Class<?> getSystemClass(final ConcurrentClassLoader caller, final String className) throws ClassNotFoundException {
        try {
            return getPlatformClassLoader().loadClass(className);
        } catch (ClassNotFoundException ignored) {
            return OUR_CLASS_LOADER != null ? OUR_CLASS_LOADER.loadClass(className) : caller.findSystemClassInternal(className);
        }
    }

    static void addInternalPackages(final List<String> list) {
        // none in Java 9+
    }

    static String getJdkModuleNameOf(final Class<?> clazz) {
        final java.lang.Module module = clazz.getModule();
        return module.isNamed() ? module.getName() : null;
    }

    static String getJdkModuleVersionOf(final Class<?> clazz) {
        final java.lang.Module module = clazz.getModule();
        final ModuleDescriptor.Version version = module.isNamed() ? module.getDescriptor().version().orElse(null) : null;
        return version == null ? null : version.toString();
    }

    // === nested util stuff, non-API ===

    private static Class<?> processFrame(Stream<StackWalker.StackFrame> stream) {
        final Iterator<StackWalker.StackFrame> iterator = stream.iterator();
        if (! iterator.hasNext()) return null;
        iterator.next();
        if (! iterator.hasNext()) return null;
        Class<?> testClass = iterator.next().getDeclaringClass();
        while (iterator.hasNext()) {
            final Class<?> item = iterator.next().getDeclaringClass();
            if (testClass != item) {
                return item;
            }
        }
        return null;
    }

    private static void processRuntimeImages(final Set<String> pathSet) {
        try {
            for (final Path root : FileSystems.getFileSystem(new URI("jrt:/")).getRootDirectories()) {
                Files.walkFileTree(root, new JrtFileVisitor(pathSet));
            }
        } catch (final URISyntaxException |IOException e) {
            throw new IllegalStateException("Unable to process java runtime images");
        }
    }

    private static class JrtFileVisitor implements FileVisitor<Path> {

        private static final String SLASH = "/";
        private static final String PACKAGES = "/packages";
        private final Set<String> pathSet;

        private JrtFileVisitor(final Set<String> pathSet) {
            this.pathSet = pathSet;
        }

        @Override
        public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
            final String d = dir.toString();
            return d.equals(SLASH) || d.startsWith(PACKAGES) ? CONTINUE : SKIP_SUBTREE;
        }

        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
            if (file.getNameCount() >= 3 && file.getName(0).toString().equals("packages")) {
                pathSet.add(file.getName(1).toString().replace('.', '/'));
            }
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

}
