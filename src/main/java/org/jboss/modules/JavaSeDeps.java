/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019 Red Hat, Inc., and individual contributors
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

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Forms a list of {@link DependencySpec} which represent the dependencies of the "java.se"
 * module
 */
final class JavaSeDeps {
    static final List<DependencySpec> list;

    static {
        final Optional<ModuleReference> javaSe = ModuleFinder.ofSystem().find("java.se");
        final ModuleDescriptor javaSeDescriptor = javaSe.isPresent() ? javaSe.get().descriptor() : null;
        if (javaSeDescriptor == null) {
            // this shouldn't happen ever, since Java 9+ always has the java.se module.
            // But, if it does happen for some reason, we tolerate it and use an empty
            // DependencySpec
            list = Collections.emptyList();
        } else {
            final List<DependencySpec> deps = new ArrayList<>();
            for (final ModuleDescriptor.Requires dep : javaSeDescriptor.requires()) {
                deps.add(new ModuleDependencySpecBuilder().setName(dep.name()).setExport(true).build());
            }
            list = Collections.unmodifiableList(deps);
        }
    }
}
