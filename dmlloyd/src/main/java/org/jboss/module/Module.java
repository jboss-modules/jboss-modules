/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.module;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class Module {

    private final URI uri;
    private final ModuleContentLoader loader;
    private final ImportingClassLoader importingClassLoader;
    private final Module[] imports;
    private final Module[] exports;
    private final EnumSet<Flag> flagSet;
    private final ClassFilter importClassFilter;
    private final ClassFilter exportClassFilter;
    private final ConcurrentMap<String, FutureClass> exportedClassCache = new ConcurrentHashMap<String, FutureClass>();
    private final String mainClassName;
    private final ModuleClassLoader classLoader;

    Module(final ModuleSpec specification, Module[] resolvedImports, Module[] resolvedExports) {
        loader = specification.getLoader();
        imports = resolvedImports;
        exports = resolvedExports;
        importClassFilter = specification.getImportClassFilter();
        exportClassFilter = specification.getExportClassFilter();
        uri = specification.getModuleUri();
        flagSet = EnumSet.copyOf(specification.getModuleFlags());
        //noinspection ThisEscapedInObjectConstruction
        importingClassLoader = new ImportingClassLoader(this);
        mainClassName = specification.getMainClass();
        classLoader = new ModuleClassLoader(this);
    }

    public void runMain(final String[] args) throws InvocationTargetException {
        final String mainClassName = this.mainClassName;
        if (mainClassName == null) {
            throw new IllegalArgumentException("No main method in module " + uri);
        }
        try {
            Class.forName(mainClassName, true, importingClassLoader).getMethod("main", String[].class).invoke(null, args);
        } catch (ClassNotFoundException e) {
            throw new NoClassDefFoundError(e.getMessage());
        } catch (NoSuchMethodException e) {
            throw new NoSuchMethodError(e.getMessage());
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public URI getUri() {
        return uri;
    }

    public final Resource getExportedResource(final String resourcePath) {
        final Iterator<Resource> i = getExportedResources(resourcePath).iterator();
        return i.hasNext() ? i.next() : null;
    }

    public final Iterable<Resource> getExportedResources(final String resourcePath) {
        // todo filter...
        return loader.getResources(resourcePath);
    }

    public Resource getExportedResource(final String rootPath, final String resourcePath) {
        // todo filter...
        return loader.getResource(rootPath, resourcePath);
    }

    Class<?> getImportedClass(final String name) {
        
        return null;
    }

    ModuleContentLoader getLoader() {
        return loader;
    }

    public URL getURL(final Resource resource, final String name) {
        try {
            return new URI("module", null, null, 0, resource.getRoot(), name, null).toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static enum Flag {
        CHILD_FIRST,
        NO_IMPORT_CACHING,
        NO_BLACKLIST_CACHING,
        NO_LOCAL_CACHING,
        DISABLE_JAVA_BYPASS,
        ENABLE_ASSERTIONS,
    }

    public final Class<?> getExportedClass(String name) {
        Class<?> clazz = null;
        if (flagSet.contains(Flag.CHILD_FIRST) && ! name.startsWith("java/")) {
            clazz = loadExportedClass(name);
            if (clazz == null) {
                clazz = importClass(name);
            }
        } else {
            clazz = importClass(name);
            if (clazz == null) {
                clazz = loadExportedClass(name);
            }
        }
        if (clazz == null) {
            futureClass.setNotFound();
        } else {
            futureClass.setResult(clazz);
        }
        return clazz;
    }

    protected Class<?> loadExportedClass(final String name) {
        final ClassLoader classLoader = this.classLoader;
        synchronized (classLoader) {
            try {
                return classLoader.loadClass(name);
            } catch (ClassNotFoundException e) {
                return null;
            }
        }
    }

    protected Class<?> importClass(final String name) {
        final Module[] imports = this.imports;
        for (Module module : imports) {
            final Class<?> exportedClass = module.getExportedClass(name);
            if (exportedClass != null) {
                return exportedClass;
            }
        }
        return null;
    }

    public ClassLoader getClassLoader() {
        return exportClassLoader;
    }

    public static final Module[] NONE = new Module[0];

    public static Module forClass(Class<?> clazz) {
        return clazz == null ? null : forClassLoader(clazz.getClassLoader());
    }

    public static Module forClassLoader(ClassLoader classLoader) {
        return classLoader == null ? null : classLoader instanceof ModuleClassLoader ? ((ModuleClassLoader) classLoader).getModule() : forClassLoader(classLoader.getParent());
    }

    public static Module require(Module module) throws ModuleNotFoundException {
        if (module == null) {
            throw new ModuleNotFoundException();
        }
        return module;
    }
}
