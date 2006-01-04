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

import org.seasar.framework.xml.TagHandler;
import org.seasar.framework.xml.TagHandlerContext;
import org.seasar.framework.xml.TagHandlerRule;
import org.seasar.jca.deploy.config.AdminObjectConfig;
import org.seasar.jca.deploy.config.ConfigProperty;
import org.seasar.jca.deploy.config.ConfigPropertyContainer;
import org.seasar.jca.deploy.config.ConnectionDefConfig;
import org.seasar.jca.deploy.config.OutboundAdapterConfig;
import org.seasar.jca.deploy.config.ResourceAdapterConfig;
import org.xml.sax.Attributes;

/**
 * @author koichik
 */
public class ResourceAdapterTagHandlerRule extends TagHandlerRule {
    private static final long serialVersionUID = 1L;

    public ResourceAdapterTagHandlerRule() {
        final ResourceAdapterConfig raConfig = new ResourceAdapterConfig();
        addTagHandler("/connector", new TagHandler() {
            private static final long serialVersionUID = 1L;

            @Override
            public void start(final TagHandlerContext context, final Attributes attributes) {
                context.push(raConfig);
                raConfig.setSpecVersion(attributes.getValue("version"));
            }
        });
        addTagHandler("display-name", new TagHandler() {
            private static final long serialVersionUID = 1L;

            @Override
            public void end(final TagHandlerContext context, final String body) {
                raConfig.setDisplayName(body);
            }
        });
        addTagHandler("vendor-name", new TagHandler() {
            private static final long serialVersionUID = 1L;

            @Override
            public void end(final TagHandlerContext context, final String body) {
                raConfig.setVendorName(body);
            }
        });
        addTagHandler("eis-type", new TagHandler() {
            private static final long serialVersionUID = 1L;

            @Override
            public void end(final TagHandlerContext context, final String body) {
                raConfig.setEisType(body);
            }
        });
        addTagHandler("resourceadapter-version", new TagHandler() {
            private static final long serialVersionUID = 1L;

            @Override
            public void end(final TagHandlerContext context, final String body) {
                raConfig.setRaVersion(body);
            }
        });
        addTagHandler("config-property", new TagHandler() {
            private static final long serialVersionUID = 1L;

            @Override
            public void start(final TagHandlerContext context, final Attributes attributes) {
                final ConfigProperty configProperty = new ConfigProperty();
                context.push(configProperty);
            }

            @Override
            public void end(final TagHandlerContext context, final String body) {
                final ConfigProperty configProperty = (ConfigProperty) context.pop();
                final ConfigPropertyContainer parameterizable = (ConfigPropertyContainer) context
                        .peek();
                parameterizable.putProperty(configProperty);
            }
        });
        addTagHandler("config-property-name", new TagHandler() {
            private static final long serialVersionUID = 1L;

            @Override
            public void end(final TagHandlerContext context, final String body) {
                final Object top = context.peek();
                if (top instanceof ConfigProperty) {
                    final ConfigProperty configProperty = (ConfigProperty) top;
                    configProperty.setName(body);
                }
            }
        });
        addTagHandler("config-property-type", new TagHandler() {
            private static final long serialVersionUID = 1L;

            @Override
            public void end(final TagHandlerContext context, final String body) {
                final Object top = context.peek();
                if (top instanceof ConfigProperty) {
                    final ConfigProperty configProperty = (ConfigProperty) top;
                    configProperty.setType(body);
                }
            }
        });
        addTagHandler("config-property-value", new TagHandler() {
            private static final long serialVersionUID = 1L;

            @Override
            public void end(final TagHandlerContext context, final String body) {
                final Object top = context.peek();
                if (top instanceof ConfigProperty) {
                    final ConfigProperty configProperty = (ConfigProperty) top;
                    configProperty.setValue(body);
                }
            }
        });
        addTagHandler("resourceadapter-class", new TagHandler() {
            private static final long serialVersionUID = 1L;

            @Override
            public void end(final TagHandlerContext context, final String body) {
                raConfig.setRaClass(body);
            }
        });
        addTagHandler("outbound-resourceadapter", new TagHandler() {
            private static final long serialVersionUID = 1L;

            @Override
            public void start(final TagHandlerContext context, final Attributes attributes) {
                final OutboundAdapterConfig outboundConfig = new OutboundAdapterConfig();
                context.push(outboundConfig);
            }

            @Override
            public void end(final TagHandlerContext context, final String body) {
                final OutboundAdapterConfig outboundConfig = (OutboundAdapterConfig) context.pop();
                raConfig.addOutboundAdapter(outboundConfig);
            }
        });
        addTagHandler("connection-definition", new TagHandler() {
            private static final long serialVersionUID = 1L;

            @Override
            public void start(final TagHandlerContext context, final Attributes attributes) {
                final OutboundAdapterConfig outboundConfig = (OutboundAdapterConfig) context.peek();
                final ConnectionDefConfig cdConfig = new ConnectionDefConfig(outboundConfig);
                context.push(cdConfig);
            }

            @Override
            public void end(final TagHandlerContext context, final String body) {
                final ConnectionDefConfig cdConfig = (ConnectionDefConfig) context.pop();
                final OutboundAdapterConfig outboundConfig = (OutboundAdapterConfig) context.peek();
                outboundConfig.addConnectionDef(cdConfig);
            }
        });
        addTagHandler("managedconnectionfactory-class", new TagHandler() {
            private static final long serialVersionUID = 1L;

            @Override
            public void end(final TagHandlerContext context, final String body) {
                final ConnectionDefConfig cdConfig = (ConnectionDefConfig) context.peek();
                cdConfig.setMcfClass(body);
            }
        });
        addTagHandler("connectionfactory-interface", new TagHandler() {
            private static final long serialVersionUID = 1L;

            @Override
            public void end(final TagHandlerContext context, final String body) {
                final ConnectionDefConfig cdConfig = (ConnectionDefConfig) context.peek();
                cdConfig.setCfInterface(body);
            }
        });
        addTagHandler("connectionfactory-impl-class", new TagHandler() {
            private static final long serialVersionUID = 1L;

            @Override
            public void end(final TagHandlerContext context, final String body) {
                final ConnectionDefConfig cdConfig = (ConnectionDefConfig) context.peek();
                cdConfig.setCfImplClass(body);
            }
        });
        addTagHandler("connection-interface", new TagHandler() {
            private static final long serialVersionUID = 1L;

            @Override
            public void end(final TagHandlerContext context, final String body) {
                final ConnectionDefConfig cdConfig = (ConnectionDefConfig) context.peek();
                cdConfig.setConnectionInterface(body);
            }
        });
        addTagHandler("connection-impl-class", new TagHandler() {
            private static final long serialVersionUID = 1L;

            @Override
            public void end(final TagHandlerContext context, final String body) {
                final ConnectionDefConfig cdConfig = (ConnectionDefConfig) context.peek();
                cdConfig.setConnectionImplClass(body);
            }
        });
        addTagHandler("transaction-support", new TagHandler() {
            private static final long serialVersionUID = 1L;

            @Override
            public void end(final TagHandlerContext context, final String body) {
                final OutboundAdapterConfig outboundConfig = (OutboundAdapterConfig) context.peek();
                outboundConfig.setTransactionSupport(body);
            }
        });
        addTagHandler("adminobject", new TagHandler() {
            private static final long serialVersionUID = 1L;

            @Override
            public void start(final TagHandlerContext context, final Attributes attributes) {
                final AdminObjectConfig adminConfig = new AdminObjectConfig();
                context.push(adminConfig);
            }

            @Override
            public void end(final TagHandlerContext context, final String body) {
                context.pop();
            }
        });
    }
}
