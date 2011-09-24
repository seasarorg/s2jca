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
package org.seasar.jca.outbound;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.transaction.TransactionManager;

import org.seasar.extension.unit.S2TestCase;

/**
 * <p>
 * ActiveMQ の提供するリソースアダプタを使ったテスト．
 * </p>
 * 
 * @author koichik
 */
public class ActiveMQTest extends S2TestCase {
    protected TransactionManager tm;
    protected ConnectionFactory cf;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        include("jms-outbound.dicon");
    }

    public void test() throws Exception {
        Thread thread = new Thread(new Runnable() {
            public void run() {
                try {
                    recvMessage();
                    recvMessage();
                } catch (Exception e) {
                    e.printStackTrace();
                    fail(e.toString());
                }
            }
        });
        thread.start();
        Thread.yield();
        sendMessage();
        sendMessage();
        thread.join();
    }

    protected void sendMessage() throws Exception {
        tm.begin();
        try {
            Connection con = cf.createConnection();
            try {
                Session session = con.createSession(true, Session.SESSION_TRANSACTED);
                try {
                    MessageProducer producer = session.createProducer(session.createQueue("foo"));
                    producer.send(session.createTextMessage("hoge"));
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

    protected void recvMessage() throws Exception {
        tm.begin();
        try {
            Connection con = cf.createConnection();
            try {
                con.start();
                Session session = con.createSession(true, Session.SESSION_TRANSACTED);
                try {
                    MessageConsumer consumer = session.createConsumer(session.createQueue("foo"));
                    TextMessage msg = (TextMessage) consumer.receive();
                    assertEquals("1", "hoge", msg.getText());
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
}
