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
package org.seasar.jca.wm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.resource.spi.work.ExecutionContext;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkCompletedException;
import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkListener;
import javax.resource.spi.work.WorkManager;
import javax.resource.spi.work.WorkRejectedException;

import junit.framework.TestCase;

/**
 * @author koichik
 */
public class WorkManagerImplTest extends TestCase {
    protected WorkManagerImpl workManager;
    protected List<String> list = Collections.synchronizedList(new ArrayList<String>());

    public WorkManagerImplTest() {
    }

    public WorkManagerImplTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        workManager = new WorkManagerImpl();
        list.clear();
    }

    @Override
    protected void tearDown() throws Exception {
        workManager.stop();
        workManager = null;
        super.tearDown();
    }

    public void testDoWork() throws Exception {
        workManager.doWork(new TestWork(), WorkManager.INDEFINITE, new ExecutionContext(),
                new TestWorkListener());
        list.add("end");

        assertEquals("1", 5, list.size());
        assertEquals("2", "accepted", list.get(0));
        assertEquals("3", "started", list.get(1));
        assertEquals("4", "run", list.get(2));
        assertEquals("5", "completed", list.get(3));
        assertEquals("6", "end", list.get(4));
    }

    public void testStartWork() throws Exception {
        workManager.startWork(new TestWork(), WorkManager.INDEFINITE, new ExecutionContext(),
                new TestWorkListener());
        list.add("end");
        sleep(200);

        assertEquals("1", 5, list.size());
        assertEquals("2", "accepted", list.get(0));
        assertEquals("3", "started", list.get(1));
        assertEquals("4", "end", list.get(2));
        assertEquals("5", "run", list.get(3));
        assertEquals("6", "completed", list.get(4));
    }

    public void testScheduleWork() throws Exception {
        workManager.scheduleWork(new TestWork(), WorkManager.INDEFINITE, new ExecutionContext(),
                new TestWorkListener());
        list.add("end");
        sleep(200);

        assertEquals("1", 5, list.size());
        assertEquals("2", "accepted", list.get(0));
        assertEquals("3", "end", list.get(1));
        assertEquals("4", "started", list.get(2));
        assertEquals("5", "run", list.get(3));
        assertEquals("6", "completed", list.get(4));
    }

    public void testReject() throws Exception {
        workManager.doWork(new TestWork(), 100, new ExecutionContext(), new TestWorkListener() {
            @Override
            public void workAccepted(WorkEvent event) {
                super.workAccepted(event);
                sleep(200);
            }

            @Override
            public void workRejected(WorkEvent event) {
                super.workRejected(event);
                assertNotNull("A", event.getException());
                assertTrue("B", event.getException() instanceof WorkRejectedException);
            }
        });
        list.add("end");
        sleep(200);

        assertEquals("1", 3, list.size());
        assertEquals("2", "accepted", list.get(0));
        assertEquals("3", "rejected", list.get(1));
        assertEquals("4", "end", list.get(2));
    }

    public void testException() throws Exception {
        workManager.doWork(new TestWork() {
            @Override
            public void run() {
                super.run();
                throw new RuntimeException();
            }
        }, 100, new ExecutionContext(), new TestWorkListener() {
            @Override
            public void workCompleted(WorkEvent event) {
                super.workCompleted(event);
                assertNotNull("A", event.getException());
                assertTrue("B", event.getException() instanceof WorkCompletedException);
                assertTrue("C", event.getException().getCause() instanceof RuntimeException);
            }
        });
        list.add("end");
        sleep(200);

        assertEquals("1", 5, list.size());
        assertEquals("2", "accepted", list.get(0));
        assertEquals("3", "started", list.get(1));
        assertEquals("4", "run", list.get(2));
        assertEquals("5", "completed", list.get(3));
        assertEquals("6", "end", list.get(4));
    }

    protected void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignore) {
        }
    }

    public class TestWork implements Work {
        public void run() {
            sleep(100);
            list.add("run");
        }

        public void release() {
            list.add("release");
        }
    }

    public class TestWorkListener implements WorkListener {
        public void workAccepted(WorkEvent event) {
            list.add("accepted");
        }

        public void workStarted(WorkEvent event) {
            list.add("started");
        }

        public void workRejected(WorkEvent event) {
            list.add("rejected");
        }

        public void workCompleted(WorkEvent event) {
            list.add("completed");
        }
    }
}
