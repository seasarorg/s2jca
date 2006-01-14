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
 * @author koichik
 */
public class ConnectionManagerImpl implements ConnectionManager, Serializable {
    private static final long serialVersionUID = 1L;

    private static final Logger logger = Logger.getLogger(ConnectionManagerImpl.class);

    protected final ManagedConnectionFactory mcf;
    protected final ConnectionEventListener listener = new Listener();
    protected ConnectionManagementPolicy policy = new NoPoolingPolicy();

    public ConnectionManagerImpl() {
        this.mcf = null;
    }

    public ConnectionManagerImpl(final ManagedConnectionFactory mcf) {
        this.mcf = mcf;
    }

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

    public void dispose() {
        policy.dispose();
    }

    protected void assertValidMCF(final ManagedConnectionFactory mcf) throws SResourceException {
        if (this.mcf != null && this.mcf != mcf) {
            throw new SResourceException("EJCA0015", new Object[] { mcf });
        }
    }

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
            policy.release(mc);
        }

        public void dispose() {
        }
    }

    protected class Listener implements ConnectionEventListener {
        public void connectionClosed(final ConnectionEvent event) {
            try {
                final ManagedConnection mc = (ManagedConnection) event.getSource();
                if (logger.isDebugEnabled()) {
                    logger.log("DJCA1002", new Object[] { mc });
                }
                policy.release(mc);
            } catch (final ResourceException e) {
                logger.log("EJCA0000", null, e);
            }
        }

        public void connectionErrorOccurred(final ConnectionEvent event) {
            try {
                final ManagedConnection mc = (ManagedConnection) event.getSource();
                logger.log("EJCA1005", new Object[] { mc });
                policy.connectionErrorOccurred(mc);
            } catch (final ResourceException e) {
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
