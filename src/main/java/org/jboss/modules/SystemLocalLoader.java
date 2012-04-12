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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class SystemLocalLoader implements LocalLoader {

    private final Set<String> pathSet;

    private SystemLocalLoader() {
        final Set<String> pathSet = new FastCopyHashSet<String>(1024);
        final Set<String> jarSet = new FastCopyHashSet<String>(1024);
        final String sunBootClassPath = AccessController.doPrivileged(new PropertyReadAction("sun.boot.class.path"));
        final String javaClassPath = AccessController.doPrivileged(new PropertyReadAction("java.class.path"));
        final String extDirs = AccessController.doPrivileged(new PropertyReadAction("java.ext.dirs"));
        processClassPathItem(sunBootClassPath, jarSet, pathSet);
        processClassPathItem(javaClassPath, jarSet, pathSet);
        processClassPathItem(extDirs, jarSet, pathSet);
        this.pathSet = pathSet;
    }

    // Public members

    public Class<?> loadClassLocal(final String name, final boolean resolve) {
        final ClassLoader scl = SYSTEM_CL;
        try {
            return Class.forName(name, resolve, scl);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    public List<Resource> loadResourceLocal(final String name) {
        final Enumeration<URL> urls;
        try {
            urls = SYSTEM_CL.getResources(name);
        } catch (IOException e) {
            return Collections.emptyList();
        }
        final List<Resource> list = new ArrayList<Resource>();
        while (urls.hasMoreElements()) {
            list.add(new URLResource(urls.nextElement()));
        }
        return list;
    }

    // Nonpublic API

    static SystemLocalLoader getInstance() {
        return INSTANCE;
    }

    Set<String> getPathSet() {
        return pathSet;
    }

    // Private members

    private static final SystemLocalLoader INSTANCE = new SystemLocalLoader();

    private static final ClassLoader SYSTEM_CL = SystemLocalLoader.class.getClassLoader();

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
                    processZip(pathSet, file);
                }
            }
            s = e + 1;
        } while (e != -1);
    }

    /**
     * @param pathSet
     * @param file
     */
    private static void processZip(Set<String> pathSet, File file) {
        try {
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
            } finally {
                zipFile.close();
            }
        } catch (IOException ex) {
            // ignore
        }
    }

    private static void processDirectory0(final Set<String> pathSet, final File file) {
        for (File entry : file.listFiles()) {
            if (entry.isDirectory()) {
                processDirectory1(pathSet, entry, file.getPath());
            } else {
                if (entry.getName().endsWith(".jar"))
                    processZip(pathSet, entry);
                else {
                    final String parent = entry.getParent();
                    if (parent != null) pathSet.add(parent);
                }
            }
        }
    }

    private static void processDirectory1(final Set<String> pathSet, final File file, final String pathBase) {
        for (File entry : file.listFiles()) {
            if (entry.isDirectory()) {
                processDirectory1(pathSet, entry, pathBase);
            } else {
                if (entry.getName().endsWith(".jar"))
                    processZip(pathSet, entry);
                else {
                    String packagePath = entry.getParent();
                    if (packagePath != null) {
                        packagePath = packagePath.substring(pathBase.length()).replace('\\', '/');;
                        if(packagePath.startsWith("/")) {
                            packagePath = packagePath.substring(1);
                        }
                        pathSet.add(packagePath);
                    }
                }
            }
        }
    }
}
