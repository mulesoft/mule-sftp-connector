/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.proxy.authenticationStrategy;

import org.apache.sshd.common.io.IoSession;
import org.mule.extension.sftp.internal.proxy.socks5.Socks5ClientConnector;

import org.apache.sshd.common.util.buffer.Buffer;

public interface AuthenticationStrategy {

  void authenticate(IoSession session, Socks5ClientConnector connector, Buffer data) throws Exception;

}
