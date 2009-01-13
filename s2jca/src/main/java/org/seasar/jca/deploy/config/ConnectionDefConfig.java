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

import javax.resource.spi.ManagedConnectionFactory;

/**
 * コネクションの構成情報を保持するクラスです．
 * <p>
 * コネクションの構成情報には次のものがあります．
 * <ul>
 * <li>{@link ManagedConnectionFactory}の実装クラス名</li>
 * <li>ConnectionFactoryのインタフェース名</li>
 * <li>ConnectionFactoryの実装クラス名</li>
 * <li>コネクションのインタフェース名</li>
 * <li>コネクションの実装クラス名</li>
 * </ul>
 * </p>
 * 
 * @author koichik
 */
public class ConnectionDefConfig extends ConfigPropertyContainer {

    // instance fields
    /** 親となる{@link OutboundAdapterConfig} */
    protected final OutboundAdapterConfig parent;

    /** {@link ManagedConnectionFactory} */
    protected String mcfClass;

    /** ConnectionFactoryのインタフェース名 */
    protected String cfInterface;

    /** ConnectionFactoryの実装クラス名 */
    protected String cfImplClass;

    /** Connectionのインタフェース名 */
    protected String connectionInterface;

    /** Connectionの実装クラス名 */
    protected String connectionImplClass;

    /**
     * インスタンスを構築します．
     * 
     * @param parent
     *            親となる{@link OutboundAdapterConfig}
     */
    public ConnectionDefConfig(final OutboundAdapterConfig parent) {
        this.parent = parent;
    }

    /**
     * 親となる{@link OutboundAdapterConfig}を返します．
     * 
     * @return 親となる{@link OutboundAdapterConfig}
     */
    public OutboundAdapterConfig getOutboundAdapter() {
        return parent;
    }

    /**
     * {@link javax.resource.spi.ManagedConnectionFactory}の実装クラス名を返します．
     * 
     * @return {@link javax.resource.spi.ManagedConnectionFactory}の実装クラス名
     */
    public String getMcfClass() {
        return this.mcfClass;
    }

    /**
     * {@link javax.resource.spi.ManagedConnectionFactory}の実装クラス名を設定します．
     * 
     * @param mcfClass
     *            {@link javax.resource.spi.ManagedConnectionFactory}の実装クラス名
     */
    public void setMcfClass(final String mcfClass) {
        this.mcfClass = mcfClass;
    }

    /**
     * ConnectionFactoryのインタフェース名を返します．
     * 
     * @return ConnectionFactoryのインタフェース名
     */
    public String getCfInterface() {
        return this.cfInterface;
    }

    /**
     * ConnectionFactoryのインタフェース名を設定します．
     * 
     * @param cfInterface
     *            ConnectionFactoryのインタフェース名
     */
    public void setCfInterface(final String cfInterface) {
        this.cfInterface = cfInterface;
    }

    /**
     * {@link javax.resource.spi.ConnectionFactory}の実装クラス名を返します．
     * 
     * @return {@link javax.resource.spi.ConnectionFactory}の実装クラス名
     */
    public String getCfImplClass() {
        return this.cfImplClass;
    }

    /**
     * {@link javax.resource.spi.ConnectionFactory}の実装クラス名を設定します．
     * 
     * @param cfImplClass
     *            {@link javax.resource.spi.ConnectionFactory}の実装クラス名
     */
    public void setCfImplClass(final String cfImplClass) {
        this.cfImplClass = cfImplClass;
    }

    /**
     * コネクションのインタフェース名を返します．
     * 
     * @return コネクションのインタフェース名
     */
    public String getConnectionInterface() {
        return this.connectionInterface;
    }

    /**
     * コネクションのインタフェース名を設定します．
     * 
     * @param connectionInterface
     *            コネクションのインタフェース名
     */
    public void setConnectionInterface(final String connectionInterface) {
        this.connectionInterface = connectionInterface;
    }

    /**
     * コネクションの実装クラス名を返します．
     * 
     * @return コネクションの実装クラス名
     */
    public String getConnectionImplClass() {
        return this.connectionImplClass;
    }

    /**
     * コネクションの実装クラス名を設定します．
     * 
     * @param connectionImplClass
     *            コネクションの実装クラス名
     */
    public void setConnectionImplClass(final String connectionImplClass) {
        this.connectionImplClass = connectionImplClass;
    }

}
