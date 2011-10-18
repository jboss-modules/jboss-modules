/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
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
import java.security.CodeSource;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class JarFileResourceLoader extends AbstractResourceLoader {
    private final JarFile jarFile;
    private final String rootName;
    private final URL rootUrl;
    private final String relativePath;
    private final File fileOfJar;

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
        final String realPath = relativePath == null ? null : PathUtils.canonicalize(relativePath);
        this.relativePath = realPath;
        try {
            rootUrl = new URI("jar", fileOfJar.toURI().toString() + (realPath == null ? "!/" : "!/" + realPath), null).toURL();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid root file specified", e);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid root file specified", e);
        }
    }

    public String getRootName() {
        return rootName;
    }

    public ClassSpec getClassSpec(final String fileName) throws IOException {
        final ClassSpec spec = new ClassSpec();
        final JarEntry entry = getJarEntry(fileName);
        if (entry == null) {
            // no such entry
            return null;
        }
        spec.setCodeSource(new CodeSource(rootUrl, entry.getCodeSigners()));
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
                baos.close();
                is.close();
                spec.setBytes(baos.toByteArray());
                return spec;
            } else if (size <= (long) Integer.MAX_VALUE) {
                final int castSize = (int) size;
                byte[] bytes = new byte[castSize];
                int a = 0, res;
                while ((res = is.read(bytes, a, castSize - a)) > 0) {
                    a += res;
                }
                // done
                is.close();
                spec.setBytes(bytes);
                return spec;
            } else {
                throw new IOException("Resource is too large to be a valid class file");
            }
        } finally {
            safeClose(is);
        }
    }

    private JarEntry getJarEntry(final String fileName) {
        return relativePath == null ? jarFile.getJarEntry(fileName) : jarFile.getJarEntry(relativePath + "/" + fileName);
    }

    private static void safeClose(final Closeable closeable) {
        if (closeable != null) try {
            closeable.close();
        } catch (IOException e) {
            // ignore
        }
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
                    safeClose(inputStream);
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
            if(name.startsWith("/"))
                name = name.substring(1);
            final JarEntry entry = getJarEntry(name);
            if (entry == null) {
                return null;
            }
            return new JarEntryResource(jarFile, entry, new URI("jar", "file:" + jarFile.getName() + "!/" + entry.getName(), null).toURL());
        } catch (MalformedURLException e) {
            // must be invalid...?  (todo: check this out)
            return null;
        } catch (URISyntaxException e) {
            // must be invalid...?  (todo: check this out)
            return null;
        }
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
        if (indexFile.exists()) {
            final long indexModified = indexFile.lastModified();
            if (indexModified != 0L && jarModified != 0L && indexModified >= jarModified) try {
                return readIndex(new FileInputStream(indexFile), index, relativePath);
            } catch (IOException e) {
                index.clear();
            }
        }
        // Next check for an internal index
        JarEntry listEntry = jarFile.getJarEntry("META-INF/PATHS.LIST");
        if (listEntry != null) {
            if (jarModified != 0L) {
                final long entryTime = listEntry.getTime();
                if (entryTime == -1L || entryTime >= jarModified) {
                    try {
                        return readIndex(jarFile.getInputStream(listEntry), index, relativePath);
                    } catch (IOException e) {
                        index.clear();
                    }
                }
            }
        }
        // Next just read the JAR
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
        if (ResourceLoaders.WRITE_INDEXES && relativePath == null) {
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
                    try {
                        writer.close();
                    } catch (IOException e) {
                        // ignored
                    }
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
        return index;
    }

    private static Collection<String> readIndex(final InputStream stream, final Collection<String> index, final String relativePath) throws IOException {
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
}
