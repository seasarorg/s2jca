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

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;

import org.seasar.extension.unit.S2TestCase;

/**
 * @author koichik
 */
public class MessageEndpointFactoryImplTest extends S2TestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        include(getClass().getSimpleName() + ".dicon");
    }

    /**
     * @throws Exception
     */
    public void testCreateEndpoint() throws Exception {
        MessageEndpointFactoryImpl factory = (MessageEndpointFactoryImpl) getComponent(MessageEndpointFactory.class);
        MessageEndpoint endpoint = factory.createEndpoint(null);
        assertTrue("1", endpoint instanceof MessageListener);
    }

    /**
     */
    public static class TestListener implements MessageListener {

        public void onMessage(Message message) {
        }

    }

}
