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
package org.seasar.jca.unit;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.easymock.MockControl;

/**
 * @author koichik
 */
public abstract class EasyMockTestCase extends TestCase {
    private List<MockControl> mockControls = new ArrayList<MockControl>();

    public EasyMockTestCase() {
    }

    public EasyMockTestCase(String name) {
        super(name);
    }

    protected MockControl createControl(final Class clazz) {
        final MockControl control = MockControl.createControl(clazz);
        mockControls.add(control);
        return control;
    }

    protected MockControl createStrictControl(final Class clazz) {
        final MockControl control = MockControl.createStrictControl(clazz);
        mockControls.add(control);
        return control;
    }

    protected void replay() {
        final Iterator it = mockControls.iterator();
        while (it.hasNext()) {
            ((MockControl) it.next()).replay();
        }
    }

    protected void verify() {
        final Iterator it = mockControls.iterator();
        while (it.hasNext()) {
            ((MockControl) it.next()).verify();
        }
    }

    protected void reset() {
        final Iterator it = mockControls.iterator();
        while (it.hasNext()) {
            ((MockControl) it.next()).reset();
        }
    }

    @Override
    public void runBare() throws Throwable {
        mockControls.clear();
        super.runBare();
    }

    protected abstract class Subsequence {
        public void doTest() throws Exception {
            verify();
            EasyMockTestCase.this.replay();
            replay();
            EasyMockTestCase.this.verify();
            EasyMockTestCase.this.reset();
        }

        protected abstract void replay() throws Exception;

        protected void verify() throws Exception {
        }
    }
}
