/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Collection;
import java.util.Collections;
import org.jboss.modules.util.TestModuleLoader;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 */
public class ClassDefinerTest extends AbstractModuleTestCase {

    private TestModuleLoader moduleLoader;

    @Before
    public void setupModuleLoader() {
        moduleLoader = new TestModuleLoader();
        ModuleSpec.Builder builder = ModuleSpec.build("org.module.foo");
        builder.addDependency(ModuleDependencySpec.JAVA_BASE);
        builder.addDependency(DependencySpec.OWN_DEPENDENCY);
        builder.addResourceRoot(ResourceLoaderSpec.createResourceLoaderSpec(new ResourceLoader() {
            public ClassSpec getClassSpec(final String fileName) {
                return null;
            }

            public PackageSpec getPackageSpec(final String name) {
                return null;
            }

            public Resource getResource(final String name) {
                return null;
            }

            public String getLibrary(final String name) {
                return null;
            }

            public Collection<String> getPaths() {
                return Collections.singletonList("org/module/foo");
            }
        }));
        moduleLoader.addModuleSpec(builder.create());
    }

    @Test
    public void testDefineClass() throws ModuleLoadException, IOException, ClassNotFoundException {
        final ClassDefiner classDefiner = ClassDefiner.getInstance();
        final Module module = moduleLoader.loadModule("org.module.foo");
        final URL resource = ClassDefinerTest.class.getClassLoader().getResource("test/GeneratedClass.class");
        assertNotNull(resource);
        final byte[] classBytes;
        try (final InputStream stream = resource.openConnection().getInputStream()) {
            try (final ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                byte[] buf = new byte[256];
                int res;
                while ((res = stream.read(buf)) != -1) {
                    os.write(buf, 0, res);
                }
                classBytes = os.toByteArray();
            }
        }
        Class<?> c1 = classDefiner.defineClass(module, "org.module.foo.GeneratedClass", new ProtectionDomain(
                new CodeSource(
                        resource, (CodeSigner[]) null
                ),
                ModulesPolicy.DEFAULT_PERMISSION_COLLECTION
        ), classBytes);
        Class<?> c2 = module.getClassLoaderPrivate().loadClass("org.module.foo.GeneratedClass");
        assertEquals(c1, c2);
        assertEquals(module.getClassLoaderPrivate(), c1.getClassLoader());
    }
}
