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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
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

    private final ModuleIdentifier identifier;
    private final List<Dependency> dependencies;
    private final ModuleContentLoader contentLoader;
    private final String mainClassName;
    private final ModuleClassLoader moduleClassLoader;
    private final ModuleLoader moduleLoader;
    private final Set<Flag> flags;

    Module(final ModuleSpec spec, final List<Dependency> dependencies, final Set<Flag> flags, final ModuleLoader moduleLoader) {
        this.moduleLoader = moduleLoader;
        identifier = spec.getIdentifier();
        contentLoader = spec.getContentLoader();
        mainClassName = spec.getMainClass();
        this.dependencies = dependencies;
        this.flags = flags;
        // should be safe, so...
        //noinspection ThisEscapedInObjectConstruction
        moduleClassLoader = new ModuleClassLoader(this, flags, spec.getAssertionSetting());
    }

    public final Class<?> getExportedClass(String className) {
        try {
            return moduleClassLoader.loadClass(className);
            // TODO: Need to make sure we should export the class (Maybe use Module.forClass(class)
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    Class<?> getImportedClass(final String className) {
        for(Dependency dependency : dependencies) {
            final Module module = dependency.getModule();
            Class<?> importedClass = module.getExportedClass(className);
            if(importedClass != null)
                return importedClass;
        }
        return null;
    }

    ClassSpec getLocalClassSpec(String className) throws IOException {
        return contentLoader.getClassSpec(className);
    }

    public final Resource getExportedResource(final String resourcePath) {
        return contentLoader.getResource(resourcePath);
    }

    public final Iterable<Resource> getExportedResources(final String resourcePath) {
        // todo filter...
        return contentLoader.getResources(resourcePath);
    }

    public final Resource getExportedResource(final String rootPath, final String resourcePath) {
        // todo filter...
        return contentLoader.getResource(rootPath, resourcePath);
    }

    public final void run(final String[] args) throws NoSuchMethodException, InvocationTargetException {
        try {
            if (mainClassName == null) {
                throw new NoSuchMethodException("No main class defined for " + this);
            }
            final Class<?> mainClass = getExportedClass(mainClassName);
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
     * Get the module for a loaded class, or {@code null} if the class did not come from any module.
     *
     * @param clazz the class
     * @return the module it came from
     */
    public static Module forClass(Class<?> clazz) {
        final ClassLoader cl = clazz.getClassLoader();
        return cl instanceof ModuleClassLoader ? ((ModuleClassLoader) cl).getModule() : null;
    }

    PackageSpec getLocalPackageSpec(final String name) throws IOException {
        return contentLoader.getPackageSpec(name);
    }

    String getLocalLibrary(final String libname) {
        return contentLoader.getLibrary(libname);
    }

    public static Module getModule(final ModuleIdentifier identifier) throws ModuleLoadException {
        return InitialModuleLoader.INSTANCE.loadModule(identifier);
    }

    public enum Flag {
        // flags here
        CHILD_FIRST,
        NO_BLACKLIST,
    }

    public String toString() {
        return "Module \"" + identifier + "\"";
    }
}
