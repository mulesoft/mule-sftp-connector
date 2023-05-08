/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.api.random.alg.impl;

import static org.mule.extension.sftp.api.random.alg.PRNGAlgorithm.NativePRNG;

import org.mule.extension.sftp.api.random.alg.MulePRNGAlgorithm;

public class NativePRNGAlgorithm extends MulePRNGAlgorithm {

  @Override
  protected String getAlgorithmName() {
    return NativePRNG.getName();
  }
}
