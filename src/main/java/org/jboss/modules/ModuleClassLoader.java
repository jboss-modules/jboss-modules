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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author thomas.diesler@jboss.com
 */
public class ModuleClassLoader extends ConcurrentClassLoader {
   
    private ModuleLogger log = NoopModuleLogger.getInstance();
    
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
    private final ModuleContentLoader contentLoader;

    ModuleClassLoader(final Module module, final Set<Module.Flag> flags, final AssertionSetting setting, final ModuleContentLoader contentLoader) {
        this.module = module;
        this.flags = flags;
        this.contentLoader = contentLoader;
        if (setting != AssertionSetting.INHERIT) {
            setDefaultAssertionStatus(setting == AssertionSetting.ENABLED);
        }
    }

    public void setModuleLogger(ModuleLogger logger) {
       log = (logger != null ? logger : NoopModuleLogger.getInstance());
    }

    protected Class<?> findClass(String className, boolean exportsOnly) throws ClassNotFoundException {
        // Check if we have already loaded it..
        Class<?> loadedClass = findLoadedClass(className);
        if (loadedClass != null) {
            return loadedClass;
        }
        
        boolean traceEnabled = log.isTraceEnabled();
        if (traceEnabled == true)
           log.trace("findClass [" + className + "] from [" + module + "] ...");
        
        final Set<Module.Flag> flags = this.flags;
        if (flags.contains(Module.Flag.CHILD_FIRST)) {
           
           if (traceEnabled == true)
              log.trace("Attempting to loadClassLocal [" + className + "] from [" + module + "] ...");
           
            loadedClass = loadClassLocal(className, exportsOnly);
            if (loadedClass != null) {
               if (traceEnabled == true)
                  log.trace("Found loadClassLocal [" + className + "] from [" + module + "]");
            }
            
            if (loadedClass == null) {
               
               if (traceEnabled == true)
                  log.trace("Attempting to getImportedClass [" + className + "] from [" + module + "] ...");
               
                loadedClass = getImportedClass(className, exportsOnly);
                if (loadedClass != null) {
                   if (traceEnabled == true)
                      log.trace("Found getImportedClass [" + className + "] from [" + module + "]");
                }
            }
        } else {
           
           if (traceEnabled == true)
              log.trace("Attempting to getImportedClass [" + className + "] from [" + module + "] ...");
           
            loadedClass = getImportedClass(className, exportsOnly);
            if (loadedClass != null) {
               if (traceEnabled == true)
                  log.trace("Found getImportedClass [" + className + "] from [" + module + "]");
            }
            
            if (loadedClass == null) {
               
               if (traceEnabled == true)
                  log.trace("Attempting to loadClassLocal [" + className + "] from [" + module + "] ...");
               
                loadedClass = loadClassLocal(className, exportsOnly);
                if (loadedClass != null) {
                   if (traceEnabled == true)
                      log.trace("Found loadClassLocal [" + className + "] from [" + module + "]");
                }
            }
        }
        if (loadedClass == null) {
           
           if (traceEnabled == true)
              log.trace("Class not found [" + className + "] from [" + module + "]");
           
            throw new ClassNotFoundException(className + " from [" + module + "]");
        }
        return loadedClass;
    }

    Class<?> getImportedClass(final String className, final boolean exportsOnly) {
        boolean traceEnabled = log.isTraceEnabled();

        final Map<String, List<Module.DependencyImport>> pathsToImports = module.getPathsToImports();

        final String path = getPathFromClassName(className);

        final List<Module.DependencyImport> dependenciesForPath = pathsToImports.get(path);
        if(dependenciesForPath == null) {
           if (traceEnabled == true)
              log.trace("No dependencies for path [" + path + "] from [" + module + "]");
            return null;
        }

        for(Module.DependencyImport dependencyImport : dependenciesForPath) {
            final Module dependency = dependencyImport.getModule();
            if(exportsOnly && !dependencyImport.isExport())
                continue;

            if (traceEnabled == true)
               log.trace("Attempting to getImportedClass [" + className + "] from [" + dependency + "] ...");

            try {
                Class<?> importedClass = dependency.getClassLoader().loadClassLocal(className, true, exportsOnly);
                if(importedClass != null) {
                   if (traceEnabled == true)
                      log.trace("Found getImportedClass [" + className + "] from [" + dependency + "]");
                    return importedClass;
                }
            } catch (ClassNotFoundException ignored){}
        }
        return null;
    }

    private Class<?> loadClassLocal(final String className, final boolean exportOnly) throws ClassNotFoundException {
        return loadClassLocal(className, false, exportOnly);
    }

