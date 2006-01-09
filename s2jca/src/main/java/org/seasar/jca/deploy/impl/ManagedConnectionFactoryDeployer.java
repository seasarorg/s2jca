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
package org.seasar.jca.deploy.impl;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ResourceAdapterAssociation;
import javax.transaction.TransactionManager;

import org.seasar.framework.beans.factory.BeanDescFactory;
import org.seasar.framework.log.Logger;
import org.seasar.jca.cm.ConnectionManagerImpl;
import org.seasar.jca.cm.policy.BasicPoolingPolicy;
import org.seasar.jca.cm.policy.LocalTransactionBoundedPoolingPolicy;
import org.seasar.jca.cm.policy.XATransactionBoundedPoolingPolicy;
import org.seasar.jca.deploy.ResourceAdapterDeployer;
import org.seasar.jca.deploy.config.ConnectionDefConfig;
import org.seasar.jca.exception.SResourceException;
import org.seasar.jca.util.ReflectionUtil;

/**
 * @author koichik
 */
public class ManagedConnectionFactoryDeployer extends AbstractDeployer<ManagedConnectionFactory> {
    private static final Logger logger = Logger.getLogger(ManagedConnectionFactoryDeployer.class);

    protected final ResourceAdapterDeployer raDeployer;
    protected TransactionManager tm;
    protected String mcfClassName;
    protected int mcfIndex;
    protected boolean managedTx = true;
    protected int minPoolSize = 5;
    protected int maxPoolSize = 10;
    protected int timeout = 600;
    protected ManagedConnectionFactory mcf;
    protected Object cf;

    public ManagedConnectionFactoryDeployer(final ResourceAdapterDeployer raDeployer) {
        this.raDeployer = raDeployer;
        setClassLoader(raDeployer.getClassLoader());
    }

    public Object createConnectionFactory() throws ResourceException {
        assertOutboundResourceAdapterConfig();
        assertConnectionDefinitionConfig();

        mcf = createManagedConnectionFactory();
        if (logger.isDebugEnabled()) {
            loggingDeployedMessage();
        }

        cf = mcf.createConnectionFactory(createConnectionManager());
        return cf;
    }

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

    protected ConnectionManager createConnectionManager() throws ResourceException {
        final ConnectionManagerImpl cm = new ConnectionManagerImpl(mcf);

        if (maxPoolSize > 0) {
            cm.addConnectionManagementPolicy(createBasicPoolingPolicy());
        }

        if (managedTx) {
            final String transactionSupport = getConnectionDefinitionConfig().getOutboundAdapter()
                    .getTransactionSupport();
            if ("XATransaction".equals(transactionSupport)) {
                cm.addConnectionManagementPolicy(new XATransactionBoundedPoolingPolicy(tm));
            } else if ("LocalTransaction".equals(transactionSupport)) {
                cm.addConnectionManagementPolicy(new LocalTransactionBoundedPoolingPolicy(tm));
            }
        }

        return cm;
    }

    protected BasicPoolingPolicy createBasicPoolingPolicy() throws ResourceException {
        final BasicPoolingPolicy policy = new BasicPoolingPolicy(raDeployer.getBootstrapContext());
        policy.setMaxPoolSize(maxPoolSize);
        policy.setMinPoolSize(minPoolSize);
        policy.setTimeout(timeout);
        return policy;
    }

    protected void assertOutboundResourceAdapterConfig() throws ResourceException {
        if (raDeployer.getResourceAdapterConfig().getOutboundAdapterSize() == 0) {
            throw new SResourceException("EJCA1015");
        }
    }

    protected void assertConnectionDefinitionConfig() throws ResourceException {
        if (getConnectionDefinitionConfig() == null) {
            throw new SResourceException("EJCA1012", new Object[] { mcfClassName });
        }
    }

    protected ConnectionDefConfig getConnectionDefinitionConfig() {
        return raDeployer.getResourceAdapterConfig().getConnectionDef(mcfClassName, mcfIndex);
    }

    protected void loggingDeployedMessage() {
        final StringBuilder buf = new StringBuilder();

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

    public TransactionManager getTransactionManager() {
        return tm;
    }

    public void setTransactionManager(final TransactionManager tm) {
        this.tm = tm;
    }

    public String getManagedConnectionFactoryClass() {
        return this.mcfClassName;
    }

    public void setManagedConnectionFactoryClass(final String mcfClassName) {
        this.mcfClassName = mcfClassName;
    }

    public int getManagedConnectionFactoryIndex() {
        return mcfIndex;
    }

    public void setManagedConnectionFactoryIndex(final int mcfIndex) {
        this.mcfIndex = mcfIndex;
    }

    public boolean isManagedTx() {
        return this.managedTx;
    }

    public void setManagedTx(final boolean managedTx) {
        this.managedTx = managedTx;
    }

    public int getMinPoolSize() {
        return minPoolSize;
    }

    public void setMinPoolSize(final int minPoolSize) {
        this.minPoolSize = minPoolSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(final int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(final int timeout) {
        this.timeout = timeout;
    }
}
