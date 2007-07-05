/*
 * Copyright 2004-2007 the Seasar Foundation and the Others.
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

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

import org.seasar.framework.log.Logger;
import org.seasar.framework.message.MessageFormatter;
import org.seasar.framework.util.ClassUtil;

/**
 * JMS用の{@link MessageEndpoint}の実装クラスです．
 * 
 * @author koichik
 */
public class JMSMessageEndpointImpl extends AbstractMessageEndpointImpl implements MessageListener {

    // static fields
    private static final Logger logger = Logger.getLogger(JMSMessageEndpointImpl.class);

    /** {@link MessageListener#onMessage(Message)}を表すメソッドオブジェクト */
    protected static final Method LISTENER_METHOD = ClassUtil.getMethod(MessageListener.class,
            "onMessage", new Class[] { Message.class });

    /** 移譲先となる本来のメッセージエンドポイント */
    protected MessageListener actualEndpoint;

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
     * @param actualEndpoint
     *            移譲先となる本来のメッセージエンドポイント
     */
    public JMSMessageEndpointImpl(final MessageEndpointFactory messageEndpointFactory,
            final TransactionManager transactionManager, final XAResource xaResource,
            final ClassLoader classLoader, final MessageListener actualEndpoint) {
        super(messageEndpointFactory, transactionManager, xaResource, classLoader);
        this.actualEndpoint = actualEndpoint;
    }

    public void onMessage(final Message message) {
        assertNotReentrant();

        setProcessing(true);
        try {
            if (isBeforeDeliveryCalled()) {
                doOnMessage(message);
            } else {
                beforeDelivery(LISTENER_METHOD);
                try {
                    doOnMessage(message);
                } finally {
                    afterDelivery();
                }
            }
        } catch (final Exception e) {
            logger.error("EJCA0000", e);
        } finally {
            setProcessing(false);
        }
    }

    /**
     * 本来のメッセージエンドポイントに処理を委譲します．
     * <p>
     * コンストラクタで渡されたクラスローダをスレッドのコンテキストクラスローダに設定して委譲します．
     * </p>
     * 
     * @param message
     *            受信したJMSメッセージ
     */
    protected void doOnMessage(final Message message) {
        if (logger.isDebugEnabled()) {
            logger.log("DJCA1029", new Object[] { this, LISTENER_METHOD });
        }

        final ClassLoader originalClassLoader = setContextClassLoader(getClassLoader());
        try {
            actualEndpoint.onMessage(message);
            setSucceeded(true);
        } catch (final RuntimeException e) {
            logger.log("EJCA1031", new Object[] { this, LISTENER_METHOD }, e);
        } finally {
            setContextClassLoader(originalClassLoader);
        }

        if (logger.isDebugEnabled()) {
            logger.log("DJCA1030", new Object[] { this, LISTENER_METHOD });
        }
    }

    /**
     * 引数で指定されたクラスローダをスレッドのコンテキストクラスローダに設定します．
     * 
     * @param loader
     *            コンテキストクラスローダに設定するクラスローダ
     * @return コンテキストクラスローダに設定されていたクラスローダ
     */
    protected ClassLoader setContextClassLoader(final ClassLoader loader) {
        final Thread thread = Thread.currentThread();
        final ClassLoader currentClassLoader = thread.getContextClassLoader();
        thread.setContextClassLoader(loader);
        return currentClassLoader;
    }

    /**
     * リエントラントに呼び出されていないことを確認します．
     * 
     * @throws IllegalStateException
     *             リエントラントに呼び出された場合
     */
    protected void assertNotReentrant() {
        if (isProcessing()) {
            final Object[] params = new Object[] { this, LISTENER_METHOD };
            logger.log("EJCA1034", params);
            throw new IllegalStateException(MessageFormatter.getSimpleMessage("EJCA1034", params));
        }
    }

}
