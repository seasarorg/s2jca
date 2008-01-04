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
package org.seasar.jca.outbound;

import java.io.Serializable;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;

import org.seasar.framework.log.Logger;
import org.seasar.jca.exception.SResourceException;
import org.seasar.jca.outbound.policy.ConnectionManagementPolicy;
import org.seasar.jca.outbound.support.ConnectionManagementContext;

/**
 * {@link ConnectionManager}の実装クラスです．
 * 
 * @author koichik
 */
public class ConnectionManagerImpl implements ConnectionManager, Serializable {

    // constants
    private static final long serialVersionUID = 1L;

    // static fields
    private static final Logger logger = Logger.getLogger(ConnectionManagerImpl.class);

    // instance fields
    /** マネージドコネクションファクトリ */
    protected final ManagedConnectionFactory mcf;

    /** コネクションイベントリスナ */
    protected final ConnectionEventListener listener = new Listener();

    /** コネクション管理ポリシー */
    protected ConnectionManagementPolicy policy = new NoPoolingPolicy();

    /**
     * インスタンスを構築します．
     */
    public ConnectionManagerImpl() {
        this.mcf = null;
    }

    /**
     * インスタンスを構築します．
     * 
     * @param mcf
     *            マネージドコネクションファクトリ
     */
    public ConnectionManagerImpl(final ManagedConnectionFactory mcf) {
        this.mcf = mcf;
    }

    /**
     * コネクション管理ポリシーを追加します．
     * 
     * @param cmPolicy
     *            コネクション管理ポリシー
     * @throws ResourceException
     *             コネクション管理ポリシーの追加で例外が発生した場合
     */
    public void addConnectionManagementPolicy(final ConnectionManagementPolicy cmPolicy)
            throws ResourceException {
        cmPolicy.initialize(mcf, this.policy);
        this.policy = cmPolicy;
    }

    public Object allocateConnection(final ManagedConnectionFactory mcf,
            final ConnectionRequestInfo info) throws ResourceException {
        assertValidMCF(mcf);
        final ConnectionManagementContext context = new ConnectionManagementContext(null, info, mcf);
        policy.allocate(context);
        final Object lch = context.getLogicalConnectionHandle();
        return lch != null ? lch : context.allocateLogicalConnectionHandle();
    }

    /**
     * インスタンスを破棄します．
     */
    public void dispose() {
        policy.dispose();
    }

    /**
     * マネージドコネクションファクトリが妥当か検証します．
     * 
     * @param mcf
     *            マネージドコネクションファクトリ
     * @throws ResourceException
     *             マネージドコネクションファクトリが不正な場合
     */
    protected void assertValidMCF(final ManagedConnectionFactory mcf) throws ResourceException {
        if (this.mcf != null && this.mcf != mcf) {
            throw new SResourceException("EJCA0015", new Object[] { mcf });
        }
    }

    /**
     * デフォルトで使用されるコネクション管理ポリシーです．
     * <p>
     * このポリシーはコネクションプールしません．
     * </p>
     * 
     * @author koichik
     */
    public class NoPoolingPolicy implements ConnectionManagementPolicy {

        public void initialize(final ManagedConnectionFactory mcf,
                final ConnectionManagementPolicy nextPolicy) throws ResourceException {
        }

        public void allocate(final ConnectionManagementContext context) throws ResourceException {
            final ManagedConnection mc = context.allocateManagedConnection();
            mc.addConnectionEventListener(listener);
        }

        public void release(final ManagedConnection mc) throws ResourceException {
            mc.removeConnectionEventListener(listener);
            mc.destroy();
            if (logger.isDebugEnabled()) {
                logger.log("DJCA1004", new Object[] { mc });
            }
        }

        public void connectionErrorOccurred(final ManagedConnection mc) throws ResourceException {
            mc.destroy();
        }

        public void dispose() {
        }
    }

    /**
     * コネクション管理ポリシーにコネクションイベントを通知するためのコネクションイベントリスナです．
     * 
     * @author koichik
     */
    protected class Listener implements ConnectionEventListener {

        public void connectionClosed(final ConnectionEvent event) {
            try {
                final ManagedConnection mc = (ManagedConnection) event.getSource();
                if (logger.isDebugEnabled()) {
                    logger.log("DJCA1002", new Object[] { mc });
                }
                policy.release(mc);
            } catch (final Exception e) {
                logger.log("EJCA0000", null, e);
            }
        }

        public void connectionErrorOccurred(final ConnectionEvent event) {
            try {
                final ManagedConnection mc = (ManagedConnection) event.getSource();
                logger.log("EJCA1005", new Object[] { mc });
                policy.connectionErrorOccurred(mc);
            } catch (final Exception e) {
                logger.log("EJCA0000", null, e);
            }
        }

        public void localTransactionCommitted(final ConnectionEvent event) {
        }

        public void localTransactionRolledback(final ConnectionEvent event) {
        }

        public void localTransactionStarted(final ConnectionEvent event) {
        }

    }

}
