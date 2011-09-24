/*
 * Copyright 2004-2011 the Seasar Foundation and the Others.
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

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.security.auth.Subject;

import org.seasar.framework.log.Logger;
import org.seasar.jca.outbound.policy.ConnectionManagementPolicy;

/**
 * マネージドコネクションをプールします．
 * 
 * @param <T>
 *            フリープール中のマネージドコネクションに関連づける値の型
 * @author koichik
 */
public class ManagedConnectionPool<T> {

    // static fields
    private static final Logger logger = Logger.getLogger(ManagedConnectionPool.class);

    // instance fields
    /** アクティブプール (使用されているマネージドコネクションの{@link Set}) */
    protected final Set<ManagedConnection> activePool;

    /** フリープール (未使用のマネージドコネクションと任意の値の{@link Map}) */
    protected final Map<ManagedConnection, T> freePool;

    /** フリープール (未使用のマネージドコネクションの{@link Set}) の更新不可能なビュー */
    protected final Set<ManagedConnection> freePoolView;

    /** 後続のコネクション管理ポリシー */
    protected final ConnectionManagementPolicy nextPolicy;

    /**
     * インスタンスを構築します．
     * 
     * @param nextPolicy
     *            後続のコネクション管理ポリシー
     */
    public ManagedConnectionPool(final ConnectionManagementPolicy nextPolicy) {
        this(nextPolicy, false);
    }

    /**
     * インスタンスを構築します．
     * 
     * @param nextPolicy
     *            後続のコネクション管理ポリシー
     * @param accessOrder
     *            フリープールをアクセス順で管理する場合は<code>true</code>
     */
    public ManagedConnectionPool(final ConnectionManagementPolicy nextPolicy,
            final boolean accessOrder) {
        this.activePool = new HashSet<ManagedConnection>();
        this.freePool = new LinkedHashMap<ManagedConnection, T>(64, 0.75f, accessOrder);
        this.freePoolView = Collections.unmodifiableSet(freePool.keySet());
        this.nextPolicy = nextPolicy;
    }

    /**
     * アクティブプール (使用されているマネージドコネクションの{@link Set}) を返します．
     * 
     * @return アクティブプール (使用されているマネージドコネクションの{@link Set})
     */
    public Set<ManagedConnection> getActivePool() {
        return activePool;
    }

    /**
     * フリープール (未使用のマネージドコネクションと任意の値の{@link Map}) の更新不可能なビューを返します．
     * 
     * @return フリープール (未使用のマネージドコネクションと任意の値の{@link Map}) の更新不可能なビュー
     */
    public Set<ManagedConnection> getFreePool() {
        return freePoolView;
    }

    /**
     * プールの現在のサイズ (アクティブプールとフリープールのサイズの合計) を返します．
     * 
     * @return プールの現在のサイズ (アクティブプールとフリープールのサイズの合計)
     */
    public int size() {
        return getFreePoolSize() + getActivePoolSize();
    }

    /**
     * アクティブプールのサイズ (使用されているマネージドコネクションの数) を返します．
     * 
     * @return アクティブプールのサイズ (使用されているマネージドコネクションの数)
     */
    public int getActivePoolSize() {
        return activePool.size();
    }

    /**
     * フリープールのサイズ (未使用のマネージドコネクションの数) を返します．
     * 
     * @return フリープールのサイズ (未使用のマネージドコネクションの数)
     */
    public int getFreePoolSize() {
        return freePool.size();
    }

    /**
     * マネージドコネクションがアクティブプールの要素なら<code>true</code>を返します．
     * 
     * @param mc
     *            マネージドコネクション
     * @return マネージドコネクションがアクティブプールの要素なら<code>true</code>
     */
    public boolean containsActive(final ManagedConnection mc) {
        return activePool.contains(mc);
    }

    /**
     * マネージドコネクションがフリープールの要素なら<code>true</code>を返します．
     * 
     * @param mc
     *            マネージドコネクション
     * @return マネージドコネクションがフリープールの要素なら<code>true</code>
     */
    public boolean containsFree(final ManagedConnection mc) {
        return freePool.containsKey(mc);
    }

    /**
     * 引数にマッチするマネージドコネクションがフリープールにあればそれを返します．
     * <p>
     * フリープールに引数にマッチするマネージドコネクションがなければ<code>null</code>を返します．
     * </p>
     * 
     * @param subject
     *            サブジェクト
     * @param info
     *            コネクション要求情報
     * @param mcf
     *            マネージドコネクションファクトリ
     * @return 引数にマッチするマネージドコネクション
     * @throws ResourceException
     *             マネージドコネクションの照会中に例外が発生した場合
     */
    public ManagedConnection getMatched(final Subject subject, final ConnectionRequestInfo info,
            final ManagedConnectionFactory mcf) throws ResourceException {
        if (freePool.isEmpty()) {
            return null;
        }
        return mcf.matchManagedConnections(freePoolView, subject, info);
    }

