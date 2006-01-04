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

import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;

import org.easymock.MockControl;
import org.seasar.jca.cm.support.ConnectionManagementContext;
import org.seasar.jca.cm.support.ConnectionManagementContextMatcher;
import org.seasar.jca.cm.support.ManagedConnectionPool;
import org.seasar.jca.ut.EasyMockTestCase;

/**
 * @author koichik
 */
public class ThreadBoundedPoolingPolicyTest extends EasyMockTestCase {
    private ThreadBoundedPoolingPolicy target;
    private ConnectionManagementPolicy policy;
    private MockControl policyControl;
    private ManagedConnectionFactory mcf;
    private MockControl mcfControl;
    private ManagedConnection[] mc = new ManagedConnection[3];
    private MockControl[] mcControl = new MockControl[3];
    private ConnectionRequestInfo info;
    private MockControl infoControl;
    private Object[] lch = new Object[3];
    private ConnectionManagementContext[] context = new ConnectionManagementContext[3];
    private Set<ManagedConnection> set1;
    private Set<ManagedConnection> set2;

    public ThreadBoundedPoolingPolicyTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        policyControl = createStrictControl(ConnectionManagementPolicy.class);
        policy = (ConnectionManagementPolicy) policyControl.getMock();
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

        set1 = new HashSet<ManagedConnection>();
        set1.add(mc[0]);
        set2 = new HashSet<ManagedConnection>();
        set2.add(mc[0]);
        set2.add(mc[1]);

