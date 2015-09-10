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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.jboss.modules.ResourceLoaders.createIterableFileResourceLoader;
import static org.jboss.modules.ResourceLoaders.createIterableJarResourceLoader;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Iterator;
import java.util.jar.JarFile;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class ResourceLoadersTest {

    private static final String JAR_RESOURCE_NAME = "WEB-INF/lib/lib.jar";

    private static final String WAR_RESOURCE_NAME = "app.war";

    private static final String EAR_RESOURCE_NAME = "foo.ear";

    private static final String[] JAR_PATHS = {
            "",
            "META-INF",
            "generator",
    };

    private static final String[] WAR_PATHS = {
            "",
            "META-INF",
            "WEB-INF",
            "WEB-INF/classes",
            "WEB-INF/classes/my",
            "WEB-INF/classes/my/company",
            "WEB-INF/lib"
    };

    private static final String[] EAR_PATHS = {
            "",
            "META-INF",
    };

    private static final String[] JAR_RESOURCE_NAMES = {
            "META-INF/MANIFEST.MF",
            "generator/Messages.class",
            "generator/Messages.class",
    };
    private static final String[] WAR_RESOURCE_NAMES = {
            "META-INF/MANIFEST.MF",
            "WEB-INF/web.xml",
            "WEB-INF/classes/my/company/GreetingServlet.class",
            "WEB-INF/classes/my/company/GreetingServlet.java",
    };
    private static final String[] EAR_RESOURCE_NAMES = {
            "META-INF/MANIFEST.MF",
            "META-INF/application.xml",
    };

    @Test
    public void testExplodedEar() throws Exception {
        test("/test/loader-test-exploded/" + EAR_RESOURCE_NAME, WAR_RESOURCE_NAME + "/", JAR_RESOURCE_NAME + "/");
    }

    @Test
    public void testPackagedEar() throws Exception {
        test("/test/loader-test-packaged/" + EAR_RESOURCE_NAME, WAR_RESOURCE_NAME, JAR_RESOURCE_NAME);
    }

    @Test
    public void testMixed1Ear() throws Exception {
        test("/test/loader-test-mixed1/" + EAR_RESOURCE_NAME, WAR_RESOURCE_NAME + "/", JAR_RESOURCE_NAME);
    }

    @Test
    public void testMixed2Ear() throws Exception {
        test("/test/loader-test-mixed2/" + EAR_RESOURCE_NAME, WAR_RESOURCE_NAME, JAR_RESOURCE_NAME + "/");
    }

    private File getResourceRoot(String path) throws URISyntaxException {
        return new File(getClass().getResource(path).toURI());
    }

    private IterableResourceLoader wrap(File resourceRoot) throws IOException {
        if (resourceRoot.isDirectory()) {
            return createIterableFileResourceLoader(EAR_RESOURCE_NAME, resourceRoot);
        } else {
            return createIterableJarResourceLoader(EAR_RESOURCE_NAME, new JarFile(resourceRoot));
        }
    }

    private IterableResourceLoader nest(String subResourceLoaderName, IterableResourceLoader delegate, String subResourcePath) throws IOException {
        return ResourceLoaders.createSubresourceIterableResourceLoader(subResourceLoaderName, delegate, subResourcePath);
    }

    private void dumpResourceLoader(IterableResourceLoader loader) throws Exception {
        synchronized (System.out) {
            System.out.println("------------------------------");
            System.out.println(loader.getRootName() + " paths: ");
            Collection<String> paths = loader.getPaths();
            for (String path : paths) {
                System.out.println(" * " + path);
            }
            System.out.println(loader.getRootName() + " resources: ");
            Iterator<Resource> i = loader.iterateResources("", true);
            Resource r;
            while (i.hasNext()) {
                r = i.next();
                System.out.println(" * " + r.getName());
            }
        }
    }

    private void test(String ear, String war, String jar) throws Exception {
        File earRoot = getResourceRoot(ear);
        IterableResourceLoader earLoader = wrap(earRoot);
        dumpResourceLoader(earLoader);
        // test ear resources
        for (String earRN : EAR_RESOURCE_NAMES) {
            // direct access
            Resource earResource = earLoader.getResource(earRN);
            assertTrue(earResource.getName().equals(earRN));
            assertFalse(earResource.isDirectory());
            // iterator access
            earResource = getResource(earLoader, earRN);
            assertTrue(earResource.getName().equals(earRN));
            assertFalse(earResource.isDirectory());
        }
        boolean explodedWar = war.endsWith("/");
        if (!explodedWar) {
            // direct access
            Resource earResource = earLoader.getResource(war);
            assertTrue(earResource.getName().equals(war));
            assertFalse(earResource.isDirectory());
            // iterator access
            earResource = getResource(earLoader, war);
            assertTrue(earResource.getName().equals(war));
            assertFalse(earResource.isDirectory());
        }
        String warLoaderName = PathUtils.canonicalize(EAR_RESOURCE_NAME + "/" + war);
        IterableResourceLoader warLoader = nest(warLoaderName, earLoader, war);
        dumpResourceLoader(warLoader);
        // test ear paths
        Collection<String> earPaths = earLoader.getPaths();
        for (String earPath : EAR_PATHS) {
            assertTrue(earPaths.contains(earPath));
        }
        earLoader.getPaths();
        // test war resources
        for (String warRN : WAR_RESOURCE_NAMES) {
            // direct access
            Resource warResource = warLoader.getResource(warRN);
            assertTrue(warResource.getName().equals(warRN));
            assertFalse(warResource.isDirectory());
            // iterator access
            warResource = getResource(warLoader, warRN);
            assertTrue(warResource.getName().equals(warRN));
            assertFalse(warResource.isDirectory());
            if (explodedWar) {
                // direct access
                Resource earResource = earLoader.getResource(war + warRN);
                assertTrue(earResource.getName().equals(war + warRN));
                assertFalse(earResource.isDirectory());
                // iterator access
                earResource = getResource(earLoader, war + warRN);
                assertTrue(earResource.getName().equals(war + warRN));
                assertFalse(earResource.isDirectory());
            }
        }
        boolean explodedJar = jar.endsWith("/");
        if (!explodedJar) {
            if (!explodedWar) {
                // direct access
                Resource warResource = warLoader.getResource(jar);
                assertTrue(warResource.getName().equals(jar));
                assertFalse(warResource.isDirectory());
                // iterator access
                warResource = getResource(warLoader, jar);
                assertTrue(warResource.getName().equals(jar));
                assertFalse(warResource.isDirectory());
            } else {
                // direct access
                Resource earResource = earLoader.getResource(war + jar);
                assertTrue(earResource.getName().equals(war + jar));
                assertFalse(earResource.isDirectory());
                // iterator access
                earResource = getResource(earLoader, war + jar);
                assertTrue(earResource.getName().equals(war + jar));
                assertFalse(earResource.isDirectory());
            }
        }
        String jarLoaderName = PathUtils.canonicalize(warLoaderName + "/" + jar);
        IterableResourceLoader jarLoader = nest(jarLoaderName, warLoader, jar);
        dumpResourceLoader(jarLoader);
        // test war paths
        Collection<String> warPaths = warLoader.getPaths();
        for (String warPath : WAR_PATHS) {
            assertTrue(warPaths.contains(warPath));
            if (explodedWar) {
                String earResourceName = warPath.equals("") ? WAR_RESOURCE_NAME : war + warPath;
                assertTrue(earPaths.contains(earResourceName));
            }
        }
        // test jar resources
        for (String jarRN : JAR_RESOURCE_NAMES) {
            // direct access
            Resource jarResource = jarLoader.getResource(jarRN);
            assertTrue(jarResource.getName().equals(jarRN));
            assertFalse(jarResource.isDirectory());
            // iterator access
            jarResource = getResource(jarLoader, jarRN);
            assertTrue(jarResource.getName().equals(jarRN));
            assertFalse(jarResource.isDirectory());
            if (explodedJar) {
                // direct access
                Resource warResource = warLoader.getResource(jar + jarRN);
                assertTrue(warResource.getName().equals(jar + jarRN));
                assertFalse(warResource.isDirectory());
                // iterator access
                warResource = getResource(warLoader, jar + jarRN);
                assertTrue(warResource.getName().equals(jar + jarRN));
                assertFalse(warResource.isDirectory());
                if (explodedWar) {
                    // direct access
                    Resource earResource = earLoader.getResource(war + jar + jarRN);
                    assertTrue(earResource.getName().equals(war + jar + jarRN));
                    assertFalse(earResource.isDirectory());
                    // iterator access
                    earResource = getResource(earLoader, war + jar + jarRN);
                    assertTrue(earResource.getName().equals(war + jar + jarRN));
                    assertFalse(earResource.isDirectory());
                }
            }
        }
        // test jar paths
        Collection<String> jarPaths = jarLoader.getPaths();
        for (String jarPath : JAR_PATHS) {
            assertTrue(jarPaths.contains(jarPath));
            if (explodedJar) {
                String warResourceName = jarPath.equals("") ? JAR_RESOURCE_NAME : jar + jarPath;
                assertTrue(warPaths.contains(warResourceName));
                if (explodedWar) {
                    String earResourceName = jarPath.equals("") ? war + JAR_RESOURCE_NAME : war + jar + jarPath;
                    assertTrue(earPaths.contains(earResourceName));
                }
            }
        }
        // assert loaders paths count
        assertTrue(jarPaths.size() == JAR_PATHS.length);
        if (explodedJar) {
            assertTrue(warPaths.size() == (WAR_PATHS.length + JAR_PATHS.length));
            if (explodedWar) {
                assertTrue(earPaths.size() == (EAR_PATHS.length + WAR_PATHS.length + JAR_PATHS.length));
            } else {
                assertTrue(earPaths.size() == (EAR_PATHS.length));
            }
        } else {
            assertTrue(warPaths.size() == WAR_PATHS.length);
            if (explodedWar) {
                assertTrue(earPaths.size() == (EAR_PATHS.length + WAR_PATHS.length));
            } else {
                assertTrue(earPaths.size() == (EAR_PATHS.length));
            }
        }
        // assert loaders resource iterator size
        int earResourcesCount = getResourcesIteratorSize(earLoader);
        int warResourcesCount = getResourcesIteratorSize(warLoader);
        int jarResourcesCount = getResourcesIteratorSize(jarLoader);
        assertTrue(jarResourcesCount == JAR_RESOURCE_NAMES.length);
        if (explodedJar) {
            assertTrue(warResourcesCount == WAR_RESOURCE_NAMES.length + JAR_RESOURCE_NAMES.length);
            if (explodedWar) {
                assertTrue(earResourcesCount == EAR_RESOURCE_NAMES.length + WAR_RESOURCE_NAMES.length + JAR_RESOURCE_NAMES.length);
            } else {
                assertTrue(earResourcesCount == EAR_RESOURCE_NAMES.length + 1);
            }
        } else {
            assertTrue(warResourcesCount == WAR_RESOURCE_NAMES.length + 1);
            if (explodedWar) {
                assertTrue(earResourcesCount == EAR_RESOURCE_NAMES.length + WAR_RESOURCE_NAMES.length + 1);
            } else {
                assertTrue(earResourcesCount == EAR_RESOURCE_NAMES.length + 1);
            }
        }
    }

    private Resource getResource(IterableResourceLoader loader, String resourceName) {
        Iterator<Resource> i = loader.iterateResources("", true);
        Resource resource;
        while (i.hasNext()) {
            resource = i.next();
            if (resource.getName().equals(resourceName)) return resource;
        }
        throw new IllegalStateException();
    }

    private int getResourcesIteratorSize(IterableResourceLoader loader) {
        Iterator<Resource> i = loader.iterateResources("", true);
        int retVal = 0;
        while (i.hasNext()) {
            i.next();
            retVal++;
        }
        return retVal;
    }

}
