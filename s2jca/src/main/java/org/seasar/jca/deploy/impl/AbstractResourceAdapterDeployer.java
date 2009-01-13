/*
 * Copyright 2004-2009 the Seasar Foundation and the Others.
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
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import javax.resource.ResourceException;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.seasar.framework.beans.factory.BeanDescFactory;
import org.seasar.framework.container.annotation.tiger.Binding;
import org.seasar.framework.container.annotation.tiger.BindingType;
import org.seasar.framework.container.annotation.tiger.DestroyMethod;
import org.seasar.framework.container.annotation.tiger.InitMethod;
import org.seasar.framework.log.Logger;
import org.seasar.framework.util.SAXParserFactoryUtil;
import org.seasar.framework.util.tiger.ReflectionUtil;
import org.seasar.framework.xml.SaxHandler;
import org.seasar.framework.xml.SaxHandlerParser;
import org.seasar.jca.deploy.ResourceAdapterDeployer;
import org.seasar.jca.deploy.config.ConnectionDefConfig;
import org.seasar.jca.deploy.config.OutboundAdapterConfig;
import org.seasar.jca.deploy.config.ResourceAdapterConfig;
import org.seasar.jca.exception.SResourceException;
import org.seasar.jca.lifecycle.BootstrapContextImpl;

/**
 * リソースアダプタをデプロイする抽象クラスです．
 * 
 * @author koichik
 */
