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

package org.jboss.modules.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Utility class providing commonly used test utilities.
 *
 * @author John E. Bailey
 */
public class Util {

    public static byte[] readBytes(final InputStream is) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            byte[] buff = new byte[1024];
            int read;
            while((read = is.read(buff)) > -1) {
                os.write(buff, 0, read);
            }
        } finally {
            is.close();
        }
        return os.toByteArray();
    }

    public static URL getResource(final Class<?> baseClass, final String path) throws Exception {
        final URL url = baseClass.getClassLoader().getResource(path);
        return url;
    }

    public static File getResourceFile(final Class<?> baseClass, final String path) throws Exception {
        return new File(getResource(baseClass, path).toURI());
    }

    public static <T> List<T> toList(Enumeration<T> enumeration) {
        final List<T> list = new ArrayList<T>();
        while(enumeration.hasMoreElements()) {
            list.add(enumeration.nextElement());
        }
        return list;
    }

    public static byte[] getClassBytes(final Class<?> aClass) throws Exception {
        final String resourcePath = getResourceNameOfClass(aClass);
        final File classFile = Util.getResourceFile(aClass, resourcePath);
        byte[] classBytes = Util.readBytes(new FileInputStream(classFile));
        return classBytes;
    }

    public static String getResourceNameOfClass(final Class<?> aClass) throws IllegalArgumentException {
        final String nameAsResourcePath = aClass.getName().replace('.', '/');
        final String resourceName = nameAsResourcePath + ".class";
        return resourceName;
    }

}
