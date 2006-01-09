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

import java.io.IOException;
import java.io.InputStream;

import javax.resource.ResourceException;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.seasar.framework.beans.factory.BeanDescFactory;
import org.seasar.framework.container.annotation.tiger.InitMethod;
import org.seasar.framework.log.Logger;
import org.seasar.framework.util.SAXParserFactoryUtil;
import org.seasar.framework.xml.SaxHandler;
import org.seasar.framework.xml.SaxHandlerParser;
import org.seasar.jca.deploy.ResourceAdapterDeployer;
import org.seasar.jca.deploy.config.ConnectionDefConfig;
import org.seasar.jca.deploy.config.OutboundAdapterConfig;
import org.seasar.jca.deploy.config.ResourceAdapterConfig;
import org.seasar.jca.util.ReflectionUtil;

/**
 * @author koichik
 */
public abstract class AbstractResourceAdapterDeployer extends AbstractDeployer<ResourceAdapter>
        implements ResourceAdapterDeployer {
    protected static final String META_INF_RA_XML = "META-INF/ra.xml";

    private static final Logger logger = Logger.getLogger(AbstractResourceAdapterDeployer.class);

    protected final BootstrapContext bc;
    protected String path;
    protected ResourceAdapter ra;
    protected ResourceAdapterConfig raConfig;

    protected AbstractResourceAdapterDeployer(final BootstrapContext bc) {
        this.bc = bc;
    }

    @InitMethod
    public void start() throws ResourceException, IOException {
        setClassLoader(createClassLoader());
        loadDeploymentDescripter();
        raConfig.putProperties(configProperties);
        ra = createResourceAdapter();
        ra.start(bc);

        if (logger.isDebugEnabled()) {
            loggingDeployedMessage();
        }
    }

    public void stop() {
        ra.stop();
        if (logger.isDebugEnabled()) {
            logger.log("DJCA1017", null);
        }
    }

    protected ResourceAdapter createResourceAdapter() {
        final Class<? extends ResourceAdapter> raClass = ReflectionUtil.forName(
                raConfig.getRaClass(), getClassLoader()).asSubclass(ResourceAdapter.class);
        final ResourceAdapter ra = ReflectionUtil.newInstance(raClass);
        applyProperties(BeanDescFactory.getBeanDesc(raClass), ra, raConfig.getPropertyValues());
        return ra;
    }

    protected void loadDeploymentDescripter() throws ResourceException, IOException {
        final SaxHandlerParser parser = createSaxHandlerParser();
        final InputStream is = getDeploymentDescripterAsInputStream();
        try {
            raConfig = ResourceAdapterConfig.class.cast(parser.parse(is));
        } finally {
            is.close();
        }
    }

    protected SaxHandlerParser createSaxHandlerParser() {
        final SAXParserFactory factory = SAXParserFactoryUtil.newInstance();
        final SAXParser saxParser = SAXParserFactoryUtil.newSAXParser(factory);
        final SaxHandler handler = new SaxHandler(new ResourceAdapterTagHandlerRule());
        return new SaxHandlerParser(handler, saxParser);
    }

    protected abstract ClassLoader createClassLoader() throws ResourceException;

    protected abstract InputStream getDeploymentDescripterAsInputStream() throws ResourceException;

    protected void loggingDeployedMessage() {
        final StringBuilder buf = new StringBuilder();

        buf.append("\t").append("display-name : ").append(raConfig.getDisplayName()).append(
                LINE_SEPARATOR);
        buf.append("\t").append("vendor-name : ").append(raConfig.getVendorName()).append(
                LINE_SEPARATOR);
        buf.append("\t").append("eis-type : ").append(raConfig.getEisType()).append(LINE_SEPARATOR);
        buf.append("\t").append("resourceadapter-version : ").append(raConfig.getRaVersion())
                .append(LINE_SEPARATOR);
        buf.append("\t").append("resourceadapter-class : ").append(raConfig.getRaClass()).append(
                LINE_SEPARATOR);

        loggingConfigProperties(raConfig.getPropertyValues(), "\t", buf);

        loggingOutboundResourceAdapter(buf);
        logger.log("DJCA1013", new Object[] { new String(buf) });
    }

    protected void loggingOutboundResourceAdapter(final StringBuilder buf) {
        for (int i = 0; i < raConfig.getOutboundAdapterSize(); ++i) {
            loggingConnectionDefinition(buf, raConfig.getOutboundAdapter(i));
            buf.append("\t").append(
                    "transaction-support : "
                            + raConfig.getOutboundAdapter(i).getTransactionSupport()).append(
                    LINE_SEPARATOR);
        }
    }

    protected void loggingConnectionDefinition(final StringBuilder buf,
            final OutboundAdapterConfig outboundConfig) {
        for (final String mcf : outboundConfig.getMcfClassNames()) {
            for (final ConnectionDefConfig cdConfig : outboundConfig.getConnectionDefs(mcf)) {
                buf.append("\t").append("managedconnectionfactory-class : ").append(
                        cdConfig.getMcfClass()).append(LINE_SEPARATOR);
                loggingConfigProperties(cdConfig.getPropertyValues(), "\t\t", buf);
            }
        }
    }

    public BootstrapContext getBootstrapContext() {
        return bc;
    }

    public String getPath() {
        return this.path;
    }

    public void setPath(final String baseDir) {
        this.path = baseDir;
    }

    public ResourceAdapter getResourceAdapter() {
        return ra;
    }

    public ResourceAdapterConfig getResourceAdapterConfig() {
        return raConfig;
    }
}
