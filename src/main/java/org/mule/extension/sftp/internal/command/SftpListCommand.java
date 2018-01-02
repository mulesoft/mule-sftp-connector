/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.command;

import static org.slf4j.LoggerFactory.getLogger;
import org.mule.extension.file.common.api.FileAttributes;
import org.mule.extension.file.common.api.FileConnectorConfig;
import org.mule.extension.file.common.api.command.ListCommand;
import org.mule.extension.sftp.api.SftpFileAttributes;
import org.mule.extension.sftp.internal.connection.SftpClient;
import org.mule.extension.sftp.internal.connection.SftpFileSystem;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
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
  public SftpListCommand(SftpFileSystem fileSystem, SftpClient client) {
    super(fileSystem, client);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Result<InputStream, SftpFileAttributes>> list(FileConnectorConfig config,
                                                            String directoryPath,
                                                            boolean recursive,
                                                            Predicate<SftpFileAttributes> matcher) {

    FileAttributes directoryAttributes = getExistingFile(directoryPath);
    Path path = Paths.get(directoryAttributes.getPath());

    if (!directoryAttributes.isDirectory()) {
      throw cannotListFileException(path);
    }

    List<Result<InputStream, SftpFileAttributes>> accumulator = new LinkedList<>();
    doList(config, directoryAttributes.getPath(), accumulator, recursive, matcher);

    return accumulator;
  }

  private void doList(FileConnectorConfig config,
                      String path,
                      List<Result<InputStream, SftpFileAttributes>> accumulator,
                      boolean recursive,
                      Predicate<SftpFileAttributes> matcher) {

    LOGGER.debug("Listing directory {}", path);
    for (SftpFileAttributes file : client.list(path)) {
      if (isVirtualDirectory(file.getName()) || !matcher.test(file)) {
        continue;
      }

      if (file.isDirectory()) {
        accumulator.add(Result.<InputStream, SftpFileAttributes>builder().output(null).attributes(file).build());

        if (recursive) {
          doList(config, file.getPath(), accumulator, recursive, matcher);
        }
      } else {
        accumulator.add(fileSystem.read(config, file.getPath(), false));
      }
    }
  }
}
