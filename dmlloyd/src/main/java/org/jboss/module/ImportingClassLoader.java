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

package org.jboss.module;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.SecureClassLoader;
import java.util.ArrayDeque;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Queue;

final class ImportingClassLoader extends SecureClassLoader {

    private final Module module;

    static {
        try {
            final Method method = ClassLoader.class.getMethod("registerAsParallelCapable");
            method.invoke(null);
        } catch (Exception e) {
            // ignore
        }
    }

    private static final class LoaderThreadHolder {

        private static final Thread classLoaderThread;
        private static final Queue<LoadRequest> REQUEST_QUEUE = new ArrayDeque<LoadRequest>();

        static {
            Thread thr = new LoaderThread();
            thr.setName("Module ClassLoader Thread");
            // This thread will always run as long as the VM is alive.
            thr.setDaemon(true);
            thr.start();
            classLoaderThread = thr;
        }

        private LoaderThreadHolder() {
        }
    }

    private static final class LoadRequest {
        private final String name;
        private final boolean resolve;
        private final ImportingClassLoader requester;
        private Class<?> result;
        private boolean done;

        private LoadRequest(final String name, final boolean resolve, final ImportingClassLoader requester) {
            this.name = name;
            this.resolve = resolve;
            this.requester = requester;
        }

        public Class<?> getResult() {
            return result;
        }

        public boolean isDone() {
            return done;
        }
    }

    protected ImportingClassLoader(final Module module) {
        this.module = module;
        // todo - don't clear if user expressly wants to inherit -ea
        clearAssertionStatus();
    }

    protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
        if (Thread.holdsLock(this) && Thread.currentThread() != LoaderThreadHolder.classLoaderThread) {
            // Only the classloader thread may take this lock; use a condition to relinquish it
            final LoadRequest req = new LoadRequest(name, resolve, this);
            final Queue<LoadRequest> queue = LoaderThreadHolder.REQUEST_QUEUE;
            synchronized (queue) {
                queue.add(req);
                queue.notify();
            }
            boolean intr = false;
            try {
                while (! req.isDone()) try {
                    wait();
                } catch (InterruptedException e) {
                    intr = true;
                }
            } finally {
                if (intr) Thread.currentThread().interrupt();
            }
            final Class<?> result = req.getResult();
            if (result == null) {
                throw new ClassNotFoundException(name);
            }
            return result;
        } else {
            // no deadlock risk!  Either the lock isn't held, or we're inside the class loader thread.
            final Class<?> c = findClass(name);
            if (resolve) resolveClass(c);
            return c;
        }
    }

    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        Class<?> result = module.getImportedClass(name);
        if (result == null) {
            throw new ClassNotFoundException(name);
        }
        return result;
    }

    public URL getResource(final String name) {
        // todo - flag for child-first resource loading?
        return super.getResource(name);
    }

    public Enumeration<URL> getResources(final String name) throws IOException {
        return findResources(name);
    }

    protected URL findResource(final String name) {
        final Enumeration<URL> e;
        try {
            e = findResources(name);
        } catch (IOException e1) {
            return null;
        }
        if (e.hasMoreElements()) {
            return e.nextElement();
        } else {
            return null;
        }
    }

    protected Enumeration<URL> findResources(final String name) throws IOException {
        final Iterable<Resource> list = module.getLoader().getResources(name);
        final Iterator<Resource> i = list.iterator();
        return new Enumeration<URL>() {
            public boolean hasMoreElements() {
                return i.hasNext();
            }

            public URL nextElement() {
                return module.getURL(i.next(), name);
            }
        };
    }

    protected String findLibrary(final String libname) {
        return module.getLoader().getLibrary(libname);
    }

    private static final class LoaderThread extends Thread {

        public final void interrupt() {
            // no interruption
        }

        public void run() {
            final Queue<LoadRequest> queue = LoaderThreadHolder.REQUEST_QUEUE;
            for (;;) try {
                LoadRequest request;
                synchronized (queue) {
                    while ((request = queue.poll()) == null) {
                        try {
                            queue.wait();
                        } catch (InterruptedException e) {
                            // impossible, but in any case ignoring is safe
                        }
                    }
                }
                final ImportingClassLoader loader = request.requester;
                Class<?> result = null;
                synchronized (loader) {
                    try {
                        result = loader.loadClass(request.name, request.resolve);
                    } catch (ClassNotFoundException e) {
                        // just leave result null
                    } finally {
                        // no matter what, the requester MUST be notified
                        // todo - separate exception type if an error occurs?
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
