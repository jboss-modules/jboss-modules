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

/**
 * Test case to verify the concurrent classloader base correctly handles common concurrency issues with classloading..
 *
 * @author John E. Bailey
 */
public class ConcurrentClassLoaderTest {
    volatile Throwable threadOneProblem;
    volatile Throwable threadTwoProblem;

    @Test
    public void testClassLoadingDeadlockAvoidance() throws Throwable {
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
                } catch (Throwable t) {
                    threadOneProblem = t;
                }
            }
        });
        final Thread threadTwo = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    latch.await();
                    classLoaderTwo.loadClass(ClassC.class.getName());
                } catch (Throwable t) {
                    threadTwoProblem = t;
                }
            }
        });
        threadOne.start();
        threadTwo.start();

        latch.countDown();

        threadOne.join();
        threadTwo.join();

        if (threadOneProblem != null) throw threadOneProblem;
        if (threadTwoProblem != null) throw threadTwoProblem;
    }

    private static final class TestConcurrentClassLoader extends ConcurrentClassLoader {
        static {
            boolean parallelOk = true;
            try {
                parallelOk = ClassLoader.registerAsParallelCapable();
            } catch (Throwable ignored) {
            }
            if (! parallelOk) {
                throw new Error("Failed to register " + TestConcurrentClassLoader.class.getName() + " as parallel-capable");
            }
        }

        private final ClassLoader realLoader;
        private final Set<String> allowedClasses = new HashSet<String>();
        private ClassLoader delegate;

        private TestConcurrentClassLoader(final ClassLoader realLoader, final Collection<String> allowedClasses) {
            this.realLoader = realLoader;
            this.allowedClasses.addAll(allowedClasses);
        }

        @Override
        protected Class<?> findClass(String className, boolean exportsOnly, final boolean resolve) throws ClassNotFoundException {
            Class<?> c = findLoadedClass(className);
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
}
