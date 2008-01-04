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
package org.seasar.jca.deploy.impl;

import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.ResourceAdapterAssociation;
import javax.resource.spi.endpoint.MessageEndpointFactory;

import org.seasar.framework.beans.factory.BeanDescFactory;
import org.seasar.framework.container.annotation.tiger.Binding;
import org.seasar.framework.container.annotation.tiger.BindingType;
import org.seasar.framework.container.annotation.tiger.DestroyMethod;
import org.seasar.framework.container.annotation.tiger.InitMethod;
import org.seasar.framework.log.Logger;
import org.seasar.framework.util.tiger.ReflectionUtil;
import org.seasar.jca.deploy.ResourceAdapterDeployer;
import org.seasar.jca.exception.SResourceException;

/**
 * メッセージエンドポイント ({@link ActivationSpec}) をデプロイするクラスです．
 * 
 * @author koichik
 */
public class ActivationSpecDeployer extends AbstractDeployer<ActivationSpec> {

    // static fields
    private static final Logger logger = Logger.getLogger(ActivationSpecDeployer.class);

    // instance fields
    /** リソースアダプタ・デプロイヤ */
    protected final ResourceAdapterDeployer raDeployer;

    /** {@link MessageEndpointFactory} */
    protected MessageEndpointFactory messageEndpointFactory;

    /** {@link ActivationSpec}の実装クラス名 */
    protected String activationSpecClassName;

    /** {@link ActivationSpec} */
    protected ActivationSpec activationSpec;

    /**
     * インスタンスを構築します．
     * 
     * @param raDeployer
     *            リソースアダプタ・デプロイヤ
     */
    public ActivationSpecDeployer(final ResourceAdapterDeployer raDeployer) {
        this.raDeployer = raDeployer;
        setClassLoader(raDeployer.getClassLoader());
    }

    /**
     * {@link MessageEndpointFactory}を設定します．
     * 
     * @param messageEndpointFactory
     *            {@link MessageEndpointFactory}
     */
    @Binding(bindingType = BindingType.MUST)
    public void setMessageEndpointFactory(final MessageEndpointFactory messageEndpointFactory) {
        this.messageEndpointFactory = messageEndpointFactory;
    }

    /**
     * {@link ActivationSpec}の実装クラス名を設定します．
     * 
     * @param activationSpecClassName
     *            {@link ActivationSpec}の実装クラス名
     */
    @Binding(bindingType = BindingType.MUST)
    public void setActivationSpecClassName(final String activationSpecClassName) {
        this.activationSpecClassName = activationSpecClassName;
    }

    /**
     * メッセージエンドポイントをアクティブ化します．
     * 
     * @throws ResourceException
     *             メッセージエンドポイントのアクティブ化中に例外が発生した場合
     */
    @InitMethod
    public void activate() throws ResourceException {
        assertInboundResourceAdapter();
        activationSpec = createActivationSpec();
        activationSpec.validate();
        raDeployer.getResourceAdapter().endpointActivation(messageEndpointFactory, activationSpec);
        if (logger.isDebugEnabled()) {
            loggingDeployedMessage();
        }
    }

    /**
     * メッセージエンドポイントを非アクティブ化します．
     * 
     * @throws ResourceException
     *             メッセージエンドポイントの非アクティブ化中に例外が発生した場合
     */
    @DestroyMethod
    public void deactivate() throws ResourceException {
        assertInboundResourceAdapter();
        raDeployer.getResourceAdapter()
                .endpointDeactivation(messageEndpointFactory, activationSpec);
        if (logger.isDebugEnabled()) {
            logger.log("DJCA1020", new Object[] { activationSpec });
        }
    }

    /**
     * {@link ActivationSpec}を作成して返します．
     * 
     * @return {@link ActivationSpec}
     * @throws ResourceException
     *             {@link ActivationSpec}を作成中に例外が発生した場合
     */
    protected ActivationSpec createActivationSpec() throws ResourceException {
        final Class<? extends ActivationSpec> activationSpecClass = ReflectionUtil.forName(
                activationSpecClassName, getClassLoader()).asSubclass(ActivationSpec.class);
        final ActivationSpec activationSpec = ReflectionUtil.newInstance(activationSpecClass);
        ResourceAdapterAssociation.class.cast(activationSpec).setResourceAdapter(
                raDeployer.getResourceAdapter());

        applyProperties(BeanDescFactory.getBeanDesc(activationSpecClass), activationSpec,
                configProperties);
        return activationSpec;
    }

    /**
     * リソースアダプタの<code>ra.xml</code>に
     * <code>connector/resource-adapter/inbound-resourceadapter</code>が
     * 定義されていることを確認します．
     * 
     * @throws ResourceException
     *             <code>ra.xml</code>に<code>connector/resource-adapter/inbound-resourceadapter</code>が定義されていない場合
     */
    protected void assertInboundResourceAdapter() throws ResourceException {
        if (raDeployer.getResourceAdapterConfig().getInboundAdapter() == null) {
            throw new SResourceException("EJCA1018");
        }
    }

    /**
     * {@link ActivationSpec}をデプロイした情報をログに出力します．
     */
    protected void loggingDeployedMessage() {
        final StringBuilder buf = new StringBuilder();

        buf.append("\t").append("activationspec-class : ").append(activationSpecClassName).append(
                LINE_SEPARATOR);
        loggingConfigProperties(configProperties, "\t", buf);
        buf.append("\t").append(activationSpec).append(LINE_SEPARATOR);

        logger.log("DJCA1019", new Object[] { new String(buf) });
    }

}
