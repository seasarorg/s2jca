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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.resource.ResourceException;

import org.seasar.framework.container.annotation.tiger.Binding;
import org.seasar.framework.container.annotation.tiger.BindingType;
import org.seasar.framework.exception.IORuntimeException;
import org.seasar.framework.exception.SIOException;
import org.seasar.jca.exception.SResourceException;

/**
 * @author koichik
 */
public class RarResourceAdapterDeployer extends AbstractResourceAdapterDeployer {
    protected JarFile rar;
    protected String tempDirName;
    protected File tempDir;

    public RarResourceAdapterDeployer() {
    }

    @Binding(bindingType = BindingType.MAY)
    public void setTempDir(final String tempDir) {
        this.tempDirName = tempDir;
    }

    @Override
    protected File[] getJarFiles() {
        try {
            createTempDir();
            createRarFile();
            final List<File> jarFiles = new ArrayList<File>();
            for (final Enumeration enumeration = rar.entries(); enumeration.hasMoreElements();) {
                final JarEntry entry = (JarEntry) enumeration.nextElement();
                final String entryName = entry.getName();
                if (entryName.endsWith(".jar")) {
                    jarFiles.add(extractJar(entry));
                }
            }
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

    protected void createRarFile() throws IOException {
        if (rar != null) {
            return;
        }
        rar = new JarFile(path);
    }

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
