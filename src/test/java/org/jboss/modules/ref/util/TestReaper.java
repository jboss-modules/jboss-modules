/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.modules.ref.util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.modules.ref.Reaper;
import org.jboss.modules.ref.Reference;

/**
 * Reaper used by tests.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 *
 */
public class TestReaper<T, A> implements Reaper<T, A>, Future<Reference<T, A>> {

    private Reference<T, A> reaped;
    private final CountDownLatch countDownLatch = new CountDownLatch(1);

    @Override
    public void reap(Reference<T, A> reference) {
        reaped = reference;
        countDownLatch.countDown();
    }

    public Reference<T, A> getReapedReference() throws InterruptedException, ExecutionException {
        return get();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return reaped != null;
    }

    @Override
    public Reference<T, A> get() throws InterruptedException, ExecutionException {
        try {
            return get(30l, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new RuntimeException("Could not get reaped in 30 second timeout.");
        }
    }

    @Override
    public Reference<T, A> get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
            TimeoutException {
        countDownLatch.await(timeout, unit);
        return reaped;
    }
}