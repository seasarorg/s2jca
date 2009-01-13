/*
 * Copyright 2004-2009 the Seasar Foundation and the Others.
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
package org.seasar.jca.deploy.config;

import java.util.List;
import java.util.Set;

import javax.resource.spi.ActivationSpec;

import org.seasar.framework.util.tiger.CollectionsUtil;

/**
 * inboundアダプタの構成情報を保持するクラスです．
 * 
 * @author koichik
 */
public class InboundAdapterConfig {

    // instance fields
    /** メッセージリスナのマップ */
    protected final List<String> messageListenerTypes = CollectionsUtil.newArrayList();

    /** {@link ActivationSpec}の実装クラス名 */
    protected String activationspecClass;

    /** 必須プロパティの名前のセット */
    protected final Set<String> requiredConfigProperties = CollectionsUtil.newHashSet();

    /**
     * インスタンスを構築します．
     */
    public InboundAdapterConfig() {
    }

    /**
     * メッセージリスナのインタフェース名を追加します．
     * 
     * @param messageListenerType
     *            メッセージリスナのインタフェース名
     */
    public void addMessageListenerType(final String messageListenerType) {
        messageListenerTypes.add(messageListenerType);
    }

    /**
     * アクティベーションスペックの実装クラス名を設定します．
     * 
     * @param activationspecClass
     *            アクティベーションスペックの実装クラス名
     */
    public void setActivationspecClass(final String activationspecClass) {
        this.activationspecClass = activationspecClass;
    }

    /**
     * 必須のプロパティ名を追加します．
     * 
     * @param requiredConfigProperty
     *            必須のプロパティ名
     */
    public void addRequiredConfigProperty(final String requiredConfigProperty) {
        requiredConfigProperties.add(requiredConfigProperty);
    }

}
