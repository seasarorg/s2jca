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

import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;

import org.seasar.framework.unit.EasyMockTestCase;
import org.seasar.jca.outbound.support.ConnectionManagementContext;
import org.seasar.jca.outbound.support.ManagedConnectionPool;

import static org.easymock.EasyMock.*;

import static org.seasar.jca.outbound.support.ConnectionManagementContextMatcher.*;

/**
 * @author koichik
 */
public class ThreadBoundedPoolingPolicyTest extends EasyMockTestCase {

    ThreadBoundedPoolingPolicy target;

    ConnectionManagementPolicy policy;

    ManagedConnectionFactory mcf;

    ManagedConnection[] mc = new ManagedConnection[3];

    ConnectionRequestInfo info;

    Object[] lch = new Object[3];

    ConnectionManagementContext[] context = new ConnectionManagementContext[3];

    Set<ManagedConnection> set1;

    Set<ManagedConnection> set2;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        policy = createStrictMock(ConnectionManagementPolicy.class);
        mcf = createStrictMock(ManagedConnectionFactory.class);
        info = createStrictMock(ConnectionRequestInfo.class);
        for (int i = 0; i < 3; ++i) {
            mc[i] = createStrictMock(ManagedConnection.class);
            lch[i] = new Object();
            context[i] = new ConnectionManagementContext(null, info, mcf);
        }

        set1 = new HashSet<ManagedConnection>();
        set1.add(mc[0]);
        set2 = new HashSet<ManagedConnection>();
        set2.add(mc[0]);
        set2.add(mc[1]);

