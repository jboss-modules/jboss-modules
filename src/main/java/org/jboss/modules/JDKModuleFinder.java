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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.modules.filter.PathFilters;

/**
 * A module finder which finds a standard JDK module, a module on the JDK module path, or the special module
 * {@code org.jboss.modules}.
 */
public final class JDKModuleFinder implements IterableModuleFinder {

    private final ConcurrentHashMap<String, FutureSpec> modules = new ConcurrentHashMap<>();
    private final List<String> moduleNames;

    private static final JDKModuleFinder INSTANCE = new JDKModuleFinder();

    private JDKModuleFinder() {
        moduleNames = Collections.unmodifiableList(Arrays.asList(
            "java.sql",
            "java.base",
            "java.compiler",
            "java.datatransfer",
            "java.desktop",
            "java.instrument",
            "java.jnlp",
            "java.logging",
            "java.management",
            "java.management.rmi",
            "java.naming",
            "java.net.http",
            "java.prefs",
            "java.rmi",
            "java.scripting",
            "java.security.jgss",
            "java.security.sasl",
            "java.smartcardio",
            "java.sql.rowset",
            "java.transaction.xa",
            "java.xml",
            "java.xml.crypto",
            "javafx.base",
            "javafx.controls",
            "javafx.fxml",
            "javafx.graphics",
            "javafx.media",
            "javafx.swing",
            "javafx.web",
            "jdk.accessibility",
            "jdk.attach",
            "jdk.compiler",
            "jdk.dynalink",
            "jdk.httpserver",
            "jdk.jartool",
            "jdk.javadoc",
            "jdk.jconsole",
            "jdk.jdi",
            "jdk.jfr",
            "jdk.jsobject",
            "jdk.management",
            "jdk.management.cmm",
            "jdk.management.jfr",
            "jdk.management.resource",
            "jdk.net",
            "jdk.plugin.dom",
            "jdk.scripting.nashorn",
            "jdk.sctp",
            "jdk.security.auth",
            "jdk.security.jgss",
            "jdk.unsupported",
            "jdk.xml.dom",
            "org.jboss.modules"
        ));
    }

    /**
     * Get the singleton instance.
     *
     * @return the singleton instance
     */
    public static JDKModuleFinder getInstance() {
        return INSTANCE;
    }

    public ModuleSpec findModule(final String name, final ModuleLoader delegateLoader) throws ModuleLoadException {
        FutureSpec futureSpec = modules.get(name);
        if (futureSpec == null) {
            final FutureSpec appearing = modules.putIfAbsent(name, futureSpec = new FutureSpec());
            if (appearing != null) {
                futureSpec = appearing;
            } else {
                switch (name) {
                    case "java.se": {
                        final ModuleSpec.Builder builder = ModuleSpec.build(name, false);
                        for (DependencySpec dep : JavaSeDeps.list) {
                            builder.addDependency(dep);
                        }
                        futureSpec.setModuleSpec(builder.create());
                        break;
                    }
                    default: {
                        final ModuleSpec moduleSpec = loadModuleSpec(name);
                        futureSpec.setModuleSpec(moduleSpec);
                        if (moduleSpec == null) {
                            modules.remove(name, futureSpec);
                        }
                        break;
                    }
                }
            }
        }
        return futureSpec.getModuleSpec();
    }

    public Iterator<String> iterateModules(final String baseName, final boolean recursive) {
        return moduleNames.iterator();
    }

    private ModuleSpec loadModuleSpec(final String name) throws ModuleLoadException {
        Set<String> paths = new HashSet<>();
        InputStream is = getClass().getResourceAsStream("/jdk-module-paths/" + name);
        if (is == null) {
            return null;
        }
        try (InputStream tmp = is) {
            try (InputStreamReader isr = new InputStreamReader(tmp, StandardCharsets.UTF_8)) {
                try (BufferedReader br = new BufferedReader(isr)) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        String trimmed = line.trim();
                        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                            continue;
                        }
                        paths.add(trimmed);
                    }
                }
            }
        } catch (IOException e) {
            throw new ModuleLoadException(e);
        }
        final ModuleSpec.Builder builder = ModuleSpec.build(name, false);
        final LocalDependencySpecBuilder dependencySpecBuilder = new LocalDependencySpecBuilder();
        dependencySpecBuilder.setLoaderPaths(paths);
        dependencySpecBuilder.setExport(true);
        dependencySpecBuilder.setImportFilter(PathFilters.acceptAll());
        dependencySpecBuilder.setLocalLoader(JDKSpecific.getSystemLocalLoader());
        builder.addDependency(dependencySpecBuilder.build());
        return builder.create();
    }

    public String toString() {
        return "JDK Module Finder";
    }

    static final class FutureSpec {
        private static final ModuleSpec MARKER = ModuleSpec.build("dummy").create();

        private volatile ModuleSpec moduleSpec = MARKER;

        FutureSpec() {
        }

        void setModuleSpec(final ModuleSpec moduleSpec) {
            synchronized (this) {
                this.moduleSpec = moduleSpec;
                notifyAll();
            }
        }

        public ModuleSpec getModuleSpec() {
            ModuleSpec moduleSpec = this.moduleSpec;
            if (moduleSpec == MARKER) {
                synchronized (this) {
                    moduleSpec = this.moduleSpec;
                    boolean intr = false;
                    try {
                        while (moduleSpec == MARKER) try {
                            wait();
                            moduleSpec = this.moduleSpec;
                        } catch (InterruptedException e) {
                            intr = true;
                        }
                    } finally {
                        if (intr) Thread.currentThread().interrupt();
                    }
                }
            }
            return moduleSpec;
        }
    }
}
