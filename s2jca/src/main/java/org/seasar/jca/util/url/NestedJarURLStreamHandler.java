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
package org.seasar.jca.util.url;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.HashSet;
import java.util.Set;

/**
 * @author koichik
 */
public class NestedJarURLStreamHandler extends sun.net.www.protocol.jar.Handler {
    protected static final Set<String> NESTED_JAR_PROTOCOLS = new HashSet<String>();
    static {
        NESTED_JAR_PROTOCOLS.add(JarURLBuilder.EAR);
        NESTED_JAR_PROTOCOLS.add(JarURLBuilder.WAR);
        NESTED_JAR_PROTOCOLS.add(JarURLBuilder.RAR);
        NESTED_JAR_PROTOCOLS.add(JarURLBuilder.EJB_JAR);
    }

    public NestedJarURLStreamHandler() {
    }

    @Override
    protected URLConnection openConnection(final URL url) throws IOException {
        if (NESTED_JAR_PROTOCOLS.contains(url.getProtocol())) {
            final URL inner = new URL(null, "jar:" + URLDecoder.decode(url.getPath(), "UTF-8"),
                    this);
            return inner.openConnection();
        }
        return super.openConnection(url);
    }

    @Override
    protected void parseURL(final URL url, final String spec, final int start, final int limit) {
        final String protocol = spec.substring(0, start - 1);
        setURL(url, protocol, "", -1, "", null, spec.substring(start), null, null);
    }
}
