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

import javax.naming.Binding;
import javax.naming.ConfigurationException;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;

import org.seasar.framework.container.ComponentNotFoundRuntimeException;
import org.seasar.framework.container.S2Container;
import org.seasar.framework.container.factory.SingletonS2ContainerFactory;
import org.seasar.framework.exception.SRuntimeException;

/**
 * @author koichik
 */
public class JndiContext implements Context {
    protected static final String ENC_PREFIX = "java:comp/env/";

    protected final Hashtable<?, ?> environment;
    protected final String path;
    protected final S2Container container;

    public JndiContext(final Hashtable<?, ?> environment) throws NamingException {
        this.environment = environment;
        this.path = String.class.cast(environment.get(PROVIDER_URL));
        try {
            final S2Container root = SingletonS2ContainerFactory.getContainer();
            container = (path == null) ? root : (S2Container) root.getComponent(path);
            if (container == null) {
                throw new ConfigurationException("namespace '" + path + "' is not found.");
            }
        } catch (final SRuntimeException e) {
            throw setCause(new ConfigurationException(e.getMessage()), e);
        }
    }

    public Hashtable<?, ?> getEnvironment() throws NamingException {
        return environment;
    }

    public String getNameInNamespace() throws NamingException {
        return path;
    }

    public Object lookup(final Name name) throws NamingException {
        final StringBuilder buf = new StringBuilder(100);
        for (int i = 0; i < name.size(); ++i) {
            buf.append(name.get(i)).append("/");
        }
        buf.setLength(buf.length() - 2);
        return lookup(new String(buf));
    }

    public Object lookup(final String name) throws NamingException {
        try {
            S2Container context = container;
            final String[] names = resolveENC(name).split("/");
            for (int i = 0; i < names.length - 1; ++i) {
                context = (S2Container) container.getComponent(names[i]);
            }
            return context.getComponent(names[names.length - 1]);
        } catch (final ComponentNotFoundRuntimeException e) {
            throw setCause(new NameNotFoundException(name), e);
        } catch (final SRuntimeException e) {
            throw setCause(new NamingException(e.getMessage()), e);
        }
    }

    public void close() throws NamingException {
    }

    protected String resolveENC(final String name) {
        if (name.startsWith(ENC_PREFIX)) {
            return name.substring(ENC_PREFIX.length());
        }
        return name;
    }

    protected NamingException setCause(final NamingException e, final Throwable cause) {
        e.initCause(cause);
        return e;
    }

    public Object addToEnvironment(final String propName, final Object propVal)
            throws NamingException {
        throw new OperationNotSupportedException("addToEnvironment");
    }

    public void bind(final Name name, final Object obj) throws NamingException {
        throw new OperationNotSupportedException("bind : " + name);
    }

    public void bind(final String name, final Object obj) throws NamingException {
        throw new OperationNotSupportedException("bind : " + name);
    }

    public Name composeName(final Name name, final Name prefix) throws NamingException {
        throw new OperationNotSupportedException("composeName : " + name);
    }

    public String composeName(final String name, final String prefix) throws NamingException {
        throw new OperationNotSupportedException("composeName : " + name);
    }

    public Context createSubcontext(final Name name) throws NamingException {
        throw new OperationNotSupportedException("createSubcontext : " + name);
    }

    public Context createSubcontext(final String name) throws NamingException {
        throw new OperationNotSupportedException("createSubcontext : " + name);
    }

    public void destroySubcontext(final Name name) throws NamingException {
        throw new OperationNotSupportedException("destroySubcontext : " + name);
    }

    public void destroySubcontext(final String name) throws NamingException {
        throw new OperationNotSupportedException("destroySubcontext : " + name);
    }

    public NameParser getNameParser(final Name name) throws NamingException {
        throw new OperationNotSupportedException("getNameParser : " + name);
    }

    public NameParser getNameParser(final String name) throws NamingException {
        throw new OperationNotSupportedException("getNameParser : " + name);
    }

    public NamingEnumeration<NameClassPair> list(final Name name) throws NamingException {
        throw new OperationNotSupportedException("list : " + name);
    }

    public NamingEnumeration<NameClassPair> list(final String name) throws NamingException {
        throw new OperationNotSupportedException("list : " + name);
    }

    public NamingEnumeration<Binding> listBindings(final Name name) throws NamingException {
        throw new OperationNotSupportedException("listBindings : " + name);
    }

    public NamingEnumeration<Binding> listBindings(final String name) throws NamingException {
        throw new OperationNotSupportedException("listBindings : " + name);
    }

    public Object lookupLink(final Name name) throws NamingException {
        throw new OperationNotSupportedException("lookupLink : " + name);
    }

    public Object lookupLink(final String name) throws NamingException {
        throw new OperationNotSupportedException("unbind : " + name);
    }

    public void rebind(final Name name, final Object obj) throws NamingException {
        throw new OperationNotSupportedException("rebind : " + name);
    }

    public void rebind(final String name, final Object obj) throws NamingException {
        throw new OperationNotSupportedException("rebind" + name);
    }

    public Object removeFromEnvironment(final String propName) throws NamingException {
        throw new OperationNotSupportedException("removeFromEnvironment : " + propName);
    }

    public void rename(final Name oldName, final Name newName) throws NamingException {
        throw new OperationNotSupportedException("rename : " + oldName + " to " + newName);
    }

    public void rename(final String oldName, final String newName) throws NamingException {
        throw new OperationNotSupportedException("rename : " + oldName + " to " + newName);
    }

    public void unbind(final Name name) throws NamingException {
        throw new OperationNotSupportedException("unbind : " + name);
    }

    public void unbind(final String name) throws NamingException {
        throw new OperationNotSupportedException("unbind : " + name);
    }
}
