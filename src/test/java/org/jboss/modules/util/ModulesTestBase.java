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
package org.jboss.modules.util;

import org.jboss.modules.DependencySpec;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.ModuleSpec;
import org.jboss.modules.filter.PathFilter;
import org.jboss.modules.filter.PathFilters;
import org.junit.Before;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Test low level modules use cases.
 * 
 * @author Thomas.Diesler@jboss.com
 * @since 15-Sep-2010
 */
public abstract class ModulesTestBase {

    private ModuleLoaderSupport moduleLoader;

    @Before
    public void setUp() throws Exception {
        moduleLoader = new ModuleLoaderSupport("default");
    }

    protected ModuleLoaderSupport getModuleLoader() {
        return moduleLoader;
    }

    protected void addModuleSpec(ModuleSpec moduleSpec) {
        moduleLoader.addModuleSpec(moduleSpec);
    }

    protected Module loadModule(ModuleIdentifier identifier) throws ModuleLoadException {
        return moduleLoader.loadModule(identifier);
    }

    protected PathFilter getPathFilter(Class<?>... classes) {
        Set<String> paths = getFilterPaths(classes);
        return PathFilters.in(paths);
    }

    protected Set<String> getFilterPaths(Class<?>... classes) {
        Set<String> paths = new HashSet<String>();
        for (Class<?> clazz : classes) {
            paths.add(getPathForClassName(clazz.getName()));
        }
        return Collections.unmodifiableSet(paths);
    }

    protected String getPathForClassName(String className) {
        className = className.substring(0, className.lastIndexOf('.'));
        className = className.replace('.', '/');
        return className;
    }

    protected void assertLoadClass(ModuleIdentifier identifier, String className) throws Exception {
        Class<?> clazz = loadClass(identifier, className);
        assertNotNull(clazz);
    }

    protected void assertLoadClass(ModuleIdentifier identifier, String className, ModuleIdentifier exporterId) throws Exception {
        Class<?> clazz = loadClass(identifier, className);
        ClassLoader wasClassLoader = clazz.getClassLoader();
        if (exporterId == null && wasClassLoader == null) {
            return;
        }
        ModuleClassLoader expClassLoader = loadModule(exporterId).getClassLoader();
        assertEquals(expClassLoader, wasClassLoader);
    }

    protected void assertLoadClassFail(ModuleIdentifier identifier, String className) throws Exception {
        try {
            Class<?> clazz = loadClass(identifier, className);
            assertNotNull("ClassNotFoundException expected for [" + className + "], but was: " + clazz, clazz);
            fail("ClassNotFoundException expected for [" + className + "], but was loaded from: " + clazz.getClassLoader());
        } catch (ClassNotFoundException ex) {
            // expected
        } catch (NoClassDefFoundError ex) {
            // expected
        }
    }

    protected Class<?> loadClass(ModuleIdentifier identifier, String className) throws Exception {
        // ClassLoader#resolveClass() only links the class; it doesn't necessarily force it to be initialized.
        // To initialize the class you can do Class.forName(name, true, classLoader)
        ModuleClassLoader classLoader = loadModule(identifier).getClassLoader();
        Class<?> clazz = Class.forName(className, true, classLoader);
        return clazz;
    }

    protected URL getResource(ModuleIdentifier identifier, String resourcePath) throws Exception {
        ModuleClassLoader classLoader = loadModule(identifier).getClassLoader();
        return classLoader.getResource(resourcePath);
    }

    static class ModuleLoaderSupport extends ModuleLoader {

        private String loaderName;
        private Map<ModuleIdentifier, ModuleSpec> modules = new HashMap<ModuleIdentifier, ModuleSpec>();

        ModuleLoaderSupport(String loaderName) {
            this.loaderName = loaderName;
        }

        void addModuleSpec(ModuleSpec moduleSpec) {
            modules.put(moduleSpec.getModuleIdentifier(), moduleSpec);
        }

        @Override
        protected ModuleSpec findModule(ModuleIdentifier identifier) throws ModuleLoadException {
            ModuleSpec moduleSpec = modules.get(identifier);
            return moduleSpec;
        }

        @Override
        protected void setAndRelinkDependencies(Module module, List<DependencySpec> dependencies) throws ModuleLoadException {
            super.setAndRelinkDependencies(module, dependencies);
        }

        @Override
        public String toString() {
            return "ModuleLoaderSupport[" + loaderName + "]";
        }
    }
}
