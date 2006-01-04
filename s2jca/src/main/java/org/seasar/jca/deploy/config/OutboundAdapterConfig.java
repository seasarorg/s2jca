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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author koichik
 */
public class OutboundAdapterConfig {
    protected Map<String, List<ConnectionDefConfig>> connectionDefs = new LinkedHashMap<String, List<ConnectionDefConfig>>();
    protected String transactionSupport;

    public void addConnectionDef(final ConnectionDefConfig connectionDef) {
        final String mcf = connectionDef.getMcfClass();
        List<ConnectionDefConfig> list = connectionDefs.get(mcf);
        if (list == null) {
            list = new ArrayList<ConnectionDefConfig>();
            connectionDefs.put(mcf, list);
        }
        list.add(connectionDef);
    }

    public int getConnectionDefSize(final String mcf) {
        final List<ConnectionDefConfig> list = connectionDefs.get(mcf);
        return list == null ? 0 : list.size();
    }

    public ConnectionDefConfig getConnectionDef(final String mcf) {
        return getConnectionDef(mcf, 0);
    }

    public ConnectionDefConfig getConnectionDef(final String mcf, final int index) {
        final List<ConnectionDefConfig> list = connectionDefs.get(mcf);
        if (list == null || index >= list.size()) {
            return null;
        }
        return list.get(index);
    }

    public List<ConnectionDefConfig> getConnectionDefs(final String mcf) {
        final List<ConnectionDefConfig> list = connectionDefs.get(mcf);
        if (list != null) {
            return list;
        }
        return new ArrayList<ConnectionDefConfig>();
    }

    public Set<String> getMcfClassNames() {
        return connectionDefs.keySet();
    }

    public String getTransactionSupport() {
        return this.transactionSupport;
    }

    public void setTransactionSupport(final String transactionSupport) {
        this.transactionSupport = transactionSupport;
    }
}
