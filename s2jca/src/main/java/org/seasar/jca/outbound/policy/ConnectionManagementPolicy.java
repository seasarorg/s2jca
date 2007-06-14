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
package org.seasar.jca.outbound.policy;

import javax.resource.ResourceException;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;

import org.seasar.jca.outbound.support.ConnectionManagementContext;

/**
 * コネクション管理ポリシーのインタフェースです．
 * <p>
 * コネクション管理ポリシーはチェーン状に連結することができます．
 * </p>
 * 
 * @author koichik
 */
public interface ConnectionManagementPolicy {

    /**
     * コネクション管理ポリシーを初期化します．
     * 
     * @param mcf
     *            マネージドコネクションファクトリ
     * @param nextPolicy
     *            後続のポリシー
     * @throws ResourceException
     *             コネクション管理ポリシーの初期渦中に例外が発生した場合
     */
    void initialize(ManagedConnectionFactory mcf, ConnectionManagementPolicy nextPolicy)
            throws ResourceException;

    /**
     * コネクションを割り当てます．
     * <p>
     * 割り当てられたコネクションは{@link ConnectionManagementContext}に設定されます．
     * </p>
     * 
     * @param context
     *            コネクション管理コンテキスト
     * @throws ResourceException
     *             コネクションの割り当て中に例外が発生した場合
     */
    void allocate(ConnectionManagementContext context) throws ResourceException;

    /**
     * コネクションを解放します．
     * 
     * @param mc
     *            マネージドコネクション
     * @throws ResourceException
     *             コネクションの解放中に例外が発生した場合
     */
    void release(ManagedConnection mc) throws ResourceException;

    /**
     * コネクションエラーが発生した場合に呼び出されます．
     * <p>
     * エラーが発生したコネクションをキャッシュしているポリシーはコネクションを破棄します．
     * </p>
     * 
     * @param mc
     *            エラーが発生したマネージドコネクション
     * @throws ResourceException
     *             処理中にエラーが発生した場合
     */
    void connectionErrorOccurred(ManagedConnection mc) throws ResourceException;

    /**
     * コネクション管理ポリシーを破棄します．
     */
    void dispose();

}
