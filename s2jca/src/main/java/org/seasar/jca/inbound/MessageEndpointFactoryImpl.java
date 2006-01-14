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
package org.seasar.jca.inbound;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import javax.jms.MessageListener;
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
import org.seasar.jca.util.ReflectionUtil;

/**
 * @author koichik
 */
@Component
public class MessageEndpointFactoryImpl implements MessageEndpointFactory {
    private static final Logger logger = Logger.getLogger(MessageEndpointFactoryImpl.class);

    protected S2Container container;
    protected TransactionManager transactionManager;
    protected Class<? extends AbstractMessageEndpointImpl> endpointClass = JmsMessageEndpointImpl.class;
    protected Class<?> listenerType = MessageListener.class;
    protected String listenerName;
    protected boolean deliveryTransacted = true;
    protected Constructor<? extends AbstractMessageEndpointImpl> endpointConstructor;
    protected ComponentDef componentDef;

    public MessageEndpointFactoryImpl() {
    }

    public S2Container getContainer() {
        return container;
    }

    @Binding(bindingType = BindingType.MUST)
    public void setContainer(S2Container container) {
        this.container = container;
    }

    public TransactionManager getTransactionManager() {
        return transactionManager;
    }

    @Binding(bindingType = BindingType.MAY)
    public void setTransactionManager(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    @Binding(bindingType = BindingType.MAY)
    public void setEndpointClass(Class<? extends AbstractMessageEndpointImpl> endpointClass) {
        this.endpointClass = endpointClass;
    }

    @Binding(bindingType = BindingType.MAY)
    public void setListenerType(Class<?> listenerType) {
        this.listenerType = listenerType;
    }

    @Binding(bindingType = BindingType.MAY)
    public void setListenerName(String listenerName) {
        this.listenerName = listenerName;
    }

    @Binding(bindingType = BindingType.MAY)
    public void setDeliveryTransacted(final boolean deliveryTransacted) {
        this.deliveryTransacted = deliveryTransacted;
    }

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
        MessageEndpoint messageEndpoint = ReflectionUtil.newInstance(endpointConstructor,
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
