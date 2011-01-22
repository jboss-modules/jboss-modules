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


/**
 * A {@code Module} specification which is used by a {@code ModuleLoader} to define new modules.
 *
 * @apiviz.exclude
 *
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ModuleSpec {

    private final ModuleIdentifier moduleIdentifier;
    private final String mainClass;
    private final AssertionSetting assertionSetting;
    private final ResourceLoaderSpec[] resourceLoaders;
    private final DependencySpec[] dependencies;
    private final LocalLoader fallbackLoader;
    private final ModuleClassLoaderFactory moduleClassLoaderFactory;

    private ModuleSpec(final ModuleIdentifier moduleIdentifier, final String mainClass, final AssertionSetting assertionSetting, final ResourceLoaderSpec[] resourceLoaders, final DependencySpec[] dependencies, final LocalLoader fallbackLoader, final ModuleClassLoaderFactory moduleClassLoaderFactory) {
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

    ResourceLoaderSpec[] getResourceLoaders() {
        return resourceLoaders;
    }

    DependencySpec[] getDependencies() {
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
        Builder setMainClass(String mainClass);

        /**
         * Set the default assertion setting for this module.
         *
         * @param assertionSetting the assertion setting
         * @return this builder
         */
        Builder setAssertionSetting(AssertionSetting assertionSetting);

        /**
         * Add a dependency specification.
         *
         * @param dependencySpec the dependency specification
         * @return this builder
         */
        Builder addDependency(DependencySpec dependencySpec);

        /**
         * Add a local resource root, from which this module will load class definitions and resources.
         *
         * @param resourceLoader the resource loader for the root
         * @return this builder
         */
        Builder addResourceRoot(ResourceLoaderSpec resourceLoader);

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
            private final List<ResourceLoaderSpec> resourceLoaders = new ArrayList<ResourceLoaderSpec>(0);
            private final List<DependencySpec> dependencies = new ArrayList<DependencySpec>();
            private LocalLoader fallbackLoader;
            private ModuleClassLoaderFactory moduleClassLoaderFactory;

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
                this.assertionSetting = assertionSetting;
                return this;
            }

            @Override
            public Builder addDependency(final DependencySpec dependencySpec) {
                dependencies.add(dependencySpec);
                return null;
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
            public ModuleSpec create() {
                return new ModuleSpec(moduleIdentifier, mainClass, assertionSetting, resourceLoaders.toArray(new ResourceLoaderSpec[resourceLoaders.size()]), dependencies.toArray(new DependencySpec[dependencies.size()]), fallbackLoader, moduleClassLoaderFactory);
            }

            @Override
            public ModuleIdentifier getIdentifier() {
                return moduleIdentifier;
            }
        };
    }


}
