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
  private static final String CONTENT_TYPE_HEADER_VALUE = "application/json";
  private static final String FILES_ENDPOINT = "/sftp/files";
  private static final String CLEAR_OS_ENDPOINT = "/os/files";
  private static final int POLLING_PROBER_TIMEOUT_MILLIS = 10000;
  private static final int POLLING_PROBER_DELAY_MILLIS = 1000;
  private static final int TIME_SLEEP_MILLIS = 5000;

  @Standalone
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
    runtime.api(api).request(FILES_ENDPOINT + "/angel-file").withPayload(PAYLOAD)
        .withHeader(CONTENT_TYPE, CONTENT_TYPE_HEADER_VALUE)
        .post();
    runtime.api(api).request(FILES_ENDPOINT + "/demon-file").withPayload(PAYLOAD2)
        .withHeader(CONTENT_TYPE, CONTENT_TYPE_HEADER_VALUE)
        .post();

    probe(POLLING_PROBER_TIMEOUT_MILLIS, POLLING_PROBER_DELAY_MILLIS, () -> {
      return getFileAndAssertContent(FILES_ENDPOINT + "/angel-file", "Aziraphale")
          && getFileAndAssertContent(FILES_ENDPOINT + "/demon-file", "Crowley");
    });

    String containerId = stopServerContainer(CONTAINER_NAME, 0);
    runtime.api(api).request(CLEAR_OS_ENDPOINT).put();
    startServerContainer(containerId);

    Thread.sleep(TIME_SLEEP_MILLIS);

    probe(POLLING_PROBER_TIMEOUT_MILLIS, POLLING_PROBER_DELAY_MILLIS, () -> {
      return getFileAndAssertContent(FILES_ENDPOINT + "/angel-file", "Aziraphale")
          && getFileAndAssertContent(FILES_ENDPOINT + "/demon-file", "Crowley");
    });
  }

  private boolean getFileAndAssertContent(String endpoint, String fileContent) throws AssertionError {
    HttpResponse responseApi = runtime.api(api).request(endpoint).get();
    assertThat(responseApi.statusCode(), is(SC_OK));
    assertThat(responseApi.asString(), containsString(fileContent));
    return true;
  }


  private static Dependency sftpConnectorDependency() {
    Dependency sftpConnector = new Dependency();
    sftpConnector.setGroupId("org.mule.connectors");
    sftpConnector.setArtifactId("mule-sftp-connector");
    sftpConnector.setVersion("1.3.9");
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
