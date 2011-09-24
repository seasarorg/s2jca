/*
 * Copyright 2004-2011 the Seasar Foundation and the Others.
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
package org.seasar.jca.deploy;

import java.io.IOException;

import javax.resource.ResourceException;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;

import org.seasar.jca.deploy.config.ResourceAdapterConfig;

/**
 * リソースアダプタをデプロイするコンポーネントのインタフェースです．
 * 
 * @author koichik
 * @see javax.resource.spi.ResourceAdapter
 */
public interface ResourceAdapterDeployer {

    /**
     * リソースアダプタをデプロイします．
     * 
     * @throws ResourceException
     *             リソースアダプタのデプロイに失敗した場合にスローされます
     * @throws IOException
     *             リソースアダプタのデプロイ中に入出力エラーが発生した場合にスローされます
     * @see javax.resource.spi.ResourceAdapter#start(BootstrapContext)
     */
    public void start() throws ResourceException, IOException;

    /**
     * リソースアダプタをアンデプロイします．
     * 
     */
    public void stop();

    /**
     * ブートストラップコンテキストを返します．
     * 
     * @return ブートストラップコンテキスト
     */
    public BootstrapContext getBootstrapContext();

    /**
     * リソースアダプタを返します．
     * 
     * @return リソースアダプタ
     */
    public ResourceAdapter getResourceAdapter();

    /**
     * リソースアダプタの構成を返します．
     * 
     * @return リソースアダプタの構成
     */
    public ResourceAdapterConfig getResourceAdapterConfig();

    /**
     * リソースアダプタのデプロイに使用するクラスローダを返します．
     * 
     * @return リソースアダプタのデプロイに使用するクラスローダ
     */
    public ClassLoader getClassLoader();

}
