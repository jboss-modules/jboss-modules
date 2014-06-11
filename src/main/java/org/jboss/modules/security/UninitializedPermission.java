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

package org.jboss.modules.security;

import java.security.Permission;

final class UninitializedPermission extends Permission {

    private static final long serialVersionUID = 468335190774708422L;

    static final UninitializedPermission INSTANCE = new UninitializedPermission();

    private UninitializedPermission() {
        super(null);
    }

    public boolean implies(final Permission permission) {
        return false;
    }

    public boolean equals(final Object obj) {
        return false;
    }

    public int hashCode() {
        return 0;
    }

    public String getActions() {
        return null;
    }
}
