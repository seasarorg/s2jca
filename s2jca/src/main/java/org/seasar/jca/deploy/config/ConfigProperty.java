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
package org.seasar.jca.deploy.config;

/**
 * 構成情報のプロパティ (名前と値のペア) を表現します．
 * 
 * @author koichik
 */
public class ConfigProperty {

    // instance fields
    /** プロパティの名前 */
    protected String name;

    /** プロパティの型 */
    protected String type;

    /** プロパティの値 */
    protected String value;

    /**
     * インスタンスを構築します．
     * 
     */
    public ConfigProperty() {
    }

    /**
     * インスタンスを構築します．
     * 
     * @param name
     *            名前
     * @param value
     *            値
     */
    public ConfigProperty(final String name, final String value) {
        this(name, String.class.getName(), value);
    }

    /**
     * インスタンスを構築します．
     * 
     * @param name
     *            名前
     * @param type
     *            値の型
     * @param value
     *            値
     */
    public ConfigProperty(final String name, final String type, final String value) {
        this.name = name;
        this.type = type;
        this.value = value;
    }

    /**
     * プロパティの名前を返します．
     * 
     * @return プロパティの名前
     */
    public String getName() {
        return this.name;
    }

    /**
     * プロパティの名前を設定します．
     * 
     * @param name
     *            プロパティの名前
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * プロパティの値の型を返します．
     * 
     * @return プロパティの値の型
     */
    public String getType() {
        return this.type;
    }

    /**
     * プロパティの値の型を設定します．
     * 
     * @param type
     *            プロパティの値の型
     */
    public void setType(final String type) {
        this.type = type;
    }

    /**
     * プロパティの値を返します．
     * 
     * @return プロパティの値
     */
    public String getValue() {
        return this.value;
    }

    /**
     * プロパティの値を設定します．
     * 
     * @param value
     *            プロパティの値
     */
    public void setValue(final String value) {
        this.value = value;
    }

}
