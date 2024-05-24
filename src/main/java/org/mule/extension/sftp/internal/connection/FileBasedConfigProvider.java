/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.connection;

import org.apache.commons.lang3.StringUtils;
import org.apache.sshd.common.config.ConfigFileReaderSupport;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

public class FileBasedConfigProvider implements ExternalConfigProvider {

  private static final List<String> CONFIG_KEY_LIST = Arrays.asList("KexAlgorithms", "Ciphers", "HostKeyAlgorithms", "MACs");
  private static final Logger LOGGER = getLogger(FileBasedConfigProvider.class);
  private final String configFilePath;

  public FileBasedConfigProvider(String configFilePath) {
    this.configFilePath = configFilePath;
  }

  @Override
  public Properties getConfigProperties() {
    Properties result = new Properties();
    if (StringUtils.isNotBlank(configFilePath)) {
      try {
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(configFilePath);
        if (Objects.isNull(inputStream)) {
          LOGGER.warn("Couldn't locate config file {}", configFilePath);
          return result;
        }
        Properties properties = readConfigFile(inputStream);
        populateSupportedProperties(properties, result);
        LOGGER.info("Read the config file {} with the props {}", configFilePath, result);
      } catch (IOException e) {
        LOGGER.warn("Could not read values from config file: " + configFilePath, e);
      }
    } else {
      LOGGER.info("SSHD Config file not provided");
    }
    return result;
  }

  private Properties readConfigFile(InputStream inputStream) throws IOException {
    Properties properties;
    try {
      properties = ConfigFileReaderSupport.readConfigFile(inputStream, true);
    } catch (Throwable throwable) {
      try {
        inputStream.close();
      } catch (Throwable th) {
        throwable.addSuppressed(th);
      }
      throw throwable;
    } finally {
      inputStream.close();
    }
    return properties;
  }

  private void populateSupportedProperties(Properties properties, Properties supportedProperties) {
    Set<String> unsupportedKeys = new HashSet<>();
    properties.forEach((key, value) -> {
      if (CONFIG_KEY_LIST.contains((String) key)) {
        String trimmedVal = trimUnwantedWhitespace((String) value);
        supportedProperties.put(key, trimmedVal);
      } else {
        unsupportedKeys.add((String) key);
      }
    });
    if (!unsupportedKeys.isEmpty()) {
      LOGGER.warn("Config keys found but ignored: {}", unsupportedKeys);
    }
  }

  private String trimUnwantedWhitespace(String value) {
    String[] values = Arrays.stream(value.split(","))
        .map(String::trim)
        .toArray(String[]::new);
    return StringUtils.join(values, ',');
  }
}
