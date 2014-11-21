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

package org.jboss.modules._private;

import java.io.FileDescriptor;
import java.net.InetAddress;
import java.security.Permission;

/**
 * The startup security manager.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class StartupSecurityManager extends SecurityManager {

    public StartupSecurityManager() {
    }

    public void checkPermission(final Permission perm) {
    }

    public void checkPermission(final Permission perm, final Object context) {
    }

    public void checkCreateClassLoader() {
    }

    public void checkAccess(final Thread t) {
    }

    public void checkAccess(final ThreadGroup g) {
    }

    public void checkExit(final int status) {
    }

    public void checkExec(final String cmd) {
    }

    public void checkLink(final String lib) {
    }

    public void checkRead(final FileDescriptor fd) {
    }

    public void checkRead(final String file) {
    }

    public void checkRead(final String file, final Object context) {
    }

    public void checkWrite(final FileDescriptor fd) {
    }

    public void checkWrite(final String file) {
    }

    public void checkDelete(final String file) {
    }

    public void checkConnect(final String host, final int port) {
    }

    public void checkConnect(final String host, final int port, final Object context) {
    }

    public void checkListen(final int port) {
    }

    public void checkAccept(final String host, final int port) {
    }

    public void checkMulticast(final InetAddress maddr) {
    }

    public void checkMulticast(final InetAddress maddr, final byte ttl) {
    }

    public void checkPropertiesAccess() {
    }

    public void checkPropertyAccess(final String key) {
    }

    public boolean checkTopLevelWindow(final Object window) {
        return true;
    }

    public void checkPrintJobAccess() {
    }

    public void checkSystemClipboardAccess() {
    }

    public void checkAwtEventQueueAccess() {
    }

    public void checkPackageAccess(final String pkg) {
    }

    public void checkPackageDefinition(final String pkg) {
    }

    public void checkSetFactory() {
    }

    public void checkMemberAccess(final Class<?> clazz, final int which) {
    }

    public void checkSecurityAccess(final String target) {
    }
}
