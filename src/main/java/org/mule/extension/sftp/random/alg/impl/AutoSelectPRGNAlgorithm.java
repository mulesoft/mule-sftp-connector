/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.random.alg.impl;

import org.mule.extension.sftp.random.alg.MulePRNGAlgorithm;

public class AutoSelectPRGNAlgorithm extends MulePRNGAlgorithm {

  @Override
  protected String getAlgorithmName() {
    return null;
  }
}
