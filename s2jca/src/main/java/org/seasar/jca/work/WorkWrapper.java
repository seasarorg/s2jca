/*
 * Copyright 2004-2011 the Seasar Foundation and the Others.
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
 * {@link Work}を{@link java.util.concurrent.ExecutorService}で実行するために{@link Runnable}インタフェースを
 * 実装したラッパークラスです．
 * 
 * @author koichik
 */
public class WorkWrapper implements Runnable {

    // static fields
    /** 現在のスレッドで実行中の{@link Work} */
    protected static ThreadLocal<Work> currentWork = new ThreadLocal<Work>();

    // instance fields
    /** {@link Work}を受け付けたワークマネージャ */
    protected WorkManagerImpl workManager;

    /** 実行する{@link Work} */
    protected Work work;

    /** {@link Work}を受け付けてからのタイムアウト時間 (ミリ秒単位) */
    protected long startTimeout;

    /** {@link Work}の実行コンテキスト */
    protected ExecutionContext execContext;

    /** {@link Work}の実行イベントを受け取るリスナ */
    protected WorkListener workListener;

    /** {@link Work}の実行と同期するためのラッチ */
    protected CountDownLatch latch;

    /** {@link Work}を受け付けた時間 */
    protected long acceptedTime;

    /** {@link Work}の実行が開始していれば<code>true</code> */
    protected boolean started;

    /** {@link Work}の実行中に発生した例外 */
    protected WorkException exception;

    /**
     * 現在のスレッドで実行中の{@link Work}を返します．
     * 
     * @return 現在のスレッドで実行中の{@link Work}
     */
    public static Work getCurrentWork() {
        return currentWork.get();
    }

    /**
     * インスタンスを構築します．
     * 
     * @param workManager
     *            ワークマネージャ
     * @param work
     *            {@link Work}
     * @param latchCount
     *            {@link Work}の実行が完了するまで同期するラッチの初期値
     */
    public WorkWrapper(final WorkManagerImpl workManager, final Work work, final int latchCount) {
        this(workManager, work, WorkManager.INDEFINITE, null, new WorkAdapter(), latchCount);
    }

    /**
     * インスタンスを構築します．
     * 
     * @param workManager
     *            ワークマネージャ
     * @param work
     *            {@link Work}
     * @param startTimeout
     *            {@link Work}を受け付けてからのタイムアウト時間 (ミリ秒単位)
     * @param execContext
     *            {@link Work}の実行コンテキスト
     * @param workListener
     *            {@link Work}の実行イベントを受け取るリスナ
     * @param latchCount
     *            {@link Work}の実行と同期するラッチの初期値
     */
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

    /**
     * {@link Executor}上で{@link Work}を実行します．
     * 
     * @param executor
     *            {@link Executor}
     * @return {@link Work}を受け付けてから実行完了までの経過時間 (ミリ秒単位)
     * @throws InterruptedException
     *             {@link Work}と同期中に割り込みが発生した場合
     */
    public long execute(final Executor executor) throws InterruptedException {
        acceptedTime = System.currentTimeMillis();
        fireWorkAcceptedEvent();
        executor.execute(this);
        latch.await();
        return System.currentTimeMillis() - acceptedTime;
    }

    /**
     * {@link Work}を解放します．
     */
    public void release() {
        work.release();
    }

    public void run() {
        try {
            started = start();
            currentWork.set(work);
            if (started) {
                doWork();
            }
        } finally {
            currentWork.remove();
            complete();
        }
    }

    /**
     * {@link Work}の実行を開始します．
     * <p>
     * 実行開始前にタイムアウト時間が経過していた場合は<code>false</code>を返します．
     * </p>
     * 
     * @return {@link Work}の実行を開始した場合は<code>true</code>
     */
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

    /**
     * {@link Work}が受け付けられてからの経過時間がタイムアウト時間を超えている場合は<code>true</code>を返します．
     * 
     * @return {@link Work}が受け付けられてからの経過時間がタイムアウト時間を超えている場合は<code>true</code>
     */
    protected boolean isTimedout() {
        if (startTimeout == WorkManager.INDEFINITE) {
            return false;
        }
        if (System.currentTimeMillis() - acceptedTime < startTimeout) {
            return false;
        }
        return true;
    }

    /**
     * {@link Work}を実行します．
     */
    protected void doWork() {
        try {
            work.run();
        } catch (final Throwable e) {
            exception = new WorkCompletedException(e);
            exception.setErrorCode(WorkException.UNDEFINED);
        }
    }

    /**
     * {@link Work}の実行を完了します．
     */
    protected void complete() {
        try {
            if (started) {
                fireWorkCompletedEvent();
            }
        } finally {
            latch.countDown();
        }
    }

    /**
     * {@link Work}が受け付けられたことを通知します．
     */
    protected void fireWorkAcceptedEvent() {
        if (workListener != null) {
            workListener.workAccepted(new WorkEvent(workManager, WorkEvent.WORK_ACCEPTED, work,
                    exception));
        }
    }

    /**
     * {@link Work}の実行が破棄されたことを通知します．
     */
    protected void fireWorkRejectedEvent() {
        if (workListener != null) {
            workListener.workRejected(new WorkEvent(workManager, WorkEvent.WORK_REJECTED, work,
                    exception, System.currentTimeMillis() - acceptedTime));
        }
    }

    /**
     * {@link Work}の実行が開始されることを通知します．
     */
    protected void fireWorkStartedEvent() {
        if (workListener != null) {
            workListener.workStarted(new WorkEvent(workManager, WorkEvent.WORK_STARTED, work,
                    exception, System.currentTimeMillis() - acceptedTime));
        }
    }

    /**
     * {@link Work}の実行が完了したことを通知します．
     */
    protected void fireWorkCompletedEvent() {
        if (workListener != null) {
            workListener.workCompleted(new WorkEvent(workManager, WorkEvent.WORK_COMPLETED, work,
                    exception));
        }
    }

}
