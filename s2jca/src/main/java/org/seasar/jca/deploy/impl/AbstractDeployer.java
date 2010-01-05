/*
 * Copyright 2004-2010 the Seasar Foundation and the Others.
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
import org.seasar.framework.util.tiger.ReflectionUtil;
import org.seasar.jca.deploy.config.ConfigProperty;

/**
 * リソースアダプタ等をデプロイする抽象基底クラスです．
 * 
 * @param <TARGET>
 *            デプロイ対象
 * @author koichik
 */
public abstract class AbstractDeployer<TARGET> {

    // constants
    /** 行区切り文字 */
    protected static final String LINE_SEPARATOR = System.getProperty("line.separator");

    /** <code>String</code>を受け取るコンストラクタの引数型の配列 */
    protected static final Class<?>[] PARAMETER_TYPE = new Class<?>[] { String.class };

    // instance fields
    /** プロパティの{@link List} */
    protected final List<ConfigProperty> configProperties = new ArrayList<ConfigProperty>();

    /** クラスローダ */
    protected ClassLoader cl;

    /**
     * インスタンスを構築します．
     */
    public AbstractDeployer() {
    }

    /**
     * プロパティを設定します．
     * 
     * @param name
     *            プロパティの名前
     * @param value
     *            プロパティの値
     */
    public void setProperty(final String name, final String value) {
        setProperty(new ConfigProperty(name, value));
    }

    /**
     * プロパティを設定します．
     * 
     * @param name
     *            プロパティの名前
     * @param type
     *            プロパティの型
     * @param value
     *            プロパティの値
     */
    public void setProperty(final String name, final String type, final String value) {
        setProperty(new ConfigProperty(name, type, value));
    }

    /**
     * プロパティを設定します．
     * 
     * @param configProperty
     *            プロパティ
     */
    public void setProperty(final ConfigProperty configProperty) {
        configProperties.add(configProperty);
    }

    /**
     * クラスローダを返します．
     * 
     * @return クラスローダ
     */
    public ClassLoader getClassLoader() {
        return cl;
    }

    /**
     * クラスローダを設定します．
     * 
     * @param cl
     *            クラスローダ
     */
    public void setClassLoader(final ClassLoader cl) {
        this.cl = cl;
    }

    /**
     * デプロイ対象にプロパティのコレクションを設定します．
     * 
     * @param beanDesc
     *            デプロイ対象クラスの<code>BeanDesc</code>
     * @param target
     *            デプロイ対象のインスタンス
     * @param properties
     *            設定するプロパティのコレクション
     */
    protected void applyProperties(final BeanDesc beanDesc, final TARGET target,
            final Collection<ConfigProperty> properties) {
        for (final ConfigProperty property : properties) {
            applyProperty(beanDesc, target, property);
        }
    }

    /**
     * デプロイ対象にプロパティを設定します．
     * 
     * @param beanDesc
     *            デプロイ対象クラスの<code>BeanDesc</code>
     * @param target
     *            デプロイ対象のインスタンス
     * @param property
     *            設定するプロパティ
     */
    protected void applyProperty(final BeanDesc beanDesc, final TARGET target,
            final ConfigProperty property) {
        final PropertyDesc desc = beanDesc.getPropertyDesc(property.getName());
        final String type = property.getType();
        final String value = property.getValue();

        if (type.equals(String.class.getName())) {
            desc.setValue(target, value);
        } else {
            final Class<?> propertyClass = ReflectionUtil.forName(property.getType());
            final Constructor<?> ctor = ReflectionUtil
                    .getConstructor(propertyClass, PARAMETER_TYPE);
            desc.setValue(target, ReflectionUtil.newInstance(ctor, new Object[] { value }));
        }
    }

    /**
     * デプロイ対象に設定するプロパティのコレクションをログ出力用にフォーマットします．
     * 
     * @param properties
     *            プロパティのコレクション
     * @param indent
     *            インデントのレベル
     * @param buf
     *            文字列バッファ
     */
    protected void loggingConfigProperties(final Collection<ConfigProperty> properties,
            final String indent, final StringBuilder buf) {
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
