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
import java.net.URL;
import java.security.SecureClassLoader;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Queue;

/**
 * A classloader which can delegate to multiple other classloaders without risk of deadlock.  A concurrent class loader
 * should only ever be delegated to by another concurrent class loader; however a concurrent class loader <i>may</i>
 * delegate to a standard hierarchical class loader.  In other words, holding a lock on another class loader while invoking
 * a method on this class loader may cause an unexpected deadlock.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class ConcurrentClassLoader extends SecureClassLoader {

    /**
     * An empty enumeration, for subclasses to use if desired.
     */
    protected static final Enumeration<URL> EMPTY_ENUMERATION = Collections.enumeration(Collections.<URL>emptySet());

    /** {@inheritDoc} */
    @Override
    public final Class<?> loadClass(final String className) throws ClassNotFoundException {
        return performLoadClass(className, false, false);
    }

    /**
     * Loads the class with the specified binary name.
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
     * @param resolve {@code true} to resolve the class after loading
     * @return the class
     * @throws ClassNotFoundException if the class isn't found
     */
    public final Class<?> loadExportedClass(final String className, boolean resolve) throws ClassNotFoundException {
        return performLoadClass(className, true, resolve);
    }

    /**
     * Find a class, possibly delegating to other loader(s).  This method should <b>never</b> synchronize across a
     * delegation method call of any sort.  The default implementation always throws {@code ClassNotFoundException}.
     *
     * @param className the class name
     * @param exportsOnly {@code true} if only exported classes should be considered
     * @param resolve {@code true} to resolve the loaded class
     * @return the class
     * @throws ClassNotFoundException if the class is not found
     */
    protected Class<?> findClass(final String className, final boolean exportsOnly, final boolean resolve) throws ClassNotFoundException {
        throw new ClassNotFoundException(className);
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
        if (name.startsWith("java/")) {
            return super.getResource(name);
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
        if (name.startsWith("java/")) {
            return super.getResources(name);
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
        if (name.startsWith("java/")) {
            return super.getResourceAsStream(name);
        }
        return findResourceAsStream(name, false);
    }

    // Private members

    /**
     * Perform a class load operation.  If the class is in the {@code "java."} package or one of its subpackages,
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
        if (className.startsWith("java.")) {
            // always delegate to system
            return findSystemClass(className);
        }
        if (Thread.holdsLock(this) && Thread.currentThread() != LoaderThreadHolder.LOADER_THREAD) {
            // Only the classloader thread may take this lock; use a condition to relinquish it
            final LoadRequest req = new LoadRequest(className, resolve, exportsOnly, this);
            final Queue<LoadRequest> queue = LoaderThreadHolder.REQUEST_QUEUE;
            synchronized (LoaderThreadHolder.REQUEST_QUEUE) {
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

            return req.result;
        } else {
            // no deadlock risk!  Either the lock isn't held, or we're inside the class loader thread.
            return findClass(className, exportsOnly, resolve);
        }
    }

    protected static final class LoaderThreadHolder {

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
        Class<?> result;
        private boolean exportsOnly;

        boolean done;

        LoadRequest(final String className, final boolean resolve, final boolean exportsOnly, final ConcurrentClassLoader requester) {
            this.className = className;
            this.resolve = resolve;
            this.exportsOnly = exportsOnly;
            this.requester = requester;
        }
    }

    static class LoaderThread extends Thread {

        @Override
        public void interrupt() {
            // no interruption
        }

        @Override
        public void run() {
            /*
             This resolves a know deadlock that can occur if one thread is in the process of defining a package as part of
             defining a class, and another thread is defining the system package that can result in loading a class.  One holds
             the Package.pkgs lock and one holds the Classloader lock.
            */
            Package.getPackages();

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
                            result = loader.performLoadClass(request.className, request.exportsOnly, request.resolve);
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
}
