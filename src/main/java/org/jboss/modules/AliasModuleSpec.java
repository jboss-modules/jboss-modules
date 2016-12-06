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
public final class AliasModuleSpec extends ModuleSpec {

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
        final PermissionCollection permissionCollection = ModulesPolicy.DEFAULT_PERMISSION_COLLECTION;
        this.concreteModuleSpec = new ConcreteModuleSpec(moduleIdentifier, mainClass, assertionSetting, resourceLoaders,
                dependencies, fallbackLoader, moduleClassLoaderFactory, classFileTransformer, properties, permissionCollection);
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