        target = new ThreadBoundedPoolingPolicy();
    }

    /**
     * <code>MethodInterceptor</code>
     * のbefore以降に取得したコネクションがafterで解放されることの擬似的なテスト． <br>
     * 
     * @throws Exception
     */
    public void testAcquireNew() throws Exception {
        target.initialize(mcf, policy);

        // <code>MethodInterceptor</code>のbeforeで行う処理．
        final Set<ManagedConnection> before = target.before();
        final ManagedConnectionPool<?> pool = target.pools.get();
        assertEquals("0", 0, before.size());
        assertEquals("1", 0, pool.getActivePoolSize());
        assertEquals("2", 0, pool.getFreePoolSize());

        // コネクション取得．
        new Subsequence() {

            @Override
            public void replay() throws Exception {
                target.allocate(context[0]);
                assertEquals("3", 1, pool.getActivePoolSize());
                assertEquals("4", 0, pool.getFreePoolSize());
            }

            @Override
            public void record() throws Exception {
                // 後続のpolicyからコネクションを取得，mc0が返される．
                policy.allocate(eqContext(context[0], mc[0], null));
            }
        }.doTest();

        // 取得したコネクションの解放 (フリープールへ)．
        // <code>MethodInvocation</code>のproceedで行う処理．
        new Subsequence() {

            @Override
            public void replay() throws Exception {
                target.release(mc[0]);
                assertEquals("5", 0, pool.getActivePoolSize());
                assertEquals("6", 1, pool.getFreePoolSize());
            }
        }.doTest();

        // <code>MethodInterceptor</code>のafterで行う処理．
        // 最初に取得したコネクションがリリースされる．
        new Subsequence() {

            @Override
            public void replay() throws Exception {
                target.after(before);
                assertEquals("7", 0, pool.getActivePoolSize());
                assertEquals("8", 0, pool.getFreePoolSize());
            }

            @Override
            public void record() throws Exception {
                // フリープールのコネクションが後続がpolicyへ渡される．
                policy.release(mc[0]);
            }
        }.doTest();
    }

    /**
     * コネクションの取得が二回行われた場合で，二番目に取得しようとしたコネクションが最初のコネクションと同じ場合のテスト． <br>
     * アスペクトが適用されたメソッドがネストした場合に，実際にコネクションを取得しなかった場合にはコネクションが解放されないことを確認する．
     * 
     * @throws Exception
     */
    public void testAcquireSame() throws Exception {
        target.initialize(mcf, policy);

        // 外側のメソッドにおける<code>MethodInterceptor</code>のbeforeで行われる処理．
        final Set<ManagedConnection> before1 = target.before();
        final ManagedConnectionPool<?> pool = target.pools.get();
        assertEquals("0", 0, before1.size());
        assertEquals("1", 0, pool.getActivePoolSize());
        assertEquals("2", 0, pool.getFreePoolSize());

        // 最初のコネクション取得．
        new Subsequence() {

            @Override
            public void replay() throws Exception {
                target.allocate(context[0]);
                assertEquals("3", 1, pool.getActivePoolSize());
                assertEquals("4", 0, pool.getFreePoolSize());
            }

            @Override
            public void record() throws Exception {
                // 後続のpolicyからコネクションを取得，mc0が返される．
                policy.allocate(eqContext(context[0], mc[0], null));
            }
        }.doTest();

        // 最初に取得したコネクションがクローズ (フリープールへ)．
        new Subsequence() {

            @Override
            public void replay() throws Exception {
                target.release(mc[0]);
                assertEquals("5", 0, pool.getActivePoolSize());
                assertEquals("6", 1, pool.getFreePoolSize());
            }
            // フリープールに戻されるだけなので後続のpolicyは呼ばれない．
        }.doTest();

        // 内側のメソッドにおける<code>MethodInterceptor</code>のbeforeで行われる処理．
        final Set<ManagedConnection> before2 = target.before();
        assertEquals("7", 1, before2.size());
        assertEquals("8", 0, pool.getActivePoolSize());
        assertEquals("9", 1, pool.getFreePoolSize());

        // 二番目のコネクション取得．
        new Subsequence() {

            @Override
            public void replay() throws Exception {
                target.allocate(context[0]);
                assertEquals("10", 1, pool.getActivePoolSize());
                assertEquals("11", 0, pool.getFreePoolSize());
            }

            @Override
            public void record() throws Exception {
                // フリープールのコネクションとマッチング，mc0が返される．
                expect(mcf.matchManagedConnections(set1, null, info)).andReturn(mc[0]);
            }
        }.doTest();

        // 2番目に取得したコネクションがクローズ (フリープールへ)．
        new Subsequence() {

            @Override
            public void replay() throws Exception {
                target.release(mc[0]);
                assertEquals("12", 0, pool.getActivePoolSize());
                assertEquals("13", 1, pool.getFreePoolSize());
            }
            // フリープールに戻されるだけなので後続のpolicyは呼ばれない．
        }.doTest();

        // 内側のメソッドにおける<code>MethodInterceptor</code>のafterで行われる処理．
        // 取得したコネクション(最初に取得したコネクションと同じ)はクローズされない．
        new Subsequence() {

            @Override
            public void replay() throws Exception {
                target.after(before2);
                assertEquals("14", 0, pool.getActivePoolSize());
                assertEquals("15", 1, pool.getFreePoolSize());
            }
            // フリープールに戻されるだけなので後続のpolicyは呼ばれない．
        }.doTest();

        // 外側のメソッドにおける<code>MethodInterceptor</code>のafterで行われる処理．
        // 最初に取得したコネクションをクローズ．
        new Subsequence() {

            @Override
            public void replay() throws Exception {
                target.after(before1);
                assertEquals("16", 0, pool.getActivePoolSize());
                assertEquals("17", 0, pool.getFreePoolSize());
            }

            @Override
            public void record() throws Exception {
                // コネクションがリリースされる．
                policy.release(mc[0]);
            }
        }.doTest();
    }

    /**
     * コネクションの取得が二回行われた場合で，二番目に取得しようとしたコネクションが最初のコネクションとは異なる場合のテスト． <br>
     * アスペクトが適用されたメソッドがネストした場合に，それぞれが実際に取得したコネクションを解放することを確認．
     * 
     * @throws Exception
     */
    public void testAcquireDiff() throws Exception {
        target.initialize(mcf, policy);

        // 外側のメソッドにおける<code>MethodInterceptor</code>のbeforeで行われる処理．
        final Set<ManagedConnection> before1 = target.before();
        final ManagedConnectionPool<?> pool = target.pools.get();
        assertEquals("0", 0, before1.size());
        assertEquals("1", 0, pool.getActivePoolSize());
        assertEquals("2", 0, pool.getFreePoolSize());

        // 最初のコネクション取得．
        new Subsequence() {

            @Override
            public void replay() throws Exception {
                target.allocate(context[0]);
                assertEquals("3", 1, pool.getActivePoolSize());
                assertEquals("4", 0, pool.getFreePoolSize());
            }

            @Override
            public void record() throws Exception {
                // 後続のpolicyからコネクションを取得，mc0が返される．
                policy.allocate(eqContext(context[0], mc[0], null));
            }
        }.doTest();

        // 最初に取得したコネクションがクローズ (フリープールへ)．
        new Subsequence() {

            @Override
            public void replay() throws Exception {
                target.release(mc[0]);
                assertEquals("5", 0, pool.getActivePoolSize());
                assertEquals("6", 1, pool.getFreePoolSize());
            }
            // フリープールに戻されるだけなので後続のpolicyは呼ばれない．
        }.doTest();

        // 内側のメソッドにおける<code>MethodInterceptor</code>のbeforeで行われる処理．
        final Set<ManagedConnection> before2 = target.before();
        assertEquals("7", 1, before2.size());
        assertEquals("8", 0, pool.getActivePoolSize());
        assertEquals("9", 1, pool.getFreePoolSize());

        // 二番目のコネクション取得．
        new Subsequence() {

            @Override
            public void replay() throws Exception {
                target.allocate(context[1]);
                assertEquals("10", 1, pool.getActivePoolSize());
                assertEquals("11", 1, pool.getActivePoolSize());
            }

            @Override
            public void record() throws Exception {
                // フリープールのコネクションとマッチング，どれもマッチしない．
                expect(mcf.matchManagedConnections(set1, null, info)).andReturn(null);
                // 後続のpolicyから新しいコネクションを取得．
                policy.allocate(eqContext(context[1], mc[1], null));
            }
        }.doTest();

        // 取得したコネクションがクローズ (フリープールへ)．
        new Subsequence() {

            @Override
            public void replay() throws Exception {
                target.release(mc[1]);
                assertEquals("12", 0, pool.getActivePoolSize());
                assertEquals("13", 2, pool.getFreePoolSize());
            }
            // フリープールに戻されるだけなので後続のpolicyは呼ばれない．
        }.doTest();

        // 内側のメソッドにおける<code>MethodInterceptor</code>のafterで行われる処理．
        // 2番目に取得したコネクション(をクローズする．
        new Subsequence() {

            @Override
            public void replay() throws Exception {
                target.after(before2);
                assertEquals("16", 0, pool.getActivePoolSize());
                assertEquals("17", 1, pool.getFreePoolSize());
            }

            @Override
            public void record() throws Exception {
                // 後続のpolicyにコネクションがリリースされる．
                policy.release(mc[1]);
            }
        }.doTest();

        // 外側のメソッドにおける<code>MethodInterceptor</code>のafterで行われる処理．
        // 最初に取得したコネクションをクローズ．
        new Subsequence() {

            @Override
            public void replay() throws Exception {
                target.after(before1);
                assertEquals("18", 0, pool.getActivePoolSize());
                assertEquals("19", 0, pool.getFreePoolSize());
            }

            @Override
            public void record() throws Exception {
                // 後続のpolicyにコネクションがリリースされる．
                policy.release(mc[0]);
            }
        }.doTest();
    }

}
