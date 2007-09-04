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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import javax.resource.spi.UnavailableException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

import org.seasar.framework.container.ComponentDef;
import org.seasar.framework.container.S2Container;
import org.seasar.framework.container.annotation.tiger.Binding;
import org.seasar.framework.container.annotation.tiger.BindingType;
import org.seasar.framework.container.annotation.tiger.Component;
import org.seasar.framework.container.annotation.tiger.InitMethod;
import org.seasar.framework.exception.SIllegalArgumentException;
import org.seasar.framework.log.Logger;
import org.seasar.framework.util.tiger.ReflectionUtil;

/**
 * {@link MessageEndpointFactory}の抽象クラスです．
 * 
 * @author koichik
 */
@Component
public abstract class AbstractMessageEndpointFactory implements MessageEndpointFactory {

    // static fields
    private static final Logger logger = Logger.getLogger(AbstractMessageEndpointFactory.class);

    // instance fields
    /** S2コンテナ */
    protected S2Container container;

    /** トランザクションマネージャ */
    protected TransactionManager transactionManager;

    /** メッセージエンドポイントの実装クラス */
    protected Class<? extends AbstractMessageEndpoint> endpointClass;

    /** メッセージエンドポイントの実装するリスナの型 */
    protected Class<?> listenerType;

    /** リスナのコンポーネント名 */
    protected String listenerName;

    /** メッセージをトランザクショナルに処理する場合は<code>true</code> */
    protected boolean deliveryTransacted = true;

    /** メッセージエンドポイントのコンストラクタ */
    protected Constructor<? extends AbstractMessageEndpoint> endpointConstructor;

    /** メッセージエンドポイントのコンポーネント定義 */
    protected ComponentDef componentDef;

    /**
     * インスタンスを構築します．
     */
    public AbstractMessageEndpointFactory() {
    }

    /**
     * インスタンスを構築します．
     * 
     * @param endpointClass
     *            メッセージエンドポイントの実装クラス
     * @param listenerType
     *            メッセージエンドポイントの実装するリスナの型
     */
    public AbstractMessageEndpointFactory(Class<? extends AbstractMessageEndpoint> endpointClass,
            Class<?> listenerType) {
        this.endpointClass = endpointClass;
        this.listenerType = listenerType;
    }

    /**
     * S2コンテナを返します．
     * 
     * @return S2コンテナ
     */
    public S2Container getContainer() {
        return container;
    }

    /**
     * S2コンテナを設定します．
     * 
     * @param container
     *            S2コンテナ
     */
    @Binding(bindingType = BindingType.MUST)
    public void setContainer(final S2Container container) {
        this.container = container;
    }

    /**
     * トランザクションマネージャを返します．
     * 
     * @return トランザクションマネージャ
     */
    public TransactionManager getTransactionManager() {
        return transactionManager;
    }

    /**
     * トランザクションマネージャを設定します．
     * 
     * @param transactionManager
     *            トランザクションマネージャ
     */
    @Binding(bindingType = BindingType.MAY)
    public void setTransactionManager(final TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    /**
     * メッセージエンドポイントの実装クラスを設定します．
     * 
     * @param endpointClass
     *            メッセージエンドポイントの実装クラス
     */
    @Binding(bindingType = BindingType.MAY)
    public void setEndpointClass(final Class<? extends AbstractMessageEndpoint> endpointClass) {
        this.endpointClass = endpointClass;
    }

    /**
     * メッセージエンドポイントの実装するリスナのインタフェース型を設定します．
     * 
     * @param listenerType
     *            メッセージエンドポイントの実装するリスナのインタフェース型
     */
    @Binding(bindingType = BindingType.MAY)
    public void setListenerType(final Class<?> listenerType) {
        this.listenerType = listenerType;
    }

    /**
     * メッセージエンドポイントのコンポーネント名を設定します．
     * <p>
     * コンポーネント名が設定されると，そのコンポーネント名でS2コンテナからメッセージエンドポイントをルックアップします．
     * コンポーネント名が設定されなかった場合はリスナのインタフェース型でS2コンテナからルックアップします．
     * </p>
     * 
     * @param listenerName
     *            メッセージエンドポイントのコンポーネント名
     */
    @Binding(bindingType = BindingType.MAY)
    public void setListenerName(final String listenerName) {
        this.listenerName = listenerName;
    }

    /**
     * メッセージをトランザクショナルに処理する場合は<code>true</code>を設定します．
     * 
     * @param deliveryTransacted
     *            メッセージをトランザクショナルに処理する場合は<code>true</code>
     */
    @Binding(bindingType = BindingType.MAY)
    public void setDeliveryTransacted(final boolean deliveryTransacted) {
        this.deliveryTransacted = deliveryTransacted;
    }

    /**
     * インスタンスを初期化します．
     */
    @InitMethod
    public void initialize() {
        if (deliveryTransacted && transactionManager == null) {
            throw new SIllegalArgumentException("EJCA1021", null);
        }
        if (!listenerType.isAssignableFrom(endpointClass)) {
            throw new SIllegalArgumentException("EJCA1022", new Object[] { listenerType,
                    endpointClass });
        }
        endpointConstructor = ReflectionUtil.getConstructor(endpointClass, new Class[] {
                MessageEndpointFactory.class, TransactionManager.class, XAResource.class,
                ClassLoader.class, listenerType });
        componentDef = container
                .getComponentDef(listenerName != null ? listenerName : listenerType);
    }

    public MessageEndpoint createEndpoint(final XAResource xaResource) throws UnavailableException {
        final MessageEndpoint messageEndpoint = ReflectionUtil.newInstance(endpointConstructor,
                new Object[] { this, transactionManager, xaResource, container.getClassLoader(),
                        componentDef.getComponent() });
        if (logger.isDebugEnabled()) {
            logger.log("DJCA1023", new Object[] { messageEndpoint });
        }
        return messageEndpoint;
    }

    public boolean isDeliveryTransacted(final Method method) throws NoSuchMethodException {
        return deliveryTransacted;
    }

}
