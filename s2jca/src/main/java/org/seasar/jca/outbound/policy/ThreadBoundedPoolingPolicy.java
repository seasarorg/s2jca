/*
 * Copyright 2004-2009 the Seasar Foundation and the Others.
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
package org.seasar.jca.outbound.policy;

import java.util.HashSet;
import java.util.Set;

import javax.resource.ResourceException;
import javax.resource.spi.ManagedConnection;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.seasar.jca.outbound.support.ConnectionManagementContext;
import org.seasar.jca.outbound.support.ManagedConnectionPool;

/**
 * 現在のスレッドにコネクションを割り当てるポリシーの実装クラスです．
 * <p>
 * このポリシーは{@link MethodInterceptor}を実装しています．
 * このインターセプタが適用されたメソッドが実行されている間に割り当てられたマネージドコネクションは，
 * メソッドの実行が終了するまでスレッドに関連づけられます． メソッドの実行が終了すると，関連づけられたマネージドコネクションは解放されます．
 * </p>
 * 
 * @author koichik
 */
public class ThreadBoundedPoolingPolicy extends AbstractPolicy implements MethodInterceptor {

    // constants
    private static final long serialVersionUID = 1L;

    // instance fields
    /** スレッドに関連づけられたマネージドコネクション */
    protected final ThreadLocal<ManagedConnectionPool<Object>> pools = new ThreadLocal<ManagedConnectionPool<Object>>() {

        @Override
        public ManagedConnectionPool<Object> initialValue() {
            return new ManagedConnectionPool<Object>(nextPolicy);
        }

    };

    /**
     * インスタンスを構築します．
     */
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
        final ManagedConnectionPool<Object> pool = pools.get();
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

    /**
     * インターセプタが適用されたメソッドの実行開始前に割り当て済みのコネクションの{@link Set}を返します．
     * 
     * @return インターセプタが適用されたメソッドの実行開始前に割り当て済みのコネクションの{@link Set}
     */
    public Set<ManagedConnection> before() {
        final ManagedConnectionPool<Object> context = pools.get();
        final Set<ManagedConnection> before = new HashSet<ManagedConnection>(context
                .getActivePool());
        before.addAll(context.getFreePool());
        return before;
    }

    /**
     * インターセプタが適用されたメソッドの実行中に割り当てられたコネクションを解放します．
     * 
     * @param before
     *            インターセプタが適用されたメソッドの実行開始前に割り当て済みのコネクションの{@link Set}
     * @throws ResourceException
     *             コネクションの解放中に例外が発生した場合
     */
    public void after(final Set<ManagedConnection> before) throws ResourceException {
        final ManagedConnectionPool<Object> context = pools.get();
        final Set<ManagedConnection> after = new HashSet<ManagedConnection>(context.getActivePool());
        after.addAll(context.getFreePool());
        after.removeAll(before);
        if (!after.isEmpty()) {
            for (final ManagedConnection mc : after) {
                context.removeFromActivePool(mc);
                context.removeFromFreePool(mc);
                nextPolicy.release(mc);
            }
        }
    }

}
