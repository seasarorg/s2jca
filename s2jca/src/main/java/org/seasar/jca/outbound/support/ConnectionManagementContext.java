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
package org.seasar.jca.outbound.support;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.security.auth.Subject;

import org.seasar.framework.log.Logger;

/**
 * @author koichik
 */
public class ConnectionManagementContext {
    private static final Logger logger = Logger.getLogger(ConnectionManagementContext.class);

    protected Subject subject;
    protected ConnectionRequestInfo info;
    protected ManagedConnectionFactory mcf;
    protected ManagedConnection mc;
    protected Object lch;

    public ConnectionManagementContext(final Subject subject, final ConnectionRequestInfo info,
            final ManagedConnectionFactory mcf) {
        this(subject, info, mcf, null, null);
    }

    public ConnectionManagementContext(final Subject subject, final ConnectionRequestInfo info,
            final ManagedConnectionFactory mcf, final ManagedConnection mc) {
        this(subject, info, mcf, mc, null);
    }

    public ConnectionManagementContext(final Subject subject, final ConnectionRequestInfo info,
            final ManagedConnectionFactory mcf, final ManagedConnection mc, final Object lch) {
        this.subject = subject;
        this.info = info;
        this.mcf = mcf;
        this.mc = mc;
        this.lch = lch;
    }

    public ManagedConnection allocateManagedConnection() throws ResourceException {
        mc = mcf.createManagedConnection(subject, info);
        if (logger.isDebugEnabled()) {
            logger.log("DJCA1000", new Object[] { mc });
        }
        return mc;
    }

    public Object allocateLogicalConnectionHandle() throws ResourceException {
        lch = mc.getConnection(subject, info);
        if (logger.isDebugEnabled()) {
            logger.log("DJCA1001", new Object[] { mc, lch });
        }
        return lch;
    }

    /**
     * �v���p�e�B subject �̒l��Ԃ��܂��B
     * 
     * @return Returns the subject.
     */
    public Subject getSubject() {
        return subject;
    }

    /**
     * �v���p�e�B subject �̒l��ݒ肵�܂��B
     * 
     * @param subject
     *            The subject to set.
     */
    public ConnectionManagementContext setSubject(final Subject subject) {
        this.subject = subject;
        return this;
    }

    /**
     * �v���p�e�B requestInfo �̒l��Ԃ��܂��B
     * 
     * @return Returns the requestInfo.
     */
    public ConnectionRequestInfo getRequestInfo() {
        return info;
    }

    /**
     * �v���p�e�B requestInfo �̒l��ݒ肵�܂��B
     * 
     * @param requestInfo
     *            The requestInfo to set.
     */
    public ConnectionManagementContext setRequestInfo(final ConnectionRequestInfo requestInfo) {
        this.info = requestInfo;
        return this;
    }

    /**
     * �v���p�e�B managedConnectionFactory �̒l��Ԃ��܂��B
     * 
     * @return Returns the managedConnectionFactory.
     */
    public ManagedConnectionFactory getManagedConnectionFactory() {
        return mcf;
    }

    /**
     * �v���p�e�B managedConnectionFactory �̒l��ݒ肵�܂��B
     * 
     * @param managedConnectionFactory
     *            The managedConnectionFactory to set.
     */
    public ConnectionManagementContext setManagedConnectionFactory(
            final ManagedConnectionFactory managedConnectionFactory) {
        this.mcf = managedConnectionFactory;
        return this;
    }

    /**
     * �v���p�e�B managedConnection �̒l��Ԃ��܂��B
     * 
     * @return Returns the managedConnection.
     */
    public ManagedConnection getManagedConnection() {
        return mc;
    }

    /**
     * �v���p�e�B managedConnection �̒l��ݒ肵�܂��B
     * 
     * @param managedConnection
     *            The managedConnection to set.
     */
    public ConnectionManagementContext setManagedConnection(
            final ManagedConnection managedConnection) {
        this.mc = managedConnection;
        return this;
    }

    /**
     * �v���p�e�B logicalConnectionHandle �̒l��Ԃ��܂��B
     * 
     * @return Returns the logicalConnectionHandle.
     */
    public Object getLogicalConnectionHandle() {
        return lch;
    }

    /**
     * �v���p�e�B logicalConnectionHandle �̒l��ݒ肵�܂��B
     * 
     * @param logicalConnectionHandle
     *            The logicalConnectionHandle to set.
     */
    public ConnectionManagementContext setLogicalConnectionHandle(
            final Object logicalConnectionHandle) {
        this.lch = logicalConnectionHandle;
        return this;
    }
}
