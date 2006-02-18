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
package org.seasar.jca.outbound.policy;

import java.util.HashSet;
import java.util.Set;
import java.util.Timer;

import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;

import org.seasar.jca.outbound.support.ConnectionManagementContext;
import org.seasar.jca.unit.EasyMockTestCase;

import static org.easymock.EasyMock.expect;

import static org.seasar.jca.outbound.support.ConnectionManagementContextMatcher.eqContext;

/**
 * @author koichik
 */
public class BasicPoolingPolicyTest extends EasyMockTestCase {
    private BasicPoolingPolicy target;
    private int status;
    private Timer timer;
    private BootstrapContext bc;
    private ConnectionManagementPolicy policy;
    private ManagedConnectionFactory mcf;
    private ManagedConnection[] mc = new ManagedConnection[3];
    private ConnectionRequestInfo info;
    private Object[] lch = new Object[3];
    private ConnectionManagementContext context[] = new ConnectionManagementContext[3];
    private Set<ManagedConnection> set1;
    private Set<ManagedConnection> set2;

    public BasicPoolingPolicyTest() {
    }

    public BasicPoolingPolicyTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        status = 0;
        timer = new Timer(true);

        bc = createStrictMock(BootstrapContext.class);
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

    }

    protected void createTarget() throws Exception {
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target = new BasicPoolingPolicy(bc);
                assertNotNull("0", BasicPoolingPolicy.timer);
            }

            @Override
            public void verify() throws Exception {
                // 最初の一回だけタイマーが作成される．
                if (BasicPoolingPolicy.timer == null) {
                    expect(bc.createTimer()).andReturn(timer);
                }
            }
        }.doTest();
    }

    /**
     * 最初(フリープール・アクティブプールとも空)にコネクションを取得する場合のテスト．
     * 
     */
    public void testAcquireFromEmpty() throws Exception {
        createTarget();
        target.setMinPoolSize(1);
        target.setMaxPoolSize(2);
        target.initialize(mcf, policy);
        assertEquals("0", 0, target.pool.getActivePoolSize());
        assertEquals("1", 0, target.pool.getFreePoolSize());

        // コネクションの取得．
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.allocate(context[0]);
                assertEquals("2", mc[0], context[0].getManagedConnection());
                assertEquals("3", 1, target.pool.getActivePoolSize());
                assertEquals("4", 0, target.pool.getFreePoolSize());
            }

            @Override
            public void verify() throws Exception {
                // 後続のpolicyからコネクションを取得，mc0が返される．
                policy.allocate(eqContext(context[0], mc[0], null));
            }
        }.doTest();

        // コネクションリリース (フリープールへ)．
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.release(mc[0]);
                assertEquals("5", 0, target.pool.getActivePoolSize());
                assertEquals("6", 1, target.pool.getFreePoolSize());
            }

            @Override
            public void verify() throws Exception {
                // コネクションのクリーンナップ
                mc[0].cleanup();
            }
        }.doTest();

        // プールの終了 (コネクションのリリース)．
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.dispose();
                assertEquals("7", 0, target.pool.getActivePoolSize());
                assertEquals("8", 0, target.pool.getFreePoolSize());
            }

            @Override
            public void verify() throws Exception {
                // フリープールのコネクションが後続がpolicyへ渡される．
                policy.release(mc[0]);
                // 後続のpolicyに終了が伝播される．
                policy.dispose();
            }
        }.doTest();
    }

    /**
     * フリープールからコネクションを取得する場合のテスト．
     * 
     */
    public void testAcquireFromFreePool() throws Exception {
        createTarget();
        target.setMinPoolSize(1);
        target.setMaxPoolSize(2);
        target.initialize(mcf, policy);
        assertEquals("0", 0, target.pool.getActivePoolSize());
        assertEquals("1", 0, target.pool.getFreePoolSize());

        // 最初のコネクション取得．
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.allocate(context[0]);
                assertEquals("2", 1, target.pool.getActivePoolSize());
                assertEquals("3", 0, target.pool.getFreePoolSize());
            }

            @Override
            public void verify() throws Exception {
                // 後続のpolicyからコネクションが取得される，mc0が返される．
                policy.allocate(eqContext(context[0], mc[0], null));
            }
        }.doTest();

        // 2番目のコネクション取得．
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.allocate(context[1]);
                assertEquals("4", 2, target.pool.getActivePoolSize());
                assertEquals("5", 0, target.pool.getFreePoolSize());
            }

            @Override
            public void verify() throws Exception {
                // 後続のpolicyからコネクションが取得され，mc1が返される．
                policy.allocate(eqContext(context[1], mc[1], null));
            }
        }.doTest();

        // 最初に取得したコネクションをリリース (フリープールへ)．
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.release(mc[0]);
                assertEquals("6", 1, target.pool.getActivePoolSize());
                assertEquals("7", 1, target.pool.getFreePoolSize());
            }

            @Override
            public void verify() throws Exception {
                // コネクションのクリーンナップ
                mc[0].cleanup();
            }
        }.doTest();

        // 2番目に取得したコネクションをリリース (フリープールへ)．
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.release(mc[1]);
                assertEquals("8", 0, target.pool.getActivePoolSize());
                assertEquals("9", 2, target.pool.getFreePoolSize());
            }

            @Override
            public void verify() throws Exception {
                // コネクションのクリーンナップ
                mc[1].cleanup();
            }
        }.doTest();

        // 3番目のコネクション取得 (フリープールから2番目のコネクションを得る)．
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.allocate(context[1].setManagedConnection(null));
                assertEquals("11", 1, target.pool.getActivePoolSize());
                assertEquals("12", 1, target.pool.getFreePoolSize());
            }

            @Override
            public void verify() throws Exception {
                // フリープールのコネクションとマッチング，mc1が返される．
                expect(mcf.matchManagedConnections(set2, null, info)).andReturn(mc[1]);
            }
        }.doTest();

        // 4番目のコネクション取得 (フリープールから最初のコネクションを得る)．
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.allocate(context[0].setManagedConnection(null));
                assertEquals("13", 2, target.pool.getActivePoolSize());
                assertEquals("14", 0, target.pool.getFreePoolSize());
            }

            @Override
            public void verify() throws Exception {
                // フリープールのコネクションとマッチング，mc0がマッチ．
                expect(mcf.matchManagedConnections(set1, null, info)).andReturn(mc[0]);
            }
        }.doTest();

        // 3番目に取得したコネクションをリリース (フリープールへ)．
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.release(mc[1]);
                assertEquals("15", 1, target.pool.getActivePoolSize());
                assertEquals("16", 1, target.pool.getFreePoolSize());
            }

            @Override
            public void verify() throws Exception {
                // コネクションのクリーンナップ
                mc[1].cleanup();
            }
        }.doTest();

        // プールを終了．
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.dispose();
                assertEquals("17", 0, target.pool.getActivePoolSize());
                assertEquals("18", 0, target.pool.getFreePoolSize());
            }

            @Override
            public void verify() throws Exception {
                // アクティブプール・フリープールのコネクションともリリースされる．
                policy.release(mc[0]);
                policy.release(mc[1]);
                policy.dispose();
            }
        }.doTest();
    }

    /**
     * プールされているコネクションがmaxPoolSizeに達している場合に
     * フリープールのコネクションにマッチしないコネクションを取得する場合のテスト． <br>
     * フリープールのコネクションを破棄して新しいコネクションを取得する．
     * 
     */
    public void testAcquireNew() throws Exception {
        createTarget();
        target.setMinPoolSize(1);
        target.setMaxPoolSize(2);
        target.initialize(mcf, policy);

        // 最初のコネクション取得．
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.allocate(context[0]);
            }

            @Override
            public void verify() throws Exception {
                // 後続のpolicyからコネクションを取得，mc0が返される．
                policy.allocate(eqContext(context[0], mc[0], null));
            }
        }.doTest();

        // 2番目のコネクション取得．
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.allocate(context[1]);
            }

            @Override
            public void verify() throws Exception {
                // 後続のpolicyからコネクションを取得，mc1が返される．
                policy.allocate(eqContext(context[1], mc[1], null));
            }
        }.doTest();

        // 取得したコネクションを両方ともリリース (フリープールへ)．
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.release(mc[0]);
                target.release(mc[1]);
                assertEquals("1", 0, target.pool.getActivePoolSize());
                assertEquals("2", 2, target.pool.getFreePoolSize());
            }

            @Override
            public void verify() throws Exception {
                // コネクションのクリーンナップ
                mc[0].cleanup();
                mc[1].cleanup();
            }
        }.doTest();

        // 3番目のコネクション取得 (最初のコネクションをプールから破棄する)．
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.allocate(context[2].setManagedConnection(null));
                assertEquals("3", 1, target.pool.getActivePoolSize());
                assertEquals("4", 1, target.pool.getFreePoolSize());
            }

            @Override
            public void verify() throws Exception {
                // フリープールのコネクションとマッチング，どれもマッチしない．
                expect(mcf.matchManagedConnections(set2, null, info)).andReturn(null);
                // フリープールのコネクションが一つリリースされる．
                policy.release(mc[0]);
                // 後続のpolicyから新しいコネクションを取得．
                policy.allocate(eqContext(context[2], mc[2], null));
            }
        }.doTest();

        // プールを終了．
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.dispose();
                assertEquals("5", 0, target.pool.getActivePoolSize());
                assertEquals("6", 0, target.pool.getFreePoolSize());
            }

            @Override
            public void verify() throws Exception {
                // アクティブプール・フリープールのコネクションともリリースされる．
                policy.release(mc[2]);
                policy.release(mc[1]);
                policy.dispose();
            }
        }.doTest();
    }

    /**
     * アクティブプールがmaxPoolSizeに達している場合のテスト． <br>
     * アクティブなコネクションがフリープールに戻されるまで待機する．
     * 
     */
    public void testAcquireFromFull() throws Exception {
        createTarget();
        target.setMinPoolSize(1);
        target.setMaxPoolSize(2);
        target.initialize(mcf, policy);

        // コネクションをリリースするスレッド．
        Thread bg = new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                    assertEquals("0", 0, status);
                    ++status;
                    // コネクションをリリース (フリープールへ)．
                    target.release(mc[0]);
                } catch (Exception ignore) {
                }
            }
        };
        bg.start();

        // 初期状態 (maxPoolSize分のコネクションがアクティブ)．
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.pool.addToActivePool(mc[0]);
                target.pool.addToActivePool(mc[1]);
                assertEquals("1", 0, status);
                assertEquals("2", 2, target.pool.getActivePoolSize());
                assertEquals("3", 0, target.pool.getFreePoolSize());
            }
        }.doTest();

        // コネクションを取得 (フリープールが空くまで待機する)．
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                assertEquals("4", 0, status);
                target.allocate(context[0]);
                assertEquals("5", 1, status);
                assertEquals("6", 2, target.pool.getActivePoolSize());
                assertEquals("7", 0, target.pool.getFreePoolSize());
            }

            @Override
            public void verify() throws Exception {
                // コネクションのクリーンナップ
                mc[0].cleanup();
                // フリープールのコネクションとマッチング，mc0が返される．
                expect(mcf.matchManagedConnections(set1, null, info)).andReturn(mc[0]);
            }
        }.doTest();
    }

    /**
     * フリープールのコネクションがタイムアウトして破棄される場合のテスト．
     * 
     */
    public void testExpire() throws Exception {
        createTarget();
        target.setMinPoolSize(1);
        target.setMaxPoolSize(2);
        target.setTimeout(3);
        target.initialize(mcf, policy);

        // 最初のコネクション取得．
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.allocate(context[0]);
            }

            @Override
            public void verify() throws Exception {
                // 後続のpolicyからコネクションを取得，mc0が返される．
                policy.allocate(eqContext(context[0], mc[0], null));
            }
        }.doTest();

        // 2番目のコネクション取得．
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.allocate(context[1]);
            }

            @Override
            public void verify() throws Exception {
                // 後続のpolicyからコネクションを取得，mc1が返される．
                policy.allocate(eqContext(context[1], mc[1], null));
            }
        }.doTest();

        // コネクションが二つともクローズされるがすぐにはpolicy.release()は呼ばれない．
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.release(mc[1]);
                // 時間を空けてリリースするため待機．
                Thread.sleep(1 * 1000);
                target.release(mc[0]);
                // mc0，mc1ともフリープールにプーリングされていることを確認．
                assertEquals("1", 0, target.pool.getActivePoolSize());
                assertEquals("2", 2, target.pool.getFreePoolSize());
            }

            @Override
            public void verify() throws Exception {
                // コネクションのクリーンナップ
                mc[0].cleanup();
                mc[1].cleanup();
            }
        }.doTest();

        // タイムアウトする直前まで待機．
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                Thread.sleep(1 * 1000);
                // まだmc0，mc1ともフリープールにプーリングされていることを確認．
                assertEquals("3", 0, target.pool.getActivePoolSize());
                assertEquals("4", 2, target.pool.getFreePoolSize());
            }
        }.doTest();

        // コネクションがタイムアウトして先にクローズが呼ばれたmc1がリリースされる．
        // しかしプールのminPoolSizeが1であるためmc0はリリースされない．
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                // コネクションがタイムアウトするまで待機．
                Thread.sleep(2 * 1000);
                // minPoolSizeが1なのでリリースされたコネクションが一つだけであることを確認．
                assertEquals("5", 0, target.pool.getActivePoolSize());
                assertEquals("6", 1, target.pool.getFreePoolSize());
            }

            @Override
            public void verify() throws Exception {
                // コネクションがリリースされる．
                policy.release(mc[1]);
            }
        }.doTest();

        // 後処理．
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.dispose();
                assertEquals("7", 0, target.pool.getActivePoolSize());
                assertEquals("8", 0, target.pool.getFreePoolSize());
            }

            @Override
            public void verify() throws Exception {
                // プールの終了によりmc0もリリースされる．
                policy.release(mc[0]);
                policy.dispose();
            }
        }.doTest();
    }
}
