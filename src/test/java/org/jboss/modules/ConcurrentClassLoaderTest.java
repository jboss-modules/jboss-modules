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

import org.jboss.modules.test.ClassA;
import org.jboss.modules.test.ClassB;
import org.jboss.modules.test.ClassC;
import org.jboss.modules.test.ClassD;
import org.jboss.modules.util.Util;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.fail;

/**
 * Test case to verify the concurrent classloader base correctly handles common concurrency issues with classloading..
 *  
 * @author John E. Bailey
 */
public class ConcurrentClassLoaderTest {

    private final CountDownLatch latch = new CountDownLatch(1);

    @Test
    public void testClassLoadingDeadlockAvoidance() throws Exception {
        /*
            Uncomment the following lines to demonstrate a deadlock that occurs with normal classloader delegation
         */
        //final DeadLockingLoader classLoaderOne = new DeadLockingLoader(ConcurrentClassLoaderTest.class.getClassLoader(), Arrays.asList(ClassA.class.getName(), ClassD.class.getName()));
        //final DeadLockingLoader classLoaderTwo = new DeadLockingLoader(ConcurrentClassLoaderTest.class.getClassLoader(), Arrays.asList(ClassB.class.getName(), ClassC.class.getName()));
        final TestConcurrentClassLoader classLoaderOne = new TestConcurrentClassLoader(ConcurrentClassLoaderTest.class.getClassLoader(), Arrays.asList(ClassA.class.getName(), ClassD.class.getName()));
        final TestConcurrentClassLoader classLoaderTwo = new TestConcurrentClassLoader(ConcurrentClassLoaderTest.class.getClassLoader(), Arrays.asList(ClassB.class.getName(), ClassC.class.getName()));        classLoaderOne.delegate = classLoaderTwo;
        classLoaderTwo.delegate = classLoaderOne;

        final CountDownLatch latch = new CountDownLatch(1);
        final Thread threadOne = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    latch.await();
                    classLoaderOne.loadClass(ClassA.class.getName());
                } catch(Exception e) {
                    fail(e.getMessage());
                }
            }
        });
        final Thread threadTwo = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    latch.await();
                    classLoaderTwo.loadClass(ClassC.class.getName());
                } catch(Exception e) {
                    fail(e.getMessage());
                }
            }
        });
        threadOne.start();
        threadTwo.start();

        latch.countDown();

        threadOne.join();
        threadTwo.join();
    }

    private static final class TestConcurrentClassLoader extends ConcurrentClassLoader {
        private final ClassLoader realLoader;
        private final Set<String> allowedClasses = new HashSet<String>();
        private ClassLoader delegate;

        private TestConcurrentClassLoader(final ClassLoader realLoader, final Collection<String> allowedClasses) {
            this.realLoader = realLoader;
            this.allowedClasses.addAll(allowedClasses);
        }

        @Override
        protected Class<?> findClass(String className, boolean exportsOnly) throws ClassNotFoundException {
            Class c = findLoadedClass(className);
            if(c == null && className.startsWith("java"))
                c = findSystemClass(className);
            if(c == null && allowedClasses.contains(className)) {
                try {
                    final byte[] classBytes = Util.getClassBytes(realLoader.loadClass(className));
                    c = defineClass(className, classBytes, 0, classBytes.length);
                } catch(Throwable t) {
                    throw new ClassNotFoundException("Failed to load class " + className, t);
                }
            }
            if(c == null)
                c =  delegate.loadClass(className);
            return c;
        }
    };

    private static final class DeadLockingLoader extends ClassLoader {
        private final ClassLoader realLoader;
        private final Set<String> allowedClasses = new HashSet<String>();
        private ClassLoader delegate;

        private DeadLockingLoader(final ClassLoader realLoader, final Collection<String> allowedClasses) {
            super(null);
            this.realLoader = realLoader;
            this.allowedClasses.addAll(allowedClasses);
        }

        @Override
        protected synchronized Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
            Class c = findLoadedClass(name);
            if(c == null && name.startsWith("java"))
                c = findSystemClass(name);
            if(c == null) {
                c = findClass(name);
            }
            if(resolve) {
                resolveClass(c);
            }
            return c;
        }

        @Override
        protected Class<?> findClass(String className) throws ClassNotFoundException {
            try {
                if(allowedClasses.contains(className)) {
                    final byte[] classBytes = Util.getClassBytes(realLoader.loadClass(className));
                    return defineClass(className, classBytes, 0, classBytes.length);
                }
                return delegate.loadClass(className);
            } catch(Throwable t) {
                throw new ClassNotFoundException("Failed to load class " + className, t);
            }
        }
    };
}
