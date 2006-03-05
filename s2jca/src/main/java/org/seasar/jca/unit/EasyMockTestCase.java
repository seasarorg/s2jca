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
import java.util.List;

import junit.framework.TestCase;

import org.easymock.EasyMock;

/**
 * @author koichik
 */
public abstract class EasyMockTestCase extends TestCase {
    // instance fields
    private List<Object> mocks = new ArrayList<Object>();

    public EasyMockTestCase() {
    }

    public EasyMockTestCase(String name) {
        super(name);
    }

    protected <T> T createMock(final Class<T> clazz) {
        final T mock = EasyMock.createMock(clazz);
        mocks.add(mock);
        return mock;
    }

    protected <T> T createNiceMock(final Class<T> clazz) {
        final T mock = EasyMock.createNiceMock(clazz);
        mocks.add(mock);
        return mock;
    }

    protected <T> T createStrictMock(final Class<T> clazz) {
        final T mock = EasyMock.createStrictMock(clazz);
        mocks.add(mock);
        return mock;
    }

    protected void replay() {
        for (final Object mock : mocks) {
            EasyMock.replay(mock);
        }
    }

    protected void verify() {
        for (final Object mock : mocks) {
            EasyMock.verify(mock);
        }
    }

    protected void reset() {
        for (final Object mock : mocks) {
            EasyMock.reset(mock);
        }
    }

    @Override
    public void runBare() throws Throwable {
        mocks.clear();
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
