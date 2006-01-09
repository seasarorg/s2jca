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
package org.seasar.jca.mi;

import javax.jms.Message;
import javax.jms.MessageListener;

import junit.framework.TestCase;

/**
 * @author koichik
 */
public class JmsMessageEndpointTest extends TestCase {
    public JmsMessageEndpointTest() {
    }

    public JmsMessageEndpointTest(String name) {
        super(name);
    }

    /**
     * �uJ2EE Connector Architecture Specification Version 1.5�v�� �u12.5.6
     * Transacted Delivery (Using Container-Managed Transaction)�v Option A
     * beforeDelivery() ����� afterDelivery() ���Ăяo����Ȃ��P�[�X�� ���X�i�[���\�b�h������I������ꍇ�̃e�X�g
     * 
     * @throws Exception
     */
    public void testTargetSuccessfullyCompletedA() throws Exception {
        JmsMessageEndpointImpl endpoint = new JmsMessageEndpointImpl(null, null, null, null,
                new MessageListener() {
                    public void onMessage(Message message) {
                    }
                });
        endpoint.onMessage(null);
        assertFalse("1", endpoint.isBeforeDeliveryCalled());
        assertFalse("2", endpoint.isSucceeded());
    }

    /**
     * �uJ2EE Connector Architecture Specification Version 1.5�v�� �u12.5.6
     * Transacted Delivery (Using Container-Managed Transaction)�v Option A
     * beforeDelivery() ����� afterDelivery() ���Ăяo����Ȃ��P�[�X�� ���X�i�[���\�b�h����O���X���[����ꍇ�̃e�X�g
     * 
     * @throws Exception
     */
    public void testTargetFailedA() throws Exception {
        JmsMessageEndpointImpl endpoint = new JmsMessageEndpointImpl(null, null, null, null,
                new MessageListener() {
                    public void onMessage(Message message) {
                        throw new RuntimeException();
                    }
                });
        try {
            endpoint.onMessage(null);
            fail("0");
        } catch (RuntimeException e) {
        }
        assertFalse("1", endpoint.isBeforeDeliveryCalled());
        assertFalse("2", endpoint.isSucceeded());
    }

    /**
     * �uJ2EE Connector Architecture Specification Version 1.5�v�� �u12.5.6
     * Transacted Delivery (Using Container-Managed Transaction)�v Option B
     * beforeDelivery() ����� afterDelivery() ���Ăяo�����P�[�X�� ���X�i�[���\�b�h������I������ꍇ�̃e�X�g
     * 
     * @throws Exception
     */
    public void testTargetSuccessfullyCompletedB() throws Exception {
        JmsMessageEndpointImpl endpoint = new JmsMessageEndpointImpl(null, null, null, null,
                new MessageListener() {
                    public void onMessage(Message message) {
                    }
                });
        endpoint.beforeDeliveryCalled = true;
        endpoint.onMessage(null);
        assertTrue("1", endpoint.isBeforeDeliveryCalled());
        assertTrue("2", endpoint.isSucceeded());
    }

    /**
     * �uJ2EE Connector Architecture Specification Version 1.5�v�� �u12.5.6
     * Transacted Delivery (Using Container-Managed Transaction)�v Option B
     * beforeDelivery() ����� afterDelivery() ���Ăяo�����P�[�X��
     * ���X�i�[���\�b�h����O���X���[���邷��ꍇ�̃e�X�g
     * 
     * @throws Exception
     */
    public void testTargetFailedB() throws Exception {
        JmsMessageEndpointImpl endpoint = new JmsMessageEndpointImpl(null, null, null, null,
                new MessageListener() {
                    public void onMessage(Message message) {
                        throw new RuntimeException();
                    }
                });
        endpoint.beforeDeliveryCalled = true;
        try {
            endpoint.onMessage(null);
            fail("0");
        } catch (RuntimeException e) {
        }
        assertTrue("1", endpoint.isBeforeDeliveryCalled());
        assertFalse("2", endpoint.isSucceeded());
    }
}
