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
package org.seasar.jca.outbound.support;

import javax.resource.spi.LocalTransaction;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.seasar.extension.jta.xa.XidImpl;
import org.seasar.framework.unit.EasyMockTestCase;

/**
 * @author koichik
 */
public class LocalTransactionXAResourceTest extends EasyMockTestCase {

    LocalTransactionXAResource target;

    LocalTransaction ltx;

    Xid xid;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ltx = createStrictMock(LocalTransaction.class);
        xid = new XidImpl();
        target = new LocalTransactionXAResource(ltx);
    }

    /**
     * @throws Exception
     */
    public void testReadOnly() throws Exception {
        new Subsequence() {

            @Override
            public void replay() throws Exception {
                ltx.begin();
            }

            @Override
            public void record() throws Exception {
                target.setReadOnly(true);
                target.start(xid, XAResource.TMNOFLAGS);
                target.end(xid, XAResource.TMSUCCESS);
                assertEquals("1", XAResource.XA_RDONLY, target.prepare(xid));
            }
        }.doTest();
    }

    /**
     * @throws Exception
     */
    public void testCommit2Phase() throws Exception {
        new Subsequence() {

            @Override
            public void replay() throws Exception {
                target.start(xid, XAResource.TMNOFLAGS);
                target.end(xid, XAResource.TMSUCCESS);
                assertEquals("1", XAResource.XA_OK, target.prepare(xid));
                target.commit(xid, false);
            }

            @Override
            public void record() throws Exception {
                ltx.begin();
                ltx.commit();
            }
        }.doTest();
    }

    /**
     * @throws Exception
     */
    public void testCommit1Phase() throws Exception {
        new Subsequence() {

            @Override
            public void replay() throws Exception {
                target.start(xid, XAResource.TMNOFLAGS);
                target.end(xid, XAResource.TMSUCCESS);
                target.commit(xid, true);
            }

            @Override
            public void record() throws Exception {
                ltx.begin();
                ltx.commit();
            }
        }.doTest();
    }

    /**
     * @throws Exception
     */
    public void testRollback1() throws Exception {
        new Subsequence() {

            @Override
            public void replay() throws Exception {
                target.start(xid, XAResource.TMNOFLAGS);
                target.end(xid, XAResource.TMSUCCESS);
                target.rollback(xid);
            }

            @Override
            public void record() throws Exception {
                ltx.begin();
                ltx.rollback();
            }
        }.doTest();
    }

    /**
     * @throws Exception
     */
    public void testRollback2() throws Exception {
        new Subsequence() {

            @Override
            public void replay() throws Exception {
                target.start(xid, XAResource.TMNOFLAGS);
                target.end(xid, XAResource.TMSUCCESS);
                target.prepare(xid);
                target.rollback(xid);
            }

            @Override
            public void record() throws Exception {
                ltx.begin();
                ltx.rollback();
            }
        }.doTest();
    }

}
