/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.connection;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Properties;

import static org.slf4j.LoggerFactory.getLogger;

public class ParameterBasedConfigProvider implements ExternalConfigProvider {

  private static final Logger LOGGER = getLogger(ParameterBasedConfigProvider.class);

  private final SecuritySettings securitySettings;

  public ParameterBasedConfigProvider(SecuritySettings securitySettings) {
    this.securitySettings = securitySettings;
  }

  @Override
  public Properties getConfigProperties() {
    Properties result = new Properties();
    if (StringUtils.isNoneBlank(securitySettings.getKexAlgorithms())) {
      result.setProperty("KexAlgorithms", trimUnwantedWhitespace((String) securitySettings.getKexAlgorithms()));
    }
    if (StringUtils.isNoneBlank(securitySettings.getCiphers())) {
      result.setProperty("Ciphers", trimUnwantedWhitespace((String) securitySettings.getCiphers()));
    }
    if (StringUtils.isNoneBlank(securitySettings.getHostKeyAlgorithms())) {
      result.setProperty("HostKeyAlgorithms", trimUnwantedWhitespace((String) securitySettings.getHostKeyAlgorithms()));
    }
    if (StringUtils.isNoneBlank(securitySettings.getMacs())) {
      result.setProperty("MACs", trimUnwantedWhitespace((String) securitySettings.getMacs()));
    }
    LOGGER.info("Properties read from the config {}", result);
    return result;
  }

  private String trimUnwantedWhitespace(String value) {
    String[] values = Arrays.stream(value.split(","))
        .map(String::trim)
        .toArray(String[]::new);
    return StringUtils.join(values, ',');
  }

}
