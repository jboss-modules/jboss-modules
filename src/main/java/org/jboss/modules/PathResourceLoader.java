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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

/**
 * Java NIO2 Path-based ResourceLoader
 *
 * @author <a href="mailto:bsideup@gmail.com">Sergei Egorov</a>
 */
class PathResourceLoader extends AbstractResourceLoader implements IterableResourceLoader {

    private final String rootName;
    protected final Path root;
    protected final AccessControlContext context;

    private final Manifest manifest;
    private final CodeSource codeSource;

    PathResourceLoader(final String rootName, final Path root, final AccessControlContext context) {
        if (rootName == null) {
            throw new IllegalArgumentException("rootName is null");
        }
        if (root == null) {
            throw new IllegalArgumentException("root is null");
        }
        if (context == null) {
            throw new IllegalArgumentException("context is null");
        }
        this.rootName = rootName;
        this.root = root;
        this.context = context;
        final Path manifestFile = root.resolve("META-INF").resolve("MANIFEST.MF");
        manifest = readManifestFile(manifestFile);

        try {
            codeSource = doPrivilegedIfNeeded(context, MalformedURLException.class, () -> new CodeSource(root.toUri().toURL(), (CodeSigner[]) null));
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid root file specified", e);
        }
    }

    private Manifest readManifestFile(final Path manifestFile) {
        try {
            return doPrivilegedIfNeeded(context, IOException.class, () -> {
                if (Files.isDirectory(manifestFile)) {
                    return null;
                }

                try (InputStream is = Files.newInputStream(manifestFile)) {
                    return new Manifest(is);
                }
            });
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public String getRootName() {
        return rootName;
    }

    @Override
    public String getLibrary(String name) {
        if (root.getFileSystem() == FileSystems.getDefault()) {
            final String mappedName = System.mapLibraryName(name);
            for (String path : NativeLibraryResourceLoader.Identification.NATIVE_SEARCH_PATHS) {
                Path testFile;
                try {
                    testFile = root.resolve(path).resolve(mappedName);
                } catch (InvalidPathException ignored) {
                    return null;
                }
                if (Files.exists(testFile)) {
                    return testFile.toAbsolutePath().toString();
                }
            }
        }
        return null;
    }

    @Override
    public ClassSpec getClassSpec(final String fileName) throws IOException {
        final Path file;
        try {
            file = root.resolve(fileName);
        } catch (InvalidPathException ignored) {
            return null;
        }

        return doPrivilegedIfNeeded(context, IOException.class, () -> {
            if (!Files.exists(file)) {
                return null;
            }
            final ClassSpec spec = new ClassSpec();
            spec.setCodeSource(codeSource);
            spec.setBytes(Files.readAllBytes(file));
            return spec;
        });
    }

    @Override
    public PackageSpec getPackageSpec(final String name) throws IOException {
        URL rootUrl = doPrivilegedIfNeeded(context, IOException.class, () -> root.toUri().toURL());
        return getPackageSpec(name, manifest, rootUrl);
    }

    @Override
    public Resource getResource(final String name) {
        final String cleanName = PathUtils.canonicalize(PathUtils.relativize(name));
        final Path file;
        try {
            file = root.resolve(cleanName);
        } catch (InvalidPathException ignored) {
            return null;
        }

        if (!doPrivilegedIfNeeded(context, () -> Files.exists(file))) {
            return null;
        }

        return new PathResource(file, cleanName, context);
    }

    @Override
    public Iterator<Resource> iterateResources(final String startPath, final boolean recursive) {
        try {
            Path path;
            try {
                path = root.resolve(PathUtils.canonicalize(PathUtils.relativize(startPath)));
            } catch (InvalidPathException ignored) {
                return Collections.emptyIterator();
            }
            return Files.walk(path, recursive ? Integer.MAX_VALUE : 1, FileVisitOption.FOLLOW_LINKS)
                    .filter(it -> !Files.isDirectory(it))
                    .<Resource>map(resourcePath -> new PathResource(resourcePath, PathUtils.toGenericSeparators(root.relativize(resourcePath).toString()), context))
                    .iterator();
        } catch (IOException e) {
            return Collections.emptyIterator();
        }
    }

    @Override
    public Collection<String> getPaths() {
        try {
            return doPrivilegedIfNeeded(context, IOException.class, () -> Files.walk(root, FileVisitOption.FOLLOW_LINKS)
                    .filter(Files::isDirectory)
                    .map(dir -> {
                        final String result = root.relativize(dir).toString();
                        final String canonical = PathUtils.toGenericSeparators(result);

                        // JBoss modules expect folders not to end with a slash, so we have to strip it.
                        if (canonical.endsWith("/")) {
                            return canonical.substring(0, canonical.length() - 1);
                        } else {
                            return canonical;
                        }
                    })
                    .collect(Collectors.toList()));
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    @Override
    public URI getLocation() {
        return doPrivilegedIfNeeded(context, root::toUri);
    }

    public ResourceLoader createSubloader(final String relativePath, final String rootName) {
        return new PathResourceLoader(rootName, root.resolve(PathUtils.relativize(PathUtils.canonicalize(relativePath))), context);
    }

    static <T, E extends Throwable> T doPrivilegedIfNeeded(AccessControlContext context, Class<E> exceptionType, PrivilegedExceptionAction<T> action) throws E {
        SecurityManager sm = System.getSecurityManager();

        if (sm == null) {
            try {
                return action.run();
            } catch (RuntimeException re) {
                throw re;
            } catch (Exception e) {
                if (exceptionType.isInstance(e)) {
                    throw exceptionType.cast(e);
                }
                throw new UndeclaredThrowableException(e);
            }
        } else {
            try {
                return AccessController.doPrivileged(action, context);
            } catch (PrivilegedActionException e) {
                try {
                    throw e.getException();
                } catch (RuntimeException re) {
                    throw re;
                } catch (Exception e1) {
                    if (exceptionType.isInstance(e1)) {
                        throw exceptionType.cast(e1);
                    }
                    throw new UndeclaredThrowableException(e1);
                }
            }
        }
    }

    static <T> T doPrivilegedIfNeeded(AccessControlContext context, PrivilegedExceptionAction<T> action) {
        SecurityManager sm = System.getSecurityManager();

        if (sm == null) {
            try {
                return action.run();
            } catch (RuntimeException re) {
                throw re;
            } catch (Exception e) {
                throw new UndeclaredThrowableException(e);
            }
        } else {
            try {
                return AccessController.doPrivileged(action, context);
            } catch (PrivilegedActionException e) {
                try {
                    throw e.getException();
                } catch (RuntimeException re) {
                    throw re;
                } catch (Exception e1) {
                    throw new UndeclaredThrowableException(e1);
                }
            }
        }
    }
}
