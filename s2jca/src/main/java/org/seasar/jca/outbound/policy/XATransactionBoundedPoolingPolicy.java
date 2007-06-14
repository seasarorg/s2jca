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

import javax.resource.ResourceException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.seasar.jca.exception.SResourceException;
import org.seasar.jca.outbound.support.ConnectionManagementContext;
import org.seasar.jca.outbound.support.ManagedConnectionPool;

/**
 * コネクションをXAトランザクションに関連づけて管理するポリシーの実装クラスです．
 * 
 * @author koichik
 */
public class XATransactionBoundedPoolingPolicy extends AbstractTransactionBoundedPoolingPolicy {

    // constants
    private static final long serialVersionUID = 1L;

    /**
     * インスタンスを構築します．
     * 
     * @param tm
     *            トランザクションマネージャ
     */
    public XATransactionBoundedPoolingPolicy(final TransactionManager tm) {
        super(tm);
    }

    @Override
    protected void associateTx(final Transaction tx, final ManagedConnectionPool<Object> pool,
            final ConnectionManagementContext context) throws ResourceException {
        try {
            tx.enlistResource(context.getManagedConnection().getXAResource());
            if (pool.size() == 0) {
                tx.registerSynchronization(this);
            }
            return;
        } catch (final RollbackException e) {
            throw new SResourceException("EJCA0000", e);
        } catch (final SystemException e) {
            throw new SResourceException("EJCA0000", e);
        }
    }

}
