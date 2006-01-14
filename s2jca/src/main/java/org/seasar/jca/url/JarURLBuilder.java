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
package org.seasar.jca.url;

import java.io.File;
import java.net.URL;
import java.net.URLEncoder;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.seasar.framework.util.StringUtil;

/**
 * @author koichik
 */
public class JarURLBuilder {
    protected static final String JAVA_PROTOCOL_HANDLER_PKGS = "java.protocol.handler.pkgs";
    protected static final String EAR = "ear";
    protected static final String WAR = "war";
    protected static final String EJB_JAR = "ejbjar";
    protected static final String RAR = "rar";
    protected static final String JAR = "jar";

    protected URL url;

    static {
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                final String pkgs = System.getProperty(JAVA_PROTOCOL_HANDLER_PKGS);
                final StringBuilder buf = new StringBuilder(pkgs != null ? pkgs.length() + 50 : 50);
                if (!StringUtil.isEmpty(pkgs)) {
                    buf.append(pkgs);
                    if (!pkgs.endsWith("|")) {
                        buf.append("|");
                    }
                }
                final String className = JarURLBuilder.class.getName();
                buf.append(className.substring(0, className.lastIndexOf('.')));
                System.setProperty(JAVA_PROTOCOL_HANDLER_PKGS, new String(buf));
                return null;
            }
        });
    }

    private JarURLBuilder(final URL url) {
        this.url = url;
    }

    public static JarURLBuilder ear(final URL url) throws Exception {
        return create(EAR, url);
    }

    public static JarURLBuilder ear(final File file) throws Exception {
        return create(EAR, file.toURL());
    }

    public static JarURLBuilder war(final URL url) throws Exception {
        return create(WAR, url);
    }

    public static JarURLBuilder war(final File file) throws Exception {
        return create(WAR, file.toURL());
    }

    public static JarURLBuilder ejbJar(final URL url) throws Exception {
        return create(EJB_JAR, url);
    }

    public static JarURLBuilder ejbJar(final File file) throws Exception {
        return create(EJB_JAR, file.toURL());
    }

    public static JarURLBuilder rar(final URL url) throws Exception {
        return create(RAR, url);
    }

    public static JarURLBuilder rar(final File file) throws Exception {
        return create(RAR, file.toURL());
    }

    public static JarURLBuilder jar(final URL url) throws Exception {
        return create(JAR, url);
    }

    public static JarURLBuilder jar(final File file) throws Exception {
        return create(JAR, file.toURL());
    }

    public JarURLBuilder ear(final String entry) throws Exception {
        wrappedIn(EAR, entry);
        return this;
    }

    public JarURLBuilder war(final String entry) throws Exception {
        wrappedIn(WAR, entry);
        return this;
    }

    public JarURLBuilder ejbJar(final String entry) throws Exception {
        wrappedIn(WAR, entry);
        return this;
    }

    public JarURLBuilder rar(final String entry) throws Exception {
        wrappedIn(RAR, entry);
        return this;
    }

    public JarURLBuilder jar(final String entry) throws Exception {
        wrappedIn(JAR, entry);
        return this;
    }

    public JarURLBuilder entry(final String entry) throws Exception {
        url = new URL(url.toString() + entry);
        return this;
    }

    protected static JarURLBuilder create(final String protocol, final URL url) throws Exception {
        return new JarURLBuilder(new URL(null, protocol + ":" + url + "!/",
                new NestedJarURLStreamHandler()));
    }

    protected void wrappedIn(final String protocol, final String entry) throws Exception {
        url = new URL(protocol + ":" + url.getProtocol() + ":"
                + URLEncoder.encode(url.getPath() + entry, "UTF-8") + "!/");
    }

    public URL toURL() throws Exception {
        return url;
    }
}
