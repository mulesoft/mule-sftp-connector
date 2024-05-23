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
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

public class FileBasedConfigProvider implements ExternalConfigProvider {

  private static final List<String> CONFIG_KEY_LIST = Arrays.asList("KexAlgorithms", "Ciphers", "HostKeyAlgorithms", "MACs");
  private static final Logger LOGGER = getLogger(FileBasedConfigProvider.class);
  public static final String CONFIG_FILE_PATH_PROPERTY = "config.file.path";
  private static final String DEFAULT_CONFIG_FILE_PATH = "mule_sshd_config";
  private final String configFilePath;

  public FileBasedConfigProvider() {
    configFilePath = Optional.ofNullable(System.getProperty(CONFIG_FILE_PATH_PROPERTY))
        .filter(StringUtils::isNotBlank)
        .orElse(DEFAULT_CONFIG_FILE_PATH);
  }

  @Override
  public Properties getConfigProperties() {
    Properties result = new Properties();
    try {
      Properties properties = readConfigFile(configFilePath);
      Set<String> unsupportedKeys = new HashSet<>();
      populateSupportedProperties(properties, result, unsupportedKeys);
      if (!unsupportedKeys.isEmpty()) {
        LOGGER.warn("Config keys found but ignored: {}", unsupportedKeys);
      }
      trimUnwantedWhitespace(result);
      LOGGER.info("Read the config file {} with the props {}", configFilePath, result);
    } catch (IOException e) {
      LOGGER.warn("Could not read values from config file: " + configFilePath, e);
    }
    return result;
  }

  private Properties readConfigFile(String configFilePath) throws IOException {
    InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(configFilePath);
    Properties properties = new Properties();
    if (inputStream != null) {
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
    }
    return properties;
  }

  private void populateSupportedProperties(Properties properties, Properties supportedProperties, Set<String> unsupportedKeys) {
    properties.forEach((key, value) -> {
      if (CONFIG_KEY_LIST.contains((String) key)) {
        supportedProperties.put(key, value);
      } else {
        unsupportedKeys.add((String) key);
      }
    });
  }

  private void trimUnwantedWhitespace(Properties supportedProperties) {
    supportedProperties.forEach((key, value) -> {
      String[] values = Arrays.stream(((String) value).split(","))
          .map(String::trim)
          .toArray(String[]::new);
      supportedProperties.setProperty((String) key, StringUtils.join(values, ','));
    });
  }
}
