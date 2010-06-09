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
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class ModuleClassLoader extends ConcurrentClassLoader {
    private static final boolean debugDefines;

    static {
        try {
            final Method method = ClassLoader.class.getMethod("registerAsParallelCapable");
            method.invoke(null);
        } catch (Exception e) {
            // ignore
        }
        debugDefines = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            public Boolean run() {
                return Boolean.valueOf(System.getProperty("jboss.modules.debug.defineClass", "false"));
            }
        }).booleanValue();
    }

    private final Module module;
    private final Set<Module.Flag> flags;

    ModuleClassLoader(final Module module, final Set<Module.Flag> flags, final AssertionSetting setting) {
        this.module = module;
        this.flags = flags;
        if (setting != AssertionSetting.INHERIT) {
            setDefaultAssertionStatus(setting == AssertionSetting.ENABLED);
        }
    }

    protected Class<?> findClass(String className, boolean exportsOnly) throws ClassNotFoundException {
        // Check if we have already loaded it..
        Class<?> loadedClass = findLoadedClass(className);
        if (loadedClass != null) {
            return loadedClass;
        }
        final Set<Module.Flag> flags = this.flags;
        if (flags.contains(Module.Flag.CHILD_FIRST)) {
            loadedClass = loadClassLocal(className);
            if (loadedClass == null) {
                loadedClass = module.getImportedClass(className, exportsOnly);
            }
            if (loadedClass == null) {
                loadedClass = findSystemClass(className);
            }
        } else {
            loadedClass = module.getImportedClass(className, exportsOnly);
            if (loadedClass == null) try {
                loadedClass = findSystemClass(className);
            } catch (ClassNotFoundException e) {
            }
            if (loadedClass == null) {
                loadedClass = loadClassLocal(className);
            }
        }
        if (loadedClass == null) {
            throw new ClassNotFoundException(className + " from [" + module+ "]");
        }
        return loadedClass;
    }

    private Class<?> loadClassLocal(String name) throws ClassNotFoundException {
        // Check to see if we can load it
        ClassSpec classSpec = null;
        try {
            classSpec = module.getLocalClassSpec(name);
        } catch (IOException e) {
            throw new ClassNotFoundException(name, e);
        } catch (RuntimeException e) {
            System.err.print("Unexpected runtime exception in module loader: ");
            e.printStackTrace(System.err);
            throw new ClassNotFoundException(name, e);
        } catch (Error e) {
            System.err.print("Unexpected error in module loader: ");
            e.printStackTrace(System.err);
            throw new ClassNotFoundException(name, e);
        }
        if (classSpec == null)
            return null;
        return defineClass(name, classSpec);
    }

    private Class<?> defineClass(final String name, final ClassSpec classSpec) {
        // Ensure that the package is loaded
        final int lastIdx = name.lastIndexOf('.');
        if (lastIdx != -1) {
            // there's a package name; get the Package for it
            final String packageName = name.substring(0, lastIdx);
            final Package pkg = getPackage(packageName);
            if (pkg != null) {
                // Package is defined already
                if (pkg.isSealed() && ! pkg.isSealed(classSpec.getCodeSource().getLocation())) {
                    // use the same message as the JDK
                    throw new SecurityException("sealing violation: package " + packageName + " is sealed");
                }
            } else {
                final PackageSpec spec;
                try {
                    spec = getModule().getLocalPackageSpec(name);
                    definePackage(packageName, spec);
                } catch (IOException e) {
                    definePackage(packageName, null);
                }
            }
        }
        final Class<?> newClass;
        try {
            final byte[] bytes = classSpec.getBytes();
            newClass = defineClass(name, bytes, 0, bytes.length, classSpec.getCodeSource());
        } catch (Error e) {
            if (debugDefines) System.err.println("Failed to define class '" + name + "': " + e);
            throw e;
        } catch (RuntimeException e) {
            if (debugDefines) System.err.println("Failed to define class '" + name + "': " + e);
            throw e;
        }
        final AssertionSetting setting = classSpec.getAssertionSetting();
        if (setting != AssertionSetting.INHERIT) {
            setClassAssertionStatus(name, setting == AssertionSetting.ENABLED);
        }
        return newClass;
    }

    private Package definePackage(final String name, final PackageSpec spec) {
        if (spec == null) {
            return definePackage(name, null, null, null, null, null, null, null);
        } else {
            final Package pkg = definePackage(name, spec.getSpecTitle(), spec.getSpecVersion(), spec.getSpecVendor(), spec.getImplTitle(), spec.getImplVersion(), spec.getImplVendor(), spec.getSealBase());
            final AssertionSetting setting = spec.getAssertionSetting();
            if (setting != AssertionSetting.INHERIT) {
                setPackageAssertionStatus(name, setting == AssertionSetting.ENABLED);
            }
            return pkg;
        }
    }

    @Override
    protected String findLibrary(final String libname) {
        return module.getLocalLibrary(libname);
    }

    @Override
    public URL findResource(String name, boolean exportsOnly) {
        URL resource = null;
        final Set<Module.Flag> flags = this.flags;
        if (flags.contains(Module.Flag.CHILD_FIRST)) {
            resource = module.getLocalResource(name);
            if (resource == null) {
                resource = module.getImportedResource(name, exportsOnly);
            }
        } else {
            resource = module.getImportedResource(name, exportsOnly);
            if (resource == null) {
                resource = module.getLocalResource(name);
            }
        }
        return resource;
    }

    @Override
    public Enumeration<URL> findResources(String name, boolean exportsOnly) throws IOException {
        final Set<URL> resources = new HashSet<URL>();
        resources.addAll(module.getLocalResources(name));
        resources.addAll(module.getImportedResources(name, exportsOnly));
        final Iterator<URL> iterator = resources.iterator();

        return new Enumeration<URL>() {
            @Override
            public boolean hasMoreElements() {
                return iterator.hasNext();
            }

            @Override
            public URL nextElement() {
                return iterator.next();
            }
        };
    }

    @Override
    public InputStream findResourceAsStream(final String name, boolean exportsOnly) {
        try {
            final URL resource = module.getExportedResource(name);
            return resource == null ? null : resource.openStream();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Get the module for this class loader.
     *
     * @return the module
     */
    public Module getModule() {
        return module;
    }

    public String toString() {
        return "ClassLoader for " + module;
    }

    public static ModuleClassLoader forModule(ModuleIdentifier identifier) throws ModuleLoadException {
        return Module.getModule(identifier).getClassLoader();
    }

    public static ModuleClassLoader forModuleName(String identifier) throws ModuleLoadException {
        return forModule(ModuleIdentifier.fromString(identifier));
    }

    public static ModuleClassLoader createAggregate(String identifier, List<String> dependencies) throws ModuleLoadException  {

        List<ModuleIdentifier> depModuleIdentifiers = new ArrayList<ModuleIdentifier>(dependencies.size());
        for(String dependencySpec : dependencies) {
            depModuleIdentifiers.add(ModuleIdentifier.fromString(dependencySpec));
        }
        return InitialModuleLoader.INSTANCE.createAggregate(ModuleIdentifier.fromString(identifier), depModuleIdentifiers).getClassLoader();
    }
}
