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
package org.seasar.jca.outbound.support;

import javax.resource.spi.ManagedConnection;

import org.easymock.ArgumentsMatcher;

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
public class ConnectionManagementContextMatcher implements ArgumentsMatcher {
    protected boolean sideEffect;
    protected ManagedConnection mc;
    protected Object lch;

    public ConnectionManagementContextMatcher() {
    }

    public ConnectionManagementContextMatcher(final ManagedConnection mc, final Object lch) {
        this.sideEffect = true;
        this.mc = mc;
        this.lch = lch;
    }

    public boolean matches(final Object[] arg0, final Object[] arg1) {
        final ConnectionManagementContext expected = (ConnectionManagementContext) arg0[0];
        final ConnectionManagementContext actual = (ConnectionManagementContext) arg1[0];

        boolean result = equals(expected.getSubject(), actual.getSubject())
                && equals(expected.getRequestInfo(), actual.getRequestInfo())
                && equals(expected.getManagedConnectionFactory(), actual
                        .getManagedConnectionFactory())
                && equals(expected.getManagedConnection(), actual.getManagedConnection())
                && equals(expected.getLogicalConnectionHandle(), actual
                        .getLogicalConnectionHandle());
        if (result && sideEffect) {
            actual.setManagedConnection(mc);
            actual.setLogicalConnectionHandle(lch);
        }
        return result;
    }

    protected boolean equals(final Object expected, final Object actual) {
        if (expected == actual) {
            return true;
        }
        if (expected == null ^ actual == null) {
            return false;
        }
        return expected != null && expected.equals(actual);
    }

    public String toString(Object[] arg0) {
        return arg0[0].toString();
    }
}
