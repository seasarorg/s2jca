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
 * {@link MessageEndpoint}の抽象クラスです．
 * 
 * @author koichik
 */
public abstract class AbstractMessageEndpointImpl implements MessageEndpoint {

    // static fields
    private static final Logger logger = Logger.getLogger(AbstractMessageEndpointImpl.class);

    // instance fields
    /** メッセージエンドポイントファクトリ */
    protected MessageEndpointFactory messageEndpointFactory;

    /** トランザクションマネージャ */
    protected TransactionManager transactionManager;

    /** トランザクション */
    protected Transaction transaction;

    /** XAリソース */
    protected XAResource xaResource;

    /** クラスローダ */
    protected ClassLoader classLoader;

    /** {@link #beforeDelivery(Method)}が呼び出された場合は<code>true</code> */
    protected boolean beforeDeliveryCalled;

    /** メッセージエンドポイント固有のハンドラメソッドを処理中なら<code>true</code> */
    protected boolean processing;

    /** メッセージエンドポイント固有のハンドラメソッドが正常に終了した場合は<code>true</code> */
    protected boolean succeeded;

    /**
     * インスタンスを構築します．
     * 
     * @param messageEndpointFactory
     *            メッセージエンドポイントファクトリ
     * @param transactionManager
     *            トランザクションマネージャ
     * @param xaResource
     *            XAリソース
     * @param classLoader
     *            クラスローダ
     */
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

    /**
     * トランザクションを開始します．
     * 
     * @throws ResourceException
     *             トランザクションの開始中に例外が発生した場合
     */
    protected void beginTransaction() throws ResourceException {
        try {
            transactionManager.begin();
            transaction = transactionManager.getTransaction();
            transaction.enlistResource(xaResource);
        } catch (final Exception e) {
            throw new SResourceException("EJCA0000", e);
        }
    }

    /**
     * トランザクションを終了します．
     * <p>
     * メッセージエンドポイント固有のハンドラメソッドが正常に終了し，トランザクションマネージャが
     * トランザクション中であればトランザクションをコミットします． それ以外の場合はトランザクションをロールバックします．
     * </p>
     * 
     * @throws ResourceException
     *             トランザクションの終了中に例外が発生した場合
     */
    protected void endTransaction() throws ResourceException {
        try {
            if (succeeded && transactionManager.getStatus() == Status.STATUS_ACTIVE) {
                transactionManager.commit();
            } else {
                transactionManager.rollback();
            }
        } catch (final Exception e) {
            throw new SResourceException("EJCA0000", e);
        }
    }

    /**
     * {@link #beforeDelivery(Method)}が呼び出されて正常に終了していなければ例外をスローします．
     * 
     * @throws IllegalStateException
     *             {@link #beforeDelivery(Method)}が呼び出されて正常に終了していない場合
     */
    protected void assertBeforeDeliveryCalled() throws IllegalStateException {
        if (!beforeDeliveryCalled) {
            logger.log("EJCA1028", new Object[] { this });
            throw new SIllegalStateException("EJCA1028", new Object[] { this });
        }
    }

    /**
     * メッセージエンドポイント固有のハンドラメソッドが処理中でないことを確認します．
     * 
     * @throws java.lang.IllegalStateException
     *             メッセージエンドポイント固有のハンドラメソッドが処理中の場合
     */
    protected void assertNotProcessing() {
        if (isProcessing()) {
            final Object[] params = new Object[] { this };
            logger.log("EJCA1032", params);
            throw new java.lang.IllegalStateException(MessageFormatter.getSimpleMessage("EJCA1032",
                    params));
        }
    }

    /**
     * メッセージエンドポイントの後処理をします．
     */
    protected void cleanup() {
        succeeded = false;
        processing = false;
        beforeDeliveryCalled = false;
        transaction = null;
    }

    /**
     * クラスローダを返します．
     * 
     * @return クラスローダ
     */
    protected ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * {@link MessageEndpoint#beforeDelivery(Method)}が呼び出されていなければ<code>true</code>を返します．
     * 
     * @return {@link MessageEndpoint#beforeDelivery(Method)}が呼び出されていなければ<code>true</code>
     */
    protected boolean isBeforeDeliveryCalled() {
        return beforeDeliveryCalled;
    }

    /**
     * メッセージエンドポイント固有のハンドラメソッドが処理中であれば<code>true</code>を返します．
     * 
     * @return メッセージエンドポイント固有のハンドラメソッドが処理中であれば<code>true</code>
     */
    protected synchronized boolean isProcessing() {
        return processing;
    }

    /**
     * メッセージエンドポイント固有のハンドラメソッドが処理中であれば<code>true</code>を設定します．
     * 
     * @param processing
     *            メッセージエンドポイント固有のハンドラメソッドが処理中であれば<code>true</code>
     */
    protected synchronized void setProcessing(final boolean processing) {
        this.processing = processing;
    }

    /**
     * メッセージエンドポイント固有のハンドラメソッドが正常に処理終了した場合は<code>true</code>を返します．
     * 
     * @return メッセージエンドポイント固有のハンドラメソッドが正常に処理終了した場合は<code>true</code>
     */
    protected boolean isSucceeded() {
        return succeeded;
    }

    /**
     * メッセージエンドポイント固有のハンドラメソッドが正常に処理終了した場合は<code>true</code>を設定します．
     * 
     * @param succeeded
     *            メッセージエンドポイント固有のハンドラメソッドが正常に処理終了した場合は<code>true</code>
     */
    protected void setSucceeded(final boolean succeeded) {
        this.succeeded = succeeded;
    }

}
