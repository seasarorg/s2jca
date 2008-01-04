/*
 * Copyright 2004-2008 the Seasar Foundation and the Others.
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
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.transaction.Status;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

import org.seasar.framework.unit.EasyMockTestCase;
import org.seasar.jca.outbound.support.ConnectionManagementContext;

import static org.easymock.EasyMock.*;

import static org.seasar.jca.outbound.support.ConnectionManagementContextMatcher.*;

/**
 * @author koichik
 */
public class XATransactionBoundedPoolingPolicyTest extends EasyMockTestCase {

    XATransactionBoundedPoolingPolicy target;

    TransactionManager tm;

    Transaction tx;

    XAResource xa;

    ConnectionManagementPolicy policy;

    ManagedConnectionFactory mcf;

    ManagedConnection[] mc = new ManagedConnection[3];

    ConnectionRequestInfo info;

    ConnectionManagementContext[] context = new ConnectionManagementContext[3];

    Object[] lch = new Object[3];

    Set<ManagedConnection> set1;

    Set<ManagedConnection> set2;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        tm = createStrictMock(TransactionManager.class);
        tx = createStrictMock(Transaction.class);
        xa = createStrictMock(XAResource.class);
        mcf = createStrictMock(ManagedConnectionFactory.class);
        info = createStrictMock(ConnectionRequestInfo.class);
        for (int i = 0; i < 3; ++i) {
            mc[i] = createStrictMock(ManagedConnection.class);
            lch[i] = new Object();
            context[i] = new ConnectionManagementContext(null, info, mcf);
        }
        policy = createStrictMock(ConnectionManagementPolicy.class);

        set1 = new HashSet<ManagedConnection>();
        set1.add(mc[0]);
        set2 = new HashSet<ManagedConnection>();
        set2.add(mc[0]);
        set2.add(mc[1]);

