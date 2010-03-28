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
import java.lang.reflect.Method;
import java.net.URL;
import java.security.SecureClassLoader;
import java.util.ArrayDeque;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Queue;

/**
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 */
public final class ModuleClassLoader extends SecureClassLoader {

    static {
        try {
            final Method method = ClassLoader.class.getMethod("registerAsParallelCapable");
            method.invoke(null);
        } catch (Exception e) {
            // ignore
        }
    }

    private final Module module;
    private final boolean childFirst;

    public ModuleClassLoader(Module module, boolean childFirst) {
        this.module = module;
        this.childFirst = childFirst;
    }

    @Override
    protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
        if (Thread.holdsLock(this) && Thread.currentThread() != LoaderThreadHolder.LOADER_THREAD) {
            // Only the classloader thread may take this lock; use a condition to relinquish it
            final LoadRequest req = new LoadRequest(className, resolve, this);
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
            Class<?> loadedClass = findClass(className);
            if (loadedClass != null && resolve) resolveClass(loadedClass);
            return loadedClass;
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        // Check if we have already loaded it..
        Class<?> loadedClass = findLoadedClass(name);

        Class<?> clazz = null;
        if (childFirst) {
            clazz = loadClassLocal(name);
            if (clazz == null) {
                clazz = module.getImportedClass(name);
            }
        } else {
            clazz = module.getImportedClass(name);
            if (clazz == null) {
                clazz = loadClassLocal(name);
            }
        }
        if (clazz == null)
            throw new ClassNotFoundException(name);
        return clazz;
    }

    private Class<?> loadClassLocal(String name) throws ClassNotFoundException {
        // Check to see if we can load it
        ClassSpec classSpec = null;
        try {
            classSpec = module.getLocalClassSpec(name);
        } catch (IOException e) {
            throw new ClassNotFoundException(name, e);
        }
        if (classSpec == null)
            return null;
        return defineClass(name, classSpec.getBytes(), 0, classSpec.getBytes().length, classSpec.getCodeSource());
    }

    @Override
    public URL getResource(String name) {
        return module.getExportedResource(name).getURL();
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        final Iterable<Resource> resources = module.getExportedResources(name);
        final Iterator<Resource> iterator = resources.iterator();

        return new Enumeration<URL>() {
            @Override
            public boolean hasMoreElements() {
                return iterator.hasNext();
            }

            @Override
            public URL nextElement() {
                return iterator.next().getURL();
            }
        };
    }

    private static final class LoaderThreadHolder {

        private static final Thread LOADER_THREAD;
        private static final Queue<LoadRequest> REQUEST_QUEUE = new ArrayDeque<LoadRequest>();

        static {
            Thread thr = new LoaderThread();
            thr.setName("Module ClassLoader Thread");
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
        private final ModuleClassLoader requester;
        private Class<?> result;
        private boolean resolve;
        private boolean done;

        public LoadRequest(final String className, final boolean resolve, final ModuleClassLoader requester) {
            this.className = className;
            this.resolve = resolve;
            this.requester = requester;
        }
    }

    static class LoaderThread extends Thread {
        @Override
        public void run() {
            final Queue<LoadRequest> queue = LoaderThreadHolder.REQUEST_QUEUE;
            for (; ;) {
                try {
                    LoadRequest request;
                    synchronized (queue) {
                        while ((request = queue.poll()) == null) {
                            try {
                                queue.wait();

                            } catch (InterruptedException e) {
                            }
                        }
                    }

                    final ModuleClassLoader loader = request.requester;
                    Class<?> result = null;
                    synchronized (loader) {
                        try {
                            result = loader.loadClass(request.className, request.resolve);
                        }
                        finally {
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
