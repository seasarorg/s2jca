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
package org.seasar.jca.outbound.policy;

import java.util.Timer;
import java.util.TimerTask;

import javax.resource.ResourceException;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.UnavailableException;

import org.seasar.framework.log.Logger;
import org.seasar.jca.exception.SResourceException;
import org.seasar.jca.outbound.support.ConnectionManagementContext;
import org.seasar.jca.outbound.support.ManagedConnectionPool;

/**
 * 単純にコネクションをプールするポリシーの実装クラスです．
 * 
 * @author koichik
 */
public class BasicPoolingPolicy extends AbstractPolicy {

    // constants
    private static final long serialVersionUID = 1L;

    // static fields
    private static final Logger logger = Logger.getLogger(BasicPoolingPolicy.class);

    /** アイドル状態のコネクションを解放するためのタイマ */
    protected static Timer timer;

    // instance fields
    /** プールするコネクションの最小値 */
    protected int minPoolSize = 5;

    /** プールするコネクションの最大値 */
    protected int maxPoolSize = 10;

    /** アイドル状態のコネクションを開放するまでの時間 (秒単位) */
    protected int timeout = 600;

    /** ブートストラップコンテキスト */
    protected final BootstrapContext bc;

    /** プールの同期オブジェクト */
    protected final Object lock = new Object();

    /** コネクションプール */
    protected ManagedConnectionPool<ExpireTask> pool;

    /**
     * インスタンスを構築します．
     * 
     * @param bc
     *            ブートストラップコンテキスト
     * @throws ResourceException
     *             インスタンスの構築中に例外が発生した場合
     */
    public BasicPoolingPolicy(final BootstrapContext bc) throws ResourceException {
        super(true);
        this.bc = bc;
        createTimer(bc);
    }

    @Override
    public void initialize(final ManagedConnectionFactory mcf,
            final ConnectionManagementPolicy nextPolicy) throws ResourceException {
        super.initialize(mcf, nextPolicy);
        pool = new ManagedConnectionPool<ExpireTask>(nextPolicy, true);
    }

    /**
     * タイマを作成して返します．
     * 
     * @param bc
     *            ブートストラップコンテキスト
     * @throws UnavailableException
     *             タイマの作成中に例外が発生した場合
     */
    protected static synchronized void createTimer(final BootstrapContext bc)
            throws UnavailableException {
        if (timer == null) {
            timer = bc.createTimer();
        }
    }

    @Override
    public void allocate(final ConnectionManagementContext context) throws ResourceException {
        assertValidMCF(context);
        checkOut(context);
    }

    @Override
    public void release(final ManagedConnection mc) throws ResourceException {
        mc.cleanup();
        if (!checkIn(mc)) {
            nextPolicy.release(mc);
        }
    }

    @Override
    public void connectionErrorOccurred(final ManagedConnection mc) throws ResourceException {
        purge(mc);
        nextPolicy.connectionErrorOccurred(mc);
    }

    @Override
    public void dispose() {
        pool.close();
        nextPolicy.dispose();
    }

    /**
     * プールされているコネクションの数が最大値に達している場合は<code>true</code>を返します．
     * 
     * @return プールされているコネクションの数が最大値に達している場合は<code>true</code>
     */
    protected boolean isFull() {
        return (pool.size()) >= maxPoolSize;
    }

    /**
     * プールされている未使用コネクションの数が最大値に達している場合は<code>true</code>を返します．
     * 
     * @return プールされている未使用コネクションの数が最大値に達している場合は<code>true</code>
     */
    protected boolean isFreePoolFull() {
        return (pool.getFreePoolSize()) >= maxPoolSize;
    }

    /**
     * プールからマネージドコネクションをチェックアウトします．
     * 
     * @param context
     *            コネクション管理コンテキスト
     * @throws ResourceException
     *             コネクションのチェックアウト中に例外が発生した場合
     */
    protected void checkOut(final ConnectionManagementContext context) throws ResourceException {
        synchronized (lock) {
            waitForFreePool();
            final ManagedConnection mc = allocateFromFreePool(context);
            if (mc != null) {
                context.setManagedConnection(mc);
                return;
            }

            if (isFreePoolFull()) {
                releaseFirstFromFree();
            }
            nextPolicy.allocate(context);
            pool.addToActivePool(context.getManagedConnection());
        }
    }

    /**
     * プールに空きができるまで待機します．
     * 
     * @throws ResourceException
     *             待機中に例外が発生した場合
     */
    protected void waitForFreePool() throws ResourceException {
        while (pool.getFreePoolSize() == 0 && pool.getActivePoolSize() >= maxPoolSize) {
            try {
                lock.wait();
            } catch (final InterruptedException e) {
                throw new SResourceException("EJCA0000", e);
            }
        }
    }

