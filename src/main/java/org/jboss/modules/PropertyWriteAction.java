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

import java.security.PrivilegedAction;

/**
 * Date: 11.05.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class PropertyWriteAction implements PrivilegedAction<String> {

    private final String key;
    private final String value;

    PropertyWriteAction(final String key, final String value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public String run() {
        return System.setProperty(key, value);
    }
}
