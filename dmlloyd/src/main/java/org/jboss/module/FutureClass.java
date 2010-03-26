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

package org.jboss.module;

final class FutureClass {

    private Object result;

    private static final Object NOT_FOUND = new Object();
    private static final Object INTERRUPTED = new Object();

    void setResult(Class<?> result) {
        synchronized (this) {
            if (this.result != null) {
                throw new IllegalStateException();
            }
            this.result = result;
            notifyAll();
        }
    }

    void setNotFound() {
        synchronized (this) {
            if (result != null) {
                throw new IllegalStateException();
            }
            result = NOT_FOUND;
            notifyAll();
        }
    }

    public Class<?> get() throws InterruptedException, LoadingThreadInterruptedException {
        synchronized (this) {
            while (result == null) {
                wait();
            }
            if (result == NOT_FOUND) {
                return null;
            } else if (result == INTERRUPTED) {
                result = null;
                throw new LoadingThreadInterruptedException();
            } else {
                return (Class<?>) result;
            }
        }
    }

    public Class<?> getUninterruptibly() throws LoadingThreadInterruptedException {
        boolean intr = false;
        try {
            synchronized (this) {
                while (result == null) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        intr = true;
                    }
                }
                if (result == NOT_FOUND) {
                    return null;
                } else if (result == INTERRUPTED) {
                    result = null;
                    throw new LoadingThreadInterruptedException();
                } else {
                    return (Class<?>) result;
                }
            }
        } finally {
            if (intr) Thread.currentThread().interrupt();
        }
    }

    public void setInterrupted() {
        synchronized (this) {
            result = INTERRUPTED;
            // notify just one to carry on the good work
            notify();
        }
    }
}