public abstract class AbstractResourceAdapterDeployer extends AbstractDeployer<ResourceAdapter>
        implements ResourceAdapterDeployer {

    // constants
    /** <code>ra.xml</code>ファイルのパス名 */
    protected static final String META_INF_RA_XML = "META-INF/ra.xml";

    // static fields
    private static final Logger logger = Logger.getLogger(AbstractResourceAdapterDeployer.class);

    // instance fields
    /** ブートストラップ・コンテキスト */
    protected BootstrapContext bc;

    /** RARファイルのパス */
    protected String path;

    /** リソースアダプタ */
    protected ResourceAdapter ra;

    /** リソースアダプタのコンフィグレーション */
    protected ResourceAdapterConfig raConfig;

    /**
     * インスタンスを構築します．
     * <p>
     * このコンストラクタで生成したインスタンスは，
     * {@link AbstractResourceAdapterDeployer#setBootstrapContext(javax.resource.spi.BootstrapContext)}で
     * ブートストラップコンテキストを設定しなくてはなりません．
     * </p>
     */
    protected AbstractResourceAdapterDeployer() {
    }

    /**
     * デフォルトのブートストラップコンテキストでインスタンスを構築します．
     * 
     * @param numThreads
     *            スレッドプールのスレッド数
     */
    protected AbstractResourceAdapterDeployer(final int numThreads) {
        bc = new BootstrapContextImpl(numThreads);
    }

    /**
     * ブートストラップ・コンテキストを設定します．
     * 
     * @param bc
     *            ブートストラップ・コンテキスト
     */
    @Binding(bindingType = BindingType.MAY)
    public void setBootstrapContext(final BootstrapContext bc) {
        this.bc = bc;
    }

    @InitMethod
    public void start() throws ResourceException, IOException {
        setClassLoader(createClassLoader());
        loadDeploymentDescripter();
        raConfig.putProperties(configProperties);
        ra = createResourceAdapter();
        ra.start(bc);

        if (logger.isDebugEnabled()) {
            loggingDeployedMessage();
        }
    }

    @DestroyMethod
    public void stop() {
        ra.stop();
        if (logger.isDebugEnabled()) {
            logger.log("DJCA1017", null);
        }
    }

    /**
     * リソースアダプタを作成してプロパティを設定します．
     * 
     * @return リソースアダプタ
     */
    protected ResourceAdapter createResourceAdapter() {
        final Class<? extends ResourceAdapter> raClass = ReflectionUtil.forName(
                raConfig.getRaClass(), getClassLoader()).asSubclass(ResourceAdapter.class);
        final ResourceAdapter ra = ReflectionUtil.newInstance(raClass);
        applyProperties(BeanDescFactory.getBeanDesc(raClass), ra, raConfig.getPropertyValues());
        return ra;
    }

    /**
     * デプロイメント・ディスクリプタを読み込みます．
     * 
     * @throws ResourceException
     *             デプロイメント・ディスクリプタの読み込み中にエラーが発生した場合
     * @throws IOException
     *             デプロイメント・ディスクリプタの読み込み中にエラーが発生した場合
     */
    protected void loadDeploymentDescripter() throws ResourceException, IOException {
        final SaxHandlerParser parser = createSaxHandlerParser();
        final InputStream is = getDeploymentDescripterAsInputStream();
        try {
            raConfig = ResourceAdapterConfig.class.cast(parser.parse(is));
        } finally {
            is.close();
        }
    }

    /**
     * SAXハンドラ・パーザを作成します．
     * 
     * @return SAXハンドラ・パーザ
     */
    protected SaxHandlerParser createSaxHandlerParser() {
        final SAXParserFactory factory = SAXParserFactoryUtil.newInstance();
        final SAXParser saxParser = SAXParserFactoryUtil.newSAXParser(factory);
        final SaxHandler handler = new SaxHandler(new ResourceAdapterTagHandlerRule());
        return new SaxHandlerParser(handler, saxParser);
    }

    /**
     * 設定されたJarファイルをクラスパスとして持つクラスローダを作成します．
     * 
     * @return クラスローダ
     * @throws ResourceException
     *             JarファイルのパスをURLで表現できなかった場合
     */
    protected ClassLoader createClassLoader() throws ResourceException {
        try {
            final URL[] urls = toURL(getJarFiles());
            return new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());
        } catch (final MalformedURLException e) {
            throw new SResourceException("EJCA0000", e);
        }
    }

    /**
     * ファイルの配列をURLの配列に変換して返します．
     * 
     * @param jars
     *            ファイルの配列
     * @return URLの配列
     * @throws MalformedURLException
     *             ファイルをURLで表現できなかった場合
     */
    protected URL[] toURL(final File[] jars) throws MalformedURLException {
        final URL[] urls = new URL[jars.length];
        for (int i = 0; i < jars.length; ++i) {
            urls[i] = jars[i].toURL();
        }
        return urls;
    }

    /**
     * リソースアダプタが持つJarファイルの配列を返します．
     * 
     * @return Jarファイルの配列
     */
    protected abstract File[] getJarFiles();

    /**
     * デプロイメント・ディスクリプタを読み込むためのバイトストリームを返します．
     * 
     * @return デプロイメント・ディスクリプタを読み込むためのバイトストリーム
     * @throws ResourceException
     *             デプロイメント・ディスクリプタを読み込むためのバイトストリームを作成できなかった場合
     */
    protected abstract InputStream getDeploymentDescripterAsInputStream() throws ResourceException;

    /**
     * リソースアダプタをデプロイした情報をログに出力します．
     */
    protected void loggingDeployedMessage() {
        final StringBuilder buf = new StringBuilder(1000);

        buf.append("\t").append("display-name : ").append(raConfig.getDisplayName()).append(
                LINE_SEPARATOR);
        buf.append("\t").append("vendor-name : ").append(raConfig.getVendorName()).append(
                LINE_SEPARATOR);
        buf.append("\t").append("eis-type : ").append(raConfig.getEisType()).append(LINE_SEPARATOR);
        buf.append("\t").append("resourceadapter-version : ").append(raConfig.getRaVersion())
                .append(LINE_SEPARATOR);
        buf.append("\t").append("resourceadapter-class : ").append(raConfig.getRaClass()).append(
                LINE_SEPARATOR);

        loggingConfigProperties(raConfig.getPropertyValues(), "\t", buf);

        loggingOutboundResourceAdapter(buf);
        logger.log("DJCA1013", new Object[] { new String(buf) });
    }

    /**
     * outboundなリソースアダプタのデプロイ情報をログ出力する文字列を作成します．
     * 
     * @param buf
     *            ログ情報を作成するバッファ
     */
    protected void loggingOutboundResourceAdapter(final StringBuilder buf) {
        for (int i = 0; i < raConfig.getOutboundAdapterSize(); ++i) {
            loggingConnectionDefinition(buf, raConfig.getOutboundAdapter(i));
            buf.append("\t").append(
                    "transaction-support : "
                            + raConfig.getOutboundAdapter(i).getTransactionSupport()).append(
                    LINE_SEPARATOR);
        }
    }

    /**
     * outboundなアダプタ構成のデプロイ情報をログ出力する文字列を作成します．
     * 
     * @param buf
     *            ログ情報を作成するバッファ
     * @param outboundConfig
     *            outboundなアダプタ構成
     */
    protected void loggingConnectionDefinition(final StringBuilder buf,
            final OutboundAdapterConfig outboundConfig) {
        for (final String mcf : outboundConfig.getMcfClassNames()) {
            for (final ConnectionDefConfig cdConfig : outboundConfig.getConnectionDefs(mcf)) {
                buf.append("\t").append("managedconnectionfactory-class : ").append(
                        cdConfig.getMcfClass()).append(LINE_SEPARATOR);
                loggingConfigProperties(cdConfig.getPropertyValues(), "\t\t", buf);
            }
        }
    }

    public BootstrapContext getBootstrapContext() {
        return bc;
    }

    /**
     * リソースアダプタのパスを返します．
     * 
     * @return リソースアダプタのパス
     */
    public String getPath() {
        return path;
    }

    /**
     * リソースアダプタのパスを設定します．
     * 
     * @param path
     *            リソースアダプタのパス
     */
    public void setPath(final String path) {
        this.path = path;
    }

    public ResourceAdapter getResourceAdapter() {
        return ra;
    }

    public ResourceAdapterConfig getResourceAdapterConfig() {
        return raConfig;
    }

    /**
     * 拡張子が{@code .jar}のファイルを選択する{@link FilenameFilter}です．
     */
    public static class JarFileFilter implements FilenameFilter {

        public boolean accept(final File dir, final String name) {
            return name.endsWith(".jar");
        }

    }

}
