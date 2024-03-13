/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
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

import static java.security.AccessController.doPrivileged;

import java.io.IOException;
import java.lang.Module;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 */
public final class JDKModuleFinder implements IterableModuleFinder {
    private final ModuleLayer layer;

    private static final JDKModuleFinder INSTANCE = new JDKModuleFinder();

    private JDKModuleFinder() {
        this.layer = ModuleLayer.boot();
    }

    public static JDKModuleFinder getInstance() {
        return INSTANCE;
    }

    public ModuleSpec findModule(final String name, final ModuleLoader delegateLoader) {
        if ("java.se".equals(name)) {
            // The `java.se` aggregator module is not included in JDK boot layer by default.
            // It becomes available when the JVM starts with the `--add-modules java.se` command line parameter.
            // By defining it here, we ensure its availability even if the JVM was not started with this parameter.
            final ModuleSpec.Builder builder = ModuleSpec.build(name, false);
            final ModuleDescriptor javaSeDescriptor = ModuleFinder.ofSystem().find(name).get().descriptor();
            DependencySpec dependencySpec;
            for (final ModuleDescriptor.Requires dep : javaSeDescriptor.requires()) {
                dependencySpec = new ModuleDependencySpecBuilder().setName(dep.name()).setExport(true).build();
                builder.addDependency(dependencySpec);
            }
            return builder.create();
        }

        final Set<String> packages;
        final Module module;
        if ("org.jboss.modules".equals(name)) {
            module = getClass().getModule();
            if (module.isNamed()) {
                packages = module.getPackages();
            } else {
                packages = Set.of(
                        "org.jboss.modules",
                        "org.jboss.modules.filter",
                        "org.jboss.modules.log",
                        "org.jboss.modules.management",
                        "org.jboss.modules.maven",
                        "org.jboss.modules.ref",
                        "org.jboss.modules.security",
                        "org.jboss.modules.xml"
                );
            }
        } else {
            final Optional<Module> moduleOptional = layer.findModule(name);
            if (! moduleOptional.isPresent()) {
                return null;
            }
            module = moduleOptional.get();
            packages = module.getPackages();
        }
        final ModuleSpec.Builder builder = ModuleSpec.build(name);
        final ModuleDescriptor descriptor = module.getDescriptor();
        if (descriptor != null) {
            final Optional<String> version = descriptor.rawVersion();
            if (version.isPresent()) builder.setVersion(Version.parse(version.get()));
            for (ModuleDescriptor.Requires require : descriptor.requires()) {
                final Set<ModuleDescriptor.Requires.Modifier> modifiers = require.modifiers();
                builder.addDependency(
                        new ModuleDependencySpecBuilder()
                                .setName(require.name())
                                .setExport(modifiers.contains(ModuleDescriptor.Requires.Modifier.TRANSITIVE))
                                .setOptional(modifiers.contains(ModuleDescriptor.Requires.Modifier.STATIC))
                                .build()
                );
            }
        }
        final Set<String> paths = new HashSet<>(packages.size());
        for (String pkg : packages) {
            paths.add(pkg.replace('.', '/'));
        }
        final LocalDependencySpecBuilder depBuilder = new LocalDependencySpecBuilder();
        depBuilder.setLoaderPaths(paths);
        depBuilder.setExport(true);
        depBuilder.setLocalLoader(new JDKModuleLoader(module, packages));
        builder.addDependency(depBuilder.build());
        return builder.create();
    }

    public String toString() {
        return "JDK Module Finder";
    }

    public Iterator<String> iterateModules(final String baseName, final boolean recursive) {
        final Set<Module> moduleSet = layer.modules();
        final ArrayList<String> nameList = new ArrayList<>(moduleSet.size() + 1);
        final Module ourModule = getClass().getModule();
        boolean foundUs = false;
        for (Module module : moduleSet) {
            if (module == ourModule) {
                foundUs = true;
            }
            nameList.add(module.getName());
        }
        if (!foundUs) {
            nameList.add("org.jboss.modules");
        }
        return nameList.iterator();
    }

    static class JDKModuleLoader implements LocalLoader {
        private final Module module;
        private final ClassLoader classLoader;
        private final Set<String> packages;

        JDKModuleLoader(final Module module, final Set<String> packages) {
            this.module = module;
            JDKModuleLoader.class.getModule().addReads(module);
            classLoader = doPrivileged(new ModuleClassLoaderAction(module));
            this.packages = packages;
        }

        public Class<?> loadClassLocal(String name, final boolean resolve) {
            final String nameDots = name.replace('/', '.');
            final int idx = nameDots.lastIndexOf('.');
            if (idx == -1) {
                // no empty package allowed
                return null;
            }
            if (packages.contains(nameDots.substring(0, idx))) {
                try {
                    final Class<?> clazz = classLoader.loadClass(nameDots);
                    if (clazz.getModule() != module) {
                        return null;
                    } else {
                        return clazz;
                    }
                } catch (ClassNotFoundException e) {
                    // unlikely
                    return null;
                }
            }
            return null;
        }

        public Package loadPackageLocal(final String name) {
            return packages.contains(name) ? classLoader.getDefinedPackage(name) : null;
        }

        public List<Resource> loadResourceLocal(final String name) {
            final URL url;
            try {
                url = new URL("jrt:/" + module.getName() + "/" + name);
            } catch (MalformedURLException e) {
                return Collections.emptyList();
            }
            try {
                final URLConnection connection = url.openConnection();
                connection.connect();
                return Collections.singletonList(new URLConnectionResource(connection));
            } catch (IOException e) {
                // connect failed; try the class loader
                final int idx = name.lastIndexOf('/');
                if (idx != -1) {
                    final String nameDots = name.substring(0, idx).replace('/', '.');
                    if (packages.contains(nameDots)) {
                        final URL resource = classLoader.getResource(name);
                        if (resource != null) {
                            try {
                                final URLConnection connection = resource.openConnection();
                                connection.connect();
                                return Collections.singletonList(new URLConnectionResource(connection));
                            } catch (IOException e2) {
                                return Collections.emptyList();
                            }
                        }
                    }
                }
                return Collections.emptyList();
            }
        }
    }
}