        target = new ThreadBoundedPoolingPolicy();
    }

    /**
     * <code>MethodInterceptor</code>
     * ��before�ȍ~�Ɏ擾�����R�l�N�V������after�ŉ������邱�Ƃ̋[���I�ȃe�X�g�D <br>
     * 
     */
    public void testAcquireNew() throws Exception {
        target.initialize(mcf, policy);

        // <code>MethodInterceptor</code>��before�ōs�������D
        final Set<ManagedConnection> before = target.before();
        final ManagedConnectionPool<?> pool = target.pools.get();
        assertEquals("0", 0, before.size());
        assertEquals("1", 0, pool.getActivePoolSize());
        assertEquals("2", 0, pool.getFreePoolSize());

        // �R�l�N�V�����擾�D
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.allocate(context[0]);
                assertEquals("3", 1, pool.getActivePoolSize());
                assertEquals("4", 0, pool.getFreePoolSize());
            }

            @Override
            public void verify() throws Exception {
                // �㑱��policy����R�l�N�V�������擾�Cmc0���Ԃ����D
                policy.allocate(context[0]);
                policyControl.setMatcher(new ConnectionManagementContextMatcher(mc[0], null));
            }
        }.doTest();

        // �擾�����R�l�N�V�����̉�� (�t���[�v�[����)�D
        // <code>MethodInvocation</code>��proceed�ōs�������D
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.release(mc[0]);
                assertEquals("5", 0, pool.getActivePoolSize());
                assertEquals("6", 1, pool.getFreePoolSize());
            }
        }.doTest();

        // <code>MethodInterceptor</code>��after�ōs�������D
        // �ŏ��Ɏ擾�����R�l�N�V�����������[�X�����D
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.after(before);
                assertEquals("7", 0, pool.getActivePoolSize());
                assertEquals("8", 0, pool.getFreePoolSize());
            }

            @Override
            public void verify() throws Exception {
                // �t���[�v�[���̃R�l�N�V�������㑱��policy�֓n�����D
                policy.release(mc[0]);
            }
        }.doTest();
    }

    /**
     * �R�l�N�V�����̎擾�����s��ꂽ�ꍇ�ŁC��ԖڂɎ擾���悤�Ƃ����R�l�N�V�������ŏ��̃R�l�N�V�����Ɠ����ꍇ�̃e�X�g�D <br>
     * �A�X�y�N�g���K�p���ꂽ���\�b�h���l�X�g�����ꍇ�ɁC���ۂɃR�l�N�V�������擾���Ȃ������ꍇ�ɂ̓R�l�N�V�������������Ȃ����Ƃ��m�F����D
     * 
     */
    public void testAcquireSame() throws Exception {
        target.initialize(mcf, policy);

        // �O���̃��\�b�h�ɂ�����<code>MethodInterceptor</code>��before�ōs���鏈���D
        final Set<ManagedConnection> before1 = target.before();
        final ManagedConnectionPool<?> pool = target.pools.get();
        assertEquals("0", 0, before1.size());
        assertEquals("1", 0, pool.getActivePoolSize());
        assertEquals("2", 0, pool.getFreePoolSize());

        // �ŏ��̃R�l�N�V�����擾�D
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.allocate(context[0]);
                assertEquals("3", 1, pool.getActivePoolSize());
                assertEquals("4", 0, pool.getFreePoolSize());
            }

            @Override
            public void verify() throws Exception {
                // �㑱��policy����R�l�N�V�������擾�Cmc0���Ԃ����D
                policy.allocate(context[0]);
                policyControl.setMatcher(new ConnectionManagementContextMatcher(mc[0], null));
            }
        }.doTest();

        // �ŏ��Ɏ擾�����R�l�N�V�������N���[�Y (�t���[�v�[����)�D
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.release(mc[0]);
                assertEquals("5", 0, pool.getActivePoolSize());
                assertEquals("6", 1, pool.getFreePoolSize());
            }
            // �t���[�v�[���ɖ߂���邾���Ȃ̂Ō㑱��policy�͌Ă΂�Ȃ��D
        }.doTest();

        // �����̃��\�b�h�ɂ�����<code>MethodInterceptor</code>��before�ōs���鏈���D
        final Set<ManagedConnection> before2 = target.before();
        assertEquals("7", 1, before2.size());
        assertEquals("8", 0, pool.getActivePoolSize());
        assertEquals("9", 1, pool.getFreePoolSize());

        // ��Ԗڂ̃R�l�N�V�����擾�D
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.allocate(context[0]);
                assertEquals("10", 1, pool.getActivePoolSize());
                assertEquals("11", 0, pool.getFreePoolSize());
            }

            @Override
            public void verify() throws Exception {
                // �t���[�v�[���̃R�l�N�V�����ƃ}�b�`���O�Cmc0���Ԃ����D
                mcf.matchManagedConnections(set1, null, info);
                mcfControl.setReturnValue(mc[0]);
            }
        }.doTest();

        // 2�ԖڂɎ擾�����R�l�N�V�������N���[�Y (�t���[�v�[����)�D
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.release(mc[0]);
                assertEquals("12", 0, pool.getActivePoolSize());
                assertEquals("13", 1, pool.getFreePoolSize());
            }
            // �t���[�v�[���ɖ߂���邾���Ȃ̂Ō㑱��policy�͌Ă΂�Ȃ��D
        }.doTest();

        // �����̃��\�b�h�ɂ�����<code>MethodInterceptor</code>��after�ōs���鏈���D
        // �擾�����R�l�N�V����(�ŏ��Ɏ擾�����R�l�N�V�����Ɠ���)�̓N���[�Y����Ȃ��D
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.after(before2);
                assertEquals("14", 0, pool.getActivePoolSize());
                assertEquals("15", 1, pool.getFreePoolSize());
            }
            // �t���[�v�[���ɖ߂���邾���Ȃ̂Ō㑱��policy�͌Ă΂�Ȃ��D
        }.doTest();

        // �O���̃��\�b�h�ɂ�����<code>MethodInterceptor</code>��after�ōs���鏈���D
        // �ŏ��Ɏ擾�����R�l�N�V�������N���[�Y�D
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.after(before1);
                assertEquals("16", 0, pool.getActivePoolSize());
                assertEquals("17", 0, pool.getFreePoolSize());
            }

            @Override
            public void verify() throws Exception {
                // �R�l�N�V�����������[�X�����D
                policy.release(mc[0]);
            }
        }.doTest();
    }

    /**
     * �R�l�N�V�����̎擾�����s��ꂽ�ꍇ�ŁC��ԖڂɎ擾���悤�Ƃ����R�l�N�V�������ŏ��̃R�l�N�V�����Ƃ͈قȂ�ꍇ�̃e�X�g�D <br>
     * �A�X�y�N�g���K�p���ꂽ���\�b�h���l�X�g�����ꍇ�ɁC���ꂼ�ꂪ���ۂɎ擾�����R�l�N�V������������邱�Ƃ��m�F�D
     * 
     * @throws Exception
     */
    public void testAcquireDiff() throws Exception {
        target.initialize(mcf, policy);

        // �O���̃��\�b�h�ɂ�����<code>MethodInterceptor</code>��before�ōs���鏈���D
        final Set<ManagedConnection> before1 = target.before();
        final ManagedConnectionPool<?> pool = target.pools.get();
        assertEquals("0", 0, before1.size());
        assertEquals("1", 0, pool.getActivePoolSize());
        assertEquals("2", 0, pool.getFreePoolSize());

        // �ŏ��̃R�l�N�V�����擾�D
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.allocate(context[0]);
                assertEquals("3", 1, pool.getActivePoolSize());
                assertEquals("4", 0, pool.getFreePoolSize());
            }

            @Override
            public void verify() throws Exception {
                // �㑱��policy����R�l�N�V�������擾�Cmc0���Ԃ����D
                policy.allocate(context[0]);
                policyControl.setMatcher(new ConnectionManagementContextMatcher(mc[0], null));
            }
        }.doTest();

        // �ŏ��Ɏ擾�����R�l�N�V�������N���[�Y (�t���[�v�[����)�D
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.release(mc[0]);
                assertEquals("5", 0, pool.getActivePoolSize());
                assertEquals("6", 1, pool.getFreePoolSize());
            }
            // �t���[�v�[���ɖ߂���邾���Ȃ̂Ō㑱��policy�͌Ă΂�Ȃ��D
        }.doTest();

        // �����̃��\�b�h�ɂ�����<code>MethodInterceptor</code>��before�ōs���鏈���D
        final Set<ManagedConnection> before2 = target.before();
        assertEquals("7", 1, before2.size());
        assertEquals("8", 0, pool.getActivePoolSize());
        assertEquals("9", 1, pool.getFreePoolSize());

        // ��Ԗڂ̃R�l�N�V�����擾�D
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.allocate(context[1]);
                assertEquals("10", 1, pool.getActivePoolSize());
                assertEquals("11", 1, pool.getActivePoolSize());
            }

            @Override
            public void verify() throws Exception {
                // �t���[�v�[���̃R�l�N�V�����ƃ}�b�`���O�C�ǂ���}�b�`���Ȃ��D
                mcf.matchManagedConnections(set1, null, info);
                mcfControl.setReturnValue(null);
                // �㑱��policy����V�����R�l�N�V�������擾�D
                policy.allocate(context[1]);
                policyControl.setMatcher(new ConnectionManagementContextMatcher(mc[1], null));
            }
        }.doTest();

        // �擾�����R�l�N�V�������N���[�Y (�t���[�v�[����)�D
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.release(mc[1]);
                assertEquals("12", 0, pool.getActivePoolSize());
                assertEquals("13", 2, pool.getFreePoolSize());
            }
            // �t���[�v�[���ɖ߂���邾���Ȃ̂Ō㑱��policy�͌Ă΂�Ȃ��D
        }.doTest();

        // �����̃��\�b�h�ɂ�����<code>MethodInterceptor</code>��after�ōs���鏈���D
        // 2�ԖڂɎ擾�����R�l�N�V����(���N���[�Y����D
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.after(before2);
                assertEquals("16", 0, pool.getActivePoolSize());
                assertEquals("17", 1, pool.getFreePoolSize());
            }

            @Override
            public void verify() throws Exception {
                // �㑱��policy�ɃR�l�N�V�����������[�X�����D
                policy.release(mc[1]);
            }
        }.doTest();

        // �O���̃��\�b�h�ɂ�����<code>MethodInterceptor</code>��after�ōs���鏈���D
        // �ŏ��Ɏ擾�����R�l�N�V�������N���[�Y�D
        new Subsequence() {
            @Override
            public void replay() throws Exception {
                target.after(before1);
                assertEquals("18", 0, pool.getActivePoolSize());
                assertEquals("19", 0, pool.getFreePoolSize());
            }

            @Override
            public void verify() throws Exception {
                // �㑱��policy�ɃR�l�N�V�����������[�X�����D
                policy.release(mc[0]);
            }
        }.doTest();
    }
}
