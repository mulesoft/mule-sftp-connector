/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.api.random.alg;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.apache.sshd.common.random.AbstractRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MulePRNGAlgorithm extends AbstractRandom {

  private static final Logger LOGGER = LoggerFactory.getLogger(MulePRNGAlgorithm.class);

  private byte[] tmp = new byte[16];
  private SecureRandom random = null;

  private String name;

  public MulePRNGAlgorithm(String name) {
    try {
      this.name = name;
      random = SecureRandom.getInstance(name);
    } catch (NoSuchAlgorithmException e) {
      LOGGER.warn("Error retrieving PRGN generator. Using default Pseudonumber Random Generator");
      fallBackToSecureRandom();
    }
  }

  private void fallBackToSecureRandom() {
    random = new SecureRandom();
  }

  public String getName() {
    return name;
  }

  public void fill(byte[] foo, int start, int len) {
    if (len > tmp.length) {
      tmp = new byte[len];
    }
    random.nextBytes(tmp);
    System.arraycopy(tmp, 0, foo, start, len);
  }

  public synchronized int random(int n) {
    return this.random.nextInt(n);
  }
}
