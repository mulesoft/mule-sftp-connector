/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.lifecycle;

import org.mule.sdk.api.artifact.lifecycle.ArtifactDisposalContext;
import org.mule.sdk.api.artifact.lifecycle.ArtifactLifecycleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.HashMap;

public class SftpLifeCycleListener implements ArtifactLifecycleListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(SftpLifeCycleListener.class);
  private static final String CLASS_NAME = "org.apache.tika.mime.MimeTypes";
  private static final String FIELD_NAME = "CLASSLOADER_SPECIFIC_DEFAULT_TYPES";

  @Override
  public void onArtifactDisposal(ArtifactDisposalContext artifactDisposalContext) {

    try {
      Class<?> mimeTypesClass = Class.forName(CLASS_NAME);
      Field field = mimeTypesClass.getDeclaredField(FIELD_NAME);
      HashMap<?, ?> map = (HashMap<?, ?>) field.get(null);
      map.clear();
    } catch (Exception ex) {
      LOGGER.error(ex.getMessage(), ex);
    }
  }
}
