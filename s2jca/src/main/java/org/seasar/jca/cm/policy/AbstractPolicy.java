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
package org.seasar.jca.cm.policy;

import java.io.Serializable;

import javax.resource.ResourceException;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;

import org.seasar.framework.exception.SIllegalArgumentException;
import org.seasar.framework.log.Logger;
import org.seasar.jca.cm.support.ConnectionManagementContext;
import org.seasar.jca.exception.SResourceException;

/**
 * @author koichik
 */
public abstract class AbstractPolicy implements ConnectionManagementPolicy, Serializable {
    private static final Logger logger = Logger.getLogger(AbstractPolicy.class);

    protected ManagedConnectionFactory mcf;
    protected ConnectionManagementPolicy nextPolicy;
    protected final boolean needMCF;

    protected AbstractPolicy(final boolean needMCF) {
        this.needMCF = needMCF;
    }

    public void initialize(final ManagedConnectionFactory mcf,
            final ConnectionManagementPolicy nextPolicy) throws ResourceException {
        if (needMCF && mcf == null) {
            throw new SIllegalArgumentException("EJCA1011", null);
        }
        this.mcf = mcf;
        this.nextPolicy = nextPolicy;
    }

    public void allocate(final ConnectionManagementContext context) throws ResourceException {
        assertValidMCF(context);
        nextPolicy.allocate(context);
    }

    public void release(final ManagedConnection mc) throws ResourceException {
        nextPolicy.release(mc);
    }

    public void connectionErrorOccurred(final ManagedConnection mc) throws ResourceException {
        nextPolicy.connectionErrorOccurred(mc);
    }

    public void dispose() {
        nextPolicy.dispose();
    }

    protected void assertValidMCF(final ConnectionManagementContext context)
            throws ResourceException {
        if (this.mcf != null && this.mcf != context.getManagedConnectionFactory()) {
            throw new SResourceException("EJCA1012");
        }
    }

    protected void silentRelease(final ManagedConnection mc) {
        try {
            nextPolicy.release(mc);
        } catch (final ResourceException e) {
            logger.log("EJCA0000", null, e);
        }
    }
}
