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
package org.seasar.jndi;

import java.util.Hashtable;

import javax.naming.ConfigurationException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;

import org.seasar.extension.unit.S2TestCase;

/**
 * @author koichik
 */
public class JndiContextTest extends S2TestCase {
    protected Context context;

    public JndiContextTest() {
    }

    public JndiContextTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        include("foo.dicon");
        include("bar.dicon");
    }

    public void test() throws Exception {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, JndiContextFactory.class.getName());
        context = new InitialContext(env);
    }

    public void testValidURL() throws Exception {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, JndiContextFactory.class.getName());
        env.put(Context.PROVIDER_URL, "fooA");
        context = new InitialContext(env);
    }

    public void testInvalidURL() throws Exception {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, JndiContextFactory.class.getName());
        env.put(Context.PROVIDER_URL, "hoge");
        try {
            new InitialContext(env);
            fail("0");
        } catch (ConfigurationException expected) {
        }
    }

    public void testLookup() throws Exception {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, JndiContextFactory.class.getName());
        context = new InitialContext(env);

        assertEquals("1", "foo A 1", context.lookup("fooA-1"));
        assertEquals("2", "bar B 2", context.lookup("barB-2"));
        try {
            context.lookup("hoge");
            fail("3");
        } catch (NameNotFoundException expected) {
        }
    }

    public void testLookupPath() throws Exception {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, JndiContextFactory.class.getName());
        context = new InitialContext(env);

        assertEquals("1", "foo A 1", context.lookup("foo/fooA/fooA-1"));
        assertEquals("2", "bar B 2", context.lookup("bar/barB/barB-2"));
        try {
            context.lookup("foo/barB/fooA-2");
            fail("3");
        } catch (NameNotFoundException expected) {
        }
    }

    public void testLookupENC() throws Exception {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, JndiContextFactory.class.getName());
        env.put(Context.PROVIDER_URL, "foo");
        context = new InitialContext(env);

        assertEquals("1", "foo A 1", context.lookup("java:comp/env/fooA/fooA-1"));
        assertEquals("2", "foo B 2", context.lookup("java:comp/env/fooB/fooB-2"));
        try {
            context.lookup("java:comp/env/barB/barA-2");
            fail("2");
        } catch (NameNotFoundException expected) {
        }
    }
}
