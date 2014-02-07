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
import java.security.AllPermission;
import java.security.PermissionCollection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@code Module} specification which is used by a {@code ModuleLoader} to define new modules.
 *
 * @apiviz.exclude
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class ModuleSpec {

    private final ModuleIdentifier moduleIdentifier;

    ModuleSpec(final ModuleIdentifier moduleIdentifier) {
        this.moduleIdentifier = moduleIdentifier;
    }

    /**
     * Get a builder for a new module specification.
     *
     * @param moduleIdentifier the module identifier
     * @return the builder
     */
    public static Builder build(final ModuleIdentifier moduleIdentifier) {
        if (moduleIdentifier == null) {
            throw new IllegalArgumentException("moduleIdentifier is null");
        }
        return new Builder() {
            private String mainClass;
            private AssertionSetting assertionSetting = AssertionSetting.INHERIT;
            private final List<ResourceLoaderSpec> resourceLoaders = new ArrayList<ResourceLoaderSpec>(0);
            private final List<DependencySpec> dependencies = new ArrayList<DependencySpec>();
            private final Map<String, String> properties = new LinkedHashMap<String, String>(0);
            private LocalLoader fallbackLoader;
            private ModuleClassLoaderFactory moduleClassLoaderFactory;
            private ClassFileTransformer classFileTransformer;
            private PermissionCollection permissionCollection;

            @Override
            public Builder setFallbackLoader(final LocalLoader fallbackLoader) {
                this.fallbackLoader = fallbackLoader;
                return this;
            }

            @Override
            public Builder setMainClass(final String mainClass) {
                this.mainClass = mainClass;
                return this;
            }

            @Override
            public Builder setAssertionSetting(final AssertionSetting assertionSetting) {
                this.assertionSetting = assertionSetting == null ? AssertionSetting.INHERIT : assertionSetting;
                return this;
            }

            @Override
            public Builder addDependency(final DependencySpec dependencySpec) {
                dependencies.add(dependencySpec);
                return this;
            }

            @Override
            public Builder addResourceRoot(final ResourceLoaderSpec resourceLoader) {
                resourceLoaders.add(resourceLoader);
                return this;
            }

            @Override
            public Builder setModuleClassLoaderFactory(final ModuleClassLoaderFactory moduleClassLoaderFactory) {
                this.moduleClassLoaderFactory = moduleClassLoaderFactory;
                return this;
            }

            @Override
            public Builder setClassFileTransformer(final ClassFileTransformer classFileTransformer) {
                this.classFileTransformer = classFileTransformer;
                return this;
            }

            @Override
            public Builder addProperty(final String name, final String value) {
                properties.put(name, value);
                return this;
            }

            @Override
            public Builder setPermissionCollection(PermissionCollection permissionCollection) {
                this.permissionCollection = permissionCollection;
                return this;
            }

            @Override
            public ModuleSpec create() {
                return new ConcreteModuleSpec(moduleIdentifier, mainClass, assertionSetting, resourceLoaders.toArray(new ResourceLoaderSpec[resourceLoaders.size()]), dependencies.toArray(new DependencySpec[dependencies.size()]), fallbackLoader, moduleClassLoaderFactory, classFileTransformer, properties, permissionCollection);
            }

            @Override
            public ModuleIdentifier getIdentifier() {
                return moduleIdentifier;
            }
        };
    }

    /**
     * Get a builder for a new module alias specification.
     *
     * @param moduleIdentifier the module identifier
     * @param aliasTarget the alias target identifier
     * @return the builder
     */
    public static AliasBuilder buildAlias(final ModuleIdentifier moduleIdentifier, final ModuleIdentifier aliasTarget) {
        if (moduleIdentifier == null) {
            throw new IllegalArgumentException("moduleIdentifier is null");
        }
        if (aliasTarget == null) {
            throw new IllegalArgumentException("aliasTarget is null");
        }
        return new AliasBuilder() {
            public ModuleSpec create() {
                return new AliasModuleSpec(moduleIdentifier, aliasTarget);
            }

            public ModuleIdentifier getIdentifier() {
                return moduleIdentifier;
            }

            public ModuleIdentifier getAliasTarget() {
                return aliasTarget;
            }
        };
    }

    /**
     * Get the module identifier for the module which is specified by this object.
     *
     * @return the module identifier
     */
    public ModuleIdentifier getModuleIdentifier() {
        return moduleIdentifier;
    }

    /**
     * A builder for new concrete module specifications.
     *
     * @apiviz.exclude
     */
    public interface Builder {

        /**
         * Set the main class for this module, or {@code null} for none.
         *
         * @param mainClass the main class name
         * @return this builder
         */
        ModuleSpec.Builder setMainClass(String mainClass);

        /**
         * Set the default assertion setting for this module.
         *
         * @param assertionSetting the assertion setting
         * @return this builder
         */
        ModuleSpec.Builder setAssertionSetting(AssertionSetting assertionSetting);

        /**
         * Add a dependency specification.
         *
         * @param dependencySpec the dependency specification
         * @return this builder
         */
        ModuleSpec.Builder addDependency(DependencySpec dependencySpec);

        /**
         * Add a local resource root, from which this module will load class definitions and resources.
         *
         * @param resourceLoader the resource loader for the root
         * @return this builder
         */
        ModuleSpec.Builder addResourceRoot(ResourceLoaderSpec resourceLoader);

        /**
         * Create the module specification from this builder.
         *
         * @return the module specification
         */
        ModuleSpec create();

        /**
         * Get the identifier of the module being defined by this builder.
         *
         * @return the module identifier
         */
        ModuleIdentifier getIdentifier();

        /**
         * Sets a "fall-back" loader that will attempt to load a class if all other mechanisms
         * are unsuccessful.
         *
         * @param fallbackLoader the fall-back loader
         * @return this builder
         */
        ModuleSpec.Builder setFallbackLoader(final LocalLoader fallbackLoader);

        /**
         * Set the module class loader factory to use to create the module class loader for this module.
         *
         * @param moduleClassLoaderFactory the factory
         * @return this builder
         */
        ModuleSpec.Builder setModuleClassLoaderFactory(ModuleClassLoaderFactory moduleClassLoaderFactory);

        /**
         * Set the class file transformer to use for this module.
         *
         * @param classFileTransformer the class file transformer
         * @return this builder
         */
        ModuleSpec.Builder setClassFileTransformer(ClassFileTransformer classFileTransformer);

        /**
         * Add a property to this module specification.
         *
         * @param name the property name
         * @param value the property value
         * @return this builder
         */
        ModuleSpec.Builder addProperty(String name, String value);

        /**
         * Set the permission collection for this module specification.  If none is given, a collection implying
         * {@link AllPermission} is assumed.
         *
         * @param permissionCollection the permission collection
         * @return this builder
         */
        ModuleSpec.Builder setPermissionCollection(PermissionCollection permissionCollection);
    }

    /**
     * A builder for new alias module specifications.
     */
    public interface AliasBuilder {

        /**
         * Create the module specification from this builder.
         *
         * @return the module specification
         */
        ModuleSpec create();

        /**
         * Get the identifier of the module being defined by this builder.
         *
         * @return the module identifier
         */
        ModuleIdentifier getIdentifier();

        /**
         * Get the identifier of the module being referenced by this builder.
         *
         * @return the module identifier
         */
        ModuleIdentifier getAliasTarget();
    }
}
