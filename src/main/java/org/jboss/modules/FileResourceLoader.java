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
import java.security.AccessController;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class FileResourceLoader implements ResourceLoader {
    private static final String ARCH_NAME;

    static {
        final PropertyReadAction osNameReadAction = new PropertyReadAction("os.name");
        final PropertyReadAction osArchReadAction = new PropertyReadAction("os.arch");
        final SecurityManager sm = System.getSecurityManager();
        final String sysName;
        final String sysArch;
        if (sm != null) {
            sysName = AccessController.doPrivileged(osNameReadAction).toUpperCase();
            sysArch = AccessController.doPrivileged(osArchReadAction).toUpperCase();
        } else {
            sysName = osNameReadAction.run().toUpperCase();
            sysArch = osArchReadAction.run().toUpperCase();
        }
        final String realName;
        final String realArch;
        if (sysName.startsWith("Linux")) {
            realName = "linux";
        } else if (sysName.startsWith("MAC OS")) {
            realName = "macosx";
        } else if (sysName.startsWith("WINDOWS")) {
            realName = "win";
        } else if (sysName.startsWith("OS/2")) {
            realName = "os2";
        } else if (sysName.startsWith("SOLARIS") || sysName.startsWith("SUNOS")) {
            realName = "solaris";
        } else if (sysName.startsWith("MPE/IX")) {
            realName = "mpeix";
        } else if (sysName.startsWith("HP-UX")) {
            realName = "hpux";
        } else if (sysName.startsWith("AIX")) {
            realName = "aix";
        } else if (sysName.startsWith("OS/390")) {
            realName = "os390";
        } else if (sysName.startsWith("FREEBSD")) {
            realName = "freebsd";
        } else if (sysName.startsWith("IRIX")) {
            realName = "irix";
        } else if (sysName.startsWith("DIGITAL UNIX")) {
            realName = "digitalunix";
        } else if (sysName.startsWith("OSF1")) {
            realName = "osf1";
        } else if (sysName.startsWith("OPENVMS")) {
            realName = "openvms";
        } else {
            realName = "unknown";
        }
        if (sysArch.startsWith("SPARCV9") || sysArch.startsWith("SPARC64")) {
            realArch = "sparcv9";
        } else if (sysArch.startsWith("SPARC")) {
            realArch = "sparc";
        } else if (sysArch.startsWith("X86_64")) {
            realArch = "x86_64";
        } else if (sysArch.startsWith("I386") || sysArch.startsWith("I586") || sysArch.startsWith("I686") || sysArch.startsWith("X86")) {
            realArch = "i686";
        } else if (sysArch.startsWith("PPC64")) {
            realArch = "ppc64";
        } else if (sysArch.startsWith("PPC") || sysArch.startsWith("POWER")) {
            realArch = "ppc";
        } else if (sysArch.startsWith("ARM")) {
            realArch = "arm";
        } else if (sysArch.startsWith("PA_RISC") || sysArch.startsWith("PA-RISC")) {
            realArch = "parisc";
        } else if (sysArch.startsWith("ALPHA")) {
            realArch = "alpha";
        } else if (sysArch.startsWith("MIPS")) {
            realArch = "mips";
        } else {
            realArch = "unknown";
        }
        ARCH_NAME = realName + "-" + realArch;
    }

    private final String rootName;
    private final File root;
    private final Manifest manifest;
    private final CodeSource codeSource;

    FileResourceLoader(final String rootName, final File root) {
        if (root == null) {
            throw new IllegalArgumentException("root is null");
        }
        if (rootName == null) {
            throw new IllegalArgumentException("rootName is null");
        }
        this.rootName = rootName;
        this.root = root;
        final File manifestFile = new File(root, "META-INF" + File.separatorChar + "MANIFEST.MF");
        manifest = readManifestFile(manifestFile);
        final URL rootUrl;
        try {
            rootUrl = root.getAbsoluteFile().toURI().toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid root file specified", e);
        }
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
        final File file = new File(root, fileName);
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
        final PackageSpec spec = new PackageSpec();
        final Manifest manifest = this.manifest;
        if (manifest == null) {
            return spec;
        }
        final Attributes mainAttribute = manifest.getAttributes(name);
        final Attributes entryAttribute = manifest.getAttributes(name);
        spec.setSpecTitle(getDefinedAttribute(Attributes.Name.SPECIFICATION_TITLE, entryAttribute, mainAttribute));
        spec.setSpecVersion(getDefinedAttribute(Attributes.Name.SPECIFICATION_VERSION, entryAttribute, mainAttribute));
        spec.setSpecVendor(getDefinedAttribute(Attributes.Name.SPECIFICATION_VENDOR, entryAttribute, mainAttribute));
        spec.setImplTitle(getDefinedAttribute(Attributes.Name.IMPLEMENTATION_TITLE, entryAttribute, mainAttribute));
        spec.setImplVersion(getDefinedAttribute(Attributes.Name.IMPLEMENTATION_VERSION, entryAttribute, mainAttribute));
        spec.setImplVendor(getDefinedAttribute(Attributes.Name.IMPLEMENTATION_VENDOR, entryAttribute, mainAttribute));
        if (Boolean.parseBoolean(getDefinedAttribute(Attributes.Name.SEALED, entryAttribute, mainAttribute))) {
            spec.setSealBase(root.toURI().toURL());
        }
        return spec;
    }

    private static String getDefinedAttribute(Attributes.Name name, Attributes entryAttribute, Attributes mainAttribute) {
        final String value = entryAttribute == null ? null : entryAttribute.getValue(name);
        return value == null ? mainAttribute == null ? null : mainAttribute.getValue(name) : value;
    }

    public String getLibrary(final String name) {
        final File file = new File(root, ARCH_NAME + File.separatorChar + name);
        return file.exists() ? file.getAbsolutePath() : null;
    }

    public Resource getResource(final String name) {
        try {
            final File file = new File(root, name);
            if (! file.exists()) {
                return null;
            }
            return new FileEntryResource(name, file, file.toURI().toURL());
        } catch (MalformedURLException e) {
            // must be invalid...?  (todo: check this out)
            return null;
        }
    }

    public Collection<String> getPaths() {
        final List<String> index = new ArrayList<String>();
        // First check for an index file
        final File indexFile = new File(root.getPath() + ".index");
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
        // Manually build index, starting with the root path
        index.add("");
        buildIndex(index, root, "");
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
        return index;
    }

    private void buildIndex(final List<String> index, final File root, final String pathBase) {
        for (File file : root.listFiles()) {
            if (file.isDirectory()) {
                index.add(pathBase + file.getName());
                buildIndex(index, file, pathBase + file.getName() + "/");
            }
        }
    }
}
