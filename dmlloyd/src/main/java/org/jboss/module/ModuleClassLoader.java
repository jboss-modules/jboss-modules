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

package org.jboss.module;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.SecureClassLoader;
import java.util.Enumeration;

/**
 * A classloader which loads exported classes from a module.
 */
public final class ModuleClassLoader extends SecureClassLoader {

    private final Module module;

    public ModuleClassLoader(final Module module) {
        this.module = module;
    }

    public Module getModule() {
        return module;
    }

    public Class<?> loadClass(final String name) throws ClassNotFoundException {
        return module.getExportedClass(name);
    }

    protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
        return super.loadClass(name, resolve);
    }

    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        return super.findClass(name);
    }

    public URL getResource(final String name) {
        return super.getResource(name);
    }

    public Enumeration<URL> getResources(final String name) throws IOException {
        return super.getResources(name);
    }

    protected URL findResource(final String name) {
        return super.findResource(name);
    }

    protected Enumeration<URL> findResources(final String name) throws IOException {
        return super.findResources(name);
    }

    public InputStream getResourceAsStream(final String name) {
        return super.getResourceAsStream(name);
    }

    protected Package getPackage(final String name) {
        return super.getPackage(name);
    }

    protected Package[] getPackages() {
        return super.getPackages();
    }

    protected String findLibrary(final String libname) {
        return super.findLibrary(libname);
    }
}
