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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.resource.ResourceException;
import javax.resource.spi.BootstrapContext;

import org.seasar.jca.exception.SResourceException;

/**
 * @author koichik
 */
public class FsResourceAdapterDeployer extends AbstractResourceAdapterDeployer {
    public FsResourceAdapterDeployer(final BootstrapContext bc) {
        super(bc);
    }

    @Override
    protected ClassLoader createClassLoader() throws ResourceException {
        try {
            final URL[] urls = toURL(getJarFiles());
            return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                public ClassLoader run() {
                    return new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());
                }
            });
        } catch (final MalformedURLException e) {
            throw new SResourceException("EJCA0000", e);
        }
    }

    protected File[] getJarFiles() {
        final File baseDir = new File(path);
        final File[] jars = baseDir.listFiles(new FilenameFilter() {
            public boolean accept(final File dir, final String name) {
                return name.endsWith(".jar");
            }
        });
        return jars;
    }

    protected URL[] toURL(final File[] jars) throws MalformedURLException {
        final URL[] urls = new URL[jars.length];
        for (int i = 0; i < jars.length; ++i) {
            urls[i] = jars[i].toURL();
        }
        return urls;
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
