/*
 * Copyright 2004-2007 the Seasar Foundation and the Others.
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

import org.seasar.framework.util.tiger.CollectionsUtil;

/**
 * リソースアダプタの構成情報を保持するクラスです．
 * <p>
 * リソースアダプタ構成情報は，複数のoutboundアダプタ構成情報と一つのinboundアダプタ構成情報を持つことができます．
 * </p>
 * 
 * @author koichik
 */
public class ResourceAdapterConfig extends ConfigPropertyContainer {

    // instance fields
    /** リソースアダプタの準拠する仕様のバージョン */
    protected String specificationVersion;

    /** リソースアダプタの表示名 */
    protected String displayName;

    /** リソースアダプタのベンダ名 */
    protected String vendorName;

    /** リソースアダプタのEISタイプ */
    protected String eisType;

    /** リソースアダプタのバージョン */
    protected String raVersion;

    /** リソースアダプタのクラス */
    protected String raClass;

    /** outboundアダプタのリスト */
    protected final List<OutboundAdapterConfig> outboundAdapters = CollectionsUtil.newArrayList();

    /** inboundアダプタの構成情報 */
    protected InboundAdapterConfig inboundAdapter;

    /**
     * インスタンスを構築します．
     * 
     */
    public ResourceAdapterConfig() {
    }

    /**
     * リソースアダプタが準拠するJCA仕様のバージョンを返します．
     * 
     * @return リソースアダプタが準拠するJCA仕様のバージョン
     */
    public String getSpecVersion() {
        return this.specificationVersion;
    }

    /**
     * リソースアダプタが準拠するJCA仕様のバージョンを設定します．
     * 
     * @param specificationVersion
     *            リソースアダプタが準拠するJCA仕様のバージョン
     */
    public void setSpecVersion(final String specificationVersion) {
        this.specificationVersion = specificationVersion;
    }

    /**
     * リソースアダプタの表示名を返します．
     * 
     * @return リソースアダプタの表示名
     */
    public String getDisplayName() {
        return this.displayName;
    }

    /**
     * リソースアダプタの表示名を設定します．
     * 
     * @param displayName
     *            リソースアダプタの表示名
     */
    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    /**
     * リソースアダプタのベンダ名を返します．
     * 
     * @return リソースアダプタのベンダ名
     */
    public String getVendorName() {
        return this.vendorName;
    }

    /**
     * リソースアダプタのベンダ名を設定します．
     * 
     * @param vendorName
     *            リソースアダプタのベンダ名
     */
    public void setVendorName(final String vendorName) {
        this.vendorName = vendorName;
    }

    /**
     * リソースアダプタのEIS名を返します．
     * 
     * @return リソースアダプタのEIS名
     */
    public String getEisType() {
        return this.eisType;
    }

    /**
     * リソースアダプタのEIS名を設定します．
     * 
     * @param eisType
     *            リソースアダプタのEIS名
     */
    public void setEisType(final String eisType) {
        this.eisType = eisType;
    }

    /**
     * リソースアダプタのバージョンを返します．
     * 
     * @return リソースアダプタのバージョン
     */
    public String getRaVersion() {
        return this.raVersion;
    }

    /**
     * リソースアダプタのバージョンを設定します．
     * 
     * @param raVersion
     *            リソースアダプタのバージョン
     */
    public void setRaVersion(final String raVersion) {
        this.raVersion = raVersion;
    }

    /**
     * リソースアダプタの実装クラス名を返します．
     * 
     * @return リソースアダプタの実装クラス名
     */
    public String getRaClass() {
        return this.raClass;
    }

    /**
     * リソースアダプタの実装クラス名を設定します．
     * 
     * @param raClass
     *            リソースアダプタの実装クラス名
     */
    public void setRaClass(final String raClass) {
        this.raClass = raClass;
    }

    /**
     * 保持しているoutboundアダプタ構成情報の数を返します．
     * 
     * @return 保持しているoutboundアダプタの数
     */
    public int getOutboundAdapterSize() {
        return outboundAdapters.size();
    }

    /**
     * 指定されたインデックスのoutboundアダプタ構成情報を返します
     * 
     * @param index
     *            インデックス
     * @return 指定されたインデックスのoutboundアダプタ構成情報
     */
    public OutboundAdapterConfig getOutboundAdapter(final int index) {
        return outboundAdapters.get(index);
    }

    /**
     * outboundアダプタ構成情報を追加します．
     * 
     * @param outboundAdapter
     *            outboundアダプタ構成情報
     */
    public void addOutboundAdapter(final OutboundAdapterConfig outboundAdapter) {
        this.outboundAdapters.add(outboundAdapter);
    }

    /**
     * 指定された{@link javax.resource.spi.ManagedConnectionFactory}が持つインデックスで指定された位置のコネクション構成情報を返します．
     * 
     * @param mcf
     *            {@link javax.resource.spi.ManagedConnectionFactory}の実装クラス名
     * @param index
     *            インデックス
     * @return 指定された{@link javax.resource.spi.ManagedConnectionFactory}が持つインデックスで指定された位置のコネクション構成情報
     */
    public ConnectionDefConfig getConnectionDef(final String mcf, final int index) {
        final ConnectionDefConfig[] config = getConnectionDef(mcf);
        if (index >= config.length) {
            return null;
        }
        return config[index];
    }

    /**
     * 指定された{@link javax.resource.spi.ManagedConnectionFactory}が持つコネクション構成情報の配列を返します．
     * 
     * @param mcf
     *            {@link javax.resource.spi.ManagedConnectionFactory}
     * @return 指定された{@link javax.resource.spi.ManagedConnectionFactory}が持つコネクション構成情報の配列
     */
    public ConnectionDefConfig[] getConnectionDef(final String mcf) {
        final List<ConnectionDefConfig> result = new ArrayList<ConnectionDefConfig>();
        for (final OutboundAdapterConfig outboundAdapter : outboundAdapters) {
            for (final ConnectionDefConfig connectionDef : outboundAdapter.getConnectionDefs(mcf)) {
                result.add(connectionDef);
            }
        }
        return result.toArray(new ConnectionDefConfig[result.size()]);
    }

    /**
     * inboudアダプタ構成情報を返します．
     * 
     * @return inboundアダプタ構成情報
     */
    public InboundAdapterConfig getInboundAdapter() {
        return inboundAdapter;
    }

    /**
     * inboundアダプタ構成情報を設定します．
     * 
     * @param inboundAdapter
     *            inboundアダプタ構成情報
     */
    public void setInboundAdapter(final InboundAdapterConfig inboundAdapter) {
        this.inboundAdapter = inboundAdapter;
    }

}
