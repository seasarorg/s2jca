/*
 * Copyright 2004-2006 the Seasar Foundation and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.seasar.jca.work;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.resource.spi.work.ExecutionContext;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkListener;
import javax.resource.spi.work.WorkManager;

import org.seasar.framework.container.annotation.tiger.Component;
import org.seasar.framework.container.annotation.tiger.InstanceType;

/**
 * @author koichik
 */
@Component(instance = InstanceType.SINGLETON)
public class WorkManagerImpl implements WorkManager {
    protected static final int SCHEDULE_WORK = 0;
    protected static final int START_WORK = 1;
    protected static final int DO_WORK = 2;

    protected final ExecutorService pool;

    public WorkManagerImpl() {
        this(1);
    }

    public WorkManagerImpl(final int maxThreads) {
        this(Executors.newFixedThreadPool(maxThreads));
    }

    public WorkManagerImpl(final ExecutorService pool) {
        this.pool = pool;
    }

    public void stop() {
        pool.shutdown();
    }

    public void doWork(final Work work) throws WorkException {
        executeWork(new WorkWrapper(this, work, DO_WORK));
    }

    public void doWork(final Work work, final long startTimeout,
            final ExecutionContext execContext, final WorkListener workListener)
            throws WorkException {
        executeWork(new WorkWrapper(this, work, startTimeout, execContext, workListener, DO_WORK));
    }

    public long startWork(final Work work) throws WorkException {
        return executeWork(new WorkWrapper(this, work, START_WORK));
    }

    public long startWork(final Work work, final long startTimeout,
            final ExecutionContext execContext, final WorkListener workListener)
            throws WorkException {
        return executeWork(new WorkWrapper(this, work, startTimeout, execContext, workListener,
                START_WORK));
    }

    public void scheduleWork(final Work work) throws WorkException {
        executeWork(new WorkWrapper(this, work, SCHEDULE_WORK));
    }

    public void scheduleWork(final Work work, final long startTimeout,
            final ExecutionContext execContext, final WorkListener workListener)
            throws WorkException {
        executeWork(new WorkWrapper(this, work, startTimeout, execContext, workListener,
                SCHEDULE_WORK));
    }

    protected long executeWork(final WorkWrapper work) throws WorkException {
        try {
            return work.execute(pool);
        } catch (final InterruptedException e) {
            final WorkException we = new WorkException(e);
            we.setErrorCode(WorkException.INTERNAL);
            throw we;
        }
    }
}
