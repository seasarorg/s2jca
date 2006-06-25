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
 * @author koichik
 */
public class MessageEndpointDeployer extends AbstractDeployer<ActivationSpec> {
    private static final Logger logger = Logger.getLogger(MessageEndpointDeployer.class);

    protected final ResourceAdapterDeployer raDeployer;
    protected MessageEndpointFactory messageEndpointFactory;
    protected String activationSpecClassName;
    protected ActivationSpec activationSpec;

    public MessageEndpointDeployer(final ResourceAdapterDeployer raDeployer) {
        this.raDeployer = raDeployer;
        setClassLoader(raDeployer.getClassLoader());
    }

    @Binding(bindingType = BindingType.MUST)
    public void setMessageEndpointFactory(MessageEndpointFactory messageEndpointFactory) {
        this.messageEndpointFactory = messageEndpointFactory;
    }

    @Binding(bindingType = BindingType.MUST)
    public void setActivationSpecClassName(String activationSpecClassName) {
        this.activationSpecClassName = activationSpecClassName;
    }

    @InitMethod
    public void activate() throws ResourceException {
        assertInboundResourceAdapter();
        activationSpec = createActivationSpec();
        activationSpec.validate();
        raDeployer.getResourceAdapter().endpointActivation(messageEndpointFactory, activationSpec);
        if (logger.isDebugEnabled()) {
            logger.log("DJCA1019", new Object[] { getLoggingMessage() });
        }
    }

    @DestroyMethod
    public void deactivate() throws ResourceException {
        assertInboundResourceAdapter();
        raDeployer.getResourceAdapter()
                .endpointDeactivation(messageEndpointFactory, activationSpec);
        if (logger.isDebugEnabled()) {
            logger.log("DJCA1020", new Object[] { activationSpec });
        }
    }

    protected ActivationSpec createActivationSpec() throws ResourceException {
        final Class<? extends ActivationSpec> activationSpecClass = ReflectionUtil.forName(
                activationSpecClassName, getClassLoader()).asSubclass(ActivationSpec.class);
        ActivationSpec activationSpec = ReflectionUtil.newInstance(activationSpecClass);
        ResourceAdapterAssociation.class.cast(activationSpec).setResourceAdapter(
                raDeployer.getResourceAdapter());

        applyProperties(BeanDescFactory.getBeanDesc(activationSpecClass), activationSpec,
                configProperties);
        return activationSpec;
    }

    protected void assertInboundResourceAdapter() throws SResourceException {
        if (raDeployer.getResourceAdapterConfig().getInboundAdapter() == null) {
            throw new SResourceException("EJCA1018");
        }
    }

    protected String getLoggingMessage() {
        final StringBuilder buf = new StringBuilder();

        buf.append("\t").append("activationspec-class : ").append(activationSpecClassName).append(
                LINE_SEPARATOR);
        loggingConfigProperties(configProperties, "\t", buf);
        buf.append("\t").append(activationSpec).append(LINE_SEPARATOR);

        return new String(buf);
    }
}
