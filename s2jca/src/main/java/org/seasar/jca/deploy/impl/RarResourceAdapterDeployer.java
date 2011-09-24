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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.resource.ResourceException;

import org.seasar.framework.container.annotation.tiger.Binding;
import org.seasar.framework.container.annotation.tiger.BindingType;
import org.seasar.framework.exception.IORuntimeException;
import org.seasar.framework.exception.SIOException;
import org.seasar.jca.exception.SResourceException;

import static org.seasar.framework.util.tiger.IterableAdapter.*;

/**
 * Rarファイルからリソースアダプタをデプロイするクラスです．
 * 
 * @author koichik
 */
public class RarResourceAdapterDeployer extends AbstractResourceAdapterDeployer {

    // instance fields
    /** Rarファイル */
    protected JarFile rar;

    /** Rarファイルを解凍するための一時ディレクトリの名前 */
    protected String tempDirName;

    /** Rarファイルを解凍するための一時ディレクトリ */
    protected File tempDir;

    /**
     * インスタンスを構築します．
     * <p>
     * このコンストラクタで生成したインスタンスは，
     * {@link AbstractResourceAdapterDeployer#setBootstrapContext(javax.resource.spi.BootstrapContext)}で
     * ブートストラップコンテキストを設定しなくてはなりません．
     * </p>
     */
    public RarResourceAdapterDeployer() {
    }

    /**
     * デフォルトのブートストラップコンテキストでインスタンスを構築します．
     * 
     * @param numThreads
     *            スレッドプールのスレッド数
     */
    public RarResourceAdapterDeployer(final int numThreads) {
        super(numThreads);
    }

    /**
     * Rarファイルを解凍するための一時ディレクトリの名前を設定します．
     * 
     * @param tempDir
     *            Rarファイルを解凍するための一時ディレクトリの名前
     */
    @Binding(bindingType = BindingType.MAY)
    public void setTempDir(final String tempDir) {
        tempDirName = tempDir;
    }

    @Override
    protected File[] getJarFiles() {
        try {
            createTempDir();
            createRarFile();
            final List<File> jarFiles = new ArrayList<File>();
            for (final JarEntry entry : iterable(rar.entries())) {
                final String entryName = entry.getName();
                if (entryName.endsWith(".jar")) {
                    jarFiles.add(extractJar(entry));
                }
            }

            final File baseDir = new File(path).getParentFile();
            jarFiles.addAll(Arrays.asList(baseDir.listFiles(new JarFileFilter())));

            return jarFiles.toArray(new File[jarFiles.size()]);
        } catch (final IOException e) {
            throw new IORuntimeException(e);
        }
    }

    @Override
    protected InputStream getDeploymentDescripterAsInputStream() throws ResourceException {
        try {
            createRarFile();
            final JarEntry entry = rar.getJarEntry(META_INF_RA_XML);
            if (entry == null) {
                throw new SResourceException("EJCA1014", new Object[] { path });
            }
            return rar.getInputStream(entry);
        } catch (final IOException e) {
            throw new SResourceException("EJCA0000", e);
        }
    }

    /**
     * Rarファイルをアクセスするための{@link JarFile}を作成します．
     * 
     * @throws IOException
     *             Rarファイルのアクセス中に例外が発生した場合
     */
    protected void createRarFile() throws IOException {
        if (rar != null) {
            return;
        }
        rar = new JarFile(path);
    }

    /**
     * Rarファイル中のJarを一時ディレクトリに解凍します．
     * 
     * @param jarEntry
     *            Rarファイル中のJarファイルを示すエントリ
     * @return 解凍されたJarファイル
     * @throws IOException
     *             Rarファイルの解凍中に例外が発生した場合
     */
    protected File extractJar(final JarEntry jarEntry) throws IOException {
        final File jarFile = File.createTempFile("s2jca-", ".jar", tempDir);
        final InputStream is = rar.getInputStream(jarEntry);
        try {
            final OutputStream os = new FileOutputStream(jarFile);
            try {
                copy(is, os);
            } finally {
                os.close();
            }
        } finally {
            is.close();
        }
        jarFile.deleteOnExit();
        return jarFile;
    }

    /**
     * Rarファイルを解凍するための一時ディレクトリを作成します．
     * 
     * @throws IOException
     *             一時ディレクトリの作成中に例外が発生した場合
     */
    protected void createTempDir() throws IOException {
        if (tempDirName == null) {
            return;
        }

        tempDir = new File(tempDirName);
        if (tempDir.exists()) {
            return;
        }
        if (!tempDir.mkdir()) {
            throw new SIOException("", new Object[] {});
        }
        tempDir.deleteOnExit();
    }

    /**
     * 入力ストリームから読み込んだバイト列を出力ストリームへコピーします．
     * 
     * @param is
     *            入力ストリーム
     * @param os
     *            出力ストリーム
     * @throws IOException
     *             コピー中に例外が発生した場合
     */
    protected void copy(final InputStream is, final OutputStream os) throws IOException {
        final BufferedInputStream bis = new BufferedInputStream(is);
        final BufferedOutputStream bos = new BufferedOutputStream(os);
        final byte[] buf = new byte[8192];
        int len = 0;
        while ((len = bis.read(buf, 0, 8192)) > 0) {
            bos.write(buf, 0, len);
        }
        bos.flush();
    }

}
