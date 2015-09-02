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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.Permission;

/**
 * A factory for {@link Permission} objects.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface PermissionFactory {

    /**
     * Construct a new instance of the permission.  The instance may be cached.
     *
     * @return the permission
     */
    Permission construct();

    /**
     * Utility method to reflectively construct a permission from a given class.
     *
     * @param permissionClass the permission class
     * @param targetName the optional target name
     * @param permissionActions the optional actions name
     * @return the permission
     * @throws IllegalAccessException if the necessary constructor is not accessible
     * @throws InvocationTargetException if the constructor failed
     * @throws InstantiationException if the object could not be instantiated for some reason
     * @throws NoSuchMethodException if none of the candidate constructors exist on the class
     */
    static Permission constructFromClass(Class<? extends Permission> permissionClass, String targetName, String permissionActions) throws IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchMethodException {
        final Constructor<? extends Permission> constructor;
        boolean hasTarget = targetName != null && ! targetName.isEmpty();
        boolean hasAction = permissionActions != null && ! permissionActions.isEmpty();
        if (hasTarget && hasAction) {
            Constructor<? extends Permission> test;
            try {
                test = permissionClass.getConstructor(String.class, String.class);
            } catch (NoSuchMethodException ignored) {
                try {
                    test = permissionClass.getConstructor(String.class);
                    hasAction = false;
                } catch (NoSuchMethodException ignored2) {
                    test = permissionClass.getConstructor();
                    hasTarget = false;
                    hasAction = false;
                }
            }
            constructor = test;
        } else if (hasTarget) {
            assert ! hasAction;
            Constructor<? extends Permission> test;
            try {
                test = permissionClass.getConstructor(String.class);
            } catch (NoSuchMethodException ignored) {
                try {
                    test = permissionClass.getConstructor(String.class, String.class);
                    hasAction = true;
                } catch (NoSuchMethodException ignored2) {
                    test = permissionClass.getConstructor();
                    hasTarget = false;
                    hasAction = false;
                }
            }
            constructor = test;
        } else {
            constructor = permissionClass.getConstructor();
        }
        if (hasTarget && hasAction) {
            return constructor.newInstance(targetName, permissionActions);
        } else if (hasTarget) {
            return constructor.newInstance(targetName);
        } else {
            return constructor.newInstance();
        }
    }
}
