/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp;

import static com.mulesoft.anypoint.tita.environment.api.artifact.Identifier.identifier;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.mulesoft.anypoint.tests.http.HttpResponse;
import com.mulesoft.anypoint.tita.environment.api.ApplicationSelector;
import com.mulesoft.anypoint.tita.environment.api.anypoint.ApiManager;
import com.mulesoft.anypoint.tita.environment.api.artifact.ApplicationBuilder;
import com.mulesoft.anypoint.tita.environment.api.artifact.Identifier;
import com.mulesoft.anypoint.tita.environment.api.artifact.policy.PolicySupplier;
import com.mulesoft.anypoint.tita.environment.api.runtime.Runtime;
import com.mulesoft.anypoint.tita.runner.ambar.Ambar;
import com.mulesoft.anypoint.tita.runner.ambar.annotation.Application;
import com.mulesoft.anypoint.tita.runner.ambar.annotation.Platform;
import com.mulesoft.anypoint.tita.runner.ambar.annotation.Policy;
import com.mulesoft.anypoint.tita.runner.ambar.annotation.TestTarget;
import com.mulesoft.anypoint.tita.runner.ambar.annotation.runtime.Standalone;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.apache.maven.model.Dependency;
import org.mule.extension.sftp.internal.source.SftpDirectoryListener;
import org.slf4j.LoggerFactory;

@RunWith(Ambar.class)
public class SftpDirectoryListenerReconnectionTestCase {

  private static final Identifier api = identifier("api1");
  private static final Identifier port = identifier("port");

  //TODO: usar atributo 'testing' de Standalone para especificar la version del runtime
  @Standalone
  Runtime runtime;

  @Before
  public void setup() {
    //Las opciones son ver si se resuelve el puerto en el before o agregar un processor
    //a la chain de processors de Ambar(ahi puedo agregar lo que quiera, incluso levantar
    //un docker ahi programaticamente, o algo asi).
  }

  @Application
  public static ApplicationBuilder app(ApplicationSelector runtimeBuilder) {
    return runtimeBuilder
        .custom("sftp-reconnection-app", "sftp-reconnection-app.xml")
        .withDependency(sftpConnectorDependency())
        .withProperty("sftp.port", "2222")
        .withApi(api, port);
  }



  @Test
  public void sftpReconnectionTestCase() {
    HttpResponse responseApi1 = runtime.api(api).request("/stop").withPayload("My payload").get();

    assertThat(responseApi1.statusCode(), is(200));

  }


  private static Dependency sftpConnectorDependency() {
    Dependency sftpConnector = new Dependency();
    sftpConnector.setGroupId("org.mule.connectors");
    sftpConnector.setArtifactId("mule-sftp-connector");
    sftpConnector.setVersion("1.3.8");
    sftpConnector.setClassifier("mule-plugin");

    return sftpConnector;
  }
}
