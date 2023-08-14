/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.api.random.alg.impl;

import org.mule.extension.sftp.api.random.alg.MulePRNGAlgorithm;

import org.apache.sshd.common.random.AbstractRandomFactory;
import org.apache.sshd.common.random.Random;

public class NativeNonBlockingPRNGAlgorithm extends AbstractRandomFactory {

  public static final String NAME = "NativePRNGNonBlocking";
  public static final NativeNonBlockingPRNGAlgorithm INSTANCE = new NativeNonBlockingPRNGAlgorithm();

  private NativeNonBlockingPRNGAlgorithm() {
    super(NAME);
  }

  public boolean isSupported() {
    return true;
  }

  public Random create() {
    return new MulePRNGAlgorithm(NAME);
  }
}
