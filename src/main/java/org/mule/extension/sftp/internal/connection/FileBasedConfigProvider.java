/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.connection;

import org.apache.commons.lang3.StringUtils;
import org.apache.sshd.common.config.ConfigFileReaderSupport;
import org.apache.sshd.common.config.keys.PublicKeyEntry;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
  private static final String CONFIG_FILE_NAME = "mule_sshd_config";
  private final Path path;

  public FileBasedConfigProvider(String filepath) {
    path = Optional.ofNullable(filepath)
        .filter(StringUtils::isNotBlank)
        .map(Paths::get)
        .orElseGet(() -> PublicKeyEntry.getDefaultKeysFolderPath().resolve(CONFIG_FILE_NAME));
  }

  @Override
  public Properties getConfigProperties() {
    Properties result = new Properties();
    try {
      if (Files.exists(path)) {
        Properties properties = ConfigFileReaderSupport.readConfigFile(path);
        Set<String> unsupportedKeys = new HashSet<>();
        populateSupportedProperties(properties, result, unsupportedKeys);
        if (!unsupportedKeys.isEmpty()) {
          LOGGER.warn("Config keys found but ignored: {}", unsupportedKeys);
        }
        trimUnwantedWhitespace(result);
        LOGGER.info("Read the config file {} with the props {}", path.getFileName(), result);
      }
    } catch (IOException e) {
      LOGGER.warn("Could not read values from config file: " + path.getFileName(), e);
    }
    return result;
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
