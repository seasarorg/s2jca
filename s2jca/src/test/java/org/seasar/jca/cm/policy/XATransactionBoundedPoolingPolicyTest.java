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
import java.util.Set;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.transaction.Status;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

import org.easymock.MockControl;
import org.seasar.jca.cm.support.ConnectionManagementContext;
import org.seasar.jca.cm.support.ConnectionManagementContextMatcher;
import org.seasar.jca.ut.EasyMockTestCase;

/**
 * @author koichik
 */
public class XATransactionBoundedPoolingPolicyTest extends EasyMockTestCase {
    private XATransactionBoundedPoolingPolicy target;
    private TransactionManager tm;
    private MockControl tmControl;
    private Transaction tx;
    private MockControl txControl;
    private XAResource xa;
    private MockControl xaControl;
    private ConnectionManagementPolicy policy;
    private MockControl policyControl;
    private ManagedConnectionFactory mcf;
    private MockControl mcfControl;
    private ManagedConnection[] mc = new ManagedConnection[3];
    private MockControl[] mcControl = new MockControl[3];
    private ConnectionRequestInfo info;
    private MockControl infoControl;
    private ConnectionManagementContext[] context = new ConnectionManagementContext[3];
    private Object[] lch = new Object[3];
    private Set<ManagedConnection> set1;
    private Set<ManagedConnection> set2;

    public XATransactionBoundedPoolingPolicyTest() {
    }

    public XATransactionBoundedPoolingPolicyTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        tmControl = createStrictControl(TransactionManager.class);
        tm = (TransactionManager) tmControl.getMock();
        txControl = createStrictControl(Transaction.class);
        tx = (Transaction) txControl.getMock();
        xaControl = createStrictControl(XAResource.class);
        xa = (XAResource) xaControl.getMock();
        mcfControl = createStrictControl(ManagedConnectionFactory.class);
        mcf = (ManagedConnectionFactory) mcfControl.getMock();
        infoControl = createStrictControl(ConnectionRequestInfo.class);
        info = (ConnectionRequestInfo) infoControl.getMock();
        for (int i = 0; i < 3; ++i) {
            mcControl[i] = createStrictControl(ManagedConnection.class);
            mc[i] = (ManagedConnection) mcControl[i].getMock();
            lch[i] = new Object();
            context[i] = new ConnectionManagementContext(null, info, mcf);
        }
        policyControl = createStrictControl(ConnectionManagementPolicy.class);
        policy = (ConnectionManagementPolicy) policyControl.getMock();

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
            public void verify() throws Exception {
                // トランザクションが開始されていない．
                tm.getTransaction();
                tmControl.setReturnValue(null);
            }
        }.doTest();
    }

    /**
     * <code>allowLocalTx</code> がデフォルト( <code>false</code>
     * )でトランザクションが開始されていない場合のテスト．
     * 
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
            public void verify() throws Exception {
                // トランザクションが開始されていない．
                tm.getTransaction();
                tmControl.setReturnValue(null);
                // allowLocalTxが設定されているのでそれでもコネクションが取得される．
                policy.allocate(context[0]);
                policyControl.setMatcher(new ConnectionManagementContextMatcher(mc[0], null));
            }
        }.doTest();

        // コネクションをリリース．
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.release(mc[0]);
            }

            @Override
            public void verify() throws Exception {
                // トランザクションが開始されていない．
                tm.getTransaction();
                tmControl.setReturnValue(null);
                // コネクションがリリースされる．
                policy.release(mc[0]);
            }
        }.doTest();
    }

    /**
     * 一般的な場合のテスト．
     * 
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
            public void verify() throws Exception {
                // トランザクションが開始されている．
                tm.getTransaction();
                tmControl.setReturnValue(tx);
                // 後続のpolicyからコネクションを取得，mc0が返される．
                policy.allocate(context[0]);
                policyControl.setMatcher(new ConnectionManagementContextMatcher(mc[0], null));
                // コネクションからXAResourceが取得される．
                // XAResourceの取得前に論理コネクションハンドルの取得 (mc.getConnection) を行う必要はない．
                // 「J2EE Connector Architecture Specification Version
                // 1.5」P7-25参照．
                // ただしSun JDBC Connector + Oracle XAConnection (10.1.0.2)では
                // 例外がスローされる．
                // Oracle XAConnection(10.1.0.4)では修正されている．
                mc[0].getXAResource();
                mcControl[0].setReturnValue(xa);
                // XAResourceがTransactionに登録される．
                tx.enlistResource(xa);
                txControl.setReturnValue(true);
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
            public void verify() throws Exception {
                // トランザクションが開始されている．
                tm.getTransaction();
                tmControl.setReturnValue(tx);
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
            public void verify() throws Exception {
                // トランザクションが開始されている．
                tm.getTransaction();
                tmControl.setReturnValue(tx);
                // フリープールのコネクションとマッチング，どれともマッチしない．
                mcf.matchManagedConnections(set1, null, info);
                mcfControl.setReturnValue(null);
                // 後続のpolicyからコネクション取得．
                policy.allocate(context[1]);
                policyControl.setMatcher(new ConnectionManagementContextMatcher(mc[1], null));
                // コネクションからXAResourceが取得される．
                mc[1].getXAResource();
                mcControl[1].setReturnValue(xa);
                // XAResourceがTransactionに登録される．
                tx.enlistResource(xa);
                txControl.setReturnValue(true);
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
            public void verify() throws Exception {
                // トランザクションが開始されている．
                tm.getTransaction();
                tmControl.setReturnValue(tx);
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
            public void verify() throws Exception {
                // トランザクションが開始されている．
                tm.getTransaction();
                tmControl.setReturnValue(tx);
                // フリープールのコネクションとマッチング，mc0が返される．
                mcf.matchManagedConnections(set2, null, info);
                mcfControl.setReturnValue(mc[0]);
            }
        }.doTest();

        // 3番目のコネクションがクローズ (フリープールへ)．
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.release(mc[0]);
            }

            @Override
            public void verify() throws Exception {
                // トランザクションが開始されている．
                tm.getTransaction();
                tmControl.setReturnValue(tx);
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
            public void verify() throws Exception {
                // トランザクションが開始されている．
                tm.getTransaction();
                tmControl.setReturnValue(tx);
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
            public void verify() throws Exception {
                // トランザクションが開始されている．
                tm.getTransaction();
                tmControl.setReturnValue(tx);
                // 後続のpolicyからコネクションを取得，mc0が返される．
                policy.allocate(context[0]);
                policyControl.setMatcher(new ConnectionManagementContextMatcher(mc[0], null));
                // コネクションからXAResourceが取得される．
                mc[0].getXAResource();
                mcControl[0].setReturnValue(xa);
                // XAResourceがTransactionに登録される．
                tx.enlistResource(xa);
                txControl.setReturnValue(true);
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
            public void verify() throws Exception {
                // トランザクションが開始されている．
                tm.getTransaction();
                tmControl.setReturnValue(tx);
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
            public void verify() throws Exception {
                // トランザクションが開始されている．
                tm.getTransaction();
                tmControl.setReturnValue(tx);
                // フリープールのコネクションをマッチング，どれともマッチしない．
                mcf.matchManagedConnections(set1, null, info);
                mcfControl.setReturnValue(null);
                // 後続のpolicyからコネクション取得，mc1が返される．
                policy.allocate(context[1]);
                policyControl.setMatcher(new ConnectionManagementContextMatcher(mc[1], null));
                // コネクションからXAResourceが取得される．
                mc[1].getXAResource();
                mcControl[1].setReturnValue(xa);
                // XAResouceがTransactionに登録される．
                tx.enlistResource(xa);
                txControl.setReturnValue(true);
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
            public void verify() throws Exception {
                // トランザクションが開始されている．
                tm.getTransaction();
                tmControl.setReturnValue(tx);
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
            public void verify() throws Exception {
                // トランザクションが開始されている．
                tm.getTransaction();
                tmControl.setReturnValue(tx);
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
            public void verify() throws Exception {
                // トランザクションが開始されている．
                tm.getTransaction();
                tmControl.setReturnValue(tx);
                // コネクションがリリースされる．
                policy.release(mc[1]);
            }
        }.doTest();
    }
}
