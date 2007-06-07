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
import java.util.Map;

import org.seasar.framework.util.tiger.CollectionsUtil;

/**
 * 構成情報としてプロパティ (名前と値のペア) を持つことのできるオブジェクトを表現するクラスです．
 * 
 * @author koichik
 */
public abstract class ConfigPropertyContainer {
    // instance fields
    /** プロパティのマップ */
    protected final Map<String, ConfigProperty> props = CollectionsUtil.newLinkedHashMap();

    /**
     * インスタンスを構築します．
     */
    public ConfigPropertyContainer() {
    }

    /**
     * プロパティを追加します．
     * 
     * @param prop
     *            プロパティ
     * @throws NullPointerException
     *             プロパティが<code>null</code>の場合にスローされます．
     */
    public void putProperty(final ConfigProperty prop) {
        props.put(prop.getName(), prop);
    }

    /**
     * 複数のプロパティを追加します．
     * 
     * @param properties
     *            プロパティのコレクション
     * @throws NullPointerException
     *             コレクションが<code>null</code>の場合にスローされます．
     */
    public void putProperties(final Collection<ConfigProperty> properties) {
        for (final ConfigProperty property : properties) {
            putProperty(property);
        }
    }

    /**
     * 名前で指定されたプロパティを返します．
     * <p>
     * 名前で指定されたプロパティを持っていない場合は<code>null</code>を返します．
     * </p>
     * 
     * @param name
     *            プロパティ名
     * @return プロパティ
     */
    public ConfigProperty getProperty(final String name) {
        return props.get(name);
    }

    /**
     * 保持しているプロパティの名前のコレクションを返します．
     * <p>
     * プロパティを保持していない場合は空のコレクションが返されます．
     * </p>
     * 
     * @return 保持しているプロパティの名前のコレクション
     */
    public Collection<String> getPropertyKeys() {
        return props.keySet();
    }

    /**
     * 保持しているプロパティの値のコレクションを返します．
     * <p>
     * プロパティを保持していない場合は空のコレクションが返されます．
     * </p>
     * 
     * @return 保持しているプロパティの値のコレクション
     */
    public Collection<ConfigProperty> getPropertyValues() {
        return props.values();
    }
}
