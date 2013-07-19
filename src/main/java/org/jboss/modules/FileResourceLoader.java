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

import static java.security.AccessController.doPrivileged;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.jar.Manifest;

/**
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class FileResourceLoader extends NativeLibraryResourceLoader implements IterableResourceLoader {

    private final String rootName;
    private final Manifest manifest;
    private final CodeSource codeSource;
    private final AccessControlContext context;

    FileResourceLoader(final String rootName, final File root, final AccessControlContext context) {
        super(root);
        if (root == null) {
            throw new IllegalArgumentException("root is null");
        }
        if (rootName == null) {
            throw new IllegalArgumentException("rootName is null");
        }
        if (context == null) {
            throw new IllegalArgumentException("context is null");
        }
        this.rootName = rootName;
        final File manifestFile = new File(root, "META-INF" + File.separatorChar + "MANIFEST.MF");
        manifest = readManifestFile(manifestFile);
        final URL rootUrl;
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            rootUrl = doPrivileged(new PrivilegedAction<URL>() {
                public URL run() {
                    try {
                        return root.getAbsoluteFile().toURI().toURL();
                    } catch (MalformedURLException e) {
                        throw new IllegalArgumentException("Invalid root file specified", e);
                    }
                }
            }, context);
        } else try {
            rootUrl = root.getAbsoluteFile().toURI().toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid root file specified", e);
        }
        this.context = context;
        codeSource = new CodeSource(rootUrl, (CodeSigner[])null);
    }

    private static Manifest readManifestFile(final File manifestFile) {
        try {
            return new Manifest(new FileInputStream(manifestFile));
        } catch (IOException e) {
            return null;
        }
    }

    public String getRootName() {
        return rootName;
    }

    public ClassSpec getClassSpec(final String fileName) throws IOException {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            try {
                return doPrivileged(new PrivilegedExceptionAction<ClassSpec>() {
                    public ClassSpec run() throws IOException {
                        return doGetClassSpec(fileName);
                    }
                }, context);
            } catch (PrivilegedActionException e) {
                try {
                    throw e.getException();
                } catch (IOException e1) {
                    throw e1;
                } catch (RuntimeException e1) {
                    throw e1;
                } catch (Exception e1) {
                    throw new UndeclaredThrowableException(e1);
                }
            }
        } else {
            return doGetClassSpec(fileName);
        }
    }

    private ClassSpec doGetClassSpec(final String fileName) throws IOException {
        final File file = new File(getRoot(), fileName);
        if (! file.exists()) {
            return null;
        }
        final long size = file.length();
        final ClassSpec spec = new ClassSpec();
        spec.setCodeSource(codeSource);
        final InputStream is = new FileInputStream(file);
        try {
            if (size <= (long) Integer.MAX_VALUE) {
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

    private static void safeClose(final Closeable closeable) {
        if (closeable != null) try {
            closeable.close();
        } catch (IOException e) {
            // ignore
        }
    }

    public PackageSpec getPackageSpec(final String name) throws IOException {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            try {
                return doPrivileged(new PrivilegedExceptionAction<PackageSpec>() {
                    public PackageSpec run() throws IOException {
                        return getPackageSpec(name, manifest, getRoot().toURI().toURL());
                    }
                }, context);
            } catch (PrivilegedActionException e) {
                try {
                    throw e.getException();
                } catch (IOException e1) {
                    throw e1;
                } catch (RuntimeException e1) {
                    throw e1;
                } catch (Exception e1) {
                    throw new UndeclaredThrowableException(e1);
                }
            }
        } else {
            return getPackageSpec(name, manifest, getRoot().toURI().toURL());
        }
    }

    public Resource getResource(final String name) {
        final String canonPath = PathUtils.canonicalize(PathUtils.relativize(name));
        final File file = new File(getRoot(), canonPath);
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            return doPrivileged(new PrivilegedAction<Resource>() {
                public Resource run() {
                    if (!file.exists()) {
                        return null;
                    } else {
                        try {
                            return new FileEntryResource(canonPath, file, file.toURI().toURL(), context);
                        } catch (MalformedURLException e) {
                            return null;
                        }
                    }
                }
            }, context);
        } else if (! file.exists()) {
            return null;
        } else {
            try {
                return new FileEntryResource(canonPath, file, file.toURI().toURL(), context);
            } catch (MalformedURLException e) {
                return null;
            }
        }
    }

    class Itr implements Iterator<Resource> {
        private final String base;
        private final String[] names;
        private final boolean recursive;
        private int i = 0;
        private Itr nested;
        private Resource next;

        Itr(final String base, final String[] names, final boolean recursive) {
            assert PathUtils.isRelative(base);
            assert names != null && names.length > 0;
            this.base = base;
            this.names = names;
            this.recursive = recursive;
        }

        public boolean hasNext() {
            final String[] names = this.names;
            if (next != null) {
                return true;
            }
            final String base = this.base;
            while (i < names.length) {
                final String current = names[i];
                final String full = base.isEmpty() ? current : base + "/" + current;
                final File file = new File(getRoot(), full);
                if (recursive && nested == null) {
                    final String[] children = file.list();
                    if (children != null && children.length > 0) {
                        nested = new Itr(full, children, recursive);
                    }
                }
                if (nested != null) {
                    if (nested.hasNext()) {
                        next = nested.next();
                        return true;
                    }
                    nested = null;
                }
                i++;
                if (file.isFile()) {
                    try {
                        next = new FileEntryResource(full, file, file.toURI().toURL(), context);
                        return true;
                    } catch (MalformedURLException ignored) {
                    }
                }
            }
            return false;
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
        }
    }

    public Iterator<Resource> iterateResources(final String startPath, final boolean recursive) {
        final String canonPath = PathUtils.canonicalize(PathUtils.relativize(startPath));
        final File start = new File(getRoot(), canonPath);
        final String[] children = start.list();
        if (children == null || children.length == 0) {
            return Collections.<Resource>emptySet().iterator();
        }
        return new Itr(canonPath, children, recursive);
    }

    public Collection<String> getPaths() {
        final List<String> index = new ArrayList<String>();
        final File indexFile = new File(getRoot().getPath() + ".index");
        if (ResourceLoaders.USE_INDEXES) {
            // First check for an index file
            if (indexFile.exists()) {
                try {
                    final BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(indexFile)));
                    try {
                        String s;
                        while ((s = r.readLine()) != null) {
                            index.add(s.trim());
                        }
                        return index;
                    } finally {
                        // if exception is thrown, undo index creation
                        r.close();
                    }
                } catch (IOException e) {
                    index.clear();
                }
            }
        }
        // Manually build index, starting with the root path
        index.add("");
        buildIndex(index, getRoot(), "");
        if (ResourceLoaders.WRITE_INDEXES) {
            // Now try to write it
            boolean ok = false;
            try {
                final FileOutputStream fos = new FileOutputStream(indexFile);
                try {
                    final OutputStreamWriter osw = new OutputStreamWriter(fos);
                    try {
                        final BufferedWriter writer = new BufferedWriter(osw);
                        try {
                            for (String name : index) {
                                writer.write(name);
                                writer.write('\n');
                            }
                            writer.close();
                            osw.close();
                            fos.close();
                            ok = true;
                        } finally {
                            safeClose(writer);
                        }
                    } finally {
                        safeClose(osw);
                    }
                } finally {
                    safeClose(fos);
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

    private void buildIndex(final List<String> index, final File root, final String pathBase) {
        File[] files = root.listFiles();
        if (files != null) for (File file : files) {
            if (file.isDirectory()) {
                index.add(pathBase + file.getName());
                buildIndex(index, file, pathBase + file.getName() + "/");
            }
        }
    }
}
