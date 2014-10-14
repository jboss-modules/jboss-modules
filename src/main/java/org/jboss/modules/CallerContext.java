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

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Various methods for obtaining caller info.
 *
 * @author Jason T. Greene
 */
class CallerContext {

    private CallerContext() {
    }

    private static final class Hack extends SecurityManager {
        @Override
        protected Class<?>[] getClassContext() {
            return super.getClassContext();
        }
    }

    private static Hack hack = AccessController.doPrivileged(new PrivilegedAction<Hack>() {
        @Override
        public Hack run() {
            return new Hack();
        }
    });

    static Class<?> getCallingClass() {
        Class<?>[] stack = hack.getClassContext();
        int i = 3;
        while (stack[i] == stack[2]) {
            // skip nested calls front the same class
            if (++i >= stack.length)
                return null;
        }

        return stack[i];
    }

    static Class<?> getCallingClass(Class<?>... excludes) {
        //  0: this class
        //  1: JBoss Modules code
        //  2: ???
        Class<?>[] stack = hack.getClassContext();
        for (int i = 2; i < stack.length; i ++) {
            final Class<?> item = stack[i];
            for (Class<?> exclude : excludes) {
                if (item != exclude) return item;
            }
        }
        return null;
    }
}
