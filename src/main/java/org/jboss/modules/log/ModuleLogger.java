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

package org.jboss.modules.log;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;

/**
 * A simple Logger abstraction.
 *
 * @author thomas.diesler@jboss.com
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public interface ModuleLogger {

    void trace(String message);

    void trace(String format, Object arg1);

    void trace(String format, Object arg1, Object arg2);

    void trace(String format, Object arg1, Object arg2, Object arg3);

    void trace(String format, Object... args);

    void trace(Throwable t, String message);

    void trace(Throwable t, String format, Object arg1);

    void trace(Throwable t, String format, Object arg1, Object arg2);

    void trace(Throwable t, String format, Object arg1, Object arg2, Object arg3);

    void trace(Throwable t, String format, Object... args);

    void greeting();

    default void moduleDefined(String name, final ModuleLoader moduleLoader) {
        moduleDefined(ModuleIdentifier.fromString(name), moduleLoader);
    }

    default void moduleDefined(ModuleIdentifier identifier, final ModuleLoader moduleLoader) {
    }

    void classDefineFailed(Throwable throwable, String className, Module module);

    void classDefined(String name, Module module);
}
