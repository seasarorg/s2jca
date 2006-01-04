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
     * <code>allowLocalTx</code> ���f�t�H���g( <code>false</code>
     * )�Ńg�����U�N�V�������J�n����Ă��Ȃ��ꍇ�̃e�X�g�D
     * 
     */
    public void testNoTransaction() throws Exception {
        target.initialize(mcf, policy);

        new Subsequence() {
            @Override
            public void replay() throws Exception {
                try {
                    // �R�l�N�V�������擾���悤�Ƃ���Ɨ�O���X���[�����D
                    target.allocate(context[0]);
                    fail("0");
                } catch (ResourceException expected) {
                }
            }

            @Override
            public void verify() throws Exception {
                // �g�����U�N�V�������J�n����Ă��Ȃ��D
                tm.getTransaction();
                tmControl.setReturnValue(null);
            }
        }.doTest();
    }

    /**
     * <code>allowLocalTx</code> ���f�t�H���g( <code>false</code>
     * )�Ńg�����U�N�V�������J�n����Ă��Ȃ��ꍇ�̃e�X�g�D
     * 
     */
    public void testNoTransactionAllowLocalTx() throws Exception {
        target.setAllowLocalTx(true);
        target.initialize(mcf, policy);

        // �R�l�N�V�����擾�D
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.allocate(context[0]);
            }

            @Override
            public void verify() throws Exception {
                // �g�����U�N�V�������J�n����Ă��Ȃ��D
                tm.getTransaction();
                tmControl.setReturnValue(null);
                // allowLocalTx���ݒ肳��Ă���̂ł���ł��R�l�N�V�������擾�����D
                policy.allocate(context[0]);
                policyControl.setMatcher(new ConnectionManagementContextMatcher(mc[0], null));
            }
        }.doTest();

        // �R�l�N�V�����������[�X�D
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.release(mc[0]);
            }

            @Override
            public void verify() throws Exception {
                // �g�����U�N�V�������J�n����Ă��Ȃ��D
                tm.getTransaction();
                tmControl.setReturnValue(null);
                // �R�l�N�V�����������[�X�����D
                policy.release(mc[0]);
            }
        }.doTest();
    }

    /**
     * ��ʓI�ȏꍇ�̃e�X�g�D
     * 
     */
    public void testNormal() throws Exception {
        target.initialize(mcf, policy);

        // �ŏ��̃R�l�N�V�����擾�`Transaction�ւ�enlistResource()�CregisterSynchronization()�D
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.allocate(context[0]);
                assertTrue("0", target.pools.containsKey(tx));
            }

            @Override
            public void verify() throws Exception {
                // �g�����U�N�V�������J�n����Ă���D
                tm.getTransaction();
                tmControl.setReturnValue(tx);
                // �㑱��policy����R�l�N�V�������擾�Cmc0���Ԃ����D
                policy.allocate(context[0]);
                policyControl.setMatcher(new ConnectionManagementContextMatcher(mc[0], null));
                // �R�l�N�V��������XAResource���擾�����D
                // XAResource�̎擾�O�ɘ_���R�l�N�V�����n���h���̎擾 (mc.getConnection) ���s���K�v�͂Ȃ��D
                // �uJ2EE Connector Architecture Specification Version
                // 1.5�vP7-25�Q�ƁD
                // ������Sun JDBC Connector + Oracle XAConnection (10.1.0.2)�ł�
                // ��O���X���[�����D
                // Oracle XAConnection(10.1.0.4)�ł͏C������Ă���D
                mc[0].getXAResource();
                mcControl[0].setReturnValue(xa);
                // XAResource��Transaction�ɓo�^�����D
                tx.enlistResource(xa);
                txControl.setReturnValue(true);
                // Transaction��Synchronization�Ƃ��ă^�[�Q�b�g���o�^�����D
                tx.registerSynchronization(target);
            }
        }.doTest();

        // �ŏ��̃R�l�N�V�������N���[�Y (�t���[�v�[����)�D
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.release(mc[0]);
            }

            @Override
            public void verify() throws Exception {
                // �g�����U�N�V�������J�n����Ă���D
                tm.getTransaction();
                tmControl.setReturnValue(tx);
                // �����[�X����Ȃ��D
            }
        }.doTest();

        // 2�Ԗڂ̃R�l�N�V�����擾�`Transaction�ւ�enlistResource()�D
        // registerSynchronization()�͌Ă΂�Ȃ����ƁD
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.allocate(context[1]);
            }

            @Override
            public void verify() throws Exception {
                // �g�����U�N�V�������J�n����Ă���D
                tm.getTransaction();
                tmControl.setReturnValue(tx);
                // �t���[�v�[���̃R�l�N�V�����ƃ}�b�`���O�C�ǂ�Ƃ��}�b�`���Ȃ��D
                mcf.matchManagedConnections(set1, null, info);
                mcfControl.setReturnValue(null);
                // �㑱��policy����R�l�N�V�����擾�D
                policy.allocate(context[1]);
                policyControl.setMatcher(new ConnectionManagementContextMatcher(mc[1], null));
                // �R�l�N�V��������XAResource���擾�����D
                mc[1].getXAResource();
                mcControl[1].setReturnValue(xa);
                // XAResource��Transaction�ɓo�^�����D
                tx.enlistResource(xa);
                txControl.setReturnValue(true);
                // Synchronization�̓o�^�͍s���Ȃ��D
            }
        }.doTest();

        // 2�Ԗڂ̃R�l�N�V�������N���[�Y (�t���[�v�[����)�D
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.release(mc[1]);
            }

            @Override
            public void verify() throws Exception {
                // �g�����U�N�V�������J�n����Ă���D
                tm.getTransaction();
                tmControl.setReturnValue(tx);
                // �����[�X����Ȃ��D
            }
        }.doTest();

        // �O�Ԗڂ̃R�l�N�V�����擾 (�ŏ��̃R�l�N�V�����ƃ}�b�`)�D
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.allocate(context[0]);
            }

            @Override
            public void verify() throws Exception {
                // �g�����U�N�V�������J�n����Ă���D
                tm.getTransaction();
                tmControl.setReturnValue(tx);
                // �t���[�v�[���̃R�l�N�V�����ƃ}�b�`���O�Cmc0���Ԃ����D
                mcf.matchManagedConnections(set2, null, info);
                mcfControl.setReturnValue(mc[0]);
            }
        }.doTest();

        // 3�Ԗڂ̃R�l�N�V�������N���[�Y (�t���[�v�[����)�D
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.release(mc[0]);
            }

            @Override
            public void verify() throws Exception {
                // �g�����U�N�V�������J�n����Ă���D
                tm.getTransaction();
                tmControl.setReturnValue(tx);
                // �����[�X����Ȃ��D
            }
        }.doTest();

        // �g�����U�N�V�����R�~�b�g (�����ŃR�l�N�V�����������[�X����邱�Ƃ��m�F)�D
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.afterCompletion(Status.STATUS_COMMITTED);
            }

            @Override
            public void verify() throws Exception {
                // �g�����U�N�V�������J�n����Ă���D
                tm.getTransaction();
                tmControl.setReturnValue(tx);
                // �㑱��policy��mc1�������[�X�����D
                policy.release(mc[1]);
                // �㑱��policy��mc0�������[�X�����D
                policy.release(mc[0]);
            }
        }.doTest();
    }

    /**
     * �g�����U�N�V���������[���o�b�N�����ꍇ�̃e�X�g�D
     * 
     */
    public void testErrorWithTransaction() throws Exception {
        target.initialize(mcf, policy);

        // �ŏ��̃R�l�N�V�����擾�`Transaction�ւ�enlistResource()�CregisterSynchronization()�D
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.allocate(context[0]);
                assertTrue("0", target.pools.containsKey(tx));
            }

            @Override
            public void verify() throws Exception {
                // �g�����U�N�V�������J�n����Ă���D
                tm.getTransaction();
                tmControl.setReturnValue(tx);
                // �㑱��policy����R�l�N�V�������擾�Cmc0���Ԃ����D
                policy.allocate(context[0]);
                policyControl.setMatcher(new ConnectionManagementContextMatcher(mc[0], null));
                // �R�l�N�V��������XAResource���擾�����D
                mc[0].getXAResource();
                mcControl[0].setReturnValue(xa);
                // XAResource��Transaction�ɓo�^�����D
                tx.enlistResource(xa);
                txControl.setReturnValue(true);
                // Transaction��Synchronization�Ƃ��ă^�[�Q�b�g���o�^�����D
                tx.registerSynchronization(target);
            }
        }.doTest();

        // �ŏ��̃R�l�N�V�������N���[�Y (�t���[�v�[����)�D
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.release(mc[0]);
            }

            @Override
            public void verify() throws Exception {
                // �g�����U�N�V�������J�n����Ă���D
                tm.getTransaction();
                tmControl.setReturnValue(tx);
                // �����[�X����Ȃ��D
            }
        }.doTest();

        // 2�Ԗڂ̃R�l�N�V�����擾�`Transaction�ւ�enlistResource()�D
        // registerSynchronization()�͌Ă΂�Ȃ����ƁD
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.allocate(context[1]);
            }

            @Override
            public void verify() throws Exception {
                // �g�����U�N�V�������J�n����Ă���D
                tm.getTransaction();
                tmControl.setReturnValue(tx);
                // �t���[�v�[���̃R�l�N�V�������}�b�`���O�C�ǂ�Ƃ��}�b�`���Ȃ��D
                mcf.matchManagedConnections(set1, null, info);
                mcfControl.setReturnValue(null);
                // �㑱��policy����R�l�N�V�����擾�Cmc1���Ԃ����D
                policy.allocate(context[1]);
                policyControl.setMatcher(new ConnectionManagementContextMatcher(mc[1], null));
                // �R�l�N�V��������XAResource���擾�����D
                mc[1].getXAResource();
                mcControl[1].setReturnValue(xa);
                // XAResouce��Transaction�ɓo�^�����D
                tx.enlistResource(xa);
                txControl.setReturnValue(true);
                // Synchronization�̓o�^�͍s���Ȃ��D
            }
        }.doTest();

        // 2�Ԗڂ̃R�l�N�V�������N���[�Y (�t���[�v�[����)�D
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.release(mc[1]);
            }

            @Override
            public void verify() throws Exception {
                // �g�����U�N�V�������J�n����Ă���D
                tm.getTransaction();
                tmControl.setReturnValue(tx);
                // �����[�X����Ȃ��D
            }
        }.doTest();

        // �ŏ��Ɏ擾�����R�l�N�V�����ɃG���[�����D
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.connectionErrorOccurred(mc[0]);
            }

            @Override
            public void verify() throws Exception {
                // �g�����U�N�V�������J�n����Ă���D
                tm.getTransaction();
                tmControl.setReturnValue(tx);
                // �㑱��policy�ɃG���[���ʒm�����D
                policy.connectionErrorOccurred(mc[0]);
            }
        }.doTest();

        // �g�����U�N�V�������[���o�b�N (��Ԗڂ̃R�l�N�V�����̂݃t���[�v�[����)�D
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.afterCompletion(Status.STATUS_ROLLEDBACK);
            }

            @Override
            public void verify() throws Exception {
                // �g�����U�N�V�������J�n����Ă���D
                tm.getTransaction();
                tmControl.setReturnValue(tx);
                // �R�l�N�V�����������[�X�����D
                policy.release(mc[1]);
            }
        }.doTest();
    }
}
