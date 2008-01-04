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
package org.seasar.jca.outbound.support;

import javax.resource.ResourceException;
import javax.resource.spi.LocalTransaction;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.seasar.extension.jta.xa.DefaultXAResource;
import org.seasar.framework.exception.SXAException;

/**
 * {@link LocalTransaction}を{@link XAResource}として扱うためのクラスです．
 * 
 * @author koichik
 */
public class LocalTransactionXAResource extends DefaultXAResource {

    // instance fields
    /** ローカルトランザクション */
    protected final LocalTransaction tx;

    /** リードオンリーなら<code>true</code> */
    protected boolean readOnly;

    /** トランザクションが進行中なら<code>true</code> */
    protected boolean progress;

    /**
     * インスタンスを構築します．
     * 
     * @param tx
     *            ローカルトランザクション
     */
    public LocalTransactionXAResource(final LocalTransaction tx) {
        this.tx = tx;
    }

    @Override
    protected void doBegin(final Xid xid) throws XAException {
        try {
            tx.begin();
            progress = true;
        } catch (final ResourceException e) {
            throw new SXAException("EJCA0000", null, e);
        }
    }

    @Override
    protected int doPrepare(final Xid xid) throws XAException {
        return readOnly ? XA_RDONLY : XA_OK;
    }

    @Override
    protected void doCommit(final Xid xid, final boolean onePhase) throws XAException {
        try {
            progress = false;
            tx.commit();
        } catch (final ResourceException e) {
            throw new SXAException("EJCA0000", null, e);
        }
    }

    @Override
    protected void doRollback(final Xid xid) throws XAException {
        try {
            progress = false;
            tx.rollback();
        } catch (final ResourceException e) {
            throw new SXAException("EJCA0000", null, e);
        }
    }

    /**
     * トランザクションが進行中なら<code>true</code>を返します．
     * 
     * @return トランザクションが進行中なら<code>true</code>
     */
    public boolean isProgress() {
        return progress;
    }

    /**
     * リードオンリーなら<code>true</code>を返します．
     * 
     * @return リードオンリーなら<code>true</code>
     */
    public boolean isReadOnly() {
        return readOnly;
    }

    /**
     * リードオンリーなら<code>true</code>を設定します．
     * 
     * @param readOnly
     *            リードオンリーなら<code>true</code>
     */
    public void setReadOnly(final boolean readOnly) {
        this.readOnly = readOnly;
    }

}
