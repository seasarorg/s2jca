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

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import javax.resource.ResourceException;
import javax.resource.spi.ManagedConnection;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.seasar.framework.log.Logger;
import org.seasar.jca.exception.SResourceException;
import org.seasar.jca.outbound.support.ConnectionManagementContext;
import org.seasar.jca.outbound.support.ManagedConnectionPool;

/**
 * @author koichik
 */
public abstract class AbstractTransactionBoundedPoolingPolicy extends AbstractPolicy implements
        Synchronization {
    private static final Logger logger = Logger
            .getLogger(AbstractTransactionBoundedPoolingPolicy.class);

    protected final Map<Transaction, ManagedConnectionPool<Object>> pools = Collections
            .synchronizedMap(new WeakHashMap<Transaction, ManagedConnectionPool<Object>>());
    protected final TransactionManager tm;
    protected boolean allowLocalTx;

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
            final ManagedConnectionPool pool = getPool(tm.getTransaction(), false);
            pool.remove(mc);
            nextPolicy.connectionErrorOccurred(mc);
        } catch (final SystemException e) {
            throw new SResourceException("EJCA0000", e);
        }
    }

    public void beforeCompletion() {
    }

    public void afterCompletion(final int status) {
        releaseContext();
    }

    protected void checkOut(final ConnectionManagementContext context) throws SResourceException,
            ResourceException {
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

    protected void allocateUnboundConnection(final ConnectionManagementContext context)
            throws ResourceException {
        if (!allowLocalTx) {
            throw new SResourceException("EJCA1009");
        }
        nextPolicy.allocate(context);
    }

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

    protected abstract void associateTx(Transaction tx, ManagedConnectionPool<Object> pool,
            ConnectionManagementContext holder) throws ResourceException;

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

    protected void releaseContext() {
        try {
            final Transaction tx = tm.getTransaction();
            final ManagedConnectionPool<Object> pool = pools.get(tx);
            if (pool != null) {
                pool.close();
            }
        } catch (final SystemException e) {
            logger.log("EJCA0000", null, e);
        }
    }

    /**
     * プロパティ allowLocalTx の値を返します。
     * 
     * @return Returns the allowLocalTx.
     */
    public boolean isAllowLocalTx() {
        return allowLocalTx;
    }

    /**
     * プロパティ allowLocalTx の値を設定します。
     * 
     * @param allowLocalTx
     *            The allowLocalTx to set.
     */
    public void setAllowLocalTx(final boolean allowLocalTx) {
        this.allowLocalTx = allowLocalTx;
    }
}
