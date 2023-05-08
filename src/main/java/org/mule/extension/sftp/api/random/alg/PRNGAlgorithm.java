/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.api.random.alg;

import org.mule.extension.sftp.api.random.alg.impl.AutoSelectPRGNAlgorithm;
import org.mule.extension.sftp.api.random.alg.impl.NativeBlockingPRNGAlgorithm;
import org.mule.extension.sftp.api.random.alg.impl.NativeNonBlockingPRNGAlgorithm;
import org.mule.extension.sftp.api.random.alg.impl.NativePRNGAlgorithm;
import org.mule.extension.sftp.api.random.alg.impl.SHA1PRNGAlgorithm;

public enum PRNGAlgorithm {

  AUTOSELECT("AUTOSELECT", AutoSelectPRGNAlgorithm.class), NativePRNG("NativePRNG", NativeBlockingPRNGAlgorithm.class), SHA1PRNG(
      "SHA1PRNG", SHA1PRNGAlgorithm.class), NativePRNGBlocking("NativePRNGBlocking",
          NativePRNGAlgorithm.class), NativePRNGNonBlocking("NativePRNGNonBlocking", NativeNonBlockingPRNGAlgorithm.class);

  private String implementationClassName;
  private String name;

  PRNGAlgorithm(String name, Class implementationClass) {
    this.name = name;
    this.implementationClassName = implementationClass.getName();
  }

  public String getImplementationClassName() {
    return implementationClassName;
  }

  public String getName() {
    return name;
  }
}
