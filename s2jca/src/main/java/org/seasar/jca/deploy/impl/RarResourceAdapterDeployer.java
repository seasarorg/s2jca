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
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.resource.ResourceException;
import javax.resource.spi.BootstrapContext;

import org.seasar.jca.exception.SResourceException;
import org.seasar.jca.url.JarURLBuilder;

/**
 * @author koichik
 */
public class RarResourceAdapterDeployer extends AbstractResourceAdapterDeployer {
    public RarResourceAdapterDeployer() {
    }

    @Override
    protected ClassLoader createClassLoader() throws ResourceException {
        try {
            final List<URL> urls = getURLs();
            return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                public ClassLoader run() {
                    return new URLClassLoader(urls.toArray(new URL[urls.size()]));
                }
            });
        } catch (final Exception e) {
            throw new SResourceException("EJCA0000", e);
        }
    }

    protected List<URL> getURLs() throws IOException, Exception {
        final List<URL> urls = new ArrayList<URL>();
        final JarFile rar = new JarFile(path);
        for (final Enumeration enumeration = rar.entries(); enumeration.hasMoreElements();) {
            final JarEntry entry = (JarEntry) enumeration.nextElement();
            final String entryName = entry.getName();
            if (entryName.endsWith(".jar")) {
                urls.add(JarURLBuilder.rar(new File(path)).jar(entryName).toURL());
            }
        }
        return urls;
    }

    @Override
    protected InputStream getDeploymentDescripterAsInputStream() throws ResourceException {
        try {
            final JarFile rar = new JarFile(path);
            final JarEntry entry = rar.getJarEntry(META_INF_RA_XML);
            if (entry == null) {
                throw new SResourceException("EJCA1014", new Object[] { path });
            }
            return rar.getInputStream(entry);
        } catch (final IOException e) {
            throw new SResourceException("EJCA0000", e);
        }
    }
}
