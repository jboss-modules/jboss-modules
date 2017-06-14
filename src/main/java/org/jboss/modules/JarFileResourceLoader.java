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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Thomas.Diesler@jboss.com
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class JarFileResourceLoader extends AbstractResourceLoader implements IterableResourceLoader {
    private static final String INDEX_FILE = "META-INF/PATHS.LIST";

    private final JarFile jarFile;
    private final String rootName;
    private final URL rootUrl;
    private final String relativePath;
    private final File fileOfJar;
    private volatile List<String> directory;

    // protected by {@code this}
    private final Map<CodeSigners, CodeSource> codeSources = new HashMap<>();

    JarFileResourceLoader(final String rootName, final JarFile jarFile) {
        this(rootName, jarFile, null);
    }

    JarFileResourceLoader(final String rootName, final JarFile jarFile, final String relativePath) {
        if (jarFile == null) {
            throw new IllegalArgumentException("jarFile is null");
        }
        if (rootName == null) {
            throw new IllegalArgumentException("rootName is null");
        }
        fileOfJar = new File(jarFile.getName());
        this.jarFile = jarFile;
        this.rootName = rootName;
        String realPath = relativePath == null ? null : PathUtils.canonicalize(relativePath);
        if (realPath != null && realPath.endsWith("/")) realPath = realPath.substring(0, realPath.length() - 1);
        this.relativePath = realPath;
        try {
            rootUrl = getJarURI(fileOfJar.toURI(), realPath).toURL();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid root file specified", e);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid root file specified", e);
        }
    }

    private static URI getJarURI(final URI original, final String nestedPath) throws URISyntaxException {
        final StringBuilder b = new StringBuilder();
        b.append("file:");
        assert original.getScheme().equals("file");
        final String path = original.getPath();
        assert path != null;
        final String host = original.getHost();
        if (host != null) {
            final String userInfo = original.getRawUserInfo();
            b.append("//");
            if (userInfo != null) {
                b.append(userInfo).append('@');
            }
            b.append(host);
        }
        b.append(path).append("!/");
        if (nestedPath != null) {
            b.append(nestedPath);
        }
        return new URI("jar", b.toString(), null);
    }

    public String getRootName() {
        return rootName;
    }

    public synchronized ClassSpec getClassSpec(final String fileName) throws IOException {
        final ClassSpec spec = new ClassSpec();
        final JarEntry entry = getJarEntry(fileName);
        if (entry == null) {
            // no such entry
            return null;
        }
        final long size = entry.getSize();
        final InputStream is = jarFile.getInputStream(entry);
        try {
            if (size == -1) {
                // size unknown
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                final byte[] buf = new byte[16384];
                int res;
                while ((res = is.read(buf)) > 0) {
                    baos.write(buf, 0, res);
                }
                // done
                CodeSource codeSource = createCodeSource(entry);
                baos.close();
                is.close();
                spec.setBytes(baos.toByteArray());
                spec.setCodeSource(codeSource);
                return spec;
            } else if (size <= (long) Integer.MAX_VALUE) {
                final int castSize = (int) size;
                byte[] bytes = new byte[castSize];
                int a = 0, res;
                while ((res = is.read(bytes, a, castSize - a)) > 0) {
                    a += res;
                }
                // consume remainder so that cert check doesn't fail in case of wonky JARs
                while (is.read() != -1) {
                    //
                }
                // done
                CodeSource codeSource = createCodeSource(entry);
                is.close();
                spec.setBytes(bytes);
                spec.setCodeSource(codeSource);
                return spec;
            } else {
                throw new IOException("Resource is too large to be a valid class file");
            }
        } finally {
            StreamUtil.safeClose(is);
        }
    }

    // this MUST only be called after the input stream is fully read (see MODULES-201)
    private CodeSource createCodeSource(final JarEntry entry) {
        final CodeSigner[] entryCodeSigners = entry.getCodeSigners();
        final CodeSigners codeSigners = entryCodeSigners == null || entryCodeSigners.length == 0 ? EMPTY_CODE_SIGNERS : new CodeSigners(entryCodeSigners);
        CodeSource codeSource = codeSources.get(codeSigners);
        if (codeSource == null) {
            codeSources.put(codeSigners, codeSource = new CodeSource(rootUrl, entryCodeSigners));
        }
        return codeSource;
    }

    private JarEntry getJarEntry(final String fileName) {
        return relativePath == null ? jarFile.getJarEntry(fileName) : jarFile.getJarEntry(relativePath + "/" + fileName);
    }

    public PackageSpec getPackageSpec(final String name) throws IOException {
        final Manifest manifest;
        if (relativePath == null) {
            manifest = jarFile.getManifest();
        } else {
            JarEntry jarEntry = getJarEntry("META-INF/MANIFEST.MF");
            if (jarEntry == null) {
                manifest = null;
            } else {
                InputStream inputStream = jarFile.getInputStream(jarEntry);
                try {
                    manifest = new Manifest(inputStream);
                } finally {
                    StreamUtil.safeClose(inputStream);
                }
            }
        }
        return getPackageSpec(name, manifest, rootUrl);
    }

    public String getLibrary(final String name) {
        // JARs cannot have libraries in them
        return null;
    }

    public Resource getResource(String name) {
        try {
            final JarFile jarFile = this.jarFile;
            name = PathUtils.canonicalize(PathUtils.relativize(name));
            final JarEntry entry = getJarEntry(name);
            if (entry == null) {
                return null;
            }
            final URI uri;
            try {
                File absoluteFile = new File(jarFile.getName()).getAbsoluteFile();
                String path = absoluteFile.getPath();
                path = PathUtils.canonicalize(path);
                if (File.separatorChar != '/') {
                    // optimizes away on platforms with /
                    path = path.replace(File.separatorChar, '/');
                }
                if (PathUtils.isRelative(path)) {
                    // should not be possible, but the JDK thinks this might happen sometimes..?
                    path = "/" + path;
                }
                if (path.startsWith("//")) {
                    // UNC path URIs have loads of leading slashes
                    path = "//" + path;
                }
                uri = new URI("file", null, path, null);
            } catch (URISyntaxException x) {
                throw new IllegalStateException(x);
            }
            return new JarEntryResource(jarFile, entry.getName(), relativePath, new URL(null, getJarURI(uri, entry.getName()).toString(), (URLStreamHandler) null));
        } catch (MalformedURLException e) {
            // must be invalid...?  (todo: check this out)
            return null;
        } catch (URISyntaxException e) {
            // must be invalid...?  (todo: check this out)
            return null;
        }
    }

    public Iterator<Resource> iterateResources(String startPath, final boolean recursive) {
        if (relativePath != null) startPath = startPath.equals("") ? relativePath : relativePath + "/" + startPath;
        final String startName = PathUtils.canonicalize(PathUtils.relativize(startPath));
        List<String> directory = this.directory;
        if (directory == null) {
            synchronized (jarFile) {
                directory = this.directory;
                if (directory == null) {
                    directory = new ArrayList<>();
                    final Enumeration<JarEntry> entries = jarFile.entries();
                    while (entries.hasMoreElements()) {
                        final JarEntry jarEntry = entries.nextElement();
                        if (! jarEntry.isDirectory()) {
                            directory.add(jarEntry.getName());
                        }
                    }
                    this.directory = directory;
                }
            }
        }
        final Iterator<String> iterator = directory.iterator();
        return new Iterator<Resource>() {
            private Resource next;

            public boolean hasNext() {
                while (next == null) {
                    if (! iterator.hasNext()) {
                        return false;
                    }
                    final String name = iterator.next();
                    if ((recursive ? PathUtils.isChild(startName, name) : PathUtils.isDirectChild(startName, name))) {
                        try {
                            next = new JarEntryResource(jarFile, name, relativePath, getJarURI(new File(jarFile.getName()).toURI(), name).toURL());
                        } catch (Exception ignored) {
                        }
                    }
                }
                return true;
            }

            public Resource next() {
                if (! hasNext()) {
                    throw new NoSuchElementException();
                }
                try {
                    return next;
                } finally {
                    next = null;
                }
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public Collection<String> getPaths() {
        final Collection<String> index = new HashSet<String>();
        index.add("");
        String relativePath = this.relativePath;
        // First check for an external index
        final JarFile jarFile = this.jarFile;
        final String jarFileName = jarFile.getName();
        final long jarModified = fileOfJar.lastModified();
        final File indexFile = new File(jarFileName + ".index");
        if (ResourceLoaders.USE_INDEXES) {
            if (indexFile.exists()) {
                final long indexModified = indexFile.lastModified();
                if (indexModified != 0L && jarModified != 0L && indexModified >= jarModified) try {
                    return readIndex(new FileInputStream(indexFile), index, relativePath);
                } catch (IOException e) {
                    index.clear();
                }
            }
        }
        // Next check for an internal index
        JarEntry listEntry = jarFile.getJarEntry(INDEX_FILE);
        if (listEntry != null) {
            try {
                return readIndex(jarFile.getInputStream(listEntry), index, relativePath);
            } catch (IOException e) {
                index.clear();
            }
        }
        // Next just read the JAR
        extractJarPaths(jarFile, relativePath, index);

        if (ResourceLoaders.WRITE_INDEXES && relativePath == null) {
            writeExternalIndex(indexFile, index);
        }
        return index;
    }

    @Override
    public void close() {
        try {
            super.close();
        } finally {
            try {
                jarFile.close();
            } catch (IOException e) {
                // ignored
            }
        }
    }

    public URI getLocation() {
        try {
            return getJarURI(fileOfJar.toURI(), "");
        } catch (URISyntaxException e) {
            return null;
        }
    }

    static void extractJarPaths(final JarFile jarFile, String relativePath,
            final Collection<String> index) {
        index.add("");
        final Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            final JarEntry jarEntry = entries.nextElement();
            final String name = jarEntry.getName();
            final int idx = name.lastIndexOf('/');
            if (idx == -1) continue;
            final String path = name.substring(0, idx);
            if (path.length() == 0 || path.endsWith("/")) {
                // invalid name, just skip...
                continue;
            }
            if (relativePath == null) {
                index.add(path);
            } else {
                if (path.startsWith(relativePath + "/")) {
                    index.add(path.substring(relativePath.length() + 1));
                }
            }
        }
    }

    static void writeExternalIndex(final File indexFile,
            final Collection<String> index) {
        // Now try to write it
        boolean ok = false;
        try {
            final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(indexFile)));
            try {
                for (String name : index) {
                    writer.write(name);
                    writer.write('\n');
                }
                writer.close();
                ok = true;
            } finally {
                StreamUtil.safeClose(writer);
            }
        } catch (IOException e) {
            // failed, ignore
        } finally {
            if (! ok) {
                // well, we tried...
                indexFile.delete();
            }
        }
    }

    static Collection<String> readIndex(final InputStream stream, final Collection<String> index, final String relativePath) throws IOException {
        final BufferedReader r = new BufferedReader(new InputStreamReader(stream));
        try {
            String s;
            while ((s = r.readLine()) != null) {
                String name = s.trim();
                if (relativePath == null) {
                    index.add(name);
                } else {
                    if (name.startsWith(relativePath + "/")) {
                        index.add(name.substring(relativePath.length() + 1));
                    }
                }
            }
            return index;
        } finally {
            // if exception is thrown, undo index creation
            r.close();
        }
    }

    static void addInternalIndex(File file, boolean modify) throws IOException {
        final JarFile oldJarFile = new JarFile(file, false);
        try {
            final Collection<String> index = new TreeSet<String>();
            final File outputFile;

            outputFile = new File(file.getAbsolutePath().replace(".jar", "-indexed.jar"));

            final ZipOutputStream zo = new ZipOutputStream(new FileOutputStream(outputFile));
            try {
                Enumeration<JarEntry> entries = oldJarFile.entries();
                while (entries.hasMoreElements()) {
                    final JarEntry entry = entries.nextElement();

                    // copy data, unless we're replacing the index
                    if (!entry.getName().equals(INDEX_FILE)) {
                        final JarEntry clone = (JarEntry) entry.clone();
                        // Compression level and format can vary across implementations
                        if (clone.getMethod() != ZipEntry.STORED)
                            clone.setCompressedSize(-1);
                        zo.putNextEntry(clone);
                        StreamUtil.copy(oldJarFile.getInputStream(entry), zo);
                    }

                    // add to the index
                    final String name = entry.getName();
                    final int idx = name.lastIndexOf('/');
                    if (idx == -1) continue;
                    final String path = name.substring(0, idx);
                    if (path.length() == 0 || path.endsWith("/")) {
                        // invalid name, just skip...
                        continue;
                    }
                    index.add(path);
                }

                // write index
                zo.putNextEntry(new ZipEntry(INDEX_FILE));
                final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(zo));
                try {
                    for (String name : index) {
                        writer.write(name);
                        writer.write('\n');
                    }
                    writer.close();
                } finally {
                    StreamUtil.safeClose(writer);
                }
                zo.close();
                oldJarFile.close();

                if (modify) {
                    file.delete();
                    if (!outputFile.renameTo(file)) {
                        throw new IOException("failed to rename " + outputFile.getAbsolutePath() + " to " + file.getAbsolutePath());
                    }
                }
            } finally {
                StreamUtil.safeClose(zo);
            }
        } finally {
            StreamUtil.safeClose(oldJarFile);
        }
    }

    private static final CodeSigners EMPTY_CODE_SIGNERS = new CodeSigners(new CodeSigner[0]);

    static final class CodeSigners {

        private final CodeSigner[] codeSigners;
        private final int hashCode;

        CodeSigners(final CodeSigner[] codeSigners) {
            this.codeSigners = codeSigners;
            hashCode = Arrays.hashCode(codeSigners);
        }

        public boolean equals(final Object obj) {
            return obj instanceof CodeSigners && equals((CodeSigners) obj);
        }

        private boolean equals(final CodeSigners other) {
            return Arrays.equals(codeSigners, other.codeSigners);
        }

        public int hashCode() {
            return hashCode;
        }
    }
}
