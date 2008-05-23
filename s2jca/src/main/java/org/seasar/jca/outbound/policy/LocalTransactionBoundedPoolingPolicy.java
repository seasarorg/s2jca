/*
 * Copyright 2004-2008 the Seasar Foundation and the Others.
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

import javax.resource.ResourceException;
import javax.resource.spi.LocalTransaction;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.seasar.jca.exception.SResourceException;
import org.seasar.jca.outbound.support.ConnectionManagementContext;
import org.seasar.jca.outbound.support.LocalTransactionXAResource;
import org.seasar.jca.outbound.support.ManagedConnectionPool;

/**
 * コネクションをリソースローカルなトランザクションに関連づけて管理するポリシーの実装クラスです．
 * 
 * @author koichik
 */
public class LocalTransactionBoundedPoolingPolicy extends AbstractTransactionBoundedPoolingPolicy {

    // constants
    private static final long serialVersionUID = 1L;

    /**
     * インスタンスを構築します．
     * 
     * @param tm
     *            トランザクションマネージャ
     */
    public LocalTransactionBoundedPoolingPolicy(final TransactionManager tm) {
        super(tm);
    }

    @Override
    protected void associateTx(final Transaction tx, final ManagedConnectionPool<Object> pool,
            final ConnectionManagementContext context) throws ResourceException {
        try {
            final Object lch = context.getLogicalConnectionHandle();
            if (lch == null) {
                context.allocateLogicalConnectionHandle();
            }

            final LocalTransaction localTx = context.getManagedConnection().getLocalTransaction();
            tx.enlistResource(new LocalTransactionXAResource(localTx));
            if (pool.size() == 0) {
                registerContext(tx);
            }
            return;
        } catch (final RollbackException e) {
            throw new SResourceException("EJCA0000", e);
        } catch (final SystemException e) {
            throw new SResourceException("EJCA0000", e);
        }
    }

}
