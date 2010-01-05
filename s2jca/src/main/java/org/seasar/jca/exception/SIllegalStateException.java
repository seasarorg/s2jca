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
package org.seasar.jca.exception;

import javax.resource.spi.IllegalStateException;

import org.seasar.framework.message.MessageFormatter;

/**
 * {@link MessageFormatter}を使用してメッセージを組み立てる{@link IllegalStateException}です．
 * 
 * @author koichik
 */
public class SIllegalStateException extends IllegalStateException {

    // constants
    private static final long serialVersionUID = 1L;

    // instance fields
    /** メッセージコード */
    protected String messageCode;

    /** メッセージ中に埋め込む引数 */
    protected Object[] args;

    /**
     * インスタンスを構築します．
     * 
     * @param messageCode
     *            メッセージコード
     */
    public SIllegalStateException(final String messageCode) {
        this(messageCode, null);
    }

    /**
     * インスタンスを構築します．
     * 
     * @param messageCode
     *            メッセージコード
     * @param args
     *            メッセージ中に埋め込む引数
     */
    public SIllegalStateException(final String messageCode, final Object[] args) {
        this(messageCode, args, null);
    }

    /**
     * インスタンスを構築します．
     * 
     * @param messageCode
     *            メッセージコード
     * @param args
     *            メッセージ中に埋め込む引数
     * @param cause
     *            この例外の原因となった例外
     */
    public SIllegalStateException(final String messageCode, final Object[] args,
            final Throwable cause) {
        super(MessageFormatter.getMessage(messageCode, args));
        this.messageCode = messageCode;
        this.args = args;
        this.initCause(cause);
    }

    /**
     * メッセージコードを返します．
     * 
     * @return メッセージコード
     */
    public String getMessageCode() {
        return messageCode;
    }

    /**
     * メッセージ中に埋め込む引数を返します．
     * 
     * @return メッセージ中に埋め込む引数
     */
    public Object[] getArgs() {
        return args;
    }

}
