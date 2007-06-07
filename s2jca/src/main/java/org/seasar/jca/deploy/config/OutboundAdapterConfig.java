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
package org.seasar.jca.deploy.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.seasar.framework.util.tiger.CollectionsUtil;

/**
 * outboundアダプタの構成情報を保持するクラスです．
 * 
 * @author koichik
 */
public class OutboundAdapterConfig {
    // instance fields
    /** コネクション定義構成情報のマップ */
    protected final Map<String, List<ConnectionDefConfig>> connectionDefs = CollectionsUtil
            .newLinkedHashMap();

    /** サポートするトランザクション制御方法の名前 */
    protected String transactionSupport;

    /**
     * インスタンスを構築します．
     * 
     */
    public OutboundAdapterConfig() {
    }

    /**
     * コネクション構成情報を追加します．
     * 
     * @param connectionDef
     *            コネクション構成情報
     */
    public void addConnectionDef(final ConnectionDefConfig connectionDef) {
        final String mcf = connectionDef.getMcfClass();
        List<ConnectionDefConfig> list = connectionDefs.get(mcf);
        if (list == null) {
            list = new ArrayList<ConnectionDefConfig>();
            connectionDefs.put(mcf, list);
        }
        list.add(connectionDef);
    }

    /**
     * {@link javax.resource.spi.ManagedConnectionFactory}の実装クラス名に関連づけられたコネクション定義の数を返します．
     * 
     * @param mcf
     *            {@link javax.resource.spi.ManagedConnectionFactory}の実装クラス名
     * @return {@link javax.resource.spi.ManagedConnectionFactory}の実装クラス名に関連づけられたコネクション定義の数
     */
    public int getConnectionDefSize(final String mcf) {
        final List<ConnectionDefConfig> list = connectionDefs.get(mcf);
        return list == null ? 0 : list.size();
    }

    /**
     * {@link javax.resource.spi.ManagedConnectionFactory}の実装クラス名に関連づけられたコネクション定義を返します．
     * <p>
     * {@link javax.resource.spi.ManagedConnectionFactory}の実装クラス名に関連づけられたコネクション定義が複数ある場合は，
     * 最初に関連づけられたコネクション定義を返します．
     * </p>
     * 
     * @param mcf
     *            {@link javax.resource.spi.ManagedConnectionFactory}の実装クラス名
     * @return {@link javax.resource.spi.ManagedConnectionFactory}の実装クラス名に関連づけられたコネクション定義
     */
    public ConnectionDefConfig getConnectionDef(final String mcf) {
        return getConnectionDef(mcf, 0);
    }

    /**
     * {@link javax.resource.spi.ManagedConnectionFactory}の実装クラス名に関連づけられたコネクション定義を返します．
     * 
     * @param mcf
     *            {@link javax.resource.spi.ManagedConnectionFactory}の実装クラス名
     * @param index
     *            インデックス
     * @return {@link javax.resource.spi.ManagedConnectionFactory}の実装クラス名に関連づけられたコネクション定義
     */
    public ConnectionDefConfig getConnectionDef(final String mcf, final int index) {
        final List<ConnectionDefConfig> list = connectionDefs.get(mcf);
        if (list == null || index >= list.size()) {
            return null;
        }
        return list.get(index);
    }

    /**
     * {@link javax.resource.spi.ManagedConnectionFactory}の実装クラス名に関連づけられたコネクション定義のリストを返します．
     * 
     * @param mcf
     *            {@link javax.resource.spi.ManagedConnectionFactory}の実装クラス名
     * @return {@link javax.resource.spi.ManagedConnectionFactory}の実装クラス名に関連づけられたコネクション定義のリスト
     */
    public List<ConnectionDefConfig> getConnectionDefs(final String mcf) {
        final List<ConnectionDefConfig> list = connectionDefs.get(mcf);
        if (list != null) {
            return list;
        }
        return new ArrayList<ConnectionDefConfig>();
    }

    /**
     * {@link javax.resource.spi.ManagedConnectionFactory}の実装クラス名を返します．
     * 
     * @return {@link javax.resource.spi.ManagedConnectionFactory}の実装クラス名
     */
    public Set<String> getMcfClassNames() {
        return connectionDefs.keySet();
    }

    /**
     * サポートするトランザクションの種類を返します．
     * 
     * @return サポートするトランザクションの種類
     */
    public String getTransactionSupport() {
        return this.transactionSupport;
    }

    /**
     * サポートするトランザクションの種類を設定します．
     * 
     * @param transactionSupport
     *            サポートするトランザクションの種類
     */
    public void setTransactionSupport(final String transactionSupport) {
        this.transactionSupport = transactionSupport;
    }
}
