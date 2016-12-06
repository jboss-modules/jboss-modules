/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
import java.util.Collections;
import java.util.Map;

import org.jboss.modules.filter.PathFilters;

/**
 * A module specification for a module alias.
 * <p>
 * Note that because of MODULES-241, alias modules are handled as a regular modules with a single dependency on the alias
 * target. Use {@link #asConcreteModuleSpec()} to get the regular {@link ConcreteModuleSpec} representing this
 * {@link AliasModuleSpec}.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
final class AliasModuleSpec extends ModuleSpec {

    private final ModuleIdentifier aliasTarget;
    private final ConcreteModuleSpec concreteModuleSpec;

    AliasModuleSpec(final ModuleIdentifier moduleIdentifier, final ModuleIdentifier aliasTarget) {
        super(moduleIdentifier);
        this.aliasTarget = aliasTarget;

        DependencySpec aliasTargetDependency = DependencySpec.createModuleDependencySpec(PathFilters.acceptAll(),
                PathFilters.acceptAll(), null, aliasTarget, false);

        final String mainClass = null;
        final AssertionSetting assertionSetting = AssertionSetting.INHERIT;
        final ResourceLoaderSpec[] resourceLoaders = ResourceLoaderSpec.NO_RESOURCE_LOADERS;
        final DependencySpec[] dependencies = new DependencySpec[] { aliasTargetDependency };
        final LocalLoader fallbackLoader = null;
        final ModuleClassLoaderFactory moduleClassLoaderFactory = null;
        final ClassFileTransformer classFileTransformer = null;
        final Map<String, String> properties = Collections.emptyMap();
        this.concreteModuleSpec = new ConcreteModuleSpec(moduleIdentifier, mainClass, assertionSetting, resourceLoaders,
                dependencies, fallbackLoader, moduleClassLoaderFactory, classFileTransformer, properties);
    }

    public ModuleIdentifier getAliasTarget() {
        return aliasTarget;
    }

    /**
     * @return a {@link ConcreteModuleSpec} instance representing this {@link AliasModuleSpec} - i.e. a
     *         {@link ConcreteModuleSpec} with single dependency on the alias target, with the import and export filters set to
     *         {@link PathFilters#acceptAll()}.
     */
    ConcreteModuleSpec asConcreteModuleSpec() {
        return concreteModuleSpec;
    }
}
