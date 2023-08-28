/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp;

import org.mule.extension.sftp.internal.connection.SftpClient;
import org.mule.extension.sftp.internal.connection.SftpClientFactory;
import org.mule.extension.sftp.internal.util.SftpUtils;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

@ArtifactClassLoaderRunnerConfig(
    applicationSharedRuntimeLibs = {
        "org.apache.sshd:sshd-sftp",
        "org.apache.sshd:sshd-common",
        "org.apache.sshd:sshd-scp",
        "org.apache.sshd:sshd-core",
        "org.bouncycastle:bcprov-jdk15on",
        "net.i2p.crypto:eddsa"
    },
    applicationRuntimeLibs = {"org.slf4j:slf4j-api"},
    exportPluginClasses = {SftpClientFactory.class, SftpClient.class, SftpUtils.class})
public abstract class AbstractSftpConnectorTestCase extends MuleArtifactFunctionalTestCase {

}