    /**
     * 未使用のプールからマネージドコネクションを割り当てます．
     * 
     * @param context
     *            コネクション管理コンテキスト
     * @return マネージドコネクション
     * @throws ResourceException
     *             割り当てられたマネージドコネクション
     */
    protected ManagedConnection allocateFromFreePool(final ConnectionManagementContext context)
            throws ResourceException {
        final ManagedConnection mc = pool.getMatched(context.getSubject(),
                context.getRequestInfo(), mcf);
        if (mc != null) {
            final ExpireTask task = pool.moveFreeToActivePool(mc);
            task.cancel();
            if (logger.isDebugEnabled()) {
                logger.log("DJCA1006", new Object[] { mc });
            }
        }
        return mc;
    }

    /**
     * 未使用コネクションのプールから先頭のコネクションを解放します．
     * 
     * @throws ResourceException
     *             コネクションの解放中に例外が発生した場合
     */
    protected void releaseFirstFromFree() throws ResourceException {
        final ManagedConnection mc = pool.getFirstFromFree();
        nextPolicy.release(mc);
        final ExpireTask task = pool.removeFromFreePool(mc);
        if (task != null) {
            task.cancel();
        }
        if (logger.isDebugEnabled()) {
            logger.log("DJCA1008", new Object[] { mc });
        }
    }

    /**
     * マネージドコネクションを未使用コネクションのプールにチェックインします．
     * <p>
     * プールしているコネクションが最大値に達している場合はマネージドコネクションを後続のコネクション管理ポリシーに渡します．
     * </p>
     * 
     * @param mc
     *            マネージドコネクション
     * @return マネージドコネクションをプールした場合は<code>true</code>
     */
    protected boolean checkIn(final ManagedConnection mc) {
        synchronized (lock) {
            if (isFreePoolFull() || !pool.removeFromActivePool(mc)) {
                return false;
            }
            pool.addToFreePool(mc, new ExpireTask(mc));
            lock.notifyAll();
        }

        if (logger.isDebugEnabled()) {
            logger.log("DJCA1007", new Object[] { mc });
        }
        return true;
    }

    /**
     * マネージドコネクションをプールから破棄します．
     * 
     * @param mc
     *            マネージドコネクション
     */
    protected void purge(final ManagedConnection mc) {
        boolean removed = false;
        TimerTask task = null;
        synchronized (lock) {
            task = pool.removeFromFreePool(mc);
            removed = task != null || pool.removeFromActivePool(mc);
            if (removed) {
                lock.notifyAll();
            }
        }
        if (task != null) {
            task.cancel();
        }
        if (removed && logger.isDebugEnabled()) {
            logger.log("DJCA1008", new Object[] { mc });
        }
    }

    /**
     * プールするコネクションの最小値を返します．
     * 
     * @return プールするコネクションの最小値
     */
    public int getMinPoolSize() {
        return minPoolSize;
    }

    /**
     * プールするコネクションの最小値を設定します．
     * 
     * @param minPoolSize
     *            プールするコネクションの最小値
     */
    public void setMinPoolSize(final int minPoolSize) {
        this.minPoolSize = minPoolSize;
    }

    /**
     * プールするコネクションの最大値を返します．
     * 
     * @return プールするコネクションの最大値
     */
    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    /**
     * プールするコネクションの最大値を設定します．
     * 
     * @param maxPoolSize
     *            プールするコネクションの最大値
     */
    public void setMaxPoolSize(final int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    /**
     * アイドル状態のコネクションを開放するまでの時間 (秒単位) を返します．
     * 
     * @return アイドル状態のコネクションを開放するまでの時間 (秒単位)
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * アイドル状態のコネクションを開放するまでの時間 (秒単位) を設定します．
     * 
     * @param timeout
     *            アイドル状態のコネクションを開放するまでの時間 (秒単位)
     */
    public void setTimeout(final int timeout) {
        this.timeout = timeout;
    }

    /**
     * アイドリング状態のままタイムアウト時間が経過したコネクションをクローズする{@link TimerTask}の実装クラスです．
     */
    public class ExpireTask extends TimerTask {

        /** アイドリング状態のマネージドコネクション */
        protected ManagedConnection managedConnection;

        /**
         * インスタンスを構築します．
         * 
         * @param mc
         *            マネージドコネクション
         */
        public ExpireTask(final ManagedConnection mc) {
            this.managedConnection = mc;
            timer.schedule(this, timeout * 1000);
        }

        @Override
        public void run() {
            synchronized (lock) {
                if (pool.getFreePoolSize() <= minPoolSize
                        || pool.removeFromFreePool(managedConnection) == null) {
                    return;
                }
            }
            if (logger.isDebugEnabled()) {
                logger.log("DJCA1008", new Object[] { managedConnection });
            }
            silentRelease(managedConnection);
        }

    }

}
