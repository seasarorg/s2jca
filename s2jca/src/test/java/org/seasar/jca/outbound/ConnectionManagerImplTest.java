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
package org.seasar.jca.outbound;

import java.sql.Connection;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;

import org.easymock.MockControl;
import org.seasar.jca.outbound.policy.ConnectionManagementPolicy;
import org.seasar.jca.outbound.support.ConnectionManagementContext;
import org.seasar.jca.outbound.support.ConnectionManagementContextMatcher;
import org.seasar.jca.unit.EasyMockTestCase;

/**
 * @author koichik
 */
public class ConnectionManagerImplTest extends EasyMockTestCase {
    private ConnectionManagerImpl target;
    private ManagedConnectionFactory mcf;
    private MockControl mcfControl;
    private ManagedConnection mc;
    private MockControl mcControl;
    private Connection lch;
    private MockControl lchControl;
    private ConnectionManagementPolicy policy;
    private MockControl policyControl;
    private ConnectionRequestInfo info;
    private MockControl infoControl;
    private ConnectionManagementContext context;

    public ConnectionManagerImplTest() {
    }

    public ConnectionManagerImplTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mcfControl = createStrictControl(ManagedConnectionFactory.class);
        mcf = (ManagedConnectionFactory) mcfControl.getMock();
        mcControl = createStrictControl(ManagedConnection.class);
        mc = (ManagedConnection) mcControl.getMock();
        lchControl = createStrictControl(Connection.class);
        lch = (Connection) lchControl.getMock();
        policyControl = createStrictControl(ConnectionManagementPolicy.class);
        policy = (ConnectionManagementPolicy) policyControl.getMock();
        infoControl = createStrictControl(ConnectionRequestInfo.class);
        info = (ConnectionRequestInfo) infoControl.getMock();
        context = new ConnectionManagementContext(null, info, mcf);

        target = new ConnectionManagerImpl(mcf);
    }

    /**
     * <code>ConnectionManagerPolicy</code> を追加するテスト． <br>
     * 追加されたpolicyに前のpolicyが渡されることを確認する． <br>
     * <code>ConnectionManager#allocateConnection()</code>
     * の呼び出しで追加されたpolicyが呼び出されることを確認する．
     * 
     * @throws Exception
     */
    public void testAddPolicy() throws Exception {
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.addConnectionManagementPolicy(policy);
                assertSame("0", policy, target.policy);
            }

            @Override
            public void verify() throws Exception {
                // 追加されたpolicyに既存のpolicyが渡される．
                policy.initialize(mcf, target.policy);
            }
        }.doTest();

        new Subsequence() {
            @Override
            public void replay() throws Exception {
                assertEquals("1", lch, target.allocateConnection(mcf, info));
            }

            @Override
            public void verify() throws Exception {
                // 追加されたpolicyが呼び出される．
                policy.allocate(context);
                policyControl.setMatcher(new ConnectionManagementContextMatcher(mc, null));
                // 論理コネクションハンドルを取得．
                mc.getConnection(null, info);
                mcControl.setReturnValue(lch);
            }
        }.doTest();
    }

    /**
     * allocate()の引数にコンストラクタで渡された <code>ManagedConnectionFactory</code> とは 異なる
     * <code>ManagedConnectionFactory</code> が渡された場合のテスト．
     * 
     * @throws Exception
     */
    public void testIllegalMCF() throws Exception {
        MockControl illegalMCFControl = createControl(ManagedConnectionFactory.class);
        final ManagedConnectionFactory illegalMCF = (ManagedConnectionFactory) illegalMCFControl
                .getMock();

        new Subsequence() {
            @Override
            public void replay() throws Exception {
                // 例外がスローされる．
                try {
                    target.allocateConnection(illegalMCF, info);
                    fail("0");
                } catch (ResourceException expected) {
                }
            }
        }.doTest();
    }

    /**
     * コネクション取得のテスト．
     * 
     * @throws Exception
     */
    public void testAllocate() throws Exception {
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                // コネクションを取得．
                assertEquals("0", lch, target.allocateConnection(mcf, info));
            }

            @Override
            public void verify() throws Exception {
                // ManagedConnectionFactoryからManagedConnectionを取得する．
                mcf.createManagedConnection(null, info);
                mcfControl.setReturnValue(mc);
                // ManagedConnectionにConnectionManagerImpl.listenerがリスナーとして登録される．
                mc.addConnectionEventListener(target.listener);
                // ManagedConnectionからコネクションを取得する．
                mc.getConnection(null, info);
                mcControl.setReturnValue(lch);
            }
        }.doTest();
    }

    /**
     * コネクションのクローズが呼び出されたイベントを受けた場合のテスト．
     * 
     * @throws Exception
     */
    public void testCloseConnection() throws Exception {
        new Subsequence() {
            /**
             * ConnectionClosedイベントを受信．
             */
            @Override
            public void replay() throws Exception {
                target.listener.connectionClosed(new ConnectionEvent(mc,
                        ConnectionEvent.CONNECTION_CLOSED));
            }

            @Override
            public void verify() throws Exception {
                // ManagedConnectionに登録したリスナーが削除される．
                mc.removeConnectionEventListener(target.listener);
                // ManagedConnectionのdestroy()が呼び出される．
                mc.destroy();
            }
        }.doTest();
    }

    /**
     * コネクションにエラーが発生したイベントを受けた場合のテスト．
     * 
     * @throws Exception
     */
    public void testConnectionErrorOccurred() throws Exception {
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                ConnectionEvent event = new ConnectionEvent(mc,
                        ConnectionEvent.CONNECTION_ERROR_OCCURRED);
                target.listener.connectionErrorOccurred(event);
            }

            @Override
            public void verify() throws Exception {
                // ManagedConnectionに登録したリスナーが削除される．
                mc.removeConnectionEventListener(target.listener);
                // ManagedConnectionのdestroy()が呼び出される．
                mc.destroy();
            }
        }.doTest();
    }
}
