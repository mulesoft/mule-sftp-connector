/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp;

import org.mule.extension.sftp.internal.SftpUtils;
import org.mule.extension.sftp.internal.connection.SftpClient;
import org.mule.extension.sftp.internal.connection.SftpClientFactory;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

@ArtifactClassLoaderRunnerConfig(exportPluginClasses = {SftpClientFactory.class, SftpClient.class, SftpUtils.class})
public abstract class AbstractSftpConnectorTestCase extends MuleArtifactFunctionalTestCase {

}
