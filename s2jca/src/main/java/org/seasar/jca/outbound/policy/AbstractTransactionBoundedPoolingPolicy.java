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
package org.seasar.jca.outbound.policy;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import javax.resource.ResourceException;
import javax.resource.spi.ManagedConnection;
import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.seasar.framework.log.Logger;
import org.seasar.jca.exception.SResourceException;
import org.seasar.jca.outbound.support.ConnectionManagementContext;
import org.seasar.jca.outbound.support.ManagedConnectionPool;

/**
 * コネクションをトランザクションに関連づけて管理するポリシーの実装クラスです．
 * 
 * @author koichik
 */
public abstract class AbstractTransactionBoundedPoolingPolicy extends AbstractPolicy {

    // constants
    private static final long serialVersionUID = 1L;

    // static fields
    private static final Logger logger = Logger
            .getLogger(AbstractTransactionBoundedPoolingPolicy.class);

    // instance fields
    /** トランザクションとマネージドコネクションプールのマッピング */
    protected final Map<Transaction, ManagedConnectionPool<Object>> pools = Collections
            .synchronizedMap(new WeakHashMap<Transaction, ManagedConnectionPool<Object>>());

    /** トランザクションマネージャ */
    protected final TransactionManager tm;

    /** リソースローカルなトランザクションを許可する場合は<code>true</code> */
    protected boolean allowLocalTx;

    /**
     * インスタンスを構築します．
     * 
     * @param tm
     *            トランザクションマネージャ
     */
    public AbstractTransactionBoundedPoolingPolicy(final TransactionManager tm) {
        super(true);
        this.tm = tm;
    }

    @Override
    public void allocate(final ConnectionManagementContext context) throws ResourceException {
        assertValidMCF(context);
        checkOut(context);
    }

    @Override
    public void release(final ManagedConnection mc) throws ResourceException {
        checkIn(mc);
    }

    @Override
    public void connectionErrorOccurred(final ManagedConnection mc) throws ResourceException {
        try {
            final ManagedConnectionPool<Object> pool = getPool(tm.getTransaction(), false);
            if (pool != null) {
                pool.remove(mc);
            }
            super.connectionErrorOccurred(mc);
        } catch (final SystemException e) {
            throw new SResourceException("EJCA0000", e);
        }
    }

    /**
     * コネクションをチェックアウトします．
     * <p>
     * 現在のトランザクションにマネージドコネクションが割り当てられていれば，それをコネクション管理コンテキストに設定します．
     * それ以外の場合は後続のコネクション管理ポリシーからコネクションを割り当てます．
     * </p>
     * 
     * @param context
     *            コネクション管理ポリシー
     * @throws ResourceException
     *             コネクションのチェックアウト中に例外が発生した場合
     */
    protected void checkOut(final ConnectionManagementContext context) throws ResourceException {
        try {
            final Transaction tx = tm.getTransaction();
            if (tx == null) {
                allocateUnboundConnection(context);
                return;
            }

            final ManagedConnectionPool<Object> pool = getPool(tx, true);
            final ManagedConnection mc = pool.getMatched(context.getSubject(), context
                    .getRequestInfo(), mcf);
            if (mc != null) {
                pool.moveFreeToActivePool(mc);
                context.setManagedConnection(mc);
            } else {
                allocateNew(context, tx, pool);
            }
        } catch (final SystemException e) {
            throw new SResourceException("EJCA0000", e);
        }
    }

    /**
     * スレッドがトランザクションに関連づけられていない場合で，ローカルトランザクションが許可されている場合は，後続のコネクション管理ポリシーからコネクションを割り当てます．
     * 
     * @param context
     *            コネクション管理ポリシー
     * @throws ResourceException
     *             コネクションの割り当て中に例外が発生した場合
     */
    protected void allocateUnboundConnection(final ConnectionManagementContext context)
            throws ResourceException {
        if (!allowLocalTx) {
            throw new SResourceException("EJCA1009");
        }
        nextPolicy.allocate(context);
    }

