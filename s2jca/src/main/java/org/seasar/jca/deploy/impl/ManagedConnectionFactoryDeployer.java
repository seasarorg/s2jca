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
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ResourceAdapterAssociation;
import javax.transaction.TransactionManager;

import org.seasar.framework.beans.factory.BeanDescFactory;
import org.seasar.framework.container.annotation.tiger.DestroyMethod;
import org.seasar.framework.log.Logger;
import org.seasar.framework.util.tiger.ReflectionUtil;
import org.seasar.jca.deploy.ResourceAdapterDeployer;
import org.seasar.jca.deploy.config.ConnectionDefConfig;
import org.seasar.jca.exception.SResourceException;
import org.seasar.jca.outbound.ConnectionManagerImpl;
import org.seasar.jca.outbound.policy.BasicPoolingPolicy;
import org.seasar.jca.outbound.policy.LocalTransactionBoundedPoolingPolicy;
import org.seasar.jca.outbound.policy.XATransactionBoundedPoolingPolicy;

/**
 * {@link ManagedConnectionFactory}をデプロイするクラスです．
 * 
 * @author koichik
 */
public class ManagedConnectionFactoryDeployer extends AbstractDeployer<ManagedConnectionFactory> {

    // static fields
    private static final Logger logger = Logger.getLogger(ManagedConnectionFactoryDeployer.class);

    // instance fields
    /** リソースアダプタ・デプロイヤ */
    protected final ResourceAdapterDeployer raDeployer;

    /** トランザクションマネージャ */
    protected TransactionManager tm;

    /** {@link ManagedConnectionFactory}の実装クラス名 */
    protected String mcfClassName;

    /**
     * <code>ra.xml</code>ファイルに記述された何番目の
     * <code>connector/resource-adapter/outbound-resourceadapter/connection-definition</code>を
     * 扱うかを示すインデックス
     */
    protected int mcfIndex;

    /** コネクションがS2JCA側からトランザクション制御される場合は<code>true</code> */
    protected boolean managedTx = true;

    /** リソースローカルなトランザクションを許可する場合は<code>true</code> */
    protected boolean allowLocalTx = false;

    /** コネクションプールの最小値 */
    protected int minPoolSize = 5;

    /** コネクションプールの最大値 */
    protected int maxPoolSize = 10;

    /** アイドル状態になったコネクションをクローズするまでのタイムアウト時間 (秒単位) */
    protected int timeout = 600;

    /** {@link ManagedConnectionFactory} */
    protected ManagedConnectionFactory mcf;

    /** {@link ConnectionManager} */
    protected ConnectionManager cm;

    /** <code>ConnectionFactory</code> */
    protected Object cf;

    /**
     * インスタンスを構築します．
     * 
     * @param raDeployer
     *            リソースアダプタ・デプロイヤ
     */
    public ManagedConnectionFactoryDeployer(final ResourceAdapterDeployer raDeployer) {
        this.raDeployer = raDeployer;
        setClassLoader(raDeployer.getClassLoader());
    }

    /**
     * <code>ConnectionFactory</code>を作成して返します．
     * 
     * @return <code>ConnectionFactory</code>
     * @throws ResourceException
     *             <code>ConnectionFactory</code>の作成中に例外が発生した場合
     */
    public Object createConnectionFactory() throws ResourceException {
        assertOutboundResourceAdapterConfig();
        assertConnectionDefinitionConfig();

        mcf = createManagedConnectionFactory();
        if (logger.isDebugEnabled()) {
            loggingDeployedMessage();
        }

        cm = createConnectionManager();

        cf = mcf.createConnectionFactory(cm);
        return cf;
    }

    /**
     * {@link ConnectionManager}を破棄します．
     */
    @DestroyMethod
    public void dispose() {
        ConnectionManagerImpl.class.cast(cm).dispose();
    }

    /**
     * {@link ManagedConnectionFactory}を作成して返します．
     * 
     * @return {@link ManagedConnectionFactory}
     * @throws ResourceException
     *             {@link ManagedConnectionFactory}の作成中に例外が発生した場合
     */
    protected ManagedConnectionFactory createManagedConnectionFactory() throws ResourceException {
        final Class<? extends ManagedConnectionFactory> mcfClass = ReflectionUtil.forName(
                mcfClassName, getClassLoader()).asSubclass(ManagedConnectionFactory.class);
        final ManagedConnectionFactory mcf = ReflectionUtil.newInstance(mcfClass);
        if (mcf instanceof ResourceAdapterAssociation) {
            ResourceAdapterAssociation.class.cast(mcf).setResourceAdapter(
                    raDeployer.getResourceAdapter());
        }

        final ConnectionDefConfig cdConfig = getConnectionDefinitionConfig();
        cdConfig.putProperties(configProperties);
        applyProperties(BeanDescFactory.getBeanDesc(mcfClass), mcf, cdConfig.getPropertyValues());
        return mcf;
    }

