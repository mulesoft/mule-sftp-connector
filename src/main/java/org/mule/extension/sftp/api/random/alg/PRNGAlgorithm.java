/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.api.random.alg;

import org.mule.extension.sftp.api.random.alg.impl.NativeBlockingPRNGRandomFactory;
import org.mule.extension.sftp.api.random.alg.impl.NativeNonBlockingPRNGFactory;
import org.mule.extension.sftp.api.random.alg.impl.NativePRNGRandomFactory;
import org.mule.extension.sftp.api.random.alg.impl.SHA1PRGRandomFactory;

import org.apache.sshd.common.random.RandomFactory;
import org.apache.sshd.common.random.SingletonRandomFactory;
import org.apache.sshd.common.util.security.SecurityUtils;

public enum PRNGAlgorithm {

  AUTOSELECT("AUTOSELECT", new SingletonRandomFactory(SecurityUtils.getRandomFactory())), SHA1PRNG("SHA1PRNG",
      SHA1PRGRandomFactory.INSTANCE), NativePRNG("NativePRNG", NativePRNGRandomFactory.INSTANCE), NativePRNGBlocking(
          "NativePRNGBlocking", NativeBlockingPRNGRandomFactory.INSTANCE), NativePRNGNonBlocking("NativePRNGNonBlocking",
              NativeNonBlockingPRNGFactory.INSTANCE);

  private RandomFactory factory;
  private String name;

  PRNGAlgorithm(String name, RandomFactory factory) {
    this.name = name;
    this.factory = factory;
  }

  public RandomFactory getRandomFactory() {
    return factory;
  }

  public String getName() {
    return name;
  }
}
