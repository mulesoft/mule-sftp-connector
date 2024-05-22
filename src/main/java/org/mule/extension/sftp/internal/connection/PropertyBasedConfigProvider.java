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
import java.util.List;
import java.util.Properties;

import static org.slf4j.LoggerFactory.getLogger;

public class PropertyBasedConfigProvider implements ExternalConfigProvider {

  private static final List<String> CONFIG_KEY_LIST = Arrays.asList("KexAlgorithms", "Ciphers", "HostKeyAlgorithms", "MACs");
  private static final Logger LOGGER = getLogger(PropertyBasedConfigProvider.class);

  @Override
  public Properties getConfigProperties() {
    Properties result = new Properties();
    Properties externalProperties = System.getProperties();
    CONFIG_KEY_LIST.stream().filter(key -> StringUtils.isNotBlank(externalProperties.getProperty(key)))
        .forEach(key -> result.setProperty(key, externalProperties.getProperty(key)));
    LOGGER.info("Properties read from the config {}", result);
    return result;
  }
}
