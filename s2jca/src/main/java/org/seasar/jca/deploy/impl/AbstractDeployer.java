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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.seasar.framework.beans.BeanDesc;
import org.seasar.framework.beans.PropertyDesc;
import org.seasar.framework.exception.ClassNotFoundRuntimeException;
import org.seasar.framework.util.ClassUtil;
import org.seasar.framework.util.ConstructorUtil;
import org.seasar.jca.deploy.config.ConfigProperty;

/**
 * @author koichik
 */
public abstract class AbstractDeployer<TARGET> {
    protected static final String LINE_SEPARATOR = System.getProperty("line.separator");
    protected static final Class[] PARAMETER_TYPE = new Class[] { String.class };

    protected final List<ConfigProperty> configProperties = new ArrayList<ConfigProperty>();
    protected ClassLoader cl;

    public void setProperty(final String name, final String value) {
        setProperty(new ConfigProperty(name, value));
    }

    public void setProperty(final String name, final String type, final String value) {
        setProperty(new ConfigProperty(name, type, value));
    }

    public void setProperty(final ConfigProperty configProperty) {
        configProperties.add(configProperty);
    }

    public ClassLoader getClassLoader() {
        return cl;
    }

    public void setClassLoader(final ClassLoader cl) {
        this.cl = cl;
    }

    protected Class forName(final String className) {
        try {
            return Class.forName(className, true, cl);
        } catch (final ClassNotFoundException ex) {
            throw new ClassNotFoundRuntimeException(ex);
        }
    }

    protected void applyProperties(final BeanDesc beanDesc, final TARGET target,
            final Collection<ConfigProperty> properties) {
        for (final ConfigProperty property : properties) {
            applyProperty(beanDesc, target, property);
        }
    }

    protected void applyProperty(final BeanDesc beanDesc, final TARGET target,
            final ConfigProperty property) {
        final PropertyDesc desc = beanDesc.getPropertyDesc(property.getName());
        final String type = property.getType();
        final String value = property.getValue();

        if (type.equals(String.class.getName())) {
            desc.setValue(target, value);
        } else {
            final Class propertyClass = forName(property.getType());
            final Constructor ctor = ClassUtil.getConstructor(propertyClass, PARAMETER_TYPE);
            desc.setValue(target, ConstructorUtil.newInstance(ctor, new Object[] { value }));
        }
    }

    protected void loggingConfigProperties(final Collection<ConfigProperty> properties,
            final String indent, final StringBuffer buf) {
        final Map<String, ConfigProperty> map = new LinkedHashMap<String, ConfigProperty>();
        for (final ConfigProperty property : properties) {
            map.put(property.getName(), property);
        }

        for (final ConfigProperty property : configProperties) {
            map.put(property.getName(), property);
        }

        for (final ConfigProperty property : map.values()) {
            buf.append(indent);
            buf.append("config-property : name='").append(property.getName());
            buf.append("'\t").append("type='").append(property.getType());
            buf.append("'\t").append("value='").append(property.getValue()).append("'");
            buf.append(LINE_SEPARATOR);
        }
    }
}