    private Class<?> loadClassLocal(final String className, final boolean checkPreviouslyLoaded, final boolean exportOnly) throws ClassNotFoundException {
        if(checkPreviouslyLoaded) {
            // Check if we have already loaded it..
            Class<?> loadedClass = findLoadedClass(className);
            if (loadedClass != null) {
                return loadedClass;
            }
        }

        if(exportOnly) {
            final String path = getPathFromClassName(className);
            final Set<String> exportedPaths = module.getExportedPaths();
            if(!exportedPaths.contains(path))
                return null;
        }

        // Check to see if we can load it
        ClassSpec classSpec = null;
        try {
            classSpec = contentLoader.getClassSpec(className);
        } catch (IOException e) {
            throw new ClassNotFoundException(className, e);
        } catch (RuntimeException e) {
            System.err.print("Unexpected runtime exception in module loader: ");
            e.printStackTrace(System.err);
            throw new ClassNotFoundException(className, e);
        } catch (Error e) {
            System.err.print("Unexpected error in module loader: ");
            e.printStackTrace(System.err);
            throw new ClassNotFoundException(className, e);
        }
        if (classSpec == null)
            return null;
        return defineClass(className, classSpec);
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
                    spec = contentLoader.getPackageSpec(name);
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

    private String getPathFromClassName(final String className) {
        int idx =  className.lastIndexOf('.');
        return idx > -1 ? className.substring(0, idx).replace('.', '/') : "" ;
    }

    @Override
    protected String findLibrary(final String libname) {
        return contentLoader.getLibrary(libname);
    }

    Resource getRawResource(final String root, final String name) {
        return contentLoader.getResource(root, name);
    }

    @Override
    public URL findResource(final String name, final boolean exportsOnly) {
        URL resource = null;
        final Set<Module.Flag> flags = this.flags;
        if (flags.contains(Module.Flag.CHILD_FIRST)) {
            resource = getLocalResource(name, exportsOnly);
            if (resource == null) {
                resource = getImportedResource(name, exportsOnly);
            }
        } else {
            resource = getImportedResource(name, exportsOnly);
            if (resource == null) {
                resource = getLocalResource(name, exportsOnly);
            }
        }
        return resource;
    }

    final URL getImportedResource(final String resourcePath, final boolean exportsOnly) {
        final Map<String, List<Module.DependencyImport>> pathsToImports = module.getPathsToImports();

        final String path = getPathFromResourceName(resourcePath);

        final List<Module.DependencyImport> dependenciesForPath = pathsToImports.get(path);
        if(dependenciesForPath == null)
            return null;

        for(Module.DependencyImport dependencyImport : dependenciesForPath) {
            final Module dependency = dependencyImport.getModule();
            if(exportsOnly && !dependencyImport.isExport())
                continue;

            URL importedResource = dependency.getClassLoader().findResource(resourcePath, true);
            if(importedResource != null)
                return importedResource;
        }
        return null;
    }

    final URL getLocalResource(final String resourcePath, final boolean exportOnly) {
        if(exportOnly) {
            final String path = getPathFromResourceName(resourcePath);
            final Set<String> exportedPaths = module.getExportedPaths();
            if(!exportedPaths.contains(path))
                return null;
        }
        final Resource localResource = contentLoader.getResource(resourcePath);
        return localResource != null ? localResource.getURL() : null;
    }

    @Override
    public Enumeration<URL> findResources(final String name, final boolean exportsOnly) throws IOException {
        final Set<URL> resources = new HashSet<URL>();
        resources.addAll(getLocalResources(name, exportsOnly));
        resources.addAll(getImportedResources(name, exportsOnly));
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

    final Collection<URL> getLocalResources(final String resourcePath, final boolean exportOnly) {
        if(exportOnly) {
            final String path = getPathFromResourceName(resourcePath);
            final Set<String> exportedPaths = module.getExportedPaths();
            if(!exportedPaths.contains(path))
                return Collections.emptyList();
        }

        Iterable<Resource> resources = contentLoader.getResources(resourcePath);
        if(resources != null) {
            final List<URL> urls = new ArrayList<URL>();
            for(Resource resource : resources) {
                urls.add(resource.getURL());
            }
            return urls;
        }
        return Collections.emptyList();
    }

    final Collection<URL> getImportedResources(final String resourcePath, final boolean exportsOnly) throws IOException {
        final Map<String, List<Module.DependencyImport>> pathsToImports = module.getPathsToImports();

        final String path = getPathFromResourceName(resourcePath);

        final List<Module.DependencyImport> dependenciesForPath = pathsToImports.get(path);
        if(dependenciesForPath == null)
            return Collections.emptySet();

        final Set<URL> importedUrls = new HashSet<URL>();
        for(Module.DependencyImport dependencyImport : dependenciesForPath) {
            final Module dependency = dependencyImport.getModule();
            if(exportsOnly && !dependencyImport.isExport())
                continue;

            final Enumeration<URL> importedResources = dependency.getClassLoader().findResources(resourcePath, true);
            if(importedResources != null) {
                while(importedResources.hasMoreElements()) {
                    final URL importedResource = importedResources.nextElement();
                    importedUrls.add(importedResource);
                }
            }
        }
        return importedUrls;
    }

    @Override
    public InputStream findResourceAsStream(final String name, boolean exportsOnly) {
        try {
            final URL resource = findResource(name, exportsOnly);
            return resource == null ? null : resource.openStream();
        } catch (IOException e) {
            return null;
        }
    }

    private String getPathFromResourceName(final String resourcePath) {
        int idx =  resourcePath.lastIndexOf('/');
        final String path = idx > -1 ? resourcePath.substring(0, idx) : resourcePath;
        return path;
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

    public static ModuleClassLoader forModule(final ModuleIdentifier identifier) throws ModuleLoadException {
        return Module.getModule(identifier).getClassLoader();
    }

    public static ModuleClassLoader forModuleName(final String identifier) throws ModuleLoadException {
        return forModule(ModuleIdentifier.fromString(identifier));
    }

    public static ModuleClassLoader createAggregate(final String identifier, final List<String> dependencies) throws ModuleLoadException  {

        List<ModuleIdentifier> depModuleIdentifiers = new ArrayList<ModuleIdentifier>(dependencies.size());
        for(String dependencySpec : dependencies) {
            depModuleIdentifiers.add(ModuleIdentifier.fromString(dependencySpec));
        }
        return InitialModuleLoader.INSTANCE.createAggregate(ModuleIdentifier.fromString(identifier), depModuleIdentifiers).getClassLoader();
    }
}
