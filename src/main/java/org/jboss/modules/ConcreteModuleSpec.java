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

   
    private final AssertionSetting assertionSetting;
    private final ResourceLoaderSpec[] resourceLoaders;
 
    private final LocalLoader fallbackLoader;
    private final ModuleClassLoaderFactory moduleClassLoaderFactory;
    private final ClassTransformer classFileTransformer;
    private final Map<String, String> properties;
    private final PermissionCollection permissionCollection;
  
    private final ConcreteModuleVersionAndDependency concreteModuleVersionAndDependency;

    ConcreteModuleSpec(final String name, final AssertionSetting assertionSetting, final ResourceLoaderSpec[] resourceLoaders, final LocalLoader fallbackLoader, final ModuleClassLoaderFactory moduleClassLoaderFactory, final ClassTransformer classFileTransformer, final Map<String, String> properties, final PermissionCollection permissionCollection,final ConcreteModuleVersionAndDependency concreteModuleVersionAndDependency) {
        super(name);
      
        this.assertionSetting = assertionSetting;
        this.resourceLoaders = resourceLoaders;
   
        this.fallbackLoader = fallbackLoader;
        this.moduleClassLoaderFactory = moduleClassLoaderFactory;
        this.classFileTransformer = classFileTransformer;
        this.properties = properties;
        this.permissionCollection = permissionCollection;
        this.concreteModuleVersionAndDependency=concreteModuleVersionAndDependency;
    }

    ConcreteModuleVersionAndDependency getConcreteModuleVersionAndDependency() {
        return concreteModuleVersionAndDependency;
    }


    AssertionSetting getAssertionSetting() {
        return assertionSetting;
    }

    ResourceLoaderSpec[] getResourceLoaders() {
        return resourceLoaders;
    }


    LocalLoader getFallbackLoader() {
        return fallbackLoader;
    }

    ModuleClassLoaderFactory getModuleClassLoaderFactory() {
        return moduleClassLoaderFactory;
    }

    ClassTransformer getClassFileTransformer() {
        return classFileTransformer;
    }

    Map<String, String> getProperties() {
        return properties;
    }

    PermissionCollection getPermissionCollection() {
        return permissionCollection;
    }

 
}
