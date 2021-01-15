/*
 * (c) 2003-2020 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.extension.sftp;

import com.mulesoft.anypoint.tita.environment.api.ApplicationSelector;
import com.mulesoft.anypoint.tita.environment.api.artifact.ApplicationBuilder;
import com.mulesoft.anypoint.tita.runner.ambar.Ambar;
import com.mulesoft.anypoint.tita.runner.ambar.annotation.Application;
import com.mulesoft.anypoint.tita.runner.ambar.annotation.runtime.Standalone;
import org.junit.runner.RunWith;

@RunWith(Ambar.class)
public class SftpDirectoryListenerReconnectionTestCase {

    @Standalone
    Runtime runtime;

    @Application
    public static ApplicationBuilder app(ApplicationSelector runtimeBuilder) {
        return runtimeBuilder.custom("sftp-reconnection-app", "sftp-reconnection-app.xml");
    }


}
