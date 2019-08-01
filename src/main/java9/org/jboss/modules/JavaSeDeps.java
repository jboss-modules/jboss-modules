/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
 *
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