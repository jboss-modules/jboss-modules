/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;


/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 */
public final class Module {
    static {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                // Set up URL handler, if it isn't already
                final String pkgs = System.getProperty("java.protocol.handler.pkgs");
                final String newPkgs;
                if (pkgs == null || pkgs.length() == 0) {
                    newPkgs = "org.jboss.modules.protocol";
                    System.setProperty("java.protocol.handler.pkgs", newPkgs);
                } else if (! pkgs.contains("org.jboss.modules.protocol")) {
                    newPkgs = pkgs + "|org.jboss.modules.protocol";
                    System.setProperty("java.protocol.handler.pkgs", newPkgs);
                }
                return null;
            }
        });
    }

    private static ModuleLoaderSelector moduleLoaderSelector = ModuleLoaderSelector.DEFAULT;

    private final ModuleIdentifier identifier;
    private final ModuleContentLoader contentLoader;
    private final String mainClassName;
    private final ModuleClassLoader moduleClassLoader;
    private final ModuleLoader moduleLoader;
    private final Set<String> exportedPaths;
    private final Map<String, List<Dependency>> pathsToImports;

    Module(final ModuleSpec spec, final Set<Flag> flags, final ModuleLoader moduleLoader, final Set<String> exportedPaths, final Map<String, List<Dependency>> pathsToImports) {
        this.moduleLoader = moduleLoader;
        identifier = spec.getIdentifier();
        contentLoader = spec.getContentLoader();
        mainClassName = spec.getMainClass();
        // should be safe, so...
        //noinspection ThisEscapedInObjectConstruction
        moduleClassLoader = new ModuleClassLoader(this, flags, spec.getAssertionSetting());

        this.exportedPaths = exportedPaths;
        this.pathsToImports = pathsToImports;
    }

    public final Resource getExportedResource(final String rootPath, final String resourcePath) {
        return contentLoader.getResource(rootPath, resourcePath);
    }

    public final void run(final String[] args) throws NoSuchMethodException, InvocationTargetException, ClassNotFoundException {
        try {
            if (mainClassName == null) {
                throw new NoSuchMethodException("No main class defined for " + this);
            }
            final Class<?> mainClass = moduleClassLoader.loadExportedClass(mainClassName);
            if (mainClass == null) {
                throw new NoSuchMethodException("No main class named '" + mainClassName + "' found in " + this);
            }
            final Method mainMethod = mainClass.getMethod("main", String[].class);
            final int modifiers = mainMethod.getModifiers();
            if (! Modifier.isStatic(modifiers)) {
                throw new NoSuchMethodException("Main method is not static for " + this);
            }
            // ignore the return value
            mainMethod.invoke(null, new Object[] {args});
        } catch (IllegalAccessException e) {
            // unexpected; should be public
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public ModuleIdentifier getIdentifier() {
        return identifier;
    }

    public ModuleLoader getModuleLoader() {
        return moduleLoader;
    }

    /**
     * Load a service from this module.
     *
     * @param serviceType the service type class
     * @param <S> the service type
     * @return the service loader
     */
    public <S> ServiceLoader<S> loadService(Class<S> serviceType) {
        return ServiceLoader.load(serviceType, moduleClassLoader);
    }

    /**
     * Load a service from the named module.
     *
     * @param moduleIdentifier the module identifier
     * @param serviceType the service type class
     * @param <S> the service type
     * @return the service loader
     * @throws ModuleLoadException if the given module could not be loaded
     */
    public static <S> ServiceLoader<S> loadService(ModuleIdentifier moduleIdentifier, Class<S> serviceType) throws ModuleLoadException {
        return Module.getModule(moduleIdentifier).loadService(serviceType);
    }

    /**
     * Load a service from the named module.
     *
     * @param moduleIdentifier the module identifier
     * @param serviceType the service type class
     * @param <S> the service type
     * @return the service loader
     * @throws ModuleLoadException if the given module could not be loaded
     */
    public static <S> ServiceLoader<S> loadService(String moduleIdentifier, Class<S> serviceType) throws ModuleLoadException {
        return loadService(ModuleIdentifier.fromString(moduleIdentifier), serviceType);
    }

    /**
     * Get the class loader for a module.
     *
     * @return the module class loader
     */
    public ModuleClassLoader getClassLoader() {
        return moduleClassLoader;
    }

    /**
     * Get all the paths exported by this module.
     *
     * @return the paths that are exported by this module 
     */
    public Set<String> getExportedPaths() {
        return exportedPaths;
    }

    /**
     * Get the module for a loaded class, or {@code null} if the class did not come from any module.
     *
     * @param clazz the class
     * @return the module it came from
     */
    public static Module forClass(Class<?> clazz) {
        final ClassLoader cl = clazz.getClassLoader();
        return cl instanceof ModuleClassLoader ? ((ModuleClassLoader) cl).getModule() : null;
    }

    /**
     * Load a class from a module.
     *
     * @param moduleIdentifier the identifier of the module from which the class should be loaded
     * @param className the class name to load
     * @param initialize {@code true} to initialize the class
     * @return the class
     * @throws ModuleLoadException if the module could not be loaded
     * @throws ClassNotFoundException if the class could not be loaded
     */
    public static Class<?> loadClass(final ModuleIdentifier moduleIdentifier, final String className, final boolean initialize) throws ModuleLoadException, ClassNotFoundException {
        return Class.forName(className, initialize, ModuleClassLoader.forModule(moduleIdentifier));
    }

    /**
     * Load a class from a module.  The class will be initialized.
     *
     * @param moduleIdentifier the identifier of the module from which the class should be loaded
     * @param className the class name to load
     * @return the class
     * @throws ModuleLoadException if the module could not be loaded
     * @throws ClassNotFoundException if the class could not be loaded
     */
    public static Class<?> loadClass(final ModuleIdentifier moduleIdentifier, final String className) throws ModuleLoadException, ClassNotFoundException {
        return Class.forName(className, true, ModuleClassLoader.forModule(moduleIdentifier));
    }

    /**
     * Load a class from a module.
     *
     * @param moduleIdentifierString the identifier of the module from which the class should be loaded
     * @param className the class name to load
     * @param initialize {@code true} to initialize the class
     * @return the class
     * @throws ModuleLoadException if the module could not be loaded
     * @throws ClassNotFoundException if the class could not be loaded
     */
    public static Class<?> loadClass(final String moduleIdentifierString, final String className, final boolean initialize) throws ModuleLoadException, ClassNotFoundException {
        return Class.forName(className, initialize, ModuleClassLoader.forModule(ModuleIdentifier.fromString(moduleIdentifierString)));
    }

    /**
     * Load a class from a module.  The class will be initialized.
     *
     * @param moduleIdentifierString the identifier of the module from which the class should be loaded
     * @param className the class name to load
     * @return the class
     * @throws ModuleLoadException if the module could not be loaded
     * @throws ClassNotFoundException if the class could not be loaded
     */
    public static Class<?> loadClass(final String moduleIdentifierString, final String className) throws ModuleLoadException, ClassNotFoundException {
        return Class.forName(className, true, ModuleClassLoader.forModule(ModuleIdentifier.fromString(moduleIdentifierString)));
    }

    PackageSpec getLocalPackageSpec(final String name) throws IOException {
        return contentLoader.getPackageSpec(name);
    }

    String getLocalLibrary(final String libname) {
        return contentLoader.getLibrary(libname);
    }

    public static Module getModule(final ModuleIdentifier identifier) throws ModuleLoadException {
        return moduleLoaderSelector.getCurrentLoader().loadModule(identifier);
    }

    public enum Flag {
        // flags here
        CHILD_FIRST
    }

    public String toString() {
        return "Module \"" + identifier + "\"";
    }

    public static void setModuleLoaderSelector(final ModuleLoaderSelector moduleLoaderSelector) {
        if(moduleLoaderSelector == null) throw new IllegalArgumentException("ModuleLoaderSelector can not be null");
        Module.moduleLoaderSelector = moduleLoaderSelector;
    }

    ModuleContentLoader getContentLoader() {
        return contentLoader;
    }

    Map<String, List<Dependency>> getPathsToImports() {
        return pathsToImports;
    }
}
