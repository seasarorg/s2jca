/*
 * Copyright 2004-2010 the Seasar Foundation and the Others.
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

/**
 * <p>
 * Sun の JDBC Connector と DriverManager を組み合わせたテスト．
 * </p>
 * 
 * @author koichik
 */
public class SunDmTest extends JdbcTestCase {
    public SunDmTest() {
    }

    public SunDmTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        include("jdbc-sun-dm.dicon");
    }

    public void testTx() throws Exception {
        exec1();
    }

    public void test() throws Exception {
        exec2();
    }
}
