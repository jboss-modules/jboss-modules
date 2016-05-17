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
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSigner;
import java.security.CodeSource;
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
final class PathResourceLoader extends AbstractResourceLoader implements IterableResourceLoader {

    private final String rootName;
    private final Path root;

    private final Manifest manifest;
    private final CodeSource codeSource;

    PathResourceLoader(final String rootName, final Path root) {
        if (rootName == null) {
            throw new IllegalArgumentException("rootName is null");
        }
        if (root == null) {
            throw new IllegalArgumentException("root is null");
        }
        this.rootName = rootName;
        this.root = root;
        final Path manifestFile = root.resolve("META-INF").resolve("MANIFEST.MF");
        manifest = readManifestFile(manifestFile);

        try {
            codeSource = new CodeSource(root.toUri().toURL(), (CodeSigner[]) null);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid root file specified", e);
        }
    }

    private static Manifest readManifestFile(final Path manifestFile) {
        if (Files.isDirectory(manifestFile)) {
            return null;
        }

        try {
            try (InputStream is = Files.newInputStream(manifestFile)) {
                return new Manifest(is);
            }
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public String getRootName() {
        return rootName;
    }

    @Override
    public ClassSpec getClassSpec(final String fileName) throws IOException {
        final Path file = root.resolve(fileName);
        if (!Files.exists(file)) {
            return null;
        }
        final ClassSpec spec = new ClassSpec();
        spec.setCodeSource(codeSource);
        spec.setBytes(Files.readAllBytes(file));
        return spec;
    }

    @Override
    public PackageSpec getPackageSpec(final String name) throws IOException {
        return getPackageSpec(name, manifest, root.toUri().toURL());
    }

    @Override
    public Resource getResource(final String name) {
        final Path file = root.resolve(PathUtils.canonicalize(PathUtils.relativize(name)));

        if (!Files.exists(file)) {
            return null;
        } else {
            return new PathResource(file);
        }
    }

    @Override
    public Iterator<Resource> iterateResources(final String startPath, final boolean recursive) {
        try {
            Path path = root.resolve(PathUtils.canonicalize(PathUtils.relativize(startPath)));
            return Files.walk(path, recursive ? Integer.MAX_VALUE : 1)
                    .filter(it -> !Files.isDirectory(it))
                    .map(root::relativize)
                    .<Resource>map(PathResource::new)
                    .iterator();
        } catch (IOException e) {
            return Collections.emptyIterator();
        }
    }

    @Override
    public Collection<String> getPaths() {
        try {
            final String separator = root.getFileSystem().getSeparator();
            return Files.walk(root)
                    .filter(Files::isDirectory)
                    .map(dir -> {
                        final String result = root.relativize(dir).toString();

                        // JBoss modules expect folders not to end with a slash, so we have to strip it.
                        if (result.endsWith(separator)) {
                            return result.substring(0, result.length() - separator.length());
                        } else {
                            return result;
                        }
                    })
                    .collect(Collectors.toList());
        } catch (IOException e) {
            // do nothing
            return Collections.emptyList();
        }
    }
}
