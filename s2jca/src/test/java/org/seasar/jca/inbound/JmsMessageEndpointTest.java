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

import java.net.URL;
import java.net.URLClassLoader;

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
     * <p>
     * 「J2EE Connector Architecture Specification Version 1.5」の 「12.5.6
     * Transacted Delivery (Using Container-Managed Transaction)」 Option A<br>
     * beforeDelivery() および afterDelivery() が呼び出されないケースで，リスナーメソッドが正常終了する場合のテスト
     * </p>
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
     * <p>
     * 「J2EE Connector Architecture Specification Version 1.5」の 「12.5.6
     * Transacted Delivery (Using Container-Managed Transaction)」 Option A<br>
     * beforeDelivery() および afterDelivery() が呼び出されないケースで
     * リスナーメソッドが例外をスローする場合のテスト．
     * </p>
     * <p>
     * 本来JCA仕様ではリスナーメソッドがスローしたい例外はリソースアダプタに伝播すべきであるが，
     * ActiveMQのリソースアダプタがメッセージ受信をやめてしまうため，例外を破棄することとする．
     * </p>
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
        endpoint.onMessage(null);
        assertFalse("1", endpoint.isBeforeDeliveryCalled());
        assertFalse("2", endpoint.isSucceeded());
    }

    /**
     * <p>
     * 「J2EE Connector Architecture Specification Version 1.5」の 「12.5.6
     * Transacted Delivery (Using Container-Managed Transaction)」 Option B<br>
     * beforeDelivery() および afterDelivery() が呼び出されるケースでリスナーメソッドが正常終了する場合のテスト
     * </p>
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
     * <p>
     * 「J2EE Connector Architecture Specification Version 1.5」の 「12.5.6
     * Transacted Delivery (Using Container-Managed Transaction)」 Option B<br>
     * beforeDelivery() および afterDelivery() が呼び出されるケースで
     * リスナーメソッドが例外をスローするする場合のテスト
     * </p>
     * <p>
     * 本来JCA仕様ではリスナーメソッドがスローしたい例外はリソースアダプタに伝播すべきであるが，
     * ActiveMQのリソースアダプタがメッセージ受信をやめてしまうため，例外を破棄することとする．
     * </p>
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
        endpoint.onMessage(null);
        assertTrue("1", endpoint.isBeforeDeliveryCalled());
        assertFalse("2", endpoint.isSucceeded());
    }

    public void testClassLoader() throws Exception {
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        final ClassLoader loader = new URLClassLoader(new URL[0]);

        JmsMessageEndpointImpl endpoint = new JmsMessageEndpointImpl(null, null, null, loader,
                new MessageListener() {
                    public void onMessage(Message message) {
                        assertSame("0", loader, Thread.currentThread().getContextClassLoader());
                    }
                });
        endpoint.doOnMessage(null);
        assertSame("1", contextClassLoader, Thread.currentThread().getContextClassLoader());
    }

    public void testClassLoaderWithException() throws Exception {
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        final ClassLoader loader = new URLClassLoader(new URL[0]);

        JmsMessageEndpointImpl endpoint = new JmsMessageEndpointImpl(null, null, null, loader,
                new MessageListener() {
                    public void onMessage(Message message) {
                        assertSame("0", loader, Thread.currentThread().getContextClassLoader());
                        throw new RuntimeException();
                    }
                });
        endpoint.doOnMessage(null);
        assertSame("1", contextClassLoader, Thread.currentThread().getContextClassLoader());
    }
}
