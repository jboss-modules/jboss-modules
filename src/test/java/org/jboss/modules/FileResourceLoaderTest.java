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

import org.jboss.modules.filter.PathFilter;
import org.junit.Assert;

import java.io.File;
import java.security.AccessController;

/**
 * Test the functionality of the FileResourceLoader
 *
 * @author John Bailey
 */
public class FileResourceLoaderTest extends AbstractResourceLoaderTestCase {

    private File resourceRoot;

    protected ResourceLoader createLoader(final PathFilter exportFilter) throws Exception {
        resourceRoot = getResource("test/fileresourceloader");
        // Copy the classfile over
        copyResource("org/jboss/modules/test/TestClass.class", "test/fileresourceloader", "org/jboss/modules/test");
        return new FileResourceLoader("test-root", resourceRoot, AccessController.getContext());
    }

    @Override
    protected void assertResource(Resource resource, String fileName) {
        final File resourceFile = getExpectedFile(fileName);

        Assert.assertEquals(resourceFile.length(), resource.getSize());
    }

    public void testGetClassSpec() throws Exception {
        super.testGetClassSpec();
    }

    protected File getExpectedFile(String fileName) {
        return new File(resourceRoot, fileName);
    }
}
