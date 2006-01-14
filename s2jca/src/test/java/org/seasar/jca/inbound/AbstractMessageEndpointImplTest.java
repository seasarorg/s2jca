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

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.Status;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

import org.easymock.MockControl;
import org.seasar.jca.inbound.AbstractMessageEndpointImpl;
import org.seasar.jca.unit.EasyMockTestCase;

/**
 * @author koichik
 */
public class AbstractMessageEndpointImplTest extends EasyMockTestCase {
    AbstractMessageEndpointImpl target;
    MessageEndpointFactory mef;
    MockControl mefControl;
    TransactionManager tm;
    MockControl tmControl;
    Transaction tx;
    MockControl txControl;
    XAResource xar;
    MockControl xarControl;
    ClassLoader cl;
    Method method;

    public AbstractMessageEndpointImplTest() {
    }

    public AbstractMessageEndpointImplTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mefControl = createStrictControl(MessageEndpointFactory.class);
        mef = MessageEndpointFactory.class.cast(mefControl.getMock());
        tmControl = createStrictControl(TransactionManager.class);
        tm = TransactionManager.class.cast(tmControl.getMock());
        txControl = createStrictControl(Transaction.class);
        tx = Transaction.class.cast(txControl.getMock());
        xarControl = createStrictControl(XAResource.class);
        xar = XAResource.class.cast(xarControl.getMock());

        cl = new URLClassLoader(new URL[0]);
        method = MessageListener.class.getMethod("onMessage", new Class[] { Message.class });
    }

    public void testDeliveryTransactedSuccessfully() throws Exception {
        target = new TestMessageEndpoint(mef, tm, xar, cl);
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

        // test beforeDelivery(Method)
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.beforeDelivery(method);
                assertSame("0", cl, Thread.currentThread().getContextClassLoader());
            }

            @Override
            public void verify() throws Exception {
                mef.isDeliveryTransacted(method);
                mefControl.setReturnValue(true);
                tm.begin();
                tm.getTransaction();
                tmControl.setReturnValue(tx);
                tx.enlistResource(xar);
                txControl.setReturnValue(true);
            }
        }.doTest();

        target.setSucceeded(true);

        // test afterDelivery()
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.afterDelivery();
                assertSame("1", contextClassLoader, Thread.currentThread().getContextClassLoader());
            }

            @Override
            public void verify() throws Exception {
                tx.getStatus();
                txControl.setReturnValue(Status.STATUS_ACTIVE);
                tx.commit();
            }
        }.doTest();
    }

    public void testDeliveryTransactedFailed() throws Exception {
        target = new TestMessageEndpoint(mef, tm, xar, cl);
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

        // test beforeDelivery(Method)
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.beforeDelivery(method);
                assertSame("0", cl, Thread.currentThread().getContextClassLoader());
            }

            @Override
            public void verify() throws Exception {
                mef.isDeliveryTransacted(method);
                mefControl.setReturnValue(true);
                tm.begin();
                tm.getTransaction();
                tmControl.setReturnValue(tx);
                tx.enlistResource(xar);
                txControl.setReturnValue(true);
            }
        }.doTest();

        // messageListener failed
        target.setSucceeded(false);

        // test afterDelivery()
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.afterDelivery();
                assertSame("1", contextClassLoader, Thread.currentThread().getContextClassLoader());
            }

            @Override
            public void verify() throws Exception {
                tx.rollback();
            }
        }.doTest();
    }

    public void testDeliveryTransactedMarkedRollback() throws Exception {
        target = new TestMessageEndpoint(mef, tm, xar, cl);
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

        // test beforeDelivery(Method)
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.beforeDelivery(method);
                assertSame("0", cl, Thread.currentThread().getContextClassLoader());
            }

            @Override
            public void verify() throws Exception {
                mef.isDeliveryTransacted(method);
                mefControl.setReturnValue(true);
                tm.begin();
                tm.getTransaction();
                tmControl.setReturnValue(tx);
                tx.enlistResource(xar);
                txControl.setReturnValue(true);
            }
        }.doTest();

        target.setSucceeded(true);

        // test afterDelivery()
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.afterDelivery();
                assertSame("1", contextClassLoader, Thread.currentThread().getContextClassLoader());
            }

            @Override
            public void verify() throws Exception {
                tx.getStatus();
                txControl.setReturnValue(Status.STATUS_MARKED_ROLLBACK);
                tx.rollback();
            }
        }.doTest();
    }

    public void testDeliveryNotTransacted() throws Exception {
        target = new TestMessageEndpoint(mef, tm, xar, cl);
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

        // test beforeDelivery(Method)
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.beforeDelivery(method);
                assertSame("0", cl, Thread.currentThread().getContextClassLoader());
            }

            @Override
            public void verify() throws Exception {
                mef.isDeliveryTransacted(method);
                mefControl.setReturnValue(false);
            }
        }.doTest();

        target.setSucceeded(true);

        // test afterDelivery()
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.afterDelivery();
                assertSame("1", contextClassLoader, Thread.currentThread().getContextClassLoader());
            }

            @Override
            public void verify() throws Exception {
            }
        }.doTest();
    }

    public static class TestMessageEndpoint extends AbstractMessageEndpointImpl {
        public TestMessageEndpoint(MessageEndpointFactory messageEndpointFactory,
                TransactionManager transactionManager, XAResource xaResource,
                ClassLoader classLoader) {
            super(messageEndpointFactory, transactionManager, xaResource, classLoader);
        }
    }
}
