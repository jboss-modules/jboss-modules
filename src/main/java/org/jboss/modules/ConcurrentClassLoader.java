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
import java.lang.reflect.Field;
import java.net.URL;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Queue;
import sun.misc.Unsafe;

/**
 * A classloader which can delegate to multiple other classloaders without risk of deadlock.  A concurrent class loader
 * should only ever be delegated to by another concurrent class loader; however a concurrent class loader <i>may</i>
 * delegate to a standard hierarchical class loader.  In other words, holding a lock on another class loader while invoking
 * a method on this class loader may cause an unexpected deadlock.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class ConcurrentClassLoader extends ClassLoader {

    private static final boolean LOCKLESS;
    private static final boolean SAFE_JDK;
    private static final boolean JDK7_PLUS;

    private static final ClassLoader definingLoader = ConcurrentClassLoader.class.getClassLoader();

    private static final ThreadLocal<Boolean> GET_PACKAGE_SUPPRESSOR = new ThreadLocal<Boolean>();

    static {
        boolean jdk7plus = false;
        boolean parallelOk = true;
        try {
            jdk7plus = parallelOk = ClassLoader.registerAsParallelCapable();
        } catch (Throwable ignored) {
        }
        if (! parallelOk) {
            throw new Error("Failed to register " + ConcurrentClassLoader.class.getName() + " as parallel-capable");
        }
        /*
         This resolves a known deadlock that can occur if one thread is in the process of defining a package as part of
         defining a class, and another thread is defining the system package that can result in loading a class.  One holds
         the Package.pkgs lock and one holds the Classloader lock.
        */
        Package.getPackages();
        // Determine whether to run in lockless mode
        boolean hasUnsafe = false;
        // For 1.6, we require Unsafe to perform lockless stuff
        if (! jdk7plus) try {
            Class.forName("sun.misc.Unsafe", false, null);
            hasUnsafe = true;
        } catch (Throwable t) {
            // ignored
        }
        final boolean isJRockit = AccessController.doPrivileged(new PropertyReadAction("java.vm.name", "")).toUpperCase(Locale.US).contains("JROCKIT");
        // But the user is always right, so if they override, respect it
        LOCKLESS = Boolean.parseBoolean(AccessController.doPrivileged(new PropertyReadAction("jboss.modules.lockless", Boolean.toString(! jdk7plus && hasUnsafe && ! isJRockit))));
        // If the JDK has safe CL, set this flag
        SAFE_JDK = Boolean.parseBoolean(AccessController.doPrivileged(new PropertyReadAction("jboss.modules.safe-jdk", Boolean.toString(jdk7plus || isJRockit))));
        JDK7_PLUS = jdk7plus;
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
        super(parent == null ? ConcurrentClassLoader.class.getClassLoader() : parent);
        if (JDK7_PLUS) {
            if (getClassLoadingLock("$TEST$") == this) {
                throw new Error("Cannot instantiate non-parallel subclass");
            }
        }
    }

    /**
     * Construct a new instance, using our class loader as the parent.
     */
    protected ConcurrentClassLoader() {
        super(ConcurrentClassLoader.class.getClassLoader());
        if (JDK7_PLUS) {
            if (getClassLoadingLock("$TEST$") == this) {
                throw new Error("Cannot instantiate non-parallel subclass");
            }
        }
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
            final Class<?> definedClass = defineClass(className, bytes, off, len, protectionDomain);
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
     * Implementation of {@link ClassLoader#findClass(String)}.
     *
     * @param className the class name
     * @return the result of {@code findClass(className, false, false)}
     */
    protected final Class<?> findClass(final String className) throws ClassNotFoundException {
        return findClass(className, false, false);
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
                // Whatever loads jboss-modules.jar should have it's classes accessible.
                // This could even be the bootclasspath, in which case CL is null, and we prefer the system CL
                return definingLoader != null ? definingLoader.getResource(name) : ClassLoader.getSystemResource(name);
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
                return definingLoader != null ? definingLoader.getResources(name) : ClassLoader.getSystemResources(name);
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
     * Returns an input stream for reading the specified resource.  If the resource starts with {@code "java/"}, then
     * this method delegates to the parent class loader.  Otherwise, this method delegates to {@link #findResourceAsStream(String, boolean)}.
     *
     * @param name the resource name
     * @return the resource stream, or {@code null} if the resource is not found
     */
    public final InputStream getResourceAsStream(final String name) {
        for (String s : Module.systemPaths) {
            if (name.startsWith(s)) {
                return definingLoader != null ? definingLoader.getResourceAsStream(name) : ClassLoader.getSystemResourceAsStream(name);
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
        for (String s : Module.systemPackages) {
            if (className.startsWith(s)) {
                return definingLoader != null ? definingLoader.loadClass(className) : findSystemClass(className);
            }
        }
        return performLoadClassChecked(className, exportsOnly, resolve);
    }

    /**
     * Perform a class load operation.  This method checks to see if the class loader object is locked; if so, it
     * unlocks it and submits the request to the class loader thread.  Otherwise, it will load the class itself by
     * delegating to {@link #findClass(String, boolean, boolean)}.
     * <p>
     * If the {@code jboss.modules.unsafe-locks} system property is set to {@code true}, then rather than using the
     * class loading thread, the lock is forcibly broken and the load retried.
     *
     * @param className the class name
     * @param exportsOnly {@code true} to consider only exported resources or {@code false} to consider all resources
     * @param resolve {@code true} to resolve the loaded class
     * @return the class returned by {@link #findClass(String, boolean, boolean)}
     * @throws ClassNotFoundException if {@link #findClass(String, boolean, boolean)} throws this exception
     */
    private Class<?> performLoadClassChecked(final String className, final boolean exportsOnly, final boolean resolve) throws ClassNotFoundException {
        if (SAFE_JDK) {
            return performLoadClassUnchecked(className, exportsOnly, resolve);
        } else if (Thread.holdsLock(this)) {
            if (LOCKLESS) {
                final Unsafe unsafe = UnsafeHolder.UNSAFE;
                unsafe.monitorExit(this);
                try {
                    return performLoadClassChecked(className, exportsOnly, resolve);
                } finally {
                    unsafe.monitorEnter(this);
                }
            }
            if (Thread.currentThread() != LoaderThreadHolder.LOADER_THREAD) {
                // Only the classloader thread may take this lock; use a condition to relinquish it
                final LoadRequest req = new LoadRequest(className, resolve, exportsOnly, this, AccessController.getContext());
                final Queue<LoadRequest> queue = LoaderThreadHolder.REQUEST_QUEUE;
                synchronized (queue) {
                    queue.add(req);
                    queue.notify();
                }
                boolean intr = false;
                try {
                    while (!req.done) try {
                        wait();
                    } catch (InterruptedException e) {
                        intr = true;
                    }
                } finally {
                    if (intr) Thread.currentThread().interrupt();
                }

                final Class<?> result = req.result;
                if (result == null) {
                    final String message = req.message;
                    throw new ClassNotFoundException(message == null ? className : message);
                }
                return result;
            }
        }
        // no deadlock risk!  Either the lock isn't held, or we're inside the class loader thread.
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

    private final UnlockedReadHashMap<String, Package> packages = new UnlockedReadHashMap<String, Package>(64);

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
            Package pkg = super.definePackage(name, specTitle, specVersion, specVendor, implTitle, implVersion, implVendor, sealBase);
            existing = packages.putIfAbsent(name, pkg);
            return existing != null ? existing : pkg;
        } finally {
            suppressor.remove();
        }
    }

    static final class LoaderThreadHolder {

        static final Thread LOADER_THREAD;
        static final Queue<LoadRequest> REQUEST_QUEUE = new ArrayDeque<LoadRequest>();

        static {
            Thread thr = new LoaderThread();
            thr.setName("ClassLoader Thread");
            // This thread will always run as long as the VM is alive.
            thr.setDaemon(true);
            thr.start();
            LOADER_THREAD = thr;
        }

        private LoaderThreadHolder() {
        }
    }

    static class LoadRequest {
        private final String className;
        private final boolean resolve;
        private final ConcurrentClassLoader requester;
        private final AccessControlContext context;
        Class<?> result;
        String message;
        private boolean exportsOnly;

        boolean done;

        LoadRequest(final String className, final boolean resolve, final boolean exportsOnly, final ConcurrentClassLoader requester, final AccessControlContext context) {
            this.className = className;
            this.resolve = resolve;
            this.exportsOnly = exportsOnly;
            this.requester = requester;
            this.context = context;
        }
    }

    static class LoaderThread extends Thread {

        @Override
        public void interrupt() {
            // no interruption
        }

        @Override
        public void run() {
            final Queue<LoadRequest> queue = LoaderThreadHolder.REQUEST_QUEUE;
            for (; ;) {
                try {
                    LoadRequest request;
                    synchronized (queue) {
                        while ((request = queue.poll()) == null) {
                            queue.wait();
                        }
                    }

                    final ConcurrentClassLoader loader = request.requester;
                    Class<?> result = null;
                    synchronized (loader) {
                        try {
                            final SecurityManager sm = System.getSecurityManager();
                            if (sm != null) {
                                final LoadRequest localRequest = request;
                                result = AccessController.doPrivileged(new PrivilegedExceptionAction<Class<?>>() {
                                    public Class<?> run() throws ClassNotFoundException {
                                        return loader.performLoadClassChecked(localRequest.className, localRequest.exportsOnly, localRequest.resolve);
                                    }
                                }, request.context);
                            } else try {
                                result = loader.performLoadClassChecked(request.className, request.exportsOnly, request.resolve);
                            } catch (ClassNotFoundException e) {
                                request.message = e.getMessage();
                            }
                        } finally {
                            // no matter what, the requester MUST be notified
                            request.result = result;
                            request.done = true;
                            loader.notifyAll();
                        }
                    }
                } catch (Throwable t) {
                    // ignore
                }
            }
        }
    }

    private static final class UnsafeHolder {
        static Unsafe UNSAFE;

        static {
            try {
                final Field field = Unsafe.class.getDeclaredField("theUnsafe");
                field.setAccessible(true);
                UNSAFE = (Unsafe) field.get(null);
            } catch (IllegalAccessException e) {
                throw new IllegalAccessError(e.getMessage());
            } catch (NoSuchFieldException e) {
                throw new NoSuchFieldError(e.getMessage());
            }
        }

        private UnsafeHolder() {
        }
    }
}
