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