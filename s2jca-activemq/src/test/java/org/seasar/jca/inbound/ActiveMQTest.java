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

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.transaction.TransactionManager;

import junit.framework.Assert;

import org.seasar.extension.unit.S2TestCase;

/**
 * <p>
 * ActiveMQ の提供するリソースアダプタを使ったテスト．
 * </p>
 * 
 * @author koichik
 */
public class ActiveMQTest extends S2TestCase {
    protected static volatile int receiveMessages;

    protected TransactionManager tm;
    protected ConnectionFactory cf;

    public ActiveMQTest() {
    }

    public ActiveMQTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        include("jms-activemq-outbound.dicon");
        include("jms-activemq-inbound.dicon");
        receiveMessages = 0;
    }

    public void test() throws Exception {
        for (int i = 0; i < 100; ++i) {
            sendMessage(i);
        }
        int prevCount = 0;
        synchronized (ActiveMQTest.class) {
            while (receiveMessages != prevCount) {
                prevCount = receiveMessages;
                ActiveMQTest.class.wait(100);
            }
            assertEquals("0", 100, receiveMessages);
        }
    }

    protected void sendMessage(int num) throws Exception {
        tm.begin();
        try {
            Connection con = cf.createConnection();
            try {
                Session session = con.createSession(true, Session.SESSION_TRANSACTED);
                try {
                    MessageProducer producer = session.createProducer(session.createQueue("foo"));
                    producer.send(session.createTextMessage(Integer.toString(num)));
                } finally {
                    session.close();
                }
            } finally {
                con.close();
            }
        } finally {
            tm.commit();
        }
    }

    public static class MessageListenerImpl extends Assert implements MessageListener {
        public void onMessage(Message message) {
            synchronized (ActiveMQTest.class) {
                ++receiveMessages;
            }
        }
    }
}
