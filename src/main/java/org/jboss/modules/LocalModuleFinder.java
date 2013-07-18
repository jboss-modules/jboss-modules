/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
import java.io.FilePermission;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import org.jboss.modules.filter.PathFilter;
import org.jboss.modules.filter.PathFilters;

import static java.security.AccessController.doPrivileged;
import static java.security.AccessController.getContext;

/**
 * A module finder which locates module specifications which are stored in a local module
 * repository on the filesystem, which uses {@code module.xml} descriptors.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class LocalModuleFinder implements ModuleFinder {

    private static final File[] NO_FILES = new File[0];

    private final File[] repoRoots;
    private final PathFilter pathFilter;
    private final AccessControlContext accessControlContext;

    private LocalModuleFinder(final File[] repoRoots, final PathFilter pathFilter, final boolean cloneRoots) {
        this.repoRoots = cloneRoots && repoRoots.length > 0 ? repoRoots.clone() : repoRoots;
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            for (File repoRoot : this.repoRoots) {
                if (repoRoot == null) sm.checkPermission(new FilePermission(new File(repoRoot, "-").getPath(), "read"));
            }
        }
        this.pathFilter = pathFilter;
        this.accessControlContext = AccessController.getContext();
    }

    /**
     * Construct a new instance.
     *
     * @param repoRoots the repository roots to use
     * @param pathFilter the path filter to use
     */
    public LocalModuleFinder(final File[] repoRoots, final PathFilter pathFilter) {
        this(repoRoots, pathFilter, true);
    }

    /**
     * Construct a new instance.
     *
     * @param repoRoots the repository roots to use
     */
    public LocalModuleFinder(final File[] repoRoots) {
        this(repoRoots, PathFilters.acceptAll());
    }

    /**
     * Construct a new instance, using the {@code module.path} system property or the {@code JAVA_MODULEPATH} environment variable
     * to get the list of module repository roots.
     * <p>
     * This is equivalent to a call to {@link LocalModuleFinder#LocalModuleFinder(boolean) LocalModuleFinder(true)}.
     * </p>
     */
    public LocalModuleFinder() {
        this(true);
    }

    /**
     * Construct a new instance, using the {@code module.path} system property or the {@code JAVA_MODULEPATH} environment variable
     * to get the list of module repository roots.
     *
     * @param supportLayersAndAddOns {@code true} if the identified module repository roots should be checked for
     *                               an internal structure of child "layer" and "add-on" directories that may also
     *                               be treated as module roots lower in precedence than the parent root. Any "layers"
     *                               subdirectories whose names are specified in a {@code layers.conf} file found in
     *                               the module repository root will be added in the precedence of order specified
     *                               in the {@code layers.conf} file; all "add-on" subdirectories will be added at
     *                               a lower precedence than all "layers" and with no guaranteed precedence order
     *                               between them. If {@code false} no check for "layer" and "add-on" directories
     *                               will be performed.
     *
     */
    public LocalModuleFinder(boolean supportLayersAndAddOns) {
        this(getRepoRoots(supportLayersAndAddOns), PathFilters.acceptAll(), false);
    }

    static File[] getRepoRoots(final boolean supportLayersAndAddOns) {
        return supportLayersAndAddOns ? LayeredModulePathFactory.resolveLayeredModulePath(getModulePathFiles()) : getModulePathFiles();
    }

    private static File[] getModulePathFiles() {
        return getFiles(System.getProperty("module.path", System.getenv("JAVA_MODULEPATH")), 0, 0);
    }

    private static File[] getFiles(final String modulePath, final int stringIdx, final int arrayIdx) {
        if (modulePath == null) return NO_FILES;
        final int i = modulePath.indexOf(File.pathSeparatorChar, stringIdx);
        final File[] files;
        if (i == -1) {
            files = new File[arrayIdx + 1];
            files[arrayIdx] = new File(modulePath.substring(stringIdx)).getAbsoluteFile();
        } else {
            files = getFiles(modulePath, i + 1, arrayIdx + 1);
            files[arrayIdx] = new File(modulePath.substring(stringIdx, i)).getAbsoluteFile();
        }
        return files;
    }

    private static String toPathString(ModuleIdentifier moduleIdentifier) {
        final StringBuilder builder = new StringBuilder(40);
        builder.append(moduleIdentifier.getName().replace('.', File.separatorChar));
        builder.append(File.separatorChar).append(moduleIdentifier.getSlot());
        builder.append(File.separatorChar);
        return builder.toString();
    }

    public ModuleSpec findModule(final ModuleIdentifier identifier, final ModuleLoader delegateLoader) throws ModuleLoadException {
        final String child = toPathString(identifier);
        if (pathFilter.accept(child)) {
            try {
                return doPrivileged(new PrivilegedExceptionAction<ModuleSpec>() {
                    public ModuleSpec run() throws Exception {
                        for (File root : repoRoots) {
                            final File file = new File(root, child);
                            final File moduleXml = new File(file, "module.xml");
                            if (moduleXml.exists()) {
                                final ModuleSpec spec = ModuleXmlParser.parseModuleXml(delegateLoader, identifier, file, moduleXml, accessControlContext);
                                if (spec == null) break;
                                return spec;
                            }
                        }
                        return null;
                    }
                }, accessControlContext);
            } catch (PrivilegedActionException e) {
                try {
                    throw e.getException();
                } catch (RuntimeException e1) {
                    throw e1;
                } catch (ModuleLoadException e1) {
                    throw e1;
                } catch (Error e1) {
                    throw e1;
                } catch (Exception e1) {
                    throw new UndeclaredThrowableException(e1);
                }
            }
        }
        return null;
    }

    /**
     * Parse a {@code module.xml} file and return the corresponding module specification.
     *
     * @param identifier the identifier to load
     * @param delegateLoader the delegate module loader to use for module specifications
     * @param roots the repository root paths to search
     * @return the module specification
     * @throws IOException if reading the module file failed
     * @throws ModuleLoadException if creating the module specification failed (e.g. due to a parse error)
     */
    public static ModuleSpec parseModuleXmlFile(final ModuleIdentifier identifier, final ModuleLoader delegateLoader, final File... roots) throws IOException, ModuleLoadException {
        final String child = toPathString(identifier);
        for (File root : roots) {
            final File file = new File(root, child);
            final File moduleXml = new File(file, "module.xml");
            if (moduleXml.exists()) {
                final ModuleSpec spec = ModuleXmlParser.parseModuleXml(delegateLoader, identifier, file, moduleXml, getContext());
                if (spec == null) break;
                return spec;
            }
        }
        return null;
    }

    public String toString() {
        final StringBuilder b = new StringBuilder();
        b.append("local module finder @").append(Integer.toHexString(hashCode())).append(" (roots: ");
        final int repoRootsLength = repoRoots.length;
        for (int i = 0; i < repoRootsLength; i++) {
            final File root = repoRoots[i];
            b.append(root);
            if (i != repoRootsLength - 1) {
                b.append(',');
            }
        }
        b.append(')');
        return b.toString();
    }
}
