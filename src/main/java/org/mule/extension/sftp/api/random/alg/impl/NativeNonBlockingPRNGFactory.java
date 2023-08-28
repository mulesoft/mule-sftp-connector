/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.api.random.alg.impl;

import org.mule.extension.sftp.api.random.alg.MulePRNGAlgorithm;

import org.apache.sshd.common.random.AbstractRandomFactory;
import org.apache.sshd.common.random.Random;

public class NativeNonBlockingPRNGFactory extends AbstractRandomFactory {

  public static final String NAME = "NativePRNGNonBlocking";
  public static final NativeNonBlockingPRNGFactory INSTANCE = new NativeNonBlockingPRNGFactory();

  private NativeNonBlockingPRNGFactory() {
    super(NAME);
  }

  public boolean isSupported() {
    return true;
  }

  public Random create() {
    return new MulePRNGAlgorithm(NAME);
  }
}
