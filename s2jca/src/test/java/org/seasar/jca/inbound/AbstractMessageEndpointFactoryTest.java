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

import java.lang.reflect.Method;

import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

import org.seasar.extension.unit.S2TestCase;
import org.seasar.framework.util.ClassUtil;

/**
 * @author koichik
 */
public class AbstractMessageEndpointFactoryTest extends S2TestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        include(getClass().getSimpleName() + ".dicon");
    }

    /**
     * @throws Exception
     */
    public void testCreateEndpoint() throws Exception {
        MessageEndpointFactory factory = (MessageEndpointFactory) getComponent(MessageEndpointFactory.class);
        MessageEndpoint endpoint = factory.createEndpoint(null);
        assertTrue(endpoint instanceof Runnable);
    }

    /**
     */
    public static class TestEndpointFactory extends AbstractMessageEndpointFactory {

        /**
         */
        public TestEndpointFactory() {
            super(TestEndpoint.class, Runnable.class);
        }
    }

    /**
     */
    public static class TestEndpoint extends AbstractMessageEndpoint implements Runnable {

        private Runnable actualEndpoint;

        /**
         * @param messageEndpointFactory
         * @param transactionManager
         * @param xaResource
         * @param classLoader
         * @param actualEndpoint
         */
        public TestEndpoint(AbstractMessageEndpointFactory messageEndpointFactory,
                TransactionManager transactionManager, XAResource xaResource,
                ClassLoader classLoader, Runnable actualEndpoint) {
            super(messageEndpointFactory, transactionManager, xaResource, classLoader);
            this.actualEndpoint = actualEndpoint;
        }

        public void run() {
            doDelivery(null);
        }

        @Override
        protected Object deligateActualEndpoint(Object arg) {
            actualEndpoint.run();
            return null;
        }

        @Override
        protected Method getListenerMethod() {
            return ClassUtil.getMethod(Runnable.class, "run", null);
        }

    }

    /**
     */
    public static class TestActualEndpoint implements Runnable {

        public void run() {
        }

    }

}
