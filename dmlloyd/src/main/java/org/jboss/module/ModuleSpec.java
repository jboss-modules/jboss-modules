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

import java.net.URI;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class ModuleSpec {
    private final List<URI> imports = new ArrayList<URI>();
    private final List<URI> exports = new ArrayList<URI>();
    private final Set<Module.Flag> moduleFlags = EnumSet.noneOf(Module.Flag.class);

    private URI moduleUri;
    private String mainClass;
    private ClassFilter importClassFilter = ClassFilter.ALL;
    private ClassFilter exportClassFilter = ClassFilter.ALL;
    private ModuleContentLoader loader;
    private String packageSpecTitle;
    private String packageSpecVersion;
    private String packageSpecVendor;
    private String packageImplTitle;
    private String packageImplVersion;
    private String packageImplVendor;

    public URI getModuleUri() {
        return moduleUri;
    }

    public void setModuleUri(final URI moduleUri) {
        if (moduleUri == null) {
            throw new IllegalArgumentException("moduleUri is null");
        }
        this.moduleUri = moduleUri;
    }

    public List<URI> getImports() {
        return imports;
    }

    public List<URI> getExports() {
        return exports;
    }

    public String getMainClass() {
        return mainClass;
    }

    public void setMainClass(final String mainClass) {
        this.mainClass = mainClass;
    }

    public ClassFilter getImportClassFilter() {
        return importClassFilter;
    }

    public void setImportClassFilter(final ClassFilter importClassFilter) {
        if (importClassFilter == null) {
            throw new IllegalArgumentException("importClassFilter is null");
        }
        this.importClassFilter = importClassFilter;
    }

    public ClassFilter getExportClassFilter() {
        return exportClassFilter;
    }

    public void setExportClassFilter(final ClassFilter exportClassFilter) {
        if (exportClassFilter == null) {
            throw new IllegalArgumentException("exportClassFilter is null");
        }
        this.exportClassFilter = exportClassFilter;
    }

    public Set<Module.Flag> getModuleFlags() {
        return moduleFlags;
    }

    public ModuleContentLoader getLoader() {
        return loader;
    }

    public void setLoader(final ModuleContentLoader loader) {
        this.loader = loader;
    }

    public String getPackageSpecTitle() {
        return packageSpecTitle;
    }

    public void setPackageSpecTitle(final String packageSpecTitle) {
        this.packageSpecTitle = packageSpecTitle;
    }

    public String getPackageSpecVersion() {
        return packageSpecVersion;
    }

    public void setPackageSpecVersion(final String packageSpecVersion) {
        this.packageSpecVersion = packageSpecVersion;
    }

    public String getPackageSpecVendor() {
        return packageSpecVendor;
    }

    public void setPackageSpecVendor(final String packageSpecVendor) {
        this.packageSpecVendor = packageSpecVendor;
    }

    public String getPackageImplTitle() {
        return packageImplTitle;
    }

    public void setPackageImplTitle(final String packageImplTitle) {
        this.packageImplTitle = packageImplTitle;
    }

    public String getPackageImplVersion() {
        return packageImplVersion;
    }

    public void setPackageImplVersion(final String packageImplVersion) {
        this.packageImplVersion = packageImplVersion;
    }

    public String getPackageImplVendor() {
        return packageImplVendor;
    }

    public void setPackageImplVendor(final String packageImplVendor) {
        this.packageImplVendor = packageImplVendor;
    }
}
