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
package org.seasar.jca.deploy.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import javax.resource.ResourceException;

import org.seasar.jca.exception.SResourceException;

/**
 * ファイルシステムからリソースアダプタをデプロイするクラスです．
 * 
 * @author koichik
 */
public class FsResourceAdapterDeployer extends AbstractResourceAdapterDeployer {

    /**
     * インスタンスを構築します．
     * <p>
     * このコンストラクタで生成したインスタンスは，
     * {@link AbstractResourceAdapterDeployer#setBootstrapContext(javax.resource.spi.BootstrapContext)}で
     * ブートストラップコンテキストを設定しなくてはなりません．
     * </p>
     */
    public FsResourceAdapterDeployer() {
    }

    /**
     * デフォルトのブートストラップコンテキストでインスタンスを構築します．
     * 
     * @param numThreads
     *            スレッドプールのスレッド数
     */
    public FsResourceAdapterDeployer(final int numThreads) {
        super(numThreads);
    }

    @Override
    protected File[] getJarFiles() {
        final File baseDir = new File(path);
        return baseDir.listFiles(new JarFileFilter());
    }

    @Override
    protected InputStream getDeploymentDescripterAsInputStream() throws ResourceException {
        final File baseDir = new File(path);
        final File dd = new File(baseDir, META_INF_RA_XML);
        try {
            return new FileInputStream(dd);
        } catch (final FileNotFoundException e) {
            throw new SResourceException("EJCA0000", e);
        }
    }

}
