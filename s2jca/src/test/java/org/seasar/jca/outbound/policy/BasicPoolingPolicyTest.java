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

import org.easymock.MockControl;
import org.seasar.jca.outbound.support.ConnectionManagementContext;
import org.seasar.jca.outbound.support.ConnectionManagementContextMatcher;
import org.seasar.jca.unit.EasyMockTestCase;

/**
 * @author koichik
 */
public class BasicPoolingPolicyTest extends EasyMockTestCase {
    private BasicPoolingPolicy target;
    private int status;
    private Timer timer;
    private BootstrapContext bc;
    private MockControl bcControl;
    private ConnectionManagementPolicy policy;
    private MockControl policyControl;
    private ManagedConnectionFactory mcf;
    private MockControl mcfControl;
    private ManagedConnection[] mc = new ManagedConnection[3];
    private MockControl[] mcControl = new MockControl[3];
    private ConnectionRequestInfo info;
    private MockControl infoControl;
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

        bcControl = createStrictControl(BootstrapContext.class);
        bc = (BootstrapContext) bcControl.getMock();
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
                // �ŏ��̈�񂾂��^�C�}�[���쐬�����D
                if (BasicPoolingPolicy.timer == null) {
                    bc.createTimer();
                    bcControl.setReturnValue(timer);
                }
            }
        }.doTest();
    }

    /**
     * �ŏ�(�t���[�v�[���E�A�N�e�B�u�v�[���Ƃ���)�ɃR�l�N�V�������擾����ꍇ�̃e�X�g�D
     * 
     */
    public void testAcquireFromEmpty() throws Exception {
        createTarget();
        target.setMinPoolSize(1);
        target.setMaxPoolSize(2);
        target.initialize(mcf, policy);
        assertEquals("0", 0, target.pool.getActivePoolSize());
        assertEquals("1", 0, target.pool.getFreePoolSize());

        // �R�l�N�V�����̎擾�D
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
                // �㑱��policy����R�l�N�V�������擾�Cmc0���Ԃ����D
                policy.allocate(context[0]);
                policyControl.setMatcher(new ConnectionManagementContextMatcher(mc[0], null));
            }
        }.doTest();

        // �R�l�N�V���������[�X (�t���[�v�[����)�D
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.release(mc[0]);
                assertEquals("5", 0, target.pool.getActivePoolSize());
                assertEquals("6", 1, target.pool.getFreePoolSize());
            }

            @Override
            public void verify() throws Exception {
                // �R�l�N�V�����̃N���[���i�b�v
                mc[0].cleanup();
            }
        }.doTest();

        // �v�[���̏I�� (�R�l�N�V�����̃����[�X)�D
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.dispose();
                assertEquals("7", 0, target.pool.getActivePoolSize());
                assertEquals("8", 0, target.pool.getFreePoolSize());
            }

            @Override
            public void verify() throws Exception {
                // �t���[�v�[���̃R�l�N�V�������㑱��policy�֓n�����D
                policy.release(mc[0]);
                // �㑱��policy�ɏI�����`�d�����D
                policy.dispose();
            }
        }.doTest();
    }

    /**
     * �t���[�v�[������R�l�N�V�������擾����ꍇ�̃e�X�g�D
     * 
     */
    public void testAcquireFromFreePool() throws Exception {
        createTarget();
        target.setMinPoolSize(1);
        target.setMaxPoolSize(2);
        target.initialize(mcf, policy);
        assertEquals("0", 0, target.pool.getActivePoolSize());
        assertEquals("1", 0, target.pool.getFreePoolSize());

        // �ŏ��̃R�l�N�V�����擾�D
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.allocate(context[0]);
                assertEquals("2", 1, target.pool.getActivePoolSize());
                assertEquals("3", 0, target.pool.getFreePoolSize());
            }

            @Override
            public void verify() throws Exception {
                // �㑱��policy����R�l�N�V�������擾�����Cmc0���Ԃ����D
                policy.allocate(context[0]);
                policyControl.setMatcher(new ConnectionManagementContextMatcher(mc[0], null));
            }
        }.doTest();

        // 2�Ԗڂ̃R�l�N�V�����擾�D
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.allocate(context[1]);
                assertEquals("4", 2, target.pool.getActivePoolSize());
                assertEquals("5", 0, target.pool.getFreePoolSize());
            }

            @Override
            public void verify() throws Exception {
                // �㑱��policy����R�l�N�V�������擾����Cmc1���Ԃ����D
                policy.allocate(context[1]);
                policyControl.setMatcher(new ConnectionManagementContextMatcher(mc[1], null));
            }
        }.doTest();

        // �ŏ��Ɏ擾�����R�l�N�V�����������[�X (�t���[�v�[����)�D
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.release(mc[0]);
                assertEquals("6", 1, target.pool.getActivePoolSize());
                assertEquals("7", 1, target.pool.getFreePoolSize());
            }

            @Override
            public void verify() throws Exception {
                // �R�l�N�V�����̃N���[���i�b�v
                mc[0].cleanup();
            }
        }.doTest();

        // 2�ԖڂɎ擾�����R�l�N�V�����������[�X (�t���[�v�[����)�D
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.release(mc[1]);
                assertEquals("8", 0, target.pool.getActivePoolSize());
                assertEquals("9", 2, target.pool.getFreePoolSize());
            }

            @Override
            public void verify() throws Exception {
                // �R�l�N�V�����̃N���[���i�b�v
                mc[1].cleanup();
            }
        }.doTest();

        // 3�Ԗڂ̃R�l�N�V�����擾 (�t���[�v�[������2�Ԗڂ̃R�l�N�V�����𓾂�)�D
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.allocate(context[1].setManagedConnection(null));
                assertEquals("11", 1, target.pool.getActivePoolSize());
                assertEquals("12", 1, target.pool.getFreePoolSize());
            }

            @Override
            public void verify() throws Exception {
                // �t���[�v�[���̃R�l�N�V�����ƃ}�b�`���O�Cmc1���Ԃ����D
                mcf.matchManagedConnections(set2, null, info);
                mcfControl.setReturnValue(mc[1]);
            }
        }.doTest();

        // 4�Ԗڂ̃R�l�N�V�����擾 (�t���[�v�[������ŏ��̃R�l�N�V�����𓾂�)�D
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.allocate(context[0].setManagedConnection(null));
                assertEquals("13", 2, target.pool.getActivePoolSize());
                assertEquals("14", 0, target.pool.getFreePoolSize());
            }

            @Override
            public void verify() throws Exception {
                // �t���[�v�[���̃R�l�N�V�����ƃ}�b�`���O�Cmc0���}�b�`�D
                mcf.matchManagedConnections(set1, null, info);
                mcfControl.setReturnValue(mc[0]);
            }
        }.doTest();

        // 3�ԖڂɎ擾�����R�l�N�V�����������[�X (�t���[�v�[����)�D
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.release(mc[1]);
                assertEquals("15", 1, target.pool.getActivePoolSize());
                assertEquals("16", 1, target.pool.getFreePoolSize());
            }

            @Override
            public void verify() throws Exception {
                // �R�l�N�V�����̃N���[���i�b�v
                mc[1].cleanup();
            }
        }.doTest();

        // �v�[�����I���D
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.dispose();
                assertEquals("17", 0, target.pool.getActivePoolSize());
                assertEquals("18", 0, target.pool.getFreePoolSize());
            }

            @Override
            public void verify() throws Exception {
                // �A�N�e�B�u�v�[���E�t���[�v�[���̃R�l�N�V�����Ƃ������[�X�����D
                policy.release(mc[0]);
                policy.release(mc[1]);
                policy.dispose();
            }
        }.doTest();
    }

    /**
     * �v�[������Ă���R�l�N�V������maxPoolSize�ɒB���Ă���ꍇ��
     * �t���[�v�[���̃R�l�N�V�����Ƀ}�b�`���Ȃ��R�l�N�V�������擾����ꍇ�̃e�X�g�D <br>
     * �t���[�v�[���̃R�l�N�V������j�����ĐV�����R�l�N�V�������擾����D
     * 
     */
    public void testAcquireNew() throws Exception {
        createTarget();
        target.setMinPoolSize(1);
        target.setMaxPoolSize(2);
        target.initialize(mcf, policy);

        // �ŏ��̃R�l�N�V�����擾�D
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.allocate(context[0]);
            }

            @Override
            public void verify() throws Exception {
                // �㑱��policy����R�l�N�V�������擾�Cmc0���Ԃ����D
                policy.allocate(context[0]);
                policyControl.setMatcher(new ConnectionManagementContextMatcher(mc[0], null));
            }
        }.doTest();

        // 2�Ԗڂ̃R�l�N�V�����擾�D
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.allocate(context[1]);
            }

            @Override
            public void verify() throws Exception {
                // �㑱��policy����R�l�N�V�������擾�Cmc1���Ԃ����D
                policy.allocate(context[1]);
                policyControl.setMatcher(new ConnectionManagementContextMatcher(mc[1], null));
            }
        }.doTest();

        // �擾�����R�l�N�V�����𗼕��Ƃ������[�X (�t���[�v�[����)�D
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
                // �R�l�N�V�����̃N���[���i�b�v
                mc[0].cleanup();
                mc[1].cleanup();
            }
        }.doTest();

        // 3�Ԗڂ̃R�l�N�V�����擾 (�ŏ��̃R�l�N�V�������v�[������j������)�D
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.allocate(context[2].setManagedConnection(null));
                assertEquals("3", 1, target.pool.getActivePoolSize());
                assertEquals("4", 1, target.pool.getFreePoolSize());
            }

            @Override
            public void verify() throws Exception {
                // �t���[�v�[���̃R�l�N�V�����ƃ}�b�`���O�C�ǂ���}�b�`���Ȃ��D
                mcf.matchManagedConnections(set2, null, info);
                mcfControl.setReturnValue(null);
                // �t���[�v�[���̃R�l�N�V������������[�X�����D
                policy.release(mc[0]);
                // �㑱��policy����V�����R�l�N�V�������擾�D
                policy.allocate(context[2]);
                policyControl.setMatcher(new ConnectionManagementContextMatcher(mc[2], null));
            }
        }.doTest();

        // �v�[�����I���D
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.dispose();
                assertEquals("5", 0, target.pool.getActivePoolSize());
                assertEquals("6", 0, target.pool.getFreePoolSize());
            }

            @Override
            public void verify() throws Exception {
                // �A�N�e�B�u�v�[���E�t���[�v�[���̃R�l�N�V�����Ƃ������[�X�����D
                policy.release(mc[2]);
                policy.release(mc[1]);
                policy.dispose();
            }
        }.doTest();
    }

    /**
     * �A�N�e�B�u�v�[����maxPoolSize�ɒB���Ă���ꍇ�̃e�X�g�D <br>
     * �A�N�e�B�u�ȃR�l�N�V�������t���[�v�[���ɖ߂����܂őҋ@����D
     * 
     */
    public void testAcquireFromFull() throws Exception {
        createTarget();
        target.setMinPoolSize(1);
        target.setMaxPoolSize(2);
        target.initialize(mcf, policy);

        // �R�l�N�V�����������[�X����X���b�h�D
        Thread bg = new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                    assertEquals("0", 0, status);
                    ++status;
                    // �R�l�N�V�����������[�X (�t���[�v�[����)�D
                    target.release(mc[0]);
                } catch (Exception ignore) {
                }
            }
        };
        bg.start();

        // ������� (maxPoolSize���̃R�l�N�V�������A�N�e�B�u)�D
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

        // �R�l�N�V�������擾 (�t���[�v�[�����󂭂܂őҋ@����)�D
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
                // �R�l�N�V�����̃N���[���i�b�v
                mc[0].cleanup();
                // �t���[�v�[���̃R�l�N�V�����ƃ}�b�`���O�Cmc0���Ԃ����D
                mcf.matchManagedConnections(set1, null, info);
                mcfControl.setReturnValue(mc[0]);
            }
        }.doTest();
    }

    /**
     * �t���[�v�[���̃R�l�N�V�������^�C���A�E�g���Ĕj�������ꍇ�̃e�X�g�D
     * 
     */
    public void testExpire() throws Exception {
        createTarget();
        target.setMinPoolSize(1);
        target.setMaxPoolSize(2);
        target.setTimeout(3);
        target.initialize(mcf, policy);

        // �ŏ��̃R�l�N�V�����擾�D
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.allocate(context[0]);
            }

            @Override
            public void verify() throws Exception {
                // �㑱��policy����R�l�N�V�������擾�Cmc0���Ԃ����D
                policy.allocate(context[0]);
                policyControl.setMatcher(new ConnectionManagementContextMatcher(mc[0], null));
            }
        }.doTest();

        // 2�Ԗڂ̃R�l�N�V�����擾�D
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.allocate(context[1]);
            }

            @Override
            public void verify() throws Exception {
                // �㑱��policy����R�l�N�V�������擾�Cmc1���Ԃ����D
                policy.allocate(context[1]);
                policyControl.setMatcher(new ConnectionManagementContextMatcher(mc[1], null));
            }
        }.doTest();

        // �R�l�N�V��������Ƃ��N���[�Y����邪�����ɂ�policy.release()�͌Ă΂�Ȃ��D
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.release(mc[1]);
                // ���Ԃ��󂯂ă����[�X���邽�ߑҋ@�D
                Thread.sleep(1 * 1000);
                target.release(mc[0]);
                // mc0�Cmc1�Ƃ��t���[�v�[���Ƀv�[�����O����Ă��邱�Ƃ��m�F�D
                assertEquals("1", 0, target.pool.getActivePoolSize());
                assertEquals("2", 2, target.pool.getFreePoolSize());
            }

            @Override
            public void verify() throws Exception {
                // �R�l�N�V�����̃N���[���i�b�v
                mc[0].cleanup();
                mc[1].cleanup();
            }
        }.doTest();

        // �^�C���A�E�g���钼�O�܂őҋ@�D
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                Thread.sleep(1 * 1000);
                // �܂�mc0�Cmc1�Ƃ��t���[�v�[���Ƀv�[�����O����Ă��邱�Ƃ��m�F�D
                assertEquals("3", 0, target.pool.getActivePoolSize());
                assertEquals("4", 2, target.pool.getFreePoolSize());
            }
        }.doTest();

        // �R�l�N�V�������^�C���A�E�g���Đ�ɃN���[�Y���Ă΂ꂽmc1�������[�X�����D
        // �������v�[����minPoolSize��1�ł��邽��mc0�̓����[�X����Ȃ��D
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                // �R�l�N�V�������^�C���A�E�g����܂őҋ@�D
                Thread.sleep(2 * 1000);
                // minPoolSize��1�Ȃ̂Ń����[�X���ꂽ�R�l�N�V������������ł��邱�Ƃ��m�F�D
                assertEquals("5", 0, target.pool.getActivePoolSize());
                assertEquals("6", 1, target.pool.getFreePoolSize());
            }

            @Override
            public void verify() throws Exception {
                // �R�l�N�V�����������[�X�����D
                policy.release(mc[1]);
            }
        }.doTest();

        // �㏈���D
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.dispose();
                assertEquals("7", 0, target.pool.getActivePoolSize());
                assertEquals("8", 0, target.pool.getFreePoolSize());
            }

            @Override
            public void verify() throws Exception {
                // �v�[���̏I���ɂ��mc0�������[�X�����D
                policy.release(mc[0]);
                policy.dispose();
            }
        }.doTest();
    }
}
