/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
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

import java.util.Set;

import org.jboss.modules.filter.ClassFilter;
import org.jboss.modules.filter.PathFilter;

final class LocalDependencySpec extends DependencySpec {
    private final LocalLoader localLoader;
    private final Set<String> loaderPaths;

    LocalDependencySpec(final PathFilter importFilter, final PathFilter exportFilter, final PathFilter resourceImportFilter, final PathFilter resourceExportFilter, final ClassFilter classImportFilter, final ClassFilter classExportFilter, final LocalLoader localLoader, final Set<String> loaderPaths) {
        super(importFilter, exportFilter, resourceImportFilter, resourceExportFilter, classImportFilter, classExportFilter);
        this.localLoader = localLoader;
        this.loaderPaths = loaderPaths;
    }

    Dependency getDependency(final Module module) {
        return new LocalDependency(getExportFilter(), getImportFilter(), getResourceExportFilter(), getResourceImportFilter(), getClassExportFilter(), getClassImportFilter(), localLoader, loaderPaths);
    }

    public String toString() {
        return "dependency on local loader " + localLoader;
    }
}
