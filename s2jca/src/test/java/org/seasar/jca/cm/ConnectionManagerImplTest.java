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
package org.seasar.jca.cm;

import java.sql.Connection;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;

import org.easymock.MockControl;
import org.seasar.jca.cm.policy.ConnectionManagementPolicy;
import org.seasar.jca.cm.support.ConnectionManagementContext;
import org.seasar.jca.cm.support.ConnectionManagementContextMatcher;
import org.seasar.jca.ut.EasyMockTestCase;

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
     * <code>ConnectionManagerPolicy</code> ��ǉ�����e�X�g�D <br>
     * �ǉ����ꂽpolicy�ɑO��policy���n����邱�Ƃ��m�F����D <br>
     * <code>ConnectionManager#allocateConnection()</code>
     * �̌Ăяo���Œǉ����ꂽpolicy���Ăяo����邱�Ƃ��m�F����D
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
                // �ǉ����ꂽpolicy�Ɋ�����policy���n�����D
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
                // �ǉ����ꂽpolicy���Ăяo�����D
                policy.allocate(context);
                policyControl.setMatcher(new ConnectionManagementContextMatcher(mc, null));
                // �_���R�l�N�V�����n���h�����擾�D
                mc.getConnection(null, info);
                mcControl.setReturnValue(lch);
            }
        }.doTest();
    }

    /**
     * allocate()�̈����ɃR���X�g���N�^�œn���ꂽ <code>ManagedConnectionFactory</code> �Ƃ� �قȂ�
     * <code>ManagedConnectionFactory</code> ���n���ꂽ�ꍇ�̃e�X�g�D
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
                // ��O���X���[�����D
                try {
                    target.allocateConnection(illegalMCF, info);
                    fail("0");
                } catch (ResourceException expected) {
                }
            }
        }.doTest();
    }

    /**
     * �R�l�N�V�����擾�̃e�X�g�D
     * 
     * @throws Exception
     */
    public void testAllocate() throws Exception {
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                // �R�l�N�V�������擾�D
                assertEquals("0", lch, target.allocateConnection(mcf, info));
            }

            @Override
            public void verify() throws Exception {
                // ManagedConnectionFactory����ManagedConnection���擾����D
                mcf.createManagedConnection(null, info);
                mcfControl.setReturnValue(mc);
                // ManagedConnection��ConnectionManagerImpl.listener�����X�i�[�Ƃ��ēo�^�����D
                mc.addConnectionEventListener(target.listener);
                // ManagedConnection����R�l�N�V�������擾����D
                mc.getConnection(null, info);
                mcControl.setReturnValue(lch);
            }
        }.doTest();
    }

    /**
     * �R�l�N�V�����̃N���[�Y���Ăяo���ꂽ�C�x���g���󂯂��ꍇ�̃e�X�g�D
     * 
     * @throws Exception
     */
    public void testCloseConnection() throws Exception {
        new Subsequence() {
            /**
             * ConnectionClosed�C�x���g����M�D
             */
            @Override
            public void replay() throws Exception {
                target.listener.connectionClosed(new ConnectionEvent(mc,
                        ConnectionEvent.CONNECTION_CLOSED));
            }

            @Override
            public void verify() throws Exception {
                // ManagedConnection�ɓo�^�������X�i�[���폜�����D
                mc.removeConnectionEventListener(target.listener);
                // ManagedConnection��destroy()���Ăяo�����D
                mc.destroy();
            }
        }.doTest();
    }

    /**
     * �R�l�N�V�����ɃG���[�����������C�x���g���󂯂��ꍇ�̃e�X�g�D
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
                // ManagedConnection�ɓo�^�������X�i�[���폜�����D
                mc.removeConnectionEventListener(target.listener);
                // ManagedConnection��destroy()���Ăяo�����D
                mc.destroy();
            }
        }.doTest();
    }
}
