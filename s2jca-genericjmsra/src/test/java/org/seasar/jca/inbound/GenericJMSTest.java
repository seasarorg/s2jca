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

public class GenericJMSTest extends S2TestCase {

    protected static volatile int receiveMessages;

    protected TransactionManager tm;
    protected ConnectionFactory cf;

    public GenericJMSTest() {
    }

    public GenericJMSTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        include("jms-inbound.dicon");
        include("jms-outbound.dicon");
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
                ActiveMQTest.class.wait(500);
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
