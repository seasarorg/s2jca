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
package org.seasar.jca.inbound;

import java.lang.reflect.Method;

import javax.resource.ResourceException;
import javax.resource.spi.IllegalStateException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.Status;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

import org.seasar.framework.log.Logger;
import org.seasar.framework.message.MessageFormatter;
import org.seasar.jca.exception.SIllegalStateException;
import org.seasar.jca.exception.SResourceException;

/**
 * @author koichik
 */
public abstract class AbstractMessageEndpointImpl implements MessageEndpoint {
    private static final Logger logger = Logger.getLogger(AbstractMessageEndpointImpl.class);

    protected MessageEndpointFactory messageEndpointFactory;
    protected TransactionManager transactionManager;
    protected Transaction transaction;
    protected XAResource xaResource;
    protected ClassLoader classLoader;
    protected boolean beforeDeliveryCalled;
    protected boolean processing;
    protected boolean succeeded;

    public AbstractMessageEndpointImpl(final MessageEndpointFactory messageEndpointFactory,
            final TransactionManager transactionManager, final XAResource xaResource,
            final ClassLoader classLoader) {
        this.messageEndpointFactory = messageEndpointFactory;
        this.transactionManager = transactionManager;
        this.xaResource = xaResource;
        this.classLoader = classLoader;
    }

    public void beforeDelivery(final Method method) throws NoSuchMethodException, ResourceException {
        if (logger.isDebugEnabled()) {
            logger.log("DJCA1024", new Object[] { this, method });
        }

        if (xaResource != null && messageEndpointFactory.isDeliveryTransacted(method)) {
            beginTransaction();
        }
        beforeDeliveryCalled = true;

        if (logger.isDebugEnabled()) {
            logger.log("DJCA1025", new Object[] { this, method });
        }
    }

    public void afterDelivery() throws ResourceException {
        assertBeforeDeliveryCalled();

        if (logger.isDebugEnabled()) {
            logger.log("DJCA1026", new Object[] { this });
        }

        try {
            if (transaction != null) {
                endTransaction();
            }
        } finally {
            cleanup();
        }

        if (logger.isDebugEnabled()) {
            logger.log("DJCA1027", new Object[] { this });
        }
    }

    public void release() {
        assertNotProcessing();
        cleanup();
        logger.log("DJCA1033", new Object[] { this });
    }

    protected void beginTransaction() throws ResourceException {
        try {
            transactionManager.begin();
            transaction = transactionManager.getTransaction();
            transaction.enlistResource(xaResource);
        } catch (final Exception e) {
            throw new SResourceException("EJCA0000", e);
        }
    }

    protected void endTransaction() throws ResourceException {
        try {
            if (succeeded && transaction.getStatus() == Status.STATUS_ACTIVE) {
                transaction.commit();
            } else {
                transaction.rollback();
            }
        } catch (final Exception e) {
            throw new SResourceException("EJCA0000", e);
        }
    }

    protected void assertBeforeDeliveryCalled() throws IllegalStateException {
        if (!beforeDeliveryCalled) {
            logger.log("EJCA1028", new Object[] { this });
            throw new SIllegalStateException("EJCA1028", new Object[] { this });
        }
    }

    protected void assertNotProcessing() {
        if (isProcessing()) {
            final Object[] params = new Object[] { this };
            logger.log("EJCA1032", params);
            throw new java.lang.IllegalStateException(MessageFormatter.getSimpleMessage("EJCA1032",
                    params));
        }
    }

    protected void cleanup() {
        succeeded = false;
        processing = false;
        beforeDeliveryCalled = false;
        transaction = null;
    }

    protected ClassLoader getClassLoader() {
        return classLoader;
    }

    protected boolean isBeforeDeliveryCalled() {
        return beforeDeliveryCalled;
    }

    protected synchronized boolean isProcessing() {
        return processing;
    }

    protected synchronized void setProcessing(final boolean processing) {
        this.processing = processing;
    }

    protected boolean isSucceeded() {
        return succeeded;
    }

    protected void setSucceeded(final boolean succeeded) {
        this.succeeded = succeeded;
    }
}
