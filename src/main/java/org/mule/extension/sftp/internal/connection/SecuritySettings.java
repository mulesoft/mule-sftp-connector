/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.connection;

import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import java.util.Objects;

import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED_TAB;

public final class SecuritySettings {

  @Parameter
  @Optional
  @Placement(tab = ADVANCED_TAB, order = 1)
  @Summary("Specifies Key Exchange Algos separated with coma")
  private String kexAlgorithms;

  @Parameter
  @Optional
  @Placement(tab = ADVANCED_TAB, order = 2)
  @Summary("Specifies the symmetric ciphers allowed")
  private String ciphers;

  @Parameter
  @Optional
  @Placement(tab = ADVANCED_TAB, order = 3)
  @Summary("Specifies host key signature algos separated with coma")
  private String hostKeyAlgorithms;

  @Parameter
  @Optional
  @Placement(tab = ADVANCED_TAB, order = 4)
  @Summary("Specifies Message Authentication Codes available")
  private String macs;

  public String getKexAlgorithms() {
    return kexAlgorithms;
  }

  public void setKexAlgorithms(String kexAlgorithms) {
    this.kexAlgorithms = kexAlgorithms;
  }

  public String getCiphers() {
    return ciphers;
  }

  public void setCiphers(String ciphers) {
    this.ciphers = ciphers;
  }

  public String getHostKeyAlgorithms() {
    return hostKeyAlgorithms;
  }

  public void setHostKeyAlgorithms(String hostKeyAlgorithms) {
    this.hostKeyAlgorithms = hostKeyAlgorithms;
  }

  public String getMacs() {
    return macs;
  }

  public void setMacs(String macs) {
    this.macs = macs;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SecuritySettings)) {
      return false;
    }
    SecuritySettings that = (SecuritySettings) o;
    return Objects.equals(kexAlgorithms, that.kexAlgorithms) &&
        Objects.equals(ciphers, that.ciphers) &&
        Objects.equals(hostKeyAlgorithms, that.hostKeyAlgorithms) &&
        Objects.equals(macs, that.macs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(kexAlgorithms, ciphers, hostKeyAlgorithms, macs);
  }
}
