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
package org.seasar.jca.outbound.support;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.security.auth.Subject;

import org.seasar.framework.log.Logger;

/**
 * コネクション管理のために必要な情報を保持するクラスです．
 * 
 * @author koichik
 */
public class ConnectionManagementContext {

    // static fields
    private static final Logger logger = Logger.getLogger(ConnectionManagementContext.class);

    // instance fields
    /** セキュリティ認証のサブジェクト */
    protected Subject subject;

    /** コネクション要求情報 */
    protected ConnectionRequestInfo info;

    /** マネージドコネクションファクトリ */
    protected ManagedConnectionFactory mcf;

    /** マネージドコネクション */
    protected ManagedConnection mc;

    /** 論理コネクションハンドラ */
    protected Object lch;

    /**
     * インスタンスを構築します．
     * 
     * @param subject
     *            サブジェクト
     * @param info
     *            コネクション要求情報
     * @param mcf
     *            マネージドコネクションファクトリ
     */
    public ConnectionManagementContext(final Subject subject, final ConnectionRequestInfo info,
            final ManagedConnectionFactory mcf) {
        this(subject, info, mcf, null, null);
    }

    /**
     * インスタンスを構築します．
     * 
     * @param subject
     *            サブジェクト
     * @param info
     *            コネクション要求情報
     * @param mcf
     *            マネージドコネクションファクトリ
     * @param mc
     *            マネージドコネクション
     */
    public ConnectionManagementContext(final Subject subject, final ConnectionRequestInfo info,
            final ManagedConnectionFactory mcf, final ManagedConnection mc) {
        this(subject, info, mcf, mc, null);
    }

    /**
     * インスタンスを構築します．
     * 
     * @param subject
     *            サブジェクト
     * @param info
     *            コネクション要求情報
     * @param mcf
     *            マネージドコネクションファクトリ
     * @param mc
     *            マネージドコネクション
     * @param lch
     *            論理コネクションハンドラ
     */
    public ConnectionManagementContext(final Subject subject, final ConnectionRequestInfo info,
            final ManagedConnectionFactory mcf, final ManagedConnection mc, final Object lch) {
        this.subject = subject;
        this.info = info;
        this.mcf = mcf;
        this.mc = mc;
        this.lch = lch;
    }

    /**
     * マネージドコネクションを割り当てます．
     * 
     * @return マネージドコネクション
     * @throws ResourceException
     *             マネージドコネクションの割り当て中に例外が発生した場合
     */
    public ManagedConnection allocateManagedConnection() throws ResourceException {
        mc = mcf.createManagedConnection(subject, info);
        if (logger.isDebugEnabled()) {
            logger.log("DJCA1000", new Object[] { mc });
        }
        return mc;
    }

    /**
     * 論理コネクションハンドラを割り当てます．
     * 
     * @return 論理コネクションハンドラ
     * @throws ResourceException
     *             論理コネクションハンドラの割り当て中に例外が発生した場合
     */
    public Object allocateLogicalConnectionHandle() throws ResourceException {
        lch = mc.getConnection(subject, info);
        if (logger.isDebugEnabled()) {
            logger.log("DJCA1001", new Object[] { mc, lch });
        }
        return lch;
    }

    /**
     * サブジェクトを返します．
     * 
     * @return サブジェクト
     */
    public Subject getSubject() {
        return subject;
    }

    /**
     * サブジェクトを設定します．
     * 
     * @param subject
     *            サブジェクト
     * @return このコネクション管理コンテキスト
     */
    public ConnectionManagementContext setSubject(final Subject subject) {
        this.subject = subject;
        return this;
    }

    /**
     * コネクション要求情報を返します．
     * 
     * @return コネクション要求情報
     */
    public ConnectionRequestInfo getRequestInfo() {
        return info;
    }

    /**
     * コネクション要求情報を設定します．
     * 
     * @param requestInfo
     *            コネクション要求情報
     * @return このコネクション管理コンテキスト
     */
    public ConnectionManagementContext setRequestInfo(final ConnectionRequestInfo requestInfo) {
        this.info = requestInfo;
        return this;
    }

    /**
     * マネージドコネクションファクトリを返します．
     * 
     * @return マネージドコネクションファクトリ
     */
    public ManagedConnectionFactory getManagedConnectionFactory() {
        return mcf;
    }

    /**
     * マネージドコネクションファクトリを設定します．
     * 
     * @param managedConnectionFactory
     *            マネージドコネクションファクトリ
     * @return このコネクション管理コンテキスト
     */
    public ConnectionManagementContext setManagedConnectionFactory(
            final ManagedConnectionFactory managedConnectionFactory) {
        this.mcf = managedConnectionFactory;
        return this;
    }

    /**
     * マネージドコネクションを返します．
     * 
     * @return マネージドコネクション
     */
    public ManagedConnection getManagedConnection() {
        return mc;
    }

    /**
     * マネージドコネクションを設定します．
     * 
     * @param managedConnection
     *            マネージドコネクション
     * @return このコネクション管理コンテキスト
     */
    public ConnectionManagementContext setManagedConnection(
            final ManagedConnection managedConnection) {
        this.mc = managedConnection;
        return this;
    }

    /**
     * 論理コネクションハンドラを返します．
     * 
     * @return 論理コネクションハンドラ
     */
    public Object getLogicalConnectionHandle() {
        return lch;
    }

    /**
     * 論理コネクションハンドラを設定します．
     * 
     * @param logicalConnectionHandle
     *            論理コネクションハンドラ
     * @return このコネクション管理コンテキスト
     */
    public ConnectionManagementContext setLogicalConnectionHandle(
            final Object logicalConnectionHandle) {
        this.lch = logicalConnectionHandle;
        return this;
    }

}