    /**
     * {@link ConnectionManager}を作成して返します．
     * 
     * @return {@link ConnectionManager}
     * @throws ResourceException
     *             {@link ConnectionManager}の作成中に例外が発生した場合
     */
    protected ConnectionManager createConnectionManager() throws ResourceException {
        final ConnectionManagerImpl cm = new ConnectionManagerImpl(mcf);

        if (maxPoolSize > 0) {
            cm.addConnectionManagementPolicy(createBasicPoolingPolicy());
        }

        if (managedTx) {
            final String transactionSupport = getConnectionDefinitionConfig().getOutboundAdapter()
                    .getTransactionSupport();
            if ("XATransaction".equals(transactionSupport)) {
                XATransactionBoundedPoolingPolicy policy = new XATransactionBoundedPoolingPolicy(tm);
                policy.setAllowLocalTx(allowLocalTx);
                cm.addConnectionManagementPolicy(policy);
            } else if ("LocalTransaction".equals(transactionSupport)) {
                LocalTransactionBoundedPoolingPolicy policy = new LocalTransactionBoundedPoolingPolicy(
                        tm);
                policy.setAllowLocalTx(allowLocalTx);
                cm.addConnectionManagementPolicy(policy);
            }
        }

        return cm;
    }

    /**
     * {@link BasicPoolingPolicy}を作成して返します．
     * 
     * @return {@link BasicPoolingPolicy}
     * @throws ResourceException
     *             {@link BasicPoolingPolicy}の作成中に例外が発生した場合
     */
    protected BasicPoolingPolicy createBasicPoolingPolicy() throws ResourceException {
        final BasicPoolingPolicy policy = new BasicPoolingPolicy(raDeployer.getBootstrapContext());
        policy.setMaxPoolSize(maxPoolSize);
        policy.setMinPoolSize(minPoolSize);
        policy.setTimeout(timeout);
        return policy;
    }

    /**
     * リソースアダプタの<code>ra.xml</code>に
     * <code>connector/resource-adapter/outbound-resourceadapter</code>が
     * 定義されていることを確認します．
     * 
     * @throws ResourceException
     *             <code>ra.xml</code>に<code>connector/resource-adapter/outbound-resourceadapter</code>が定義されていない場合
     */
    protected void assertOutboundResourceAdapterConfig() throws ResourceException {
        if (raDeployer.getResourceAdapterConfig().getOutboundAdapterSize() == 0) {
            throw new SResourceException("EJCA1015");
        }
    }

    /**
     * リソースアダプタの<code>ra.xml</code>に
     * <code>connector/resource-adapter/outbound-resourceadapter/connection-definition</code>を
     * 定義されていることを確認します．
     * 
     * @throws ResourceException
     *             <code>ra.xml</code>に<code>connector/resource-adapter/outbound-resourceadapter/connection-definition</code>が定義されていない場合
     */
    protected void assertConnectionDefinitionConfig() throws ResourceException {
        if (getConnectionDefinitionConfig() == null) {
            throw new SResourceException("EJCA1012", new Object[] { mcfClassName });
        }
    }

    /**
     * リソースアダプタ・デプロイヤから
     * <code>connector/resource-adapter/outbound-resourceadapter/connection-definition</code>を
     * 取得して返します．
     * 
     * @return <code>connector/resource-adapter/outbound-resourceadapter/connection-definition</code>
     */
    protected ConnectionDefConfig getConnectionDefinitionConfig() {
        return raDeployer.getResourceAdapterConfig().getConnectionDef(mcfClassName, mcfIndex);
    }

    /**
     * {@link ManagedConnectionFactory}をデプロイした情報をログに出力します．
     */
    protected void loggingDeployedMessage() {
        final StringBuilder buf = new StringBuilder(1000);

        buf.append("\t").append("managedconnectionfactory-class : ").append(mcfClassName).append(
                LINE_SEPARATOR);

        final ConnectionDefConfig cdConfig = getConnectionDefinitionConfig();
        buf.append("\t").append("connectionfactory-interface : ").append(cdConfig.getCfInterface())
                .append(LINE_SEPARATOR);
        buf.append("\t").append("connectionfactory-impl-class : ")
                .append(cdConfig.getCfImplClass()).append(LINE_SEPARATOR);
        buf.append("\t").append("connection-interface : ")
                .append(cdConfig.getConnectionInterface()).append(LINE_SEPARATOR);
        buf.append("\t").append("connection-impl-class : ").append(
                cdConfig.getConnectionImplClass()).append(LINE_SEPARATOR);

        loggingConfigProperties(cdConfig.getPropertyValues(), "\t", buf);

        logger.log("DJCA1016", new Object[] { new String(buf) });
    }

