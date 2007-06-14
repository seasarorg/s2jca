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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Timer;

import javax.resource.spi.BootstrapContext;
import javax.resource.spi.UnavailableException;
import javax.resource.spi.XATerminator;
import javax.resource.spi.work.WorkManager;

import org.seasar.framework.container.annotation.tiger.Binding;
import org.seasar.framework.container.annotation.tiger.BindingType;
import org.seasar.jca.work.WorkManagerImpl;

/**
 * {@link BootstrapContext}の実装クラスです．
 * 
 * @author koichik
 */
public class BootstrapContextImpl implements BootstrapContext {

    // instance fields
    /** ワークマネージャ */
    protected WorkManager workManager;

    /**
     * インスタンスを構築します．
     * <p>
     * このコンストラクタで生成したインスタンスは， {@link #setWorkManager(WorkManager)}で
     * ワークマネージャを設定しなくてはなりません．
     * </p>
     */
    public BootstrapContextImpl() {
    }

    /**
     * 指定されたスレッド数を持つ{@link WorkManagerImpl}を使用してインスタンスを構築します．
     * 
     * @param numThreads
     *            スレッドプールのスレッド数
     */
    public BootstrapContextImpl(final int numThreads) {
        this(new WorkManagerImpl(numThreads));
    }

    /**
     * インスタンスを構築します．
     * 
     * @param workManager
     *            ワークマネージャ
     */
    public BootstrapContextImpl(WorkManager workManager) {
        this.workManager = workManager;
    }

    public Timer createTimer() throws UnavailableException {
        return AccessController.doPrivileged(new PrivilegedAction<Timer>() {

            public Timer run() {
                return new Timer(true);
            }
        });
    }

    public WorkManager getWorkManager() {
        return workManager;
    }

    public XATerminator getXATerminator() {
        throw new UnsupportedOperationException();
    }

    /**
     * ワークマネージャを設定します．
     * 
     * @param workManager
     *            ワークマネージャ
     */
    @Binding(bindingType = BindingType.MAY)
    public void setWorkManager(final WorkManager workManager) {
        this.workManager = workManager;
    }

}
