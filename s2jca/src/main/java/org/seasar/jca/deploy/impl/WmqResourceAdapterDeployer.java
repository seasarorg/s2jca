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
package org.seasar.jca.deploy.impl;

/**
 * <a href="https://genericjmsra.dev.java.net/">Generic Resource Adapter for JMS</a>と
 * IBM WebSphere MQ (formerly known MQSeries) を組み合わせて利用するためのリソースアダプタ・デプロイヤです．
 * <p>
 * Generic Resource Adapter for JMSの次のプロパティを設定します．
 * </p>
 * <table border="1">
 * <tr>
 * <th>プロパティ</th>
 * <th>値</th>
 * </tr>
 * <tr>
 * <td><code>SupportsXA</code></td>
 * <td><code>true</code></td>
 * </tr>
 * <tr>
 * <td><code>ProviderIntegrationMode</code></td>
 * <td><code>javabean</code></td>
 * </tr>
 * <tr>
 * <td><code>ConnectionFactoryClassName</code></td>
 * <td><code>com.ibm.mq.jms.MQConnectionFactory</code></td>
 * </tr>
 * <tr>
 * <td><code>QueueConnectionFactoryClassName</code></td>
 * <td><code>com.ibm.mq.jms.MQQueueConnectionFactory</code></td>
 * </tr>
 * <tr>
 * <td><code>TopicConnectionFactoryClassName</code></td>
 * <td><code>com.ibm.mq.jms.MQTopicConnectionFactory</code></td>
 * </tr>
 * <tr>
 * <td><code>XAConnectionFactoryClassName</code></td>
 * <td><code>com.ibm.mq.jms.MQXAConnectionFactory</code></td>
 * </tr>
 * <tr>
 * <td><code>XAQueueConnectionFactoryClassName</code></td>
 * <td><code>com.ibm.mq.jms.MQXAQueueConnectionFactory</code></td>
 * </tr>
 * <tr>
 * <td><code>XATopicConnectionFactoryClassName</code></td>
 * <td><code>com.ibm.mq.jms.MQXATopicConnectionFactory</code></td>
 * </tr>
 * <tr>
 * <td><code>QueueClassName</code></td>
 * <td><code>com.ibm.mq.jms.MQQueue</code></td>
 * </tr>
 * <tr>
 * <td><code>TopicClassName</code></td>
 * <td><code>com.ibm.mq.jms.MQTopic</code></td>
 * </tr>
 * <tr>
 * <td><code>RMPolicy</code></td>
 * <td><code>OnePerPhysicalConnection</code></td>
 * </tr>
 * <p>
 * 上記以外のプロパティが必要な場合は{@link #setProperty(String, String)}で設定することができます．
 * <p>
 * 
 * @author koichik
 */
public class WmqResourceAdapterDeployer extends RarResourceAdapterDeployer {

    /**
     * インスタンスを構築します．
     * <p>
     * このコンストラクタで生成したインスタンスは，
     * {@link AbstractResourceAdapterDeployer#setBootstrapContext(javax.resource.spi.BootstrapContext)}で
     * ブートストラップコンテキストを設定しなくてはなりません．
     * </p>
     */
    public WmqResourceAdapterDeployer() {
        setupProperties();
    }

    /**
     * デフォルトのブートストラップコンテキストでインスタンスを構築します．
     * 
     * @param numThreads
     *            スレッドプールのスレッド数
     */
    public WmqResourceAdapterDeployer(int numThreads) {
        super(numThreads);
        setupProperties();
    }

    /**
     * Generic Resource Adapter for JMSとIBM WebSphere
     * MQを組み合わせて利用するためのプロパティを設定します．
     */
    protected void setupProperties() {
        setProperty("SupportsXA", "true");
        setProperty("ProviderIntegrationMode", "javabean");
        setProperty("ConnectionFactoryClassName", "com.ibm.mq.jms.MQConnectionFactory");
        setProperty("QueueConnectionFactoryClassName", "com.ibm.mq.jms.MQQueueConnectionFactory");
        setProperty("TopicConnectionFactoryClassName", "com.ibm.mq.jms.MQTopicConnectionFactory");
        setProperty("XAConnectionFactoryClassName", "com.ibm.mq.jms.MQXAConnectionFactory");
        setProperty("XAQueueConnectionFactoryClassName",
                "com.ibm.mq.jms.MQXAQueueConnectionFactory");
        setProperty("XATopicConnectionFactoryClassName",
                "com.ibm.mq.jms.MQXATopicConnectionFactory");
        setProperty("QueueClassName", "com.ibm.mq.jms.MQQueue");
        setProperty("TopicClassName", "com.ibm.mq.jms.MQTopic");
        setProperty("RMPolicy", "OnePerPhysicalConnection");
    }

}
