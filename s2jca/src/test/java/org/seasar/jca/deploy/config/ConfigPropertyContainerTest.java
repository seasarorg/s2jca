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
package org.seasar.jca.deploy.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

/**
 * @author koichik
 */
public class ConfigPropertyContainerTest extends TestCase {

    public ConfigPropertyContainerTest(String name) {
        super(name);
    }

    public void test() throws Exception {
        ConfigPropertyContainer config = new ConfigPropertyContainer() {
        };
        config.putProperty(new ConfigProperty("foo", "FOO"));
        assertEquals("1", 1, config.props.size());
        assertEquals("2", "FOO", config.getProperty("foo").getValue());

        config.putProperty(new ConfigProperty("foo", "FOOFOO"));
        assertEquals("3", 1, config.props.size());
        assertEquals("4", "FOOFOO", config.getProperty("foo").getValue());

        config.putProperty(new ConfigProperty("bar", "BAR"));
        assertEquals("5", 2, config.props.size());
        assertEquals("6", "BAR", config.getProperty("bar").getValue());

        Collection<String> keys = config.getPropertyKeys();
        assertEquals("7", 2, keys.size());
        assertTrue("8", keys.contains("foo"));
        assertTrue("9", keys.contains("bar"));

        Collection<ConfigProperty> values = config.getPropertyValues();
        Set<String> expectedValueNames = new HashSet<String>();
        expectedValueNames.add("FOOFOO");
        expectedValueNames.add("BAR");
        assertEquals("10", 2, values.size());
        for (ConfigProperty prop : values) {
            assertTrue("10", expectedValueNames.contains(prop.getValue()));
        }

        List<ConfigProperty> l = new ArrayList<ConfigProperty>();
        l.add(new ConfigProperty("hoge1", "HOGE1"));
        l.add(new ConfigProperty("hoge2", "HOGE2"));
        l.add(new ConfigProperty("hoge3", "HOGE3"));
        config.putProperties(l);
        assertEquals("13", 5, config.props.size());
        assertEquals("14", "HOGE1", config.getProperty("hoge1").getValue());
        assertEquals("15", "HOGE2", config.getProperty("hoge2").getValue());
        assertEquals("16", "HOGE3", config.getProperty("hoge3").getValue());
    }
}
