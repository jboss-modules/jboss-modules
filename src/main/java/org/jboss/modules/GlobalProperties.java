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

import static java.security.AccessController.doPrivileged;

import java.security.PrivilegedAction;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class GlobalProperties {

    private static final SystemProperties rootProperties = doPrivileged(new PrivilegedAction<SystemProperties>() {
        public SystemProperties run() {
            final SystemProperties root = new SystemProperties(System.getProperties());
            System.setProperties(root);
            return root;
        }
    });

    private GlobalProperties() {}

    static SystemProperties getRootProperties() {
        return rootProperties;
    }
}
