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

import java.lang.instrument.ClassFileTransformer;
import java.security.PermissionCollection;
import java.util.Map;

/**
 * A {@code Module} specification for a concrete module implementation.
 *
 * @apiviz.exclude
 *
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ConcreteModuleSpec extends ModuleSpec {

    private final String mainClass;
    private final AssertionSetting assertionSetting;
    private final ResourceLoaderSpec[] resourceLoaders;
    private final DependencySpec[] dependencies;
    private final LocalLoader fallbackLoader;
    private final ModuleClassLoaderFactory moduleClassLoaderFactory;
    private final ClassFileTransformer classFileTransformer;
    private final Map<String, String> properties;
    private final PermissionCollection permissionCollection;

    ConcreteModuleSpec(final ModuleIdentifier moduleIdentifier, final String mainClass, final AssertionSetting assertionSetting, final ResourceLoaderSpec[] resourceLoaders, final DependencySpec[] dependencies, final LocalLoader fallbackLoader, final ModuleClassLoaderFactory moduleClassLoaderFactory, final ClassFileTransformer classFileTransformer, final Map<String, String> properties, final PermissionCollection permissionCollection) {
        super(moduleIdentifier);
        this.mainClass = mainClass;
        this.assertionSetting = assertionSetting;
        this.resourceLoaders = resourceLoaders;
        this.dependencies = dependencies;
        this.fallbackLoader = fallbackLoader;
        this.moduleClassLoaderFactory = moduleClassLoaderFactory;
        this.classFileTransformer = classFileTransformer;
        this.properties = properties;
        this.permissionCollection = permissionCollection;
    }

    public String getMainClass() {
        return mainClass;
    }

    AssertionSetting getAssertionSetting() {
        return assertionSetting;
    }

    ResourceLoaderSpec[] getResourceLoaders() {
        return resourceLoaders;
    }

    DependencySpec[] getDependenciesInternal() {
        return dependencies;
    }

    public DependencySpec[] getDependencies() {
        return dependencies.length == 0 ? dependencies : dependencies.clone();
    }

    LocalLoader getFallbackLoader() {
        return fallbackLoader;
    }

    ModuleClassLoaderFactory getModuleClassLoaderFactory() {
        return moduleClassLoaderFactory;
    }

    ClassFileTransformer getClassFileTransformer() {
        return classFileTransformer;
    }

    Map<String, String> getProperties() {
        return properties;
    }

    PermissionCollection getPermissionCollection() {
        return permissionCollection;
    }
}
