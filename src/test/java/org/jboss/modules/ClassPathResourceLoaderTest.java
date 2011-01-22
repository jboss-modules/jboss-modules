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

import org.jboss.modules.filter.PathFilter;
import org.junit.Assert;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * <p>
 * Test {@link ClassPathResourceLoader}.
 * </p>
 *
 * @author <a href="mailto:olaf.bergner@gmx.de">Olaf Bergner</a>
 */
public class ClassPathResourceLoaderTest extends AbstractResourceLoaderTestCase {

    private static final String REPOSITORY_ROOT_DIR = "test/classpathresourceloader/";

    private static final String MODULE_ROOT_PATH = "test/test/main/";

    private static final String RESOURCE_ROOT_PATH = "resourceroot/";

    private File repositoryRootDir;

    @Override
    protected ResourceLoader createLoader(PathFilter exportFilter) throws Exception {
        // Copy the classfile over
        copyResource("org/jboss/modules/test/TestClass.class", REPOSITORY_ROOT_DIR + MODULE_ROOT_PATH + RESOURCE_ROOT_PATH,
                "org/jboss/modules/test");

        this.repositoryRootDir = getResource(REPOSITORY_ROOT_DIR);
        final URLClassLoader classLoaderDelegate = new URLClassLoader(new URL[]{this.repositoryRootDir.toURI().toURL()});

        return new ClassPathResourceLoader(classLoaderDelegate, MODULE_ID, RESOURCE_ROOT_PATH, RESOURCE_ROOT_PATH);
    }

    @Override
    protected void assertResource(Resource resource, String fileName) {
        try {
            final File expectedFile = new File(this.repositoryRootDir, MODULE_ROOT_PATH + RESOURCE_ROOT_PATH + fileName);
            Assert.assertEquals(expectedFile.toURI().toURL(), resource.getURL());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
