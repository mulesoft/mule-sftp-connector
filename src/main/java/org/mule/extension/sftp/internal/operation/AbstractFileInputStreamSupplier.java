/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.operation;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import static java.lang.String.format;
import static java.lang.Thread.sleep;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.extension.sftp.api.FileAttributes;
import org.mule.extension.sftp.internal.exception.DeletedFileWhileReadException;
import org.mule.extension.sftp.internal.exception.FileBeingModifiedException;
import org.mule.extension.sftp.internal.stream.ExceptionInputStream;
import org.mule.runtime.api.exception.MuleRuntimeException;

import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.slf4j.Logger;

/**
 * This is an abstract class that implements {@link Supplier<InputStream>} adding the logic to check that the file size is stable.
 * Each subclass should give implementations of {@link AbstractFileInputStreamSupplier#getUpdatedAttributes()}, to be able to get
 * the size of the file, and {@link AbstractFileInputStreamSupplier#getContentInputStream()}, to get the InputStream that is
 * supplied.
 * 
 * @since 1.2
 */
public abstract class AbstractFileInputStreamSupplier implements Supplier<InputStream> {

  private static final AtomicBoolean alreadyLoggedWarning = new AtomicBoolean();
  private static final String WAIT_WARNING_MESSAGE =
      "With the purpose of performing a size check on the file {}, this thread will sleep. The connector has no control of" +
          " which type of thread the sleep will take place on, this can lead to running out of thread if the time for " +
          "'timeBetweenSizeCheck' is big or a lot of files are being read concurrently. This warning will only be shown once.";

  private static final Logger LOGGER = getLogger(AbstractFileInputStreamSupplier.class);
  private static final String STARTING_WAIT_MESSAGE = "Starting wait to check if the file size of the file %s is stable.";
  protected static final String FILE_NO_LONGER_EXISTS_MESSAGE =
      "Error reading file from path %s. It no longer exists at the time of reading.";
  private static final int MAX_SIZE_CHECK_RETRIES = 2;
  private static final String FILE_ON_PATH_MSG = "File on path ";

  protected org.mule.extension.sftp.api.FileAttributes attributes;
  private Long timeBetweenSizeCheck;

  protected AbstractFileInputStreamSupplier(org.mule.extension.sftp.api.FileAttributes attributes, Long timeBetweenSizeCheck) {
    this.attributes = attributes;
    this.timeBetweenSizeCheck = timeBetweenSizeCheck;
  }

  @Override
  public InputStream get() {
    org.mule.extension.sftp.api.FileAttributes updatedAttributes = null;
    if (timeBetweenSizeCheck != null && timeBetweenSizeCheck > 0) {
      updatedAttributes = getUpdatedStableAttributes();
      if (updatedAttributes == null) {
        onFileDeleted();
      }
    }
    try {
      return getContentInputStream();
    } catch (RuntimeException e) {
      return new ExceptionInputStream(e);
    }
  }

  private org.mule.extension.sftp.api.FileAttributes getUpdatedStableAttributes() {
    org.mule.extension.sftp.api.FileAttributes oldAttributes;
    org.mule.extension.sftp.api.FileAttributes updatedAttributes = attributes;
    int retries = 0;
    do {
      oldAttributes = updatedAttributes;
      try {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug(format(STARTING_WAIT_MESSAGE, attributes.getPath()));
        }
        if (alreadyLoggedWarning.compareAndSet(false, true)) {
          LOGGER.warn(WAIT_WARNING_MESSAGE, attributes.getPath());
        }
        sleep(timeBetweenSizeCheck);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new MuleRuntimeException(createStaticMessage("Execution was interrupted while waiting to recheck file sizes"),
                                       e);
      }
      updatedAttributes = getUpdatedAttributes();
    } while (updatedAttributes != null && updatedAttributes.getSize() != oldAttributes.getSize()
        && retries++ < MAX_SIZE_CHECK_RETRIES);
    if (retries > MAX_SIZE_CHECK_RETRIES) {
      throw new FileBeingModifiedException(createStaticMessage(FILE_ON_PATH_MSG + attributes.getPath()
          + " is still being written."));
    }
    return updatedAttributes;
  }

  protected void onFileDeleted() {
    throw new DeletedFileWhileReadException(createStaticMessage(FILE_ON_PATH_MSG + attributes.getPath()
        + " was read but does not exist anymore."));
  }

  protected void onFileDeleted(Exception e) {
    throw new DeletedFileWhileReadException(createStaticMessage(FILE_ON_PATH_MSG + attributes.getPath()
        + " was read but does not exist anymore."), e);
  }

  /**
   * Gets the updated attributes of the file.
   * 
   * @return the updated attributes accourding to the path of the variable attributes passed in the constructor
   */
  protected abstract FileAttributes getUpdatedAttributes();

  /**
   * Gets the {@link InputStream} of the file described by the attributes passed to the constructor
   * 
   * @return the {@link InputStream} of the file
   */
  protected abstract InputStream getContentInputStream();

}
