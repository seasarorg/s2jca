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
package org.seasar.jca.cm.policy;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.resource.ResourceException;
import javax.resource.spi.ManagedConnection;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.seasar.jca.cm.support.ConnectionManagementContext;
import org.seasar.jca.cm.support.ManagedConnectionPool;

/**
 * @author koichik
 */
public class ThreadBoundedPoolingPolicy extends AbstractPolicy implements MethodInterceptor {
    private static final long serialVersionUID = 1L;

    protected final ThreadLocal<ManagedConnectionPool<Object>> pools = new ThreadLocal<ManagedConnectionPool<Object>>() {
        @Override
        public ManagedConnectionPool<Object> initialValue() {
            return new ManagedConnectionPool<Object>(nextPolicy);
        }
    };

    public ThreadBoundedPoolingPolicy() {
        super(true);
    }

    @Override
    public void allocate(final ConnectionManagementContext context) throws ResourceException {
        final ManagedConnectionPool<Object> pool = pools.get();
        final ManagedConnection mc = pool.getMatched(context.getSubject(),
                context.getRequestInfo(), mcf);
        if (mc != null) {
            pool.moveFreeToActivePool(mc);
            context.setManagedConnection(mc);
        } else {
            nextPolicy.allocate(context);
            pool.addToActivePool(context.getManagedConnection());
        }
    }

    @Override
    public void release(final ManagedConnection mc) throws ResourceException {
        final ManagedConnectionPool pool = pools.get();
        if (pool.moveActiveToFreePool(mc)) {
            return;
        }
        nextPolicy.release(mc);
    }

    public Object invoke(final MethodInvocation invocation) throws Throwable {
        final Set<ManagedConnection> before = before();
        try {
            return invocation.proceed();
        } finally {
            after(before);
        }
    }

    public Set<ManagedConnection> before() {
        final ManagedConnectionPool<Object> context = pools.get();
        final Set<ManagedConnection> before = new HashSet<ManagedConnection>(context
                .getActivePool());
        before.addAll(context.getFreePool());
        return before;
    }

    public void after(final Set<ManagedConnection> before) throws ResourceException {
        final ManagedConnectionPool<Object> context = pools.get();
        final Set<ManagedConnection> after = new HashSet<ManagedConnection>(context.getActivePool());
        after.addAll(context.getFreePool());
        after.removeAll(before);
        if (!after.isEmpty()) {
            for (final Iterator it = after.iterator(); it.hasNext();) {
                final ManagedConnection mc = (ManagedConnection) it.next();
                context.removeFromActivePool(mc);
                context.removeFromFreePool(mc);
                nextPolicy.release(mc);
            }
        }
    }
}
