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
package org.seasar.jca.cm.support;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.security.auth.Subject;

import org.seasar.framework.log.Logger;
import org.seasar.jca.cm.policy.ConnectionManagementPolicy;

/**
 * @author koichik
 */
public class ManagedConnectionPool<T> {
    private static final Logger logger = Logger.getLogger(ManagedConnectionPool.class);

    protected final Set<ManagedConnection> activePool;
    protected final Map<ManagedConnection, T> freePool;
    protected final Set<ManagedConnection> freePoolView;
    protected final ConnectionManagementPolicy nextPolicy;

    public ManagedConnectionPool(final ConnectionManagementPolicy nextPolicy) {
        this(nextPolicy, false);
    }

    public ManagedConnectionPool(final ConnectionManagementPolicy nextPolicy, boolean accessOrder) {
        this.activePool = new HashSet<ManagedConnection>();
        this.freePool = new LinkedHashMap<ManagedConnection, T>(64, 0.75f, accessOrder);
        this.freePoolView = Collections.unmodifiableSet(freePool.keySet());
        this.nextPolicy = nextPolicy;
    }

    public Set<ManagedConnection> getActivePool() {
        return activePool;
    }

    public Set<ManagedConnection> getFreePool() {
        return freePoolView;
    }

    public int size() {
        return getFreePoolSize() + getActivePoolSize();
    }

    public int getActivePoolSize() {
        return activePool.size();
    }

    public int getFreePoolSize() {
        return freePool.size();
    }

    public boolean containsActive(final ManagedConnection mc) {
        return activePool.contains(mc);
    }

    public boolean containsFree(final ManagedConnection mc) {
        return freePool.containsKey(mc);
    }

    public ManagedConnection getMatched(final Subject subject, final ConnectionRequestInfo info,
            final ManagedConnectionFactory mcf) throws ResourceException {
        if (freePool.isEmpty()) {
            return null;
        }
        return mcf.matchManagedConnections(freePoolView, subject, info);
    }

    public ManagedConnection getFirstFromFree() {
        if (freePool.isEmpty()) {
            return null;
        }
        return freePoolView.iterator().next();
    }

    public void addToActivePool(final ManagedConnection mc) {
        activePool.add(mc);
    }

    public void addToFreePool(final ManagedConnection mc) {
        addToFreePool(mc, null);
    }

    public void addToFreePool(final ManagedConnection mc, final T opaque) {
        freePool.put(mc, opaque);
        freePool.get(mc);
    }

    public boolean moveActiveToFreePool(final ManagedConnection mc) {
        return moveActiveToFreePool(mc, null);
    }

    public boolean moveActiveToFreePool(final ManagedConnection mc, final T opaque) {
        if (activePool.remove(mc)) {
            freePool.put(mc, opaque);
            return true;
        }
        return false;
    }

    public T moveFreeToActivePool(final ManagedConnection mc) {
        if (!freePool.containsKey(mc)) {
            return null;
        }
        final T opaque = removeFromFreePool(mc);
        activePool.add(mc);
        return opaque;
    }

    public boolean removeFromActivePool(final ManagedConnection mc) {
        return activePool.remove(mc);
    }

    public T removeFromFreePool(final ManagedConnection mc) {
        if (!freePool.containsKey(mc)) {
            return null;
        }
        return freePool.remove(mc);
    }

    public boolean remove(final ManagedConnection mc) {
        return removeFromActivePool(mc) || removeFromFreePool(mc) != null;
    }

    public void close() {
        for (final Iterator<ManagedConnection> it = activePool.iterator(); it.hasNext();) {
            final ManagedConnection mc = it.next();
            it.remove();
            try {
                nextPolicy.release(mc);
            } catch (final ResourceException e) {
                logger.log("EJCA0000", null, e);
            }
        }

        for (final Iterator<ManagedConnection> it = freePool.keySet().iterator(); it.hasNext();) {
            final ManagedConnection mc = it.next();
            it.remove();
            try {
                nextPolicy.release(mc);
            } catch (final ResourceException e) {
                logger.log("EJCA0000", null, e);
            }
        }
    }
}
