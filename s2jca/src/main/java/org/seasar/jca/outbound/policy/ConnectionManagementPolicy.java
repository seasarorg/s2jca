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
 * @author koichik
 */
public interface ConnectionManagementPolicy {
    void initialize(ManagedConnectionFactory mcf, ConnectionManagementPolicy nextPolicy)
            throws ResourceException;

    void allocate(ConnectionManagementContext context) throws ResourceException;

    void release(ManagedConnection mc) throws ResourceException;

    void connectionErrorOccurred(ManagedConnection mc) throws ResourceException;

    void dispose();
}
