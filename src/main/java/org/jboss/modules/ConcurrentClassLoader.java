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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A classloader which can delegate to multiple other classloaders without risk of deadlock.  A concurrent class loader
 * should only ever be delegated to by another concurrent class loader; however a concurrent class loader <i>may</i>
 * delegate to a standard hierarchical class loader.  In other words, holding a lock on another class loader while invoking
 * a method on this class loader may cause an unexpected deadlock.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class ConcurrentClassLoader extends NamedClassLoader {

    private static final ThreadLocal<Boolean> GET_PACKAGE_SUPPRESSOR = new ThreadLocal<Boolean>();

    static {
        if (! ClassLoader.registerAsParallelCapable()) {
            throw new Error("Failed to register " + ConcurrentClassLoader.class.getName() + " as parallel-capable");
        }
        /*
         This resolves a known deadlock that can occur if one thread is in the process of defining a package as part of
         defining a class, and another thread is defining the system package that can result in loading a class.  One holds
         the Package.pkgs lock and one holds the Classloader lock.
        */
        Package.getPackages();
    }

    /**
     * An empty enumeration, for subclasses to use if desired.
     */
    protected static final Enumeration<URL> EMPTY_ENUMERATION = Collections.enumeration(Collections.<URL>emptySet());

    /**
     * Construct a new instance with the given parent class loader, which must be a concurrent class loader, or {@code null}
     * to create a root concurrent class loader.
     *
     * @param parent the parent class loader
     */
    protected ConcurrentClassLoader(final ConcurrentClassLoader parent) {
        this(parent, null);
    }

    /**
     * Construct a new instance, using our class loader as the parent.
     */
    protected ConcurrentClassLoader() {
        this((String) null);
    }

    /**
     * Construct a new instance with the given parent class loader, which must be a concurrent class loader, or {@code null}
     * to create a root concurrent class loader.
     *
     * @param parent the parent class loader
     * @param name the name of this class loader, or {@code null} if it is unnamed
     */
    protected ConcurrentClassLoader(final ConcurrentClassLoader parent, final String name) {
        super(parent == null ? JDKSpecific.getPlatformClassLoader() : parent, name);
        if (! JDKSpecific.isParallelCapable(this)) {
            throw new Error("Cannot instantiate non-parallel subclass");
        }
    }

    /**
     * Construct a new instance, using our class loader as the parent.
     *
     * @param name the name of this class loader, or {@code null} if it is unnamed
     */
    protected ConcurrentClassLoader(String name) {
        super(JDKSpecific.getPlatformClassLoader(), name);
        if (! JDKSpecific.isParallelCapable(this)) {
            throw new Error("Cannot instantiate non-parallel subclass");
        }
    }

    static Object getLockForClass(ConcurrentClassLoader cl, String name) {
        return cl.getClassLoadingLock(name);
    }

    /**
     * Loads the class with the specified binary name.  Equivalent to calling {@link #loadClass(String, boolean) loadClass(className, false)}.
     *
     * @param className The binary name of the class
     * @return the resulting {@code Class} instance
     * @throws ClassNotFoundException if the class was not found
     */
    @Override
    public final Class<?> loadClass(final String className) throws ClassNotFoundException {
        return performLoadClass(className, false, false);
    }

    /**
     * Loads the class with the specified binary name.
     *
     * @param className The binary name of the class
     * @param resolve {@code true} if the class should be linked after loading
     * @return the resulting {@code Class} instance
     */
    @Override
    public final Class<?> loadClass(final String className, boolean resolve) throws ClassNotFoundException {
        return performLoadClass(className, false, resolve);
    }

    /**
     * Same as {@link #loadClass(String)}, except only exported classes will be considered.
     *
     * @param className the class name
     * @return the class
     * @throws ClassNotFoundException if the class isn't found
     */
    public final Class<?> loadExportedClass(final String className) throws ClassNotFoundException {
        return performLoadClass(className, true, false);
    }

    /**
     * Same as {@link #loadClass(String,boolean)}, except only exported classes will be considered.
     *
     * @param className the class name
     * @param resolve {@code true} if the class should be linked after loading
     * @return the class
     * @throws ClassNotFoundException if the class isn't found
     */
    public final Class<?> loadExportedClass(final String className, boolean resolve) throws ClassNotFoundException {
        return performLoadClass(className, true, resolve);
    }

    /**
     * Find a class, possibly delegating to other loader(s).  This method should <b>never</b> synchronize across a
     * delegation method call of any sort.  The default implementation always throws {@code ClassNotFoundException}.
     * <p>
     * If a class is to be defined by this method, it should be done via one of the atomic {@code defineOrLoadClass}
     * methods rather than {@code defineClass()} in order to avoid spurious exceptions.
     *
     * @param className the class name
     * @param exportsOnly {@code true} if only exported classes should be considered
     * @param resolve {@code true} if the class should be linked after loading
     * @return the class
     * @throws ClassNotFoundException if the class is not found
     */
    protected Class<?> findClass(final String className, final boolean exportsOnly, final boolean resolve) throws ClassNotFoundException {
        throw new ClassNotFoundException(className);
    }

    /**
     * Atomically define or load the named class.  If the class is already defined, the existing class is returned.
     *
     * @param className the class name to define or load
     * @param bytes the bytes to use to define the class
     * @param off the offset into the byte array at which the class bytes begin
     * @param len the number of bytes in the class
     * @return the class
     */
    protected final Class<?> defineOrLoadClass(final String className, final byte[] bytes, int off, int len) {
        try {
            final Class<?> definedClass = defineClass(className, bytes, off, len);
            return definedClass;
        } catch (LinkageError e) {
            final Class<?> loadedClass = findLoadedClass(className);
            if (loadedClass != null) {
                return loadedClass;
            }
            throw e;
        }
    }

    /**
     * Atomically define or load the named class.  If the class is already defined, the existing class is returned.
     *
     * @param className the class name to define or load
     * @param bytes the bytes to use to define the class
     * @param off the offset into the byte array at which the class bytes begin
     * @param len the number of bytes in the class
     * @param protectionDomain the protection domain for the defined class
     * @return the class
     */
    protected final Class<?> defineOrLoadClass(final String className, final byte[] bytes, int off, int len, ProtectionDomain protectionDomain) {
        try {
            return defineClass(className, bytes, off, len, protectionDomain);
        } catch (LinkageError e) {
            final Class<?> loadedClass = findLoadedClass(className);
            if (loadedClass != null) {
                return loadedClass;
            }
            throw e;
        }
    }

    /**
     * Implementation of {@link ClassLoader#findClass(String)}.
     *
     * @param className the class name
     * @return the result of {@code findClass(className, false, false)}
     */
    protected final Class<?> findClass(final String className) throws ClassNotFoundException {
        return findClass(className, false, false);
    }

    /**
     * Implementation of {@link ClassLoader#findClass(String, String)}.
     *
     * @param moduleName the Java module name
     * @param className the class name
     * @return the result of {@code findClass(className, false, false)}
     */
    protected final Class<?> findClass(final String moduleName, final String className) {
        try {
            return findClass(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * Finds the resource with the given name.  The name of a resource is a {@code '/'}-separated path name that
     * identifies the resource.  If the resource name starts with {@code "java/"} then the parent class loader is used.
     * Otherwise, this method delegates to {@link #findResource(String, boolean)}.
     *
     * @param name the name of the resource
     * @return the resource URL, or {@code null} if no such resource exists or the invoker does not have adequate
     * permission to access it
     */
    public final URL getResource(final String name) {
        for (String s : Module.systemPaths) {
            if (name.startsWith(s)) {
                return JDKSpecific.getSystemResource(name);
            }
        }
        return findResource(name, false);
    }

    /**
     * Finds all available resources with the given name.
     *
     * @see #getResource(String)
     *
     * @param name the resource name
     * @return an enumeration over all the resource URLs; if no resources could be found, the enumeration will be empty
     * @throws IOException if an I/O error occurs
     */
    public final Enumeration<URL> getResources(final String name) throws IOException {
        for (String s : Module.systemPaths) {
            if (name.startsWith(s)) {
                return JDKSpecific.getSystemResources(name);
            }
        }
        return findResources(name, false);
    }

    /**
     * Find the resource with the given name and exported status.
     *
     * @see #getResource(String)
     *
     * @param name the resource name
     * @param exportsOnly {@code true} to consider only exported resources or {@code false} to consider all resources
     * @return the resource URL
     */
    protected URL findResource(final String name, final boolean exportsOnly) {
        return null;
    }

    /**
     * Never used.  {@link ClassLoader#getResource(String)} and related methods can cause a loop condition
     * when this method is implemented; use {@link #findResource(String, boolean)} instead.
     *
     * @param name ignored
     * @return {@code null} always
     */
    protected final URL findResource(final String name) {
        // Always return null so that we don't go into a loop from super.getResource*().
        return null;
    }

    /**
     * Find the resource with the given name in specified java module.
     *
     * @see #getResource(String)
     *
     * @param moduleName java module name
     * @param name the resource name
     * @return the resource URL
     */
    protected final URL findResource(final String moduleName, final String name) throws IOException {
        return getResource(name);
    }

    /**
     * Finds the resources with the given name and exported status.
     *
     * @see #getResources(String)
     *
     * @param name the resource name
     * @param exportsOnly {@code true} to consider only exported resources or {@code false} to consider all resources
     * @return the resource enumeration
     * @throws IOException if an I/O error occurs
     */
    protected Enumeration<URL> findResources(final String name, final boolean exportsOnly) throws IOException {
        return EMPTY_ENUMERATION;
    }

    /**
     * Never used.  {@link ClassLoader#getResources(String)} and related methods can cause a loop condition
     * when this method is implemented; use {@link #findResources(String, boolean)} instead.  By default, returns
     * an empty enumeration.
     *
     * @param name ignored
     * @return an empty enumeration
     */
    protected final Enumeration<URL> findResources(final String name) {
        return EMPTY_ENUMERATION;
    }

    /**
     * Finds the resource with the given name and exported status, returning the resource content as a stream.
     *
     * @param name the resource name
     * @param exportsOnly {@code true} to consider only exported resources or {@code false} to consider all resources
     * @return the resource stream, or {@code null} if the resource is not found
     */
    protected InputStream findResourceAsStream(final String name, final boolean exportsOnly) {
        final URL url = findResource(name, exportsOnly);
        try {
            return url == null ? null : url.openStream();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Returns an input stream for reading the specified resource.  This method delegates to {@link #findResourceAsStream(String, boolean)}.
     *
     * @param name the resource name
     * @return the resource stream, or {@code null} if the resource is not found
     */
    public final InputStream getResourceAsStream(final String name) {
        for (String s : Module.systemPaths) {
            if (name.startsWith(s)) {
                return JDKSpecific.getSystemResourceAsStream(name);
            }
        }
        return findResourceAsStream(name, false);
    }

    // Private members

    /**
     * Perform a class load operation.  If the class is in the package or a subpackage of a package in the system packages list,
     * the parent class loader is used to load the class.  Otherwise, this method checks to see if the class loader
     * object is locked; if so, it unlocks it and submits the request to the class loader thread.  Otherwise, it will
     * load the class itself by delegating to {@link #findClass(String, boolean, boolean)}.
     *
     * @param className the class name
     * @param exportsOnly {@code true} to consider only exported resources or {@code false} to consider all resources
     * @param resolve {@code true} to resolve the loaded class
     * @return the class returned by {@link #findClass(String, boolean, boolean)}
     * @throws ClassNotFoundException if {@link #findClass(String, boolean, boolean)} throws this exception
     */
    private Class<?> performLoadClass(String className, boolean exportsOnly, final boolean resolve) throws ClassNotFoundException {

        if (className == null) {
            throw new IllegalArgumentException("name is null");
        }
        if (className.length() == 0) {
            throw new IllegalArgumentException("name is empty");
        }
        for (String s : Module.systemPackages) {
            if (className.startsWith(s)) {
                return JDKSpecific.getSystemClass(this, className);
            }
        }
        return performLoadClassUnchecked(className, exportsOnly, resolve);
    }

    private Class<?> performLoadClassUnchecked(final String className, final boolean exportsOnly, final boolean resolve) throws ClassNotFoundException {
        if (className.charAt(0) == '[') {
            // Use Class.forName to load the array type
            final Class<?> array = Class.forName(className, false, this);
            if (resolve) {
                resolveClass(array);
            }
            return array;
        }
        return findClass(className, exportsOnly, resolve);
    }

    private final ConcurrentHashMap<String, Package> packages = new ConcurrentHashMap<>();

    Class<?> findSystemClassInternal(String name) throws ClassNotFoundException {
        return findSystemClass(name);
    }

    /**
     * Load a package which is visible to this class loader.
     *
     * @param name the package name
     * @return the package, or {@code null} if no such package is visible to this class loader
     */
    protected final Package getPackage(final String name) {
        final String packageName = name + ".";
        for (String s : Module.systemPackages) {
            if (packageName.startsWith(s)) {
                return Package.getPackage(name);
            }
        }
        if (GET_PACKAGE_SUPPRESSOR.get() == Boolean.TRUE) {
            return null;
        }
        return getPackageByName(name);
    }

    /**
     * Perform the actual work to load a package which is visible to this class loader.  By default, uses a simple
     * parent-first delegation strategy.
     *
     * @param name the package name
     * @return the package, or {@code null} if no such package is visible to this class loader
     */
    protected Package getPackageByName(final String name) {
        final Package parentPackage = super.getPackage(name);
        return parentPackage == null ? findLoadedPackage(name) : parentPackage;
    }

    /**
     * Get all defined packages which are visible to this class loader.
     *
     * @return the packages
     */
    protected Package[] getPackages() {
        ArrayList<Package> list = new ArrayList<Package>();
        list.addAll(packages.values());
        list.addAll(Arrays.asList(super.getPackages()));
        return list.toArray(new Package[list.size()]);
    }

    /**
     * Load a package from this class loader only.
     *
     * @param name the package name
     * @return the package or {@code null} if no such package is defined by this class loader
     */
    protected final Package findLoadedPackage(final String name) {
        return packages.get(name);
    }

    /**
     * Defines a package by name in this <tt>ConcurrentClassLoader</tt>.  If the package was already defined, the
     * existing package is returned instead.
     *
     * @param name the package name
     * @param specTitle the specification title
     * @param specVersion the specification version
     * @param specVendor the specification vendor
     * @param implTitle the implementation title
     * @param implVersion the implementation version
     * @param implVendor the implementation vendor
     * @param sealBase if not {@code null}, then this package is sealed with respect to the given code source URL
     *
     * @return the newly defined package, or the existing one if one was already defined
     */
    protected Package definePackage(final String name, final String specTitle, final String specVersion, final String specVendor, final String implTitle, final String implVersion, final String implVendor, final URL sealBase) throws IllegalArgumentException {
        ThreadLocal<Boolean> suppressor = GET_PACKAGE_SUPPRESSOR;
        suppressor.set(Boolean.TRUE);
        try {
            Package existing = packages.get(name);
            if (existing != null) {
                return existing;
            }
            Package pkg;
            try {
                pkg = super.definePackage(name, specTitle, specVersion, specVendor, implTitle, implVersion, implVendor, sealBase);
            } catch (final IllegalArgumentException iae) {
                pkg = super.getPackage(name);
                if (pkg == null) throw iae;
            }
            existing = packages.putIfAbsent(name, pkg);
            return existing != null ? existing : pkg;
        } finally {
            suppressor.remove();
        }
    }
}
