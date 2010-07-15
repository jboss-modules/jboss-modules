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
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * The special module classloader for the system module.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class SystemModuleClassLoader extends ModuleClassLoader {

    SystemModuleClassLoader(final Module module, final Set<Module.Flag> flags, final AssertionSetting setting) {
        super(module, flags, setting, null);
    }

    Set<String> getExportedPaths() {
        final HashSet<String> packageSet = new HashSet<String>(128);
        final HashSet<String> jarSet = new HashSet<String>(128);
        processClassPathItem(System.getProperty("sun.boot.class.path"), jarSet, packageSet);
        processClassPathItem(System.getProperty("java.class.path"), jarSet, packageSet);
        return packageSet;
    }

    private void processClassPathItem(final String classPath, final Set<String> jarSet, final Set<String> packageSet) {
        if (classPath == null) return;
        int s = 0, e;
        do {
            e = classPath.indexOf(File.pathSeparatorChar, s);
            String item = e == -1 ? classPath.substring(s) : classPath.substring(s, e);
            if (! jarSet.contains(item)) {
                final File file = new File(item);
                if (file.isDirectory()) {
                    processDirectory0(packageSet, file);
                } else {
                    try {
                        final ZipFile zipFile = new ZipFile(file);
                        try {
                            final Enumeration<? extends ZipEntry> entries = zipFile.entries();
                            while (entries.hasMoreElements()) {
                                final ZipEntry entry = entries.nextElement();
                                final String name = entry.getName();
                                final int lastSlash = name.lastIndexOf('/');
                                if (lastSlash != -1) {
                                    final String dirName = name.substring(0, lastSlash);
                                    if (dirName.equals("META-INF")) {
                                        // skip non-exported META-INF
                                        continue;
                                    }
                                    packageSet.add(dirName);
                                }
                            }
                        } finally {
                            zipFile.close();
                        }
                    } catch (IOException ex) {
                        // ignore
                    }
                }
            }
            s = e + 1;
        } while (e != -1);
    }

    private static void processDirectory0(final Set<String> packageSet, final File file) {
        for (File entry : file.listFiles()) {
            if (entry.getName().equals("META-INF")) {
                // skip non-exported META-INF
                continue;
            }
            if (entry.isDirectory()) {
                processDirectory1(packageSet, entry, file.getPath());
            } else {
                final String parent = entry.getParent();
                if (parent != null) packageSet.add(parent);
            }
        }
    }

    private static void processDirectory1(final Set<String> packageSet, final File file, final String pathBase) {
        for (File entry : file.listFiles()) {
            if (entry.isDirectory()) {
                processDirectory1(packageSet, entry, pathBase);
            } else {
                String packagePath = entry.getParent();
                if (packagePath != null) {
                    packagePath = packagePath.substring(pathBase.length()).replace('\\', '/');;
                    if(packagePath.startsWith("/")) {
                        packagePath = packagePath.substring(1);
                    }
                    packageSet.add(packagePath);
                }
            }
        }
    }

    protected Class<?> findClass(final String className, final boolean exportsOnly) throws ClassNotFoundException {
        return findSystemClass(className);
    }

    protected String findLibrary(final String libname) {
        return null;
    }

    public URL findResource(final String name, final boolean exportsOnly) {
        return getSystemResource(name);
    }

    public Enumeration<URL> findResources(final String name, final boolean exportsOnly) throws IOException {
        return getSystemResources(name);
    }

    public InputStream findResourceAsStream(final String name, final boolean exportsOnly) {
        return getSystemResourceAsStream(name);
    }

    Resource getRawResource(final String root, final String name) {
        if ("".equals(root)) {
            return new URLResource(getResource(name));
        } else {
            return null;
        }
    }
}
