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
package org.seasar.jca.deploy.config;

/**
 * @author koichik
 */
public class ConnectionDefConfig extends ConfigPropertyContainer {
    protected final OutboundAdapterConfig parent;
    protected String mcfClass;
    protected String cfInterface;
    protected String cfImplClass;
    protected String connectionInterface;
    protected String connectionImplClass;

    public ConnectionDefConfig(final OutboundAdapterConfig parent) {
        this.parent = parent;
    }

    public OutboundAdapterConfig getOutboundAdapter() {
        return parent;
    }

    public String getMcfClass() {
        return this.mcfClass;
    }

    public void setMcfClass(final String mcfClass) {
        this.mcfClass = mcfClass;
    }

    public String getCfInterface() {
        return this.cfInterface;
    }

    public void setCfInterface(final String cfInterface) {
        this.cfInterface = cfInterface;
    }

    public String getCfImplClass() {
        return this.cfImplClass;
    }

    public void setCfImplClass(final String cfImplClass) {
        this.cfImplClass = cfImplClass;
    }

    public String getConnectionInterface() {
        return this.connectionInterface;
    }

    public void setConnectionInterface(final String connectionInterface) {
        this.connectionInterface = connectionInterface;
    }

    public String getConnectionImplClass() {
        return this.connectionImplClass;
    }

    public void setConnectionImplClass(final String connectionImplClass) {
        this.connectionImplClass = connectionImplClass;
    }
}
