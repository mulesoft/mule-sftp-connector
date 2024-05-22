/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.connection;

import java.util.Properties;

@FunctionalInterface
public interface ExternalConfigProvider {

  /**
   * Provides the properties as fetched by individual config providers.
   * @return Properties Contains the property map provided by the external config.
   */
  Properties getConfigProperties();

}
