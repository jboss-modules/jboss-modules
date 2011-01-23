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

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;

/**
 * <p>
 * Test {@link ClassPathModuleLoader}.
 * </p>
 *
 * @author <a href="mailto:olaf.bergner@gmx.de">Olaf Bergner</a>
 */
public class ClassPathModuleLoaderTest extends AbstractModuleTestCase {

    private static final String CLASSPATH_REPOSITORY_ROOT_DIR = "test/repo/";

    private ModuleLoader moduleLoader;

    @Before
    public void setupModuleLoader() throws Exception {
        final File classPathRepositoryRootDir = getResource(CLASSPATH_REPOSITORY_ROOT_DIR);
        final URLClassLoader classLoaderDelegate = new URLClassLoader(new URL[]{classPathRepositoryRootDir.toURI().toURL()});
        this.moduleLoader = new ClassPathModuleLoader(classLoaderDelegate, "");
    }

    @Test
    public void testBasicLoad() throws Exception {
        Module module = this.moduleLoader.loadModule(MODULE_ID);
        assertNotNull("loadModule(" + MODULE_ID + ") returned null for a valid module identifier", module);
    }

    @Test
    public void testLoadWithDeps() throws Exception {
        final ModuleIdentifier moduleWithDepsId = ModuleIdentifier.fromString("test.with-deps");
        Module module = this.moduleLoader.loadModule(moduleWithDepsId);
        assertNotNull("loadModule(" + moduleWithDepsId
                + ") returned null for a module identifier pointing to a module having dependencies", module);
    }

    @Test(expected = ModuleNotFoundException.class)
    public void testLoadWithBadDeps() throws Exception {
        this.moduleLoader.loadModule(ModuleIdentifier.fromString("test.bad-deps.1_0"));
    }

    @Test
    public void testLoadWithCircularDeps() throws Exception {
        final ModuleIdentifier circularDepsA = ModuleIdentifier.fromString("test.circular-deps-A");
        assertNotNull(
                "loadModule("
                        + circularDepsA
                        + ") returned null for a module identifier pointing to a module " +
                        "participating in a circular dependencies chain",
                this.moduleLoader.loadModule(circularDepsA));

        final ModuleIdentifier circularDepsB = ModuleIdentifier.fromString("test.circular-deps-B");
        assertNotNull(
                "loadModule("
                        + circularDepsB
                        + ") returned null for a module identifier pointing to a module " +
                        "participating in a circular dependencies chain",
                this.moduleLoader.loadModule(circularDepsB));

        final ModuleIdentifier circularDepsC = ModuleIdentifier.fromString("test.circular-deps-C");
        assertNotNull(
                "loadModule("
                        + circularDepsC
                        + ") returned null for a module identifier pointing to a module " +
                        "participating in a circular dependencies chain",
                this.moduleLoader.loadModule(circularDepsC));

        final ModuleIdentifier circularDepsD = ModuleIdentifier.fromString("test.circular-deps-D");
        assertNotNull(
                "loadModule("
                        + circularDepsD
                        + ") returned null for a module identifier pointing to a module " +
                        "participating in a circular dependencies chain",
                this.moduleLoader.loadModule(circularDepsD));
    }
}

