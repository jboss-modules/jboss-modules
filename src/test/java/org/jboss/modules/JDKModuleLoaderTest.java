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

import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

/**
 */
public class JDKModuleLoaderTest extends AbstractModuleTestCase {
    @Test
    public void testLoadModulesClassFile() throws ModuleLoadException {
        final Module module = Module.getSystemModuleLoader().loadModule("org.jboss.modules");
        final URL resource = module.getClassLoader().getResource("org/jboss/modules/Main.class");
        Assert.assertNotNull("Main.class", resource);
    }

    /**
     * Tests that loading of "java.se" module works and classes that are present
     * in modules "required" by this "java.se" module can be loaded too
     *
     * @throws Exception
     */
    @Test
    public void testJavaSeModuleLoad() throws Exception {
        final Module module = Module.getBootModuleLoader().loadModule("java.se");
        Assert.assertNotNull("java.se module not found", module);
        final String resultSetClassName = "java.sql.ResultSet";
        final Class<?> klass = module.getClassLoader().loadClass(resultSetClassName);
        Assert.assertNotNull(resultSetClassName + " class couldn't be loaded from java.se module", klass);

        // test a class that was introduced in Java 11
        String specString = System.getProperty("java.specification.version");
        Pattern pat = Pattern.compile("(?:1\\.)?(\\d+)");
        Matcher matcher = pat.matcher(specString);
        Assume.assumeTrue("Java 11 is required to test loading of classes " +
                "in java.net.http module", matcher.matches() && Integer.parseInt(matcher.group(1)) >= 11);
        final String httpClientClassName = "java.net.http.HttpClient";
        final Class<?> httpClientClass = module.getClassLoader().loadClass(httpClientClassName);
        Assert.assertNotNull(httpClientClassName + " class couldn't be loaded from java.se module", httpClientClass);
    }
}
