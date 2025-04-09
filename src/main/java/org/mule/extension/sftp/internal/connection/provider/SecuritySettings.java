/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.connection.provider;

import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Path;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import static org.mule.runtime.api.meta.model.display.PathModel.Type.FILE;
import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED_TAB;

public class SecuritySettings {

  @Parameter
  @Optional
  @Placement(tab = ADVANCED_TAB)
  @Summary("Path to the file with the ssh override configurations")
  @DisplayName("SSH Config Override File")
  @Path(type = FILE)
  private String sshConfigOverride;

  public String getSshConfigOverride() {
    return sshConfigOverride;
  }

  public void setSshConfigOverride(String sshConfigOverride) {
    this.sshConfigOverride = sshConfigOverride;
  }

}
