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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

import junit.framework.TestCase;

/**
 * @author koichik
 */
public class JarURLBuilderTest extends TestCase {

    public JarURLBuilderTest() {
        super();
    }

    public JarURLBuilderTest(String name) {
        super(name);
    }

    public void testRAR() throws Exception {
        URL rarFile = getClass().getClassLoader().getResource("foo.rar");
        URL url = JarURLBuilder.rar(rarFile).jar("foo.jar").entry("foo.txt").toURL();
        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
        try {
            assertEquals("", "Foo", reader.readLine());
        } finally {
            reader.close();
        }
    }

    public void testEAR() throws Exception {
        URL earFile = getClass().getClassLoader().getResource("foo.ear");
        URL url = JarURLBuilder.ear(earFile).war("bar.war").jar("WEB-INF/lib/baz.jar").entry(
                "hoge.txt").toURL();
        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
        try {
            assertEquals("", "HOGE", reader.readLine());
        } finally {
            reader.close();
        }
    }
}
