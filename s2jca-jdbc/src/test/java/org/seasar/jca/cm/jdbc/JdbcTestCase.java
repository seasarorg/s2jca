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
package org.seasar.jca.cm.jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.sql.DataSource;
import javax.transaction.TransactionManager;

import org.seasar.extension.unit.S2TestCase;

/**
 * @author koichik
 */
public abstract class JdbcTestCase extends S2TestCase {
    protected TransactionManager tm;
    protected DataSource ds;

    public JdbcTestCase() {
    }

    public JdbcTestCase(String name) {
        super(name);
    }

    protected void exec1() throws Exception {
        query();
    }

    protected void exec2() throws Exception {
        tm.begin();
        try {
            query();
        } finally {
            tm.rollback();
        }

        tm.begin();
        try {
            query();
        } finally {
            tm.rollback();
        }
    }

    protected void query() throws Exception {
        Connection con = ds.getConnection();
        try {
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery("select ENAME from EMP");
            while (rs.next()) {
                System.out.println(rs.getString("ENAME"));
            }
        } finally {
            con.close();
        }
    }
}
