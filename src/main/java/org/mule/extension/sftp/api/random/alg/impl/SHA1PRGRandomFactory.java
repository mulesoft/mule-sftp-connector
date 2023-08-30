/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.api.random.alg.impl;

import org.apache.sshd.common.random.AbstractRandomFactory;
import org.apache.sshd.common.random.Random;

import org.mule.extension.sftp.api.random.alg.MulePRNGAlgorithm;

public class SHA1PRGRandomFactory extends AbstractRandomFactory {

  public static final String NAME = "SHA1PRNG";
  public static final SHA1PRGRandomFactory INSTANCE = new SHA1PRGRandomFactory();

  private SHA1PRGRandomFactory() {
    super(NAME);
  }

  public boolean isSupported() {
    return true;
  }

  public Random create() {
    return new MulePRNGAlgorithm(NAME);
  }
}
