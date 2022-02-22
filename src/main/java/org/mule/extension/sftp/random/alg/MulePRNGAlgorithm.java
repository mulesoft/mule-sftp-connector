/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.random.alg;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// FIXME
public abstract class MulePRNGAlgorithm {

  private static final Logger LOGGER = LoggerFactory.getLogger(MulePRNGAlgorithm.class);

  private byte[] tmp = new byte[16];
  private SecureRandom random = null;

  public MulePRNGAlgorithm() {

    try {
      String algorithmName = getAlgorithmName();
      if (algorithmName == null) {
        fallBackToSecureRandom();
        return;
      }
      random = SecureRandom.getInstance(algorithmName);
    } catch (NoSuchAlgorithmException e) {
      LOGGER.warn("Error retrieving PRGN generator. Using default Pseudonumber Random Generator");
      fallBackToSecureRandom();
    }
  }

  private void fallBackToSecureRandom() {
    random = new SecureRandom();
  }

  public void fill(byte[] foo, int start, int len) {
    if (len > tmp.length) {
      tmp = new byte[len];
    }
    random.nextBytes(tmp);
    System.arraycopy(tmp, 0, foo, start, len);
  }

  protected abstract String getAlgorithmName();

}
