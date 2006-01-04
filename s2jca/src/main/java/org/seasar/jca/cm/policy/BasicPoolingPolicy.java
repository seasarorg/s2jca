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

import java.util.Timer;
import java.util.TimerTask;

import javax.resource.ResourceException;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.UnavailableException;

import org.seasar.framework.log.Logger;
import org.seasar.jca.cm.support.ConnectionManagementContext;
import org.seasar.jca.cm.support.ManagedConnectionPool;
import org.seasar.jca.exception.SResourceException;

/**
 * @author koichik
 */
public class BasicPoolingPolicy extends AbstractPolicy {
    private static final long serialVersionUID = 1L;

    private static final Logger logger = Logger.getLogger(BasicPoolingPolicy.class);
    protected static Timer timer;

    protected int minPoolSize = 5;
    protected int maxPoolSize = 10;
    protected int timeout = 600;
    protected final BootstrapContext bc;
    protected final Object lock = new Object();
    protected ManagedConnectionPool<ExpireTask> pool;

    public BasicPoolingPolicy(final BootstrapContext bc) throws ResourceException {
        super(true);
        this.bc = bc;
        createTimer(bc);
    }

    @Override
    public void initialize(final ManagedConnectionFactory mcf,
            final ConnectionManagementPolicy nextPolicy) throws ResourceException {
        super.initialize(mcf, nextPolicy);
        pool = new ManagedConnectionPool<ExpireTask>(nextPolicy, true);
    }

    protected static synchronized void createTimer(final BootstrapContext bc)
            throws UnavailableException {
        if (timer == null) {
            timer = bc.createTimer();
        }
    }

    @Override
    public void allocate(final ConnectionManagementContext context) throws ResourceException {
        assertValidMCF(context);
        checkOut(context);
    }

    @Override
    public void release(final ManagedConnection mc) throws ResourceException {
        mc.cleanup();
        if (!checkIn(mc)) {
            nextPolicy.release(mc);
        }
    }

    @Override
    public void connectionErrorOccurred(final ManagedConnection mc) throws ResourceException {
        purge(mc);
        nextPolicy.connectionErrorOccurred(mc);
    }

    @Override
    public void dispose() {
        pool.close();
        nextPolicy.dispose();
    }

    protected boolean isFull() {
        return (pool.size()) >= maxPoolSize;
    }

    protected boolean isFreePoolFull() {
        return (pool.getFreePoolSize()) >= maxPoolSize;
    }

    protected void checkOut(final ConnectionManagementContext context) throws ResourceException {
        synchronized (lock) {
            waitForFreePool();
            ManagedConnection mc = allocateFromFreePool(context);
            if (mc != null) {
                context.setManagedConnection(mc);
                return;
            }

            if (isFreePoolFull()) {
                releaseFirstFromFree();
            }
            nextPolicy.allocate(context);
            pool.addToActivePool(context.getManagedConnection());
        }
    }

    protected void waitForFreePool() throws SResourceException {
        while (pool.getFreePoolSize() == 0 && pool.getActivePoolSize() >= maxPoolSize) {
            try {
                lock.wait();
            } catch (final InterruptedException e) {
                throw new SResourceException("EJCA0000", e);
            }
        }
    }

    protected ManagedConnection allocateFromFreePool(final ConnectionManagementContext context)
            throws ResourceException {
        final ManagedConnection mc = pool.getMatched(context.getSubject(),
                context.getRequestInfo(), mcf);
        if (mc != null) {
            ExpireTask task = pool.moveFreeToActivePool(mc);
            task.cancel();
            if (logger.isDebugEnabled()) {
                logger.log("DJCA1006", new Object[] { mc });
            }
        }
        return mc;
    }

    protected void releaseFirstFromFree() throws ResourceException {
        final ManagedConnection mc = pool.getFirstFromFree();
        nextPolicy.release(mc);
        final ExpireTask task = pool.removeFromFreePool(mc);
        if (task != null) {
            task.cancel();
        }
        if (logger.isDebugEnabled()) {
            logger.log("DJCA1008", new Object[] { mc });
        }
    }

    protected boolean checkIn(final ManagedConnection mc) {
        synchronized (lock) {
            if (isFreePoolFull() || !pool.removeFromActivePool(mc)) {
                return false;
            }
            pool.addToFreePool(mc, new ExpireTask(mc));
            lock.notifyAll();
        }

        if (logger.isDebugEnabled()) {
            logger.log("DJCA1007", new Object[] { mc });
        }
        return true;
    }

    protected void purge(final ManagedConnection mc) {
        boolean removed = false;
        TimerTask task = null;
        synchronized (lock) {
            task = pool.removeFromFreePool(mc);
            removed = task != null || pool.removeFromActivePool(mc);
            if (removed) {
                lock.notifyAll();
            }
        }
        if (task != null) {
            task.cancel();
        }
        if (removed && logger.isDebugEnabled()) {
            logger.log("DJCA1008", new Object[] { mc });
        }
    }

    public int getMinPoolSize() {
        return minPoolSize;
    }

    public void setMinPoolSize(final int minPoolSize) {
        this.minPoolSize = minPoolSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(final int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(final int timeout) {
        this.timeout = timeout;
    }

    public class ExpireTask extends TimerTask {
        protected ManagedConnection managedConnection;

        public ExpireTask(final ManagedConnection mc) {
            this.managedConnection = mc;
            timer.schedule(this, timeout * 1000);
        }

        @Override
        public void run() {
            synchronized (lock) {
                if (pool.getFreePoolSize() <= minPoolSize
                        || pool.removeFromFreePool(managedConnection) == null) {
                    return;
                }
            }
            if (logger.isDebugEnabled()) {
                logger.log("DJCA1008", new Object[] { managedConnection });
            }
            silentRelease(managedConnection);
        }
    }
}
