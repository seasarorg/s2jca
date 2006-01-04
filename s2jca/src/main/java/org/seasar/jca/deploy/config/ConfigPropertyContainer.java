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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author koichik
 */
public abstract class ConfigPropertyContainer {
    protected final Map<String, ConfigProperty> props = new LinkedHashMap<String, ConfigProperty>();

    public void putProperty(final ConfigProperty prop) {
        props.put(prop.getName(), prop);
    }

    public void putProperties(final Collection<ConfigProperty> properties) {
        for (final ConfigProperty property : properties) {
            putProperty(property);
        }
    }

    public ConfigProperty getProperty(final String name) {
        return props.get(name);
    }

    public Collection<String> getPropertyKeys() {
        return props.keySet();
    }

    public Collection<ConfigProperty> getPropertyValues() {
        return props.values();
    }
}
