/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.operation;

import static org.mule.extension.sftp.internal.util.UriUtils.createUri;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.extension.sftp.api.FileAttributes;
import org.mule.extension.sftp.api.SftpFileAttributes;
import org.mule.extension.sftp.internal.config.FileConnectorConfig;
import org.mule.extension.sftp.internal.connection.SftpClient;
import org.mule.extension.sftp.internal.connection.SftpFileSystemConnection;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

import org.slf4j.Logger;

/**
 * A {@link SftpCommand} which implements the {@link ListCommand} contract
 *
 * @since 1.0
 */
public final class SftpListCommand extends SftpCommand implements ListCommand<SftpFileAttributes> {

  private static final Logger LOGGER = getLogger(SftpListCommand.class);

  /**
   * {@inheritDoc}
   */
  public SftpListCommand(SftpFileSystemConnection fileSystem, SftpClient client) {
    super(fileSystem, client);
  }

  /**
   * {@inheritDoc}
   */
  public List<Result<String, SftpFileAttributes>> list(FileConnectorConfig config,
                                                       String directoryPath,
                                                       boolean recursive,
                                                       Predicate<SftpFileAttributes> matcher,
                                                       Long timeBetweenSizeCheck) {

    FileAttributes directoryAttributes = getExistingFile(directoryPath);
    URI uri = createUri(directoryAttributes.getPath(), "");

    if (!directoryAttributes.isDirectory()) {
      throw cannotListFileException(uri);
    }

    List<Result<String, SftpFileAttributes>> accumulator = new LinkedList<>();
    doList(config, directoryAttributes.getPath(), accumulator, recursive, matcher, timeBetweenSizeCheck);

    return accumulator;
  }

  private void doList(FileConnectorConfig config,
                      String path,
                      List<Result<String, SftpFileAttributes>> accumulator,
                      boolean recursive,
                      Predicate<SftpFileAttributes> matcher,
                      Long timeBetweenSizeCheck) {


    LOGGER.trace("Listing directory trace {}", path);
    for (SftpFileAttributes file : client.list(path)) {

      if (isVirtualDirectory(file.getName())) {
        continue;
      }
      if (file.isDirectory()) {
        if (matcher.test(file)) {
          LOGGER.debug("Listing directory debug {}", path);
          accumulator.add(Result.<String, SftpFileAttributes>builder().output(file.getPath()).attributes(file).build());
        }
        if (recursive) {
          doList(config, file.getPath(), accumulator, recursive, matcher, timeBetweenSizeCheck);
        }
      } else {
        if (matcher.test(file)) {
          LOGGER.debug("Listing directory debug {}", path);
          accumulator.add(Result.<String, SftpFileAttributes>builder().output(file.getPath()).attributes(file)
              .build());
        }
      }
    }
  }
}
