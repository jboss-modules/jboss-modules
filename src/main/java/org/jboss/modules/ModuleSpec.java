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

import java.util.ArrayList;
import java.util.List;

import org.jboss.modules.DependencySpec.DependencyBuilder;
import org.jboss.modules.DependencySpec.DependencyBuilderBase;


/**
 * A {@code Module} specification which is used by a {@code ModuleLoader} to define new modules.
 *
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ModuleSpec {

    private final ModuleIdentifier moduleIdentifier;
    private final String mainClass;
    private final AssertionSetting assertionSetting;
    private final ResourceLoader[] resourceLoaders;
    private final DependencySpec.SpecifiedDependency[] dependencies;
    private final LocalLoader fallbackLoader;
    private final ModuleClassLoaderFactory moduleClassLoaderFactory;

    private ModuleSpec(final ModuleIdentifier moduleIdentifier, final String mainClass, final AssertionSetting assertionSetting, final ResourceLoader[] resourceLoaders, final DependencySpec.SpecifiedDependency[] dependencies, final LocalLoader fallbackLoader, final ModuleClassLoaderFactory moduleClassLoaderFactory) {
        this.moduleIdentifier = moduleIdentifier;
        this.mainClass = mainClass;
        this.assertionSetting = assertionSetting;
        this.resourceLoaders = resourceLoaders;
        this.dependencies = dependencies;
        this.fallbackLoader = fallbackLoader;
        this.moduleClassLoaderFactory = moduleClassLoaderFactory;
    }

    /**
     * Get the module identifier for the module which is specified by this object.
     *
     * @return the module identifier
     */
    public ModuleIdentifier getModuleIdentifier() {
        return moduleIdentifier;
    }

    String getMainClass() {
        return mainClass;
    }

    AssertionSetting getAssertionSetting() {
        return assertionSetting;
    }

    ResourceLoader[] getResourceLoaders() {
        return resourceLoaders;
    }

    DependencySpec.SpecifiedDependency[] getDependencies() {
        return dependencies;
    }

    LocalLoader getFallbackLoader() {
        return fallbackLoader;
    }

    ModuleClassLoaderFactory getModuleClassLoaderFactory() {
        return moduleClassLoaderFactory;
    }

    /**
     * A builder for new module specifications.
     */
    public interface Builder extends DependencyBuilderBase {

        /**
         * Set the main class for this module, or {@code null} for none.
         *
         * @param mainClass the main class name
         * @return this builder
         */
        Builder setMainClass(String mainClass);

        /**
         * Set the default assertion setting for this module.
         *
         * @param assertionSetting the assertion setting
         * @return this builder
         */
        Builder setAssertionSetting(AssertionSetting assertionSetting);

        /** {@inheritDoc} */
        @Override
        Builder addLocalDependency(final LocalDependencySpec spec);

        /** {@inheritDoc} */
        @Override
        Builder addLocalDependency();

        /** {@inheritDoc} */
        @Override
        Builder addModuleDependency(ModuleDependencySpec dependencySpec);

        /**
         * Add a local resource root, from which this module will load class definitions and resources.
         *
         * @param resourceLoader the resource loader for the root
         * @return this builder
         */
        Builder addResourceRoot(ResourceLoader resourceLoader);

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
        Builder setFallbackLoader(final LocalLoader fallbackLoader);

        /**
         * Set the module class loader factory to use to create the module class loader for this module.
         *
         * @param moduleClassLoaderFactory the factory
         * @return this builder
         */
        Builder setModuleClassLoaderFactory(ModuleClassLoaderFactory moduleClassLoaderFactory);
    }


    /**
     * Get a builder for a new module specification.
     *
     * @param moduleIdentifier the module identifier
     * @return the builder
     */
    public static Builder build(final ModuleIdentifier moduleIdentifier) {
        return new Builder() {
            private String mainClass;
            private AssertionSetting assertionSetting = AssertionSetting.INHERIT;
            private final List<ResourceLoader> resourceLoaders = new ArrayList<ResourceLoader>(0);
            private LocalLoader fallbackLoader;
            private DependencyBuilder depBuilder = new DependencySpec.DependencyBuilderImpl();
            private ModuleClassLoaderFactory moduleClassLoaderFactory;

            public Builder setFallbackLoader(final LocalLoader fallbackLoader) {
                this.fallbackLoader = fallbackLoader;
                return this;
            }

            public Builder addLocalDependency(final LocalDependencySpec spec) {
                depBuilder.addLocalDependency(spec);
                return this;
            }

            public Builder addLocalDependency() {
                depBuilder.addLocalDependency();
                return this;
            }

            public Builder addModuleDependency(final ModuleDependencySpec dependencySpec) {
                depBuilder.addModuleDependency(dependencySpec);
                return this;
            }

            @Override
            public Builder setMainClass(final String mainClass) {
                this.mainClass = mainClass;
                return this;
            }

            @Override
            public Builder setAssertionSetting(AssertionSetting assertionSetting) {
                this.assertionSetting = assertionSetting;
                return this;
            }

            @Override
            public Builder addResourceRoot(final ResourceLoader resourceLoader) {
                resourceLoaders.add(resourceLoader);
                return this;
            }

            public Builder setModuleClassLoaderFactory(final ModuleClassLoaderFactory moduleClassLoaderFactory) {
                this.moduleClassLoaderFactory = moduleClassLoaderFactory;
                return this;
            }

            @Override
            public ModuleSpec create() {
                List<DependencySpec.SpecifiedDependency> dependencies = depBuilder.create().dependencies;
                return new ModuleSpec(moduleIdentifier, mainClass, assertionSetting, resourceLoaders.toArray(new ResourceLoader[resourceLoaders.size()]), dependencies.toArray(new DependencySpec.SpecifiedDependency[dependencies.size()]), fallbackLoader, moduleClassLoaderFactory);
            }

            @Override
            public ModuleIdentifier getIdentifier() {
                return moduleIdentifier;
            }
        };
    }


}