    /**
     * 後続のコネクション管理ポリシーから取得したマネージドコネクションをプールします．
     * 
     * @param context
     *            コネクション管理ポリシー
     * @param tx
     *            トランザクション
     * @param pool
     *            マネージドコネクションのプール
     * @throws ResourceException
     *             コネクションの取得中に例外が発生した場合
     */
    protected void allocateNew(final ConnectionManagementContext context, final Transaction tx,
            final ManagedConnectionPool<Object> pool) throws ResourceException {
        ManagedConnection mc;
        nextPolicy.allocate(context);
        associateTx(tx, pool, context);
        mc = context.getManagedConnection();
        pool.addToActivePool(mc);
        if (logger.isDebugEnabled()) {
            logger.log("DJCA1010", new Object[] { mc, tx });
        }
    }

    /**
     * コネクションをトランザクションに関連づけます．
     * 
     * @param tx
     *            トランザクション
     * @param pool
     *            マネージドコネクションのプール
     * @param context
     *            コネクション管理コンテキスト
     * @throws ResourceException
     *             関連づけ中に例外が発生した場合
     */
    protected abstract void associateTx(Transaction tx, ManagedConnectionPool<Object> pool,
            ConnectionManagementContext context) throws ResourceException;

    /**
     * マネージドコネクションをプールに戻します．
     * <p>
     * 現在のトランザクションにプールが関連づけられていない場合は後続のコネクション管理ポリシーに返します．
     * </p>
     * 
     * @param mc
     *            マネージドコネクション
     * @throws ResourceException
     *             チェックイン中に例外が発生した場合
     */
    protected void checkIn(final ManagedConnection mc) throws ResourceException {
        try {
            final ManagedConnectionPool<Object> pool = getPool(tm.getTransaction(), false);
            if (pool != null) {
                pool.moveActiveToFreePool(mc);
            } else {
                nextPolicy.release(mc);
            }
        } catch (final SystemException e) {
            throw new SResourceException("EJCA0000", e);
        }
    }

    /**
     * 現在のトランザクションに関連づけられたマネージドコネクションのプールを返します．
     * 
     * @param tx
     *            トランザクション
     * @param create
     *            現在のトランザクションにプールがまだ関連づけられていない場合に新たにプールを作成する場合は<code>true</code>
     * @return マネージドコネクションのプール
     */
    protected ManagedConnectionPool<Object> getPool(final Transaction tx, final boolean create) {
        if (tx == null) {
            return null;
        }

        ManagedConnectionPool<Object> pool = pools.get(tx);
        if (pool == null && create) {
            pool = new ManagedConnectionPool<Object>(nextPolicy);
            pools.put(tx, pool);
        }
        return pool;
    }

    /**
     * 現在のトランザクションにマネージドコネクションのプールを関連づけます．
     * 
     * @param tx
     *            トランザクション
     * @throws IllegalStateException
     *             例外が発生した場合
     * @throws RollbackException
     *             例外が発生した場合
     * @throws SystemException
     *             例外が発生した場合
     */
    protected void registerContext(final Transaction tx) throws IllegalStateException,
            RollbackException, SystemException {
        tx.registerSynchronization(new Synchronization() {

            public void beforeCompletion() {
            }

            public void afterCompletion(int status) {
                releaseContext(tx);
            }

        });
    }

    /**
     * 現在のトランザクションに関連づけられているマネージドコネクションのプールをクローズします．
     * 
     * @param tx
     *            トランザクション
     */
    protected void releaseContext(final Transaction tx) {
        final ManagedConnectionPool<Object> pool = pools.get(tx);
        if (pool != null) {
            pool.close();
        }
    }

    /**
     * リソースローカルなトランザクションが許可されている場合は<code>true</code>を返します．
     * 
     * @return リソースローカルなトランザクションが許可されている場合は<code>true</code>
     */
    public boolean isAllowLocalTx() {
        return allowLocalTx;
    }

    /**
     * リソースローカルなトランザクションが許可されている場合は<code>true</code>を設定します．
     * 
     * @param allowLocalTx
     *            リソースローカルなトランザクションが許可されている場合は<code>true</code>
     */
    public void setAllowLocalTx(final boolean allowLocalTx) {
        this.allowLocalTx = allowLocalTx;
    }
}
