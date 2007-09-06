/**
 * 
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
 * @author koichik
 * 
 */
public class GenericJMSTest extends S2TestCase {
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