    /**
     * フリープールから最初のマネージドコネクションを返します．
     * 
     * @return フリープール中の最初のマネージドコネクション
     */
    public ManagedConnection getFirstFromFree() {
        if (freePool.isEmpty()) {
            return null;
        }
        return freePoolView.iterator().next();
    }

    /**
     * マネージドコネクションをアクティブプールに追加します．
     * 
     * @param mc
     *            マネージドコネクション
     */
    public void addToActivePool(final ManagedConnection mc) {
        activePool.add(mc);
    }

    /**
     * マネージドコネクションをフリープールに追加します．
     * 
     * @param mc
     *            マネージドコネクション
     */
    public void addToFreePool(final ManagedConnection mc) {
        addToFreePool(mc, null);
    }

    /**
     * マネージドコネクションをフリープールに追加します．
     * 
     * @param mc
     *            マネージドコネクション
     * @param opaque
     *            マネージドコネクションに関連づける任意の値
     */
    public void addToFreePool(final ManagedConnection mc, final T opaque) {
        freePool.put(mc, opaque);
        freePool.get(mc);
    }

    /**
     * マネージドコネクションをアクティブプールからフリープールに移動します．
     * <p>
     * マネージドコネクションがアクティブプールのメンバでない場合は<code>false</code>を返します．
     * </p>
     * 
     * @param mc
     *            マネージドコネクション
     * @return マネージドコネクションを移動した場合は<code>true</code>
     */
    public boolean moveActiveToFreePool(final ManagedConnection mc) {
        return moveActiveToFreePool(mc, null);
    }

    /**
     * マネージドコネクションをアクティブプールからフリープールに移動します．
     * <p>
     * マネージドコネクションがアクティブプールのメンバでない場合は<code>false</code>を返します．
     * </p>
     * 
     * @param mc
     *            マネージドコネクション
     * @param opaque
     *            マネージドコネクションに関連づける任意の値
     * @return マネージドコネクションを移動した場合は<code>true</code>
     */
    public boolean moveActiveToFreePool(final ManagedConnection mc, final T opaque) {
        if (activePool.remove(mc)) {
            freePool.put(mc, opaque);
            return true;
        }
        return false;
    }

    /**
     * マネージドコネクションをフリープールからアクティブプールに移動します．
     * <p>
     * マネージドコネクションがフリープールのメンバでない場合は<code>null</code>を返します．
     * </p>
     * 
     * @param mc
     *            マネージドコネクション
     * @return マネージドコネクションに関連づけられていた任意の値
     */
    public T moveFreeToActivePool(final ManagedConnection mc) {
        if (!freePool.containsKey(mc)) {
            return null;
        }
        final T opaque = removeFromFreePool(mc);
        activePool.add(mc);
        return opaque;
    }

    /**
     * マネージドコネクションをアクティブプールから削除します．
     * <p>
     * マネージドコネクションがアクティブプールのメンバでない場合は<code>false</code>を返します．
     * </p>
     * 
     * @param mc
     *            マネージドコネクション
     * @return マネージドコネクションを削除した場合は<code>true</code>
     */
    public boolean removeFromActivePool(final ManagedConnection mc) {
        return activePool.remove(mc);
    }

    /**
     * マネージドコネクションをフリープールから削除します．
     * <p>
     * マネージドコネクションがフリープールのメンバでない場合は<code>null</code>を返します．
     * </p>
     * 
     * @param mc
     *            マネージドコネクション
     * @return マネージドコネクションに関連づけられていた任意の値
     */
    public T removeFromFreePool(final ManagedConnection mc) {
        if (!freePool.containsKey(mc)) {
            return null;
        }
        return freePool.remove(mc);
    }

    /**
     * マネージドコネクションをアクティブプールまたはフリープールから削除します．
     * <p>
     * マネージドコネクションがアクティブプール，フリープールいずれのメンバでもない場合は<code>null</code>を返します．
     * </p>
     * 
     * @param mc
     *            マネージドコネクション
     * @return マネージドコネクションを削除した場合は<code>true</code>
     */
    public boolean remove(final ManagedConnection mc) {
        return removeFromActivePool(mc) || removeFromFreePool(mc) != null;
    }

    /**
     * コネクションプールをクローズします．
     */
    public void close() {
        for (final Iterator<ManagedConnection> it = activePool.iterator(); it.hasNext();) {
            final ManagedConnection mc = it.next();
            it.remove();
            try {
                nextPolicy.release(mc);
            } catch (final ResourceException e) {
                logger.log("EJCA0000", null, e);
            }
        }

        for (final Iterator<ManagedConnection> it = freePool.keySet().iterator(); it.hasNext();) {
            final ManagedConnection mc = it.next();
            it.remove();
            try {
                nextPolicy.release(mc);
            } catch (final ResourceException e) {
                logger.log("EJCA0000", null, e);
            }
        }
    }

}
