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

import org.jboss.modules.filter.PathFilters;

import java.io.File;
import java.security.AccessController;

/**
 * Date: 06.05.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
final class ClassPathModuleLoader extends ModuleLoader {

    static final String[] NO_STRINGS = new String[0];
    static final String CLASSPATH_STRING = "Classpath";
    private final ModuleLoader delegateLoader;
    private final String classPath;
    private final String dependencies;
    private final String mainClass;

    ClassPathModuleLoader(final ModuleLoader delegateLoader, final String mainClass, String classPath, final String dependencies) {
        this.delegateLoader = delegateLoader;
        this.dependencies = dependencies;
        if (isEmpty(classPath)) {
            classPath = System.getenv().getOrDefault("CLASSPATH","");
        }
        this.classPath = classPath;
        AccessController.doPrivileged(new PropertyWriteAction("java.class.path", classPath));
        this.mainClass = mainClass;
    }

    private static boolean isEmpty(final String classPath) {
        return classPath == null || classPath.isEmpty();
    }

    @Override
    protected Module preloadModule(final String name) throws ModuleLoadException {
        if (name.equals(CLASSPATH_STRING)) {
            return loadModuleLocal(name);
        } else if (delegateLoader != null) {
            return preloadModule(name, delegateLoader);
        } else {
            return null;
        }
    }

    @Override
    protected ModuleSpec findModule(final String name) throws ModuleLoadException {
        ModuleSpec.Builder builder = ModuleSpec.build(name);
        builder.setMainClass(mainClass);
        // Process the classpath
        addClassPath(builder, classPath);
        // Add the dependencies
        final String[] dependencyEntries = (dependencies == null ? NO_STRINGS : dependencies.split(","));
        for (String dependencyEntry : dependencyEntries) {
            dependencyEntry = dependencyEntry.trim();
            if (! dependencyEntry.isEmpty()) {
                final DependencySpec spec = DependencySpec.createModuleDependencySpec(
                        PathFilters.getMetaInfSubdirectoriesWithoutMetaInfFilter(),
                        PathFilters.rejectAll(),
                        delegateLoader,
                        dependencyEntry,
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
                            builder.addResourceRoot(ResourceLoaderSpec.createResourceLoaderSpec(ResourceLoaders.createJarResourceLoader(root.getParent(), JDKSpecific.getJarPath(root, true))));
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
