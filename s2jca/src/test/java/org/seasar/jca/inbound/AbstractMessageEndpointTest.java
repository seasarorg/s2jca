/*
 * Copyright 2004-2011 the Seasar Foundation and the Others.
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

import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

import org.seasar.framework.unit.EasyMockTestCase;
import org.seasar.framework.util.ClassUtil;
import org.seasar.jca.exception.SResourceException;

import static org.easymock.EasyMock.*;

/**
 * @author koichik
 */
public class AbstractMessageEndpointTest extends EasyMockTestCase {

    AbstractMessageEndpoint target;

    MessageEndpointFactory mef;

    TransactionManager tm;

    Transaction tx;

    XAResource xar;

    ClassLoader cl;

    Method method;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mef = createStrictMock(MessageEndpointFactory.class);
        tm = createStrictMock(TransactionManager.class);
        tx = createStrictMock(Transaction.class);
        xar = createStrictMock(XAResource.class);

        cl = new URLClassLoader(new URL[0]);
        method = Runnable.class.getMethod("run");
    }

    /**
     * @throws Exception
     */
    public void testDeliveryTransactedSuccessfully() throws Exception {
        target = new TestMessageEndpoint(mef, tm, xar, cl);

        // test beforeDelivery(Method)
        new Subsequence() {

            @Override
            public void replay() throws Exception {
                target.beforeDelivery(method);
            }

            @Override
            public void record() throws Exception {
                expect(mef.isDeliveryTransacted(method)).andReturn(true);
                tm.begin();
                expect(tm.getTransaction()).andReturn(tx);
                expect(tx.enlistResource(xar)).andReturn(true);
            }
        }.doTest();

        target.setSucceeded(true);

        // test afterDelivery()
        new Subsequence() {

            @Override
            public void replay() throws Exception {
                target.afterDelivery();
            }

            @Override
            public void record() throws Exception {
                expect(tm.getStatus()).andReturn(Status.STATUS_ACTIVE);
                tm.commit();
            }
        }.doTest();
    }

    /**
     * @throws Exception
     */
    public void testDeliveryTransactedBeforeDeliveryFailed() throws Exception {
        target = new TestMessageEndpoint(mef, tm, xar, cl);

        // test beforeDelivery(Method)
        new Subsequence() {

            @Override
            public void replay() throws Exception {
                try {
                    target.beforeDelivery(method);
                    fail();
                } catch (SResourceException expected) {
                }
            }

            @Override
            public void record() throws Exception {
                expect(mef.isDeliveryTransacted(method)).andReturn(true);
                tm.begin();
                expect(tm.getTransaction()).andReturn(tx);
                expect(tx.enlistResource(xar)).andThrow(new SystemException("Enlist Failed"));
                tm.rollback();
            }
        }.doTest();
    }

    /**
     * @throws Exception
     */
    public void testDeliveryTransactedFailed() throws Exception {
        target = new TestMessageEndpoint(mef, tm, xar, cl);

        // test beforeDelivery(Method)
        new Subsequence() {

            @Override
            public void replay() throws Exception {
                target.beforeDelivery(method);
            }

            @Override
            public void record() throws Exception {
                expect(mef.isDeliveryTransacted(method)).andReturn(true);
                tm.begin();
                expect(tm.getTransaction()).andReturn(tx);
                expect(tx.enlistResource(xar)).andReturn(true);
            }
        }.doTest();

        // messageListener failed
        target.setSucceeded(false);

        // test afterDelivery()
        new Subsequence() {

            @Override
            public void replay() throws Exception {
                target.afterDelivery();
            }

            @Override
            public void record() throws Exception {
                tm.rollback();
            }
        }.doTest();
    }

    /**
     * @throws Exception
     */
    public void testDeliveryTransactedMarkedRollback() throws Exception {
        target = new TestMessageEndpoint(mef, tm, xar, cl);

        // test beforeDelivery(Method)
        new Subsequence() {

            @Override
            public void replay() throws Exception {
                target.beforeDelivery(method);
            }

            @Override
            public void record() throws Exception {
                expect(mef.isDeliveryTransacted(method)).andReturn(true);
                tm.begin();
                expect(tm.getTransaction()).andReturn(tx);
                expect(tx.enlistResource(xar)).andReturn(true);
            }
        }.doTest();

        target.setSucceeded(true);

        // test afterDelivery()
        new Subsequence() {

            @Override
            public void replay() throws Exception {
                target.afterDelivery();
            }

            @Override
            public void record() throws Exception {
                expect(tm.getStatus()).andReturn(Status.STATUS_MARKED_ROLLBACK);
                tm.rollback();
            }
        }.doTest();
    }

    /**
     * @throws Exception
     */
    public void testDeliveryNotTransacted() throws Exception {
        target = new TestMessageEndpoint(mef, tm, xar, cl);

        // test beforeDelivery(Method)
        new Subsequence() {

            @Override
            public void replay() throws Exception {
                target.beforeDelivery(method);
            }

            @Override
            public void record() throws Exception {
                expect(mef.isDeliveryTransacted(method)).andReturn(false);
            }
        }.doTest();

        target.setSucceeded(true);

        // test afterDelivery()
        new Subsequence() {

            @Override
            public void replay() throws Exception {
                target.afterDelivery();
            }

            @Override
            public void record() throws Exception {
            }
        }.doTest();
    }

    /**
     */
    public static class TestMessageEndpoint extends AbstractMessageEndpoint implements Runnable {

        /**
         * @param messageEndpointFactory
         * @param transactionManager
         * @param xaResource
         * @param classLoader
         * @param actualEndpoint
         */
        public TestMessageEndpoint(MessageEndpointFactory messageEndpointFactory,
                TransactionManager transactionManager, XAResource xaResource,
                ClassLoader classLoader) {
            super(messageEndpointFactory, transactionManager, xaResource, classLoader);
        }

        public void run() {
            doDelivery(null);
        }

        @Override
        protected Object deligateActualEndpoint(Object arg) {
            return null;
        }

        @Override
        protected Method getListenerMethod() {
            return ClassUtil.getMethod(Runnable.class, "run", null);
        }

    }

}
