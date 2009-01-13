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
package org.seasar.jca.outbound.support;

import javax.resource.spi.ManagedConnection;

import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;

/**
 * <p>
 * {@link ConnectionManagementContext}をプロパティの同値性で比較する
 * <code>org.easymock.ArgumentsMatcher</code> の実装．
 * </p>
 * <p>
 * 比較が同値であった場合には， <strong>副作用 </strong>として <code>managedConnection</code> および
 * <code>logicalConnectionHandle</code> プロパティの値をコンストラクタで渡された値に置換する． <br>
 * これは EasyMock が提供するモックのラッパーとして使用することで，戻り値以外の振る舞いを制御したい場合に使用する．
 * </p>
 * 
 * @author koichik
 */
public class ConnectionManagementContextMatcher implements IArgumentMatcher {

    ConnectionManagementContext expected;

    ManagedConnection mc;

    Object lch;

    /**
     * @param expected
     * @param mc
     * @param lch
     * @return コネクション管理コンテキスト
     */
    public static ConnectionManagementContext eqContext(final ConnectionManagementContext expected,
            final ManagedConnection mc, final Object lch) {
        EasyMock.reportMatcher(new ConnectionManagementContextMatcher(expected, mc, lch));
        return null;
    }

    /**
     * @param expected
     * @param mc
     * @param lch
     */
    public ConnectionManagementContextMatcher(final ConnectionManagementContext expected,
            final ManagedConnection mc, final Object lch) {
        this.expected = expected;
        this.mc = mc;
        this.lch = lch;
    }

    public boolean matches(final Object actual) {
        if (!(actual instanceof ConnectionManagementContext)) {
            return false;
        }
        final ConnectionManagementContext context = ConnectionManagementContext.class.cast(actual);

        boolean result = equals(expected.getSubject(), context.getSubject())
                && equals(expected.getRequestInfo(), context.getRequestInfo())
                && equals(expected.getManagedConnectionFactory(), context
                        .getManagedConnectionFactory())
                && equals(expected.getManagedConnection(), context.getManagedConnection())
                && equals(expected.getLogicalConnectionHandle(), context
                        .getLogicalConnectionHandle());
        context.setManagedConnection(mc);
        context.setLogicalConnectionHandle(lch);
        return result;
    }

    /**
     * @param expected
     * @param actual
     * @return <code>expected</code>と<code>actual</code>が同値なら<code>true</code>
     */
    protected boolean equals(final Object expected, final Object actual) {
        if (expected == actual) {
            return true;
        }
        if (expected == null ^ actual == null) {
            return false;
        }
        return expected != null && expected.equals(actual);
    }

    public void appendTo(final StringBuffer buf) {
        buf.append("ConnectionManagementMatcher");
    }

}
