/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.command;

import org.mule.extension.sftp.api.FileAttributes;
import org.mule.extension.sftp.api.FileConnectorConfig;
import org.mule.extension.sftp.api.SftpFileAttributes;
import org.mule.extension.sftp.api.command.ListCommand;
import org.mule.extension.sftp.internal.connection.SftpClient;
import org.mule.extension.sftp.internal.connection.SftpFileSystem;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.slf4j.Logger;

import java.io.InputStream;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

import static org.mule.extension.sftp.api.util.UriUtils.createUri;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * A {@link SftpCommand} which implements the {@link ListCommand} contract
 *
 * @since 1.0
 */
public final class SftpListCommand extends SftpCommand implements ListCommand<SftpFileAttributes> {

  private static final Logger LOGGER = getLogger(SftpListCommand.class);
  private final SftpReadCommand sftpReadCommand;

  /**
   * {@inheritDoc}
   */
  public SftpListCommand(SftpFileSystem fileSystem, SftpClient client, SftpReadCommand sftpReadCommand) {
    super(fileSystem, client);
    this.sftpReadCommand = sftpReadCommand;
  }

  /**
   * {@inheritDoc}
   */
  public List<Result<InputStream, SftpFileAttributes>> list(FileConnectorConfig config,
                                                            String directoryPath,
                                                            boolean recursive,
                                                            Predicate<SftpFileAttributes> matcher,
                                                            Long timeBetweenSizeCheck) {

    FileAttributes directoryAttributes = getExistingFile(directoryPath);
    URI uri = createUri(directoryAttributes.getPath(), "");

    if (!directoryAttributes.isDirectory()) {
      throw cannotListFileException(uri);
    }

    List<Result<InputStream, SftpFileAttributes>> accumulator = new LinkedList<>();
    doList(config, directoryAttributes.getPath(), accumulator, recursive, matcher, timeBetweenSizeCheck);

    return accumulator;
  }

  private void doList(FileConnectorConfig config,
                      String path,
                      List<Result<InputStream, SftpFileAttributes>> accumulator,
                      boolean recursive,
                      Predicate<SftpFileAttributes> matcher,
                      Long timeBetweenSizeCheck) {


    LOGGER.debug("Listing directory {}", path);
    for (SftpFileAttributes file : client.list(path)) {

      if (isVirtualDirectory(file.getName())) {
        continue;
      }
      if (file.isDirectory()) {
        if (matcher.test(file)) {
          accumulator.add(Result.<InputStream, SftpFileAttributes>builder().output(null).attributes(file).build());
        }
        if (recursive) {
          doList(config, file.getPath(), accumulator, recursive, matcher, timeBetweenSizeCheck);
        }
      } else {
        if (matcher.test(file)) {
          accumulator.add(sftpReadCommand.read(config, file, false, timeBetweenSizeCheck));
        }
      }
    }
  }
}