        target = new XATransactionBoundedPoolingPolicy(tm);
    }

    /**
     * <code>allowLocalTx</code> がデフォルト( <code>false</code>
     * )でトランザクションが開始されていない場合のテスト．
     * 
     * @throws Exception
     */
    public void testNoTransaction() throws Exception {
        target.initialize(mcf, policy);

        new Subsequence() {

            @Override
            public void replay() throws Exception {
                try {
                    // コネクションを取得しようとすると例外がスローされる．
                    target.allocate(context[0]);
                    fail("0");
                } catch (ResourceException expected) {
                }
            }

            @Override
            public void record() throws Exception {
                // トランザクションが開始されていない．
                expect(tm.getTransaction()).andReturn(null);
            }
        }.doTest();
    }

    /**
     * <code>allowLocalTx</code> がデフォルト( <code>false</code>
     * )でトランザクションが開始されていない場合のテスト．
     * 
     * @throws Exception
     */
    public void testNoTransactionAllowLocalTx() throws Exception {
        target.setAllowLocalTx(true);
        target.initialize(mcf, policy);

        // コネクション取得．
        new Subsequence() {

            @Override
            public void replay() throws Exception {
                target.allocate(context[0]);
            }

            @Override
            public void record() throws Exception {
                // トランザクションが開始されていない．
                expect(tm.getTransaction()).andReturn(null);
                // allowLocalTxが設定されているのでそれでもコネクションが取得される．
                policy.allocate(eqContext(context[0], mc[0], null));
            }
        }.doTest();

        // コネクションをリリース．
        new Subsequence() {

            @Override
            public void replay() throws Exception {
                target.release(mc[0]);
            }

            @Override
            public void record() throws Exception {
                // トランザクションが開始されていない．
                expect(tm.getTransaction()).andReturn(null);
                // コネクションがリリースされる．
                policy.release(mc[0]);
            }
        }.doTest();
    }

    /**
     * 一般的な場合のテスト．
     * 
     * @throws Exception
     */
    public void testNormal() throws Exception {
        target.initialize(mcf, policy);

        // 最初のコネクション取得～TransactionへのenlistResource()，registerSynchronization()．
        new Subsequence() {

            @Override
            public void replay() throws Exception {
                target.allocate(context[0]);
                assertTrue("0", target.pools.containsKey(tx));
            }

            @Override
            public void record() throws Exception {
                // トランザクションが開始されている．
                expect(tm.getTransaction()).andReturn(tx);
                // 後続のpolicyからコネクションを取得，mc0が返される．
                policy.allocate(eqContext(context[0], mc[0], null));
                // コネクションからXAResourceが取得される．
                // XAResourceの取得前に論理コネクションハンドルの取得 (mc.getConnection) を行う必要はない．
                // 「J2EE Connector Architecture Specification Version
                // 1.5」P7-25参照．
                // ただしSun JDBC Connector + Oracle XAConnection (10.1.0.2)では
                // 例外がスローされる．
                // Oracle XAConnection(10.1.0.4)では修正されている．
                expect(mc[0].getXAResource()).andReturn(xa);
                // XAResourceがTransactionに登録される．
                expect(tx.enlistResource(xa)).andReturn(true);
                // TransactionにSynchronizationとしてターゲットが登録される．
                tx.registerSynchronization(target);
            }
        }.doTest();

        // 最初のコネクションがクローズ (フリープールへ)．
        new Subsequence() {

            @Override
            public void replay() throws Exception {
                target.release(mc[0]);
            }

            @Override
            public void record() throws Exception {
                // トランザクションが開始されている．
                expect(tm.getTransaction()).andReturn(tx);
                // リリースされない．
            }
        }.doTest();

        // 2番目のコネクション取得～TransactionへのenlistResource()．
        // registerSynchronization()は呼ばれないこと．
        new Subsequence() {

            @Override
            public void replay() throws Exception {
                target.allocate(context[1]);
            }

            @Override
            public void record() throws Exception {
                // トランザクションが開始されている．
                expect(tm.getTransaction()).andReturn(tx);
                // フリープールのコネクションとマッチング，どれともマッチしない．
                expect(mcf.matchManagedConnections(set1, null, info)).andReturn(null);
                // 後続のpolicyからコネクション取得．
                policy.allocate(eqContext(context[1], mc[1], null));
                // コネクションからXAResourceが取得される．
                expect(mc[1].getXAResource()).andReturn(xa);
                // XAResourceがTransactionに登録される．
                expect(tx.enlistResource(xa)).andReturn(true);
                // Synchronizationの登録は行われない．
            }
        }.doTest();

        // 2番目のコネクションがクローズ (フリープールへ)．
        new Subsequence() {

            @Override
            public void replay() throws Exception {
                target.release(mc[1]);
            }

            @Override
            public void record() throws Exception {
                // トランザクションが開始されている．
                expect(tm.getTransaction()).andReturn(tx);
                // リリースされない．
            }
        }.doTest();

        // 三番目のコネクション取得 (最初のコネクションとマッチ)．
        new Subsequence() {

            @Override
            public void replay() throws Exception {
                target.allocate(context[0]);
            }

            @Override
            public void record() throws Exception {
                // トランザクションが開始されている．
                expect(tm.getTransaction()).andReturn(tx);
                // フリープールのコネクションとマッチング，mc0が返される．
                expect(mcf.matchManagedConnections(set2, null, info)).andReturn(mc[0]);
            }
        }.doTest();

        // 3番目のコネクションがクローズ (フリープールへ)．
        new Subsequence() {

            @Override
            public void replay() throws Exception {
                target.release(mc[0]);
            }

            @Override
            public void record() throws Exception {
                // トランザクションが開始されている．
                expect(tm.getTransaction()).andReturn(tx);
                // リリースされない．
            }
        }.doTest();

        // トランザクションコミット (ここでコネクションがリリースされることを確認)．
        new Subsequence() {

            @Override
            public void replay() throws Exception {
                target.afterCompletion(Status.STATUS_COMMITTED);
            }

            @Override
            public void record() throws Exception {
                // トランザクションが開始されている．
                expect(tm.getTransaction()).andReturn(tx);
                // 後続のpolicyにmc1がリリースされる．
                policy.release(mc[1]);
                // 後続のpolicyにmc0がリリースされる．
                policy.release(mc[0]);
            }
        }.doTest();
    }

    /**
     * トランザクションがロールバックされる場合のテスト．
     * 
     * @throws Exception
     */
    public void testErrorWithTransaction() throws Exception {
        target.initialize(mcf, policy);

        // 最初のコネクション取得～TransactionへのenlistResource()，registerSynchronization()．
        new Subsequence() {

            @Override
            public void replay() throws Exception {
                target.allocate(context[0]);
                assertTrue("0", target.pools.containsKey(tx));
            }

            @Override
            public void record() throws Exception {
                // トランザクションが開始されている．
                expect(tm.getTransaction()).andReturn(tx);
                // 後続のpolicyからコネクションを取得，mc0が返される．
                policy.allocate(eqContext(context[0], mc[0], null));
                // コネクションからXAResourceが取得される．
                expect(mc[0].getXAResource()).andReturn(xa);
                // XAResourceがTransactionに登録される．
                expect(tx.enlistResource(xa)).andReturn(true);
                // TransactionにSynchronizationとしてターゲットが登録される．
                tx.registerSynchronization(target);
            }
        }.doTest();

        // 最初のコネクションがクローズ (フリープールへ)．
        new Subsequence() {

            @Override
            public void replay() throws Exception {
                target.release(mc[0]);
            }

            @Override
            public void record() throws Exception {
                // トランザクションが開始されている．
                expect(tm.getTransaction()).andReturn(tx);
                // リリースされない．
            }
        }.doTest();

        // 2番目のコネクション取得～TransactionへのenlistResource()．
        // registerSynchronization()は呼ばれないこと．
        new Subsequence() {

            @Override
            public void replay() throws Exception {
                target.allocate(context[1]);
            }

            @Override
            public void record() throws Exception {
                // トランザクションが開始されている．
                expect(tm.getTransaction()).andReturn(tx);
                // フリープールのコネクションをマッチング，どれともマッチしない．
                expect(mcf.matchManagedConnections(set1, null, info)).andReturn(null);
                // 後続のpolicyからコネクション取得，mc1が返される．
                policy.allocate(eqContext(context[1], mc[1], null));
                // コネクションからXAResourceが取得される．
                expect(mc[1].getXAResource()).andReturn(xa);
                // XAResouceがTransactionに登録される．
                expect(tx.enlistResource(xa)).andReturn(true);
                // Synchronizationの登録は行われない．
            }
        }.doTest();

        // 2番目のコネクションがクローズ (フリープールへ)．
        new Subsequence() {

            @Override
            public void replay() throws Exception {
                target.release(mc[1]);
            }

            @Override
            public void record() throws Exception {
                // トランザクションが開始されている．
                expect(tm.getTransaction()).andReturn(tx);
                // リリースされない．
            }
        }.doTest();

        // 最初に取得したコネクションにエラー発生．
        new Subsequence() {

            @Override
            public void replay() throws Exception {
                target.connectionErrorOccurred(mc[0]);
            }

            @Override
            public void record() throws Exception {
                // トランザクションが開始されている．
                expect(tm.getTransaction()).andReturn(tx);
                // 後続のpolicyにエラーが通知される．
                policy.connectionErrorOccurred(mc[0]);
            }
        }.doTest();

        // トランザクションロールバック (二番目のコネクションのみフリープールへ)．
        new Subsequence() {

            @Override
            public void replay() throws Exception {
                target.afterCompletion(Status.STATUS_ROLLEDBACK);
            }

            @Override
            public void record() throws Exception {
                // トランザクションが開始されている．
                expect(tm.getTransaction()).andReturn(tx);
                // コネクションがリリースされる．
                policy.release(mc[1]);
            }
        }.doTest();
    }

}
