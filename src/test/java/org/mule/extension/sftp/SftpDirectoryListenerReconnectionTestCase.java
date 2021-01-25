/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp;

import static org.mule.extension.sftp.internal.lifecycle.SftpServerContainerLifecycleManger.startServerContainer;
import static org.mule.extension.sftp.internal.lifecycle.SftpServerContainerLifecycleManger.stopServerContainer;
import static com.mulesoft.anypoint.tita.environment.api.artifact.Identifier.identifier;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpStatus.SC_OK;

import static org.hamcrest.core.Is.is;

import com.mulesoft.anypoint.tests.http.HttpResponse;
import com.mulesoft.anypoint.tita.environment.api.ApplicationSelector;
import com.mulesoft.anypoint.tita.environment.api.artifact.ApplicationBuilder;
import com.mulesoft.anypoint.tita.environment.api.artifact.Identifier;
import com.mulesoft.anypoint.tita.environment.api.runtime.Runtime;
import com.mulesoft.anypoint.tita.runner.ambar.Ambar;
import com.mulesoft.anypoint.tita.runner.ambar.annotation.Application;
import com.mulesoft.anypoint.tita.runner.ambar.annotation.runtime.Standalone;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.apache.maven.model.Dependency;

import static org.mule.tck.probe.PollingProber.probe;

@RunWith(Ambar.class)
public class SftpDirectoryListenerReconnectionTestCase {

  private static final Identifier api = identifier("api1");
  private static final Identifier api2 = identifier("api2");
  private static final Identifier port = identifier("port");
  private static final String PAYLOAD = "{\"Angel\":\"Aziraphale\"}";
  private static final String PAYLOAD2 = "{\"Demon\":\"Crowley\"}";
  private static final String CONTAINER_NAME = "openssh";
  private static final int POLLING_PROBER_TIMEOUT_MILLIS = 10000;
  private static final int POLLING_PROBER_DELAY_MILLIS = 1000;
  private static final int TIME_SLEEP_MILLIS = 5000;

  //@Standalone(testing = "4.2.2-20201020")
  Runtime runtime;

  @Application
  public static ApplicationBuilder app(ApplicationSelector runtimeBuilder) {
    return runtimeBuilder
        .custom("sftp-reconnection-app", "sftp-reconnection-app.xml")
        .withDependency(sftpConnectorDependency())
        .withDependency(osConnectorDependency())
        .withProperty("sftp.port", System.getProperty("sftp.listener.port"))
        .withApi(api, port)
        .withApi(api2, port);
  }


  @Test
  public void sftpReconnectionTestCase() throws Exception {
    runtime.api(api).request("/sftp/files/angel-file").withPayload(PAYLOAD).withHeader(CONTENT_TYPE, "application/json").post();

    probe(POLLING_PROBER_TIMEOUT_MILLIS, POLLING_PROBER_DELAY_MILLIS, () -> {
      HttpResponse responseApi2 = runtime.api(api).request("/sftp/files/angel-file").get();
      assertThat(responseApi2.statusCode(), is(SC_OK));
      assertThat(responseApi2.asString(), containsString("Aziraphale"));
      return true;
    });

    String containerId = stopServerContainer(CONTAINER_NAME, 0);
    Thread.sleep(TIME_SLEEP_MILLIS);
    startServerContainer(containerId);

    runtime.api(api).request("/sftp/files/demon-file").withPayload(PAYLOAD2).withHeader(CONTENT_TYPE, "application/json").post();

    probe(POLLING_PROBER_TIMEOUT_MILLIS, POLLING_PROBER_DELAY_MILLIS, () -> {
      HttpResponse responseApi2 = runtime.api(api).request("/sftp/files/demon-file").get();
      assertThat(responseApi2.statusCode(), is(SC_OK));
      assertThat(responseApi2.asString(), containsString("Crowley"));
      return true;
    });

  }


  private static Dependency sftpConnectorDependency() {
    Dependency sftpConnector = new Dependency();
    sftpConnector.setGroupId("org.mule.connectors");
    sftpConnector.setArtifactId("mule-sftp-connector");
    sftpConnector.setVersion("1.3.8");
    sftpConnector.setClassifier("mule-plugin");

    return sftpConnector;
  }

  private static Dependency osConnectorDependency() {
    Dependency osConnector = new Dependency();
    osConnector.setGroupId("org.mule.connectors");
    osConnector.setArtifactId("mule-objectstore-connector");
    osConnector.setVersion("1.1.5");
    osConnector.setClassifier("mule-plugin");

    return osConnector;
  }


}
