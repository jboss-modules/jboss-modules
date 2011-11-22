/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import org.jboss.modules.filter.PathFilters;

import java.io.File;
import java.security.AccessController;
import java.util.jar.JarFile;

/**
 * Date: 06.05.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
final class ClassPathModuleLoader extends ModuleLoader {

    private static final String[] NO_STRINGS = new String[0];
    private final ModuleLoader delegateLoader;
    private final String classPath;
    private final String dependencies;
    private final String mainClass;

    ClassPathModuleLoader(final ModuleLoader delegateLoader, final String mainClass, String classPath, final String dependencies) {
        this.delegateLoader = delegateLoader;
        this.dependencies = dependencies;
        if (isEmpty(classPath)) {
            classPath = System.getenv().get("CLASSPATH");
        }
        this.classPath = classPath;
        AccessController.doPrivileged(new PropertyWriteAction("java.class.path", classPath));
        this.mainClass = mainClass;
    }

    private static boolean isEmpty(final String classPath) {
        return classPath == null || classPath.isEmpty();
    }

    @Override
    protected Module preloadModule(final ModuleIdentifier identifier) throws ModuleLoadException {
        if (identifier.equals(ModuleIdentifier.CLASSPATH)) {
            return loadModuleLocal(identifier);
        } else if (delegateLoader != null) {
            return preloadModule(identifier, delegateLoader);
        } else {
            return null;
        }
    }

    @Override
    protected ModuleSpec findModule(final ModuleIdentifier moduleIdentifier) throws ModuleLoadException {
        ModuleSpec.Builder builder = ModuleSpec.build(moduleIdentifier);
        builder.setMainClass(mainClass);
        // Process the classpath
        addClassPath(builder, classPath);
        // Add the dependencies
        final String[] dependencyEntries = (dependencies == null ? NO_STRINGS : dependencies.split(","));
        for (String dependencyEntry : dependencyEntries) {
            dependencyEntry = dependencyEntry.trim();
            if (! dependencyEntry.isEmpty()) {
                final ModuleIdentifier depModId = ModuleIdentifier.fromString(dependencyEntry);
                final DependencySpec spec = DependencySpec.createModuleDependencySpec(
                        PathFilters.getMetaInfSubdirectoriesWithoutMetaInfFilter(),
                        PathFilters.rejectAll(),
                        delegateLoader,
                        depModId,
                        false);
                builder.addDependency(spec);
            }
        }
        builder.addDependency(DependencySpec.createSystemDependencySpec(JDKPaths.JDK));
        builder.addDependency(DependencySpec.createLocalDependencySpec());
        return builder.create();
    }

    @Override
    public String toString() {
        return "Class path module loader for path '" + classPath + "'";
    }

    /**
     * Adds the class path entries as dependencies on the builder.
     *
     * @param builder   the builder to add the dependency entries to.
     * @param classPath the class path to process
     *
     * @throws ModuleLoadException if the class path entry is not found or the entry is a directory.
     */
    private void addClassPath(final ModuleSpec.Builder builder, final String classPath) throws ModuleLoadException {
        String[] classPathEntries = (classPath == null ? NO_STRINGS : classPath.split(File.pathSeparator));
        final File workingDir = new File(System.getProperty("user.dir"));
        for (String entry : classPathEntries) {
            if (!entry.isEmpty()) {
                try {
                    // Find the directory
                    File root = new File(entry);
                    if (! root.isAbsolute()) {
                        root = new File(workingDir, root.getPath());
                    }
                    if (root.isFile()) {
                        try {
                            builder.addResourceRoot(ResourceLoaderSpec.createResourceLoaderSpec(ResourceLoaders.createJarResourceLoader(root.getParent(), new JarFile(root, true))));
                        } catch (Exception e) {
                            Module.log.trace(e, "Resource %s does not appear to be a valid JAR. Loaded as file resource.", root);
                            builder.addResourceRoot(ResourceLoaderSpec.createResourceLoaderSpec(ResourceLoaders.createFileResourceLoader(entry, root)));
                        }
                    } else {
                        builder.addResourceRoot(ResourceLoaderSpec.createResourceLoaderSpec(ResourceLoaders.createFileResourceLoader(entry, root)));
                    }
                } catch (Exception e) {
                    throw new ModuleLoadException(String.format("File %s in class path not valid.", entry), e);
                }
            }
        }
    }
}
