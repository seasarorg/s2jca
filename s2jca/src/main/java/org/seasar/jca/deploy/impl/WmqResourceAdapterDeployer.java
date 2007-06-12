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

package org.seasar.jca.deploy.impl;

/**
 * @author koichik
 * 
 */
public class WmqResourceAdapterDeployer extends RarResourceAdapterDeployer {
    public WmqResourceAdapterDeployer() {
        setProperty("supportsXA", "true");
        setProperty("ProviderIntegrationMode", "javabean");
        setProperty("connectionFactoryClassName", "com.ibm.mq.jms.MQConnectionFactory");
        setProperty("queueConnectionFactoryClassName", "com.ibm.mq.jms.MQQueueConnectionFactory");
        setProperty("topicConnectionFactoryClassName", "com.ibm.mq.jms.MQTopicConnectionFactory");
        setProperty("XAConnectionFactoryClassName", "com.ibm.mq.jms.MQXAConnectionFactory");
        setProperty("XAQueueConnectionFactoryClassName",
                "com.ibm.mq.jms.MQXAQueueConnectionFactory");
        setProperty("XATopicConnectionFactoryClassName",
                "com.ibm.mq.jms.MQXATopicConnectionFactory");
        setProperty("queueClassName", "com.ibm.mq.jms.MQQueue");
        setProperty("topicClassName", "com.ibm.mq.jms.MQTopic");
    }
}
