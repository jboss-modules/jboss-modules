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

import org.junit.BeforeClass;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Abstract Test Case used as a base for all module tests.
 *
 * @author John Bailey
 */
public class AbstractModuleTestCase {
    protected static final ModuleIdentifier MODULE_ID = new ModuleIdentifier("test", "test", "1.0");

    @BeforeClass
    public static void initUrlHandler() {
        try {
            URL.setURLStreamHandlerFactory(new ModularURLStreamHandlerFactory());
        } catch (Throwable t) {
        }
    }

    protected byte[] readBytes(final InputStream is) throws IOException {

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            byte[] buff = new byte[1024];
            int read;
            while ((read = is.read(buff)) > -1) {
                os.write(buff, 0, read);
            }
        } finally {
            is.close();
        }
        return os.toByteArray();
    }
}
