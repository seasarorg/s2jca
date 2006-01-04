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
package org.seasar.jca.wm;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

import javax.resource.spi.work.ExecutionContext;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkAdapter;
import javax.resource.spi.work.WorkCompletedException;
import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkListener;
import javax.resource.spi.work.WorkManager;
import javax.resource.spi.work.WorkRejectedException;

/**
 * @author koichik
 */
public class WorkWrapper implements Runnable {
    protected WorkManagerImpl workManager;
    protected Work work;
    protected long startTimeout;
    protected ExecutionContext execContext;
    protected WorkListener workListener;
    protected CountDownLatch latch;
    protected long acceptedTime;
    protected boolean started;
    protected WorkException exception;

    public WorkWrapper(final WorkManagerImpl workManager, final Work work, final int latchCount) {
        this(workManager, work, WorkManager.INDEFINITE, null, new WorkAdapter(), latchCount);
    }

    public WorkWrapper(final WorkManagerImpl workManager, final Work work, final long startTimeout,
            final ExecutionContext execContext, final WorkListener workListener,
            final int latchCount) {
        this.workManager = workManager;
        this.work = work;
        this.startTimeout = startTimeout;
        this.execContext = execContext;
        this.workListener = workListener;
        this.latch = new CountDownLatch(latchCount);
    }

    public long execute(final Executor executor) throws InterruptedException {
        acceptedTime = System.currentTimeMillis();
        fireWorkAcceptedEvent();
        executor.execute(this);
        latch.await();
        return System.currentTimeMillis() - acceptedTime;
    }

    public void release() {
        work.release();
    }

    public void run() {
        try {
            started = start();
            if (started) {
                doWork();
            }
        } finally {
            complete();
        }
    }

    protected boolean start() {
        try {
            if (isTimedout()) {
                exception = new WorkRejectedException();
                exception.setErrorCode(WorkException.START_TIMED_OUT);
                fireWorkRejectedEvent();
                return false;
            }
            fireWorkStartedEvent();
            return true;
        } finally {
            latch.countDown();
        }
    }

    protected void doWork() {
        try {
            work.run();
        } catch (final Throwable e) {
            exception = new WorkCompletedException(e);
            exception.setErrorCode(WorkException.UNDEFINED);
        }
    }

    protected void complete() {
        try {
            if (started) {
                fireWorkCompletedEvent();
            }
        } finally {
            latch.countDown();
        }
    }

    private boolean isTimedout() {
        if (startTimeout == WorkManager.INDEFINITE) {
            return false;
        }
        if (System.currentTimeMillis() - acceptedTime < startTimeout) {
            return false;
        }
        return true;
    }

    protected void fireWorkAcceptedEvent() {
        workListener.workAccepted(new WorkEvent(workManager, WorkEvent.WORK_ACCEPTED, work,
                exception));
    }

    protected void fireWorkRejectedEvent() {
        workListener.workRejected(new WorkEvent(workManager, WorkEvent.WORK_REJECTED, work,
                exception, System.currentTimeMillis() - acceptedTime));
    }

    protected void fireWorkStartedEvent() {
        workListener.workStarted(new WorkEvent(workManager, WorkEvent.WORK_STARTED, work,
                exception, System.currentTimeMillis() - acceptedTime));
    }

    protected void fireWorkCompletedEvent() {
        workListener.workCompleted(new WorkEvent(workManager, WorkEvent.WORK_COMPLETED, work,
                exception));
    }
}
