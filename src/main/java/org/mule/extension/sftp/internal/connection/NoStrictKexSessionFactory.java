/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.connection;

import org.apache.sshd.client.ClientFactoryManager;
import org.apache.sshd.client.session.ClientSessionImpl;
import org.apache.sshd.client.session.SessionFactory;
import org.apache.sshd.common.io.IoSession;


class NoStrictKexSessionFactory extends SessionFactory {

  NoStrictKexSessionFactory(ClientFactoryManager client) {
    super(client);
  }

  @Override
  protected ClientSessionImpl doCreateSession(IoSession ioSession) throws Exception {
    return new NoStrictKexSession(getClient(), ioSession);
  }
}


