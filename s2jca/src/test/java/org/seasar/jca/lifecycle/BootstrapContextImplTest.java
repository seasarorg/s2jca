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
package org.seasar.jca.lifecycle;

import java.util.Timer;

import javax.resource.spi.BootstrapContext;

import junit.framework.TestCase;

/**
 * @author koichik
 */
public class BootstrapContextImplTest extends TestCase {

    BootstrapContext target;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        target = new BootstrapContextImpl();
    }

    /**
     * @throws Exception
     */
    public void testTimer() throws Exception {
        Timer tm1 = target.createTimer();
        assertNotNull("0", tm1);

        Timer tm2 = target.createTimer();
        assertNotNull("1", tm1);
        assertNotSame("2", tm1, tm2);
    }

}