    /**
     * トランザクションマネージャを返します．
     * 
     * @return トランザクションマネージャ
     */
    public TransactionManager getTransactionManager() {
        return tm;
    }

    /**
     * トランザクションマネージャを設定します．
     * 
     * @param tm
     *            トランザクションマネージャ
     */
    public void setTransactionManager(final TransactionManager tm) {
        this.tm = tm;
    }

    /**
     * {@link ManagedConnectionFactory}の実装クラスを返します．
     * 
     * @return {@link ManagedConnectionFactory}の実装クラス
     */
    public String getManagedConnectionFactoryClass() {
        return this.mcfClassName;
    }

    /**
     * {@link ManagedConnectionFactory}の実装クラスを設定します．
     * 
     * @param mcfClassName
     *            {@link ManagedConnectionFactory}の実装クラス
     */
    public void setManagedConnectionFactoryClass(final String mcfClassName) {
        this.mcfClassName = mcfClassName;
    }

    /**
     * <code>ra.xml</code>ファイルに記述された何番目の
     * <code>connector/resource-adapter/outbound-resourceadapter/connection-definition</code>を
     * 扱うかを示すインデックスを返します．
     * 
     * @return <code>ra.xml</code>ファイルに記述された何番目の<code>connector/resource-adapter/outbound-resourceadapter/connection-definition</code>を扱うかを示すインデックス
     */
    public int getManagedConnectionFactoryIndex() {
        return mcfIndex;
    }

    /**
     * <code>ra.xml</code>ファイルに記述された何番目の
     * <code>connector/resource-adapter/outbound-resourceadapter/connection-definition</code>を
     * 扱うかを示すインデックスを設定します．
     * 
     * @param mcfIndex
     *            <code>ra.xml</code>ファイルに記述された何番目の<code>connector/resource-adapter/outbound-resourceadapter/connection-definition</code>を扱うかを示すインデックス
     */
    public void setManagedConnectionFactoryIndex(final int mcfIndex) {
        this.mcfIndex = mcfIndex;
    }

    /**
     * コネクションがS2JCA側からトランザクション制御される場合は<code>true</code>を返します．
     * 
     * @return コネクションがS2JCA側からトランザクション制御される場合は<code>true</code>
     */
    public boolean isManagedTx() {
        return this.managedTx;
    }

    /**
     * コネクションがS2JCA側からトランザクション制御される場合は<code>true</code>を設定します．
     * 
     * @param managedTx
     *            コネクションがS2JCA側からトランザクション制御される場合は<code>true</code>
     */
    public void setManagedTx(final boolean managedTx) {
        this.managedTx = managedTx;
    }

    /**
     * リソースローカルなトランザクションが許可されている場合は<code>true</code>を返します．
     * 
     * @return リソースローカルなトランザクションが許可されている場合は<code>true</code>
     */
    public boolean isAllowLocalTx() {
        return allowLocalTx;
    }

    /**
     * リソースローカルなトランザクションを許可する場合は<code>true</code>を設定します．
     * 
     * @param allowLocalTx
     *            リソースローカルなトランザクションを許可する場合は<code>true</code>
     */
    public void setAllowLocalTx(final boolean allowLocalTx) {
        this.allowLocalTx = allowLocalTx;
    }

    /**
     * コネクションプールの最小値を返します．
     * 
     * @return コネクションプールの最小値
     */
    public int getMinPoolSize() {
        return minPoolSize;
    }

    /**
     * コネクションプールの最小値を設定します．
     * 
     * @param minPoolSize
     *            コネクションプールの最小値
     */
    public void setMinPoolSize(final int minPoolSize) {
        this.minPoolSize = minPoolSize;
    }

    /**
     * コネクションプールの最大値を返します．
     * 
     * @return コネクションプールの最大値
     */
    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    /**
     * コネクションプールの最大値を設定します．
     * 
     * @param maxPoolSize
     *            コネクションプールの最大値
     */
    public void setMaxPoolSize(final int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    /**
     * アイドル状態になったコネクションをクローズするまでのタイムアウト時間 (秒単位) を返します．
     * 
     * @return アイドル状態になったコネクションをクローズするまでのタイムアウト時間 (秒単位)
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * アイドル状態になったコネクションをクローズするまでのタイムアウト時間 (秒単位) を設定します．
     * 
     * @param timeout
     *            アイドル状態になったコネクションをクローズするまでのタイムアウト時間 (秒単位)
     */
    public void setTimeout(final int timeout) {
        this.timeout = timeout;
    }

}
