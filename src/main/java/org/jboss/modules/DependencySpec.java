/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

/**
 * @author <a href="mailto:jbailey@redhat.com">John Bailey</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class DependencySpec {
    private ModuleIdentifier moduleIdentifier;
    private boolean export;
    private boolean optional;

    public DependencySpec() {
    }

    public boolean isExport() {
        return export;
    }

    public void setExport(final boolean export) {
        this.export = export;
    }

    public ModuleIdentifier getModuleIdentifier() {
        return moduleIdentifier;
    }

    public void setModuleIdentifier(final ModuleIdentifier moduleIdentifier) {
        this.moduleIdentifier = moduleIdentifier;
    }

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(final boolean optional) {
        this.optional = optional;
    }
}
