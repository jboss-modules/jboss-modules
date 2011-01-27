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
import java.util.jar.JarFile;

/**
 * A local filesystem-backed module loader.
 *
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class LocalModuleLoader extends ModuleLoader {

    private static class LocalResourceLoaderFactory implements ResourceLoaderFactory {

        @Override
        public ResourceLoader create(URL moduleRoot, String resourceRootName, String resourceRootPath) throws IllegalArgumentException {
            ResourceLoader resourceLoader = null;
            final File resourceRootFile = new File(moduleRoot.getPath(), resourceRootPath);
            if (resourceRootFile.isDirectory()) {
                resourceLoader = new FileResourceLoader(resourceRootName, resourceRootFile);
            } else {
                try {
                    resourceLoader = new JarFileResourceLoader(resourceRootName, new JarFile(resourceRootFile));
                } catch (IOException e) {
                    // TODO: What now?
                }
            }

            return resourceLoader;
        }
    }

    private final File[] repoRoots;

    private final ModuleXmlParser moduleXmlParser;

    /**
     * Construct a new instance.
     *
     * @param repoRoots the array of repository roots to look for modules
     */
    public LocalModuleLoader(final File[] repoRoots) {
        this.repoRoots = repoRoots;
        this.moduleXmlParser = new ModuleXmlParser(new LocalResourceLoaderFactory());
    }

    /**
     * Construct a new instance, using the {@code module.path} system property or the {@code MODULEPATH} environment variable
     * to get the list of module repository roots.
     */
    public LocalModuleLoader() {
        final String modulePath = System.getProperty("module.path", System.getenv("MODULEPATH"));
        if (modulePath == null) {
            //noinspection ZeroLengthArrayAllocation
            repoRoots = new File[0];
        } else {
            repoRoots = getFiles(modulePath, 0, 0);
        }
        this.moduleXmlParser = new ModuleXmlParser(new LocalResourceLoaderFactory());
    }

    private static File[] getFiles(final String modulePath, final int stringIdx, final int arrayIdx) {
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

    /**
     * {@inheritDoc}
     */
    @Override
    protected Module preloadModule(final ModuleIdentifier identifier) throws ModuleLoadException {
        if (identifier.equals(ModuleIdentifier.SYSTEM)) {
            return preloadModule(ModuleIdentifier.SYSTEM, SystemClassPathModuleLoader.getInstance());
        }
        return super.preloadModule(identifier);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ModuleSpec findModule(final ModuleIdentifier moduleIdentifier) throws ModuleLoadException {
        final File moduleRoot = getModuleRoot(moduleIdentifier);
        if (moduleRoot == null)
            throw new ModuleNotFoundException("Module " + moduleIdentifier + " is not found");

        final File moduleXml = new File(moduleRoot, "module.xml");
        return parseModuleInfoFile(moduleIdentifier, moduleRoot, moduleXml);
    }

    private File getModuleRoot(final ModuleIdentifier moduleIdentifier) {
        final String child = toPathString(moduleIdentifier);
        for (File root : repoRoots) {
            final File file = new File(root, child);
            if (file.exists() && new File(file, "module.xml").exists()) return file;
        }
        return null;
    }

    private static String toPathString(ModuleIdentifier moduleIdentifier) {
        final StringBuilder builder = new StringBuilder();
        builder.append(moduleIdentifier.getName().replace('.', File.separatorChar));
        builder.append(File.separatorChar).append(moduleIdentifier.getSlot());
        builder.append(File.separatorChar);
        return builder.toString();
    }

    private ModuleSpec parseModuleInfoFile(final ModuleIdentifier moduleIdentifier, final File moduleRoot, final File moduleInfoFile) throws ModuleLoadException {
        return this.moduleXmlParser.parse(moduleIdentifier, moduleRoot, moduleInfoFile);
    }

    public String toString() {
        final StringBuilder b = new StringBuilder();
        b.append("local module loader @").append(Integer.toHexString(hashCode())).append(" (roots: ");
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
