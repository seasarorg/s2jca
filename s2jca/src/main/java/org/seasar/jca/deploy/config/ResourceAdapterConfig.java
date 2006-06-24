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

import java.util.ArrayList;
import java.util.List;

/**
 * @author koichik
 */
public class ResourceAdapterConfig extends ConfigPropertyContainer {
    protected String specificationVersion;
    protected String displayName;
    protected String vendorName;
    protected String eisType;
    protected String raVersion;
    protected String raClass;
    protected final List<OutboundAdapterConfig> outboundAdapters = new ArrayList<OutboundAdapterConfig>();
    protected InboundAdapterConfig inboundAdapter;

    public String getSpecVersion() {
        return this.specificationVersion;
    }

    public void setSpecVersion(final String specificationVersion) {
        this.specificationVersion = specificationVersion;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    public String getVendorName() {
        return this.vendorName;
    }

    public void setVendorName(final String vendorName) {
        this.vendorName = vendorName;
    }

    public String getEisType() {
        return this.eisType;
    }

    public void setEisType(final String eisType) {
        this.eisType = eisType;
    }

    public String getRaVersion() {
        return this.raVersion;
    }

    public void setRaVersion(final String raVersion) {
        this.raVersion = raVersion;
    }

    public String getRaClass() {
        return this.raClass;
    }

    public void setRaClass(final String raClass) {
        this.raClass = raClass;
    }

    public int getOutboundAdapterSize() {
        return outboundAdapters.size();
    }

    public OutboundAdapterConfig getOutboundAdapter(int index) {
        return outboundAdapters.get(index);
    }

    public void addOutboundAdapter(final OutboundAdapterConfig outboundAdapter) {
        this.outboundAdapters.add(outboundAdapter);
    }

    public ConnectionDefConfig getConnectionDef(final String mcf, int index) {
        ConnectionDefConfig[] config = getConnectionDef(mcf);
        if (index >= config.length) {
            return null;
        }
        return config[index];
    }

    public ConnectionDefConfig[] getConnectionDef(final String mcf) {
        final List<ConnectionDefConfig> result = new ArrayList<ConnectionDefConfig>();
        for (final OutboundAdapterConfig outboundAdapter : outboundAdapters) {
            for (final ConnectionDefConfig connectionDef : outboundAdapter.getConnectionDefs(mcf)) {
                result.add(connectionDef);
            }
        }
        return result.toArray(new ConnectionDefConfig[result.size()]);
    }

    public InboundAdapterConfig getInboundAdapter() {
        return inboundAdapter;
    }

    public void setInboundAdapter(final InboundAdapterConfig inboundAdapter) {
        this.inboundAdapter = inboundAdapter;
    }
}
