/*
 * Copyright 2004-2010 the Seasar Foundation and the Others.
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

import org.seasar.framework.container.annotation.tiger.Binding;
import org.seasar.framework.container.annotation.tiger.BindingType;
import org.seasar.framework.container.annotation.tiger.DestroyMethod;
import org.seasar.framework.container.annotation.tiger.InitMethod;
import org.seasar.jca.deploy.ResourceAdapterDeployer;

/**
 * メッセージエンドポイントを開始・終了するためのコンポーネントです．
 * 
 * @author koichik
 */
public class MessageEndpointActivator {

    /** {@link ResourceAdapterDeployer} */
    protected ActivationSpecDeployer activationSpecDeployer;

    
    /**
     * 開始・終了するエンドポイントの{@link ResourceAdapterDeployer}を設定します．
     * 
     * @param activationSpecDeployer {@link ResourceAdapterDeployer}
     */
    @Binding(bindingType=BindingType.MUST)
    public void setActivationSpecDeployer(final ActivationSpecDeployer activationSpecDeployer) {
        this.activationSpecDeployer = activationSpecDeployer;
    }

    /**
     * メッセージエンドポイントを開始します．
     * 
     * @throws ResourceException
     *             メッセージエンドポイントの開始中に例外が発生した場合
     */
    @InitMethod
    public void start() throws ResourceException {
        activationSpecDeployer.activate();
    }

    /**
     * メッセージエンドポイントを終了します．
     * 
     * @throws ResourceException
     *             メッセージエンドポイントの終了中に例外が発生した場合
     */
    @DestroyMethod
    public void stop() throws ResourceException {
        activationSpecDeployer.deactivate();
    }

}
