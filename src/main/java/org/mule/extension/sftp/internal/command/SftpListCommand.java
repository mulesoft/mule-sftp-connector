/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.command;

import static java.lang.Thread.sleep;
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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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

  public List<Result<InputStream, SftpFileAttributes>> list(FileConnectorConfig config,
                                                            String directoryPath,
                                                            boolean recursive,
                                                            Predicate<SftpFileAttributes> matcher,
                                                            long sizeCheckWaitTime) {
    if (sizeCheckWaitTime <= 0) {
      return list(config, directoryPath, recursive, matcher);
    }
    FileAttributes directoryAttributes = getExistingFile(directoryPath);
    Path path = Paths.get(directoryAttributes.getPath());

    if (!directoryAttributes.isDirectory()) {
      throw cannotListFileException(path);
    }
    List<Result<InputStream, SftpFileAttributes>> accumulator = new LinkedList<>();
    doListStable(config, directoryAttributes.getPath(), accumulator, recursive, matcher, sizeCheckWaitTime);
    return accumulator;
  }

  private void doListStable(FileConnectorConfig config, String path, List<Result<InputStream, SftpFileAttributes>> accumulator,
                            boolean recursive, Predicate<SftpFileAttributes> matcher, long sizeCheckWaitTime) {
    Map<String, SftpFileAttributes> filesBeforeDelayAccumulator = new HashMap<>();
    doListFileAttributes(config, path, filesBeforeDelayAccumulator, recursive, matcher);
    try {
      sleep(sizeCheckWaitTime);
    } catch (InterruptedException e) {
      throw exception("Execution was interrupted while waiting to recheck file sizes ", e);
    }
    Map<String, SftpFileAttributes> filesAfterDelayAccumulator = new HashMap<>();
    doListFileAttributes(config, path, filesAfterDelayAccumulator, recursive, matcher);

    for (Map.Entry<String, SftpFileAttributes> file : filesBeforeDelayAccumulator.entrySet()) {
      SftpFileAttributes fileAttributes = file.getValue();

      Long sizeBeforeDelay = fileAttributes.getSize();
      Long sizeAfterDelay = filesAfterDelayAccumulator.get(file.getKey()).getSize();
      if (sizeBeforeDelay.equals(sizeAfterDelay)) {
        if (isVirtualDirectory(fileAttributes.getName())) {
          continue;
        }
        if (fileAttributes.isDirectory()) {
          if (matcher.test(fileAttributes)) {
            accumulator.add(Result.<InputStream, SftpFileAttributes>builder().output(null).attributes(fileAttributes).build());
          }
        } else {
          if (matcher.test(fileAttributes)) {
            accumulator.add(fileSystem.read(config, fileAttributes.getPath(), false));
          }
        }
      }
    }
  }

  private void doListFileAttributes(FileConnectorConfig config,
                                    String path,
                                    Map<String, SftpFileAttributes> accumulator,
                                    boolean recursive,
                                    Predicate<SftpFileAttributes> matcher) {
    LOGGER.debug("Listing directory {}", path);
    for (SftpFileAttributes file : client.list(path)) {
      if (isVirtualDirectory(file.getName())) {
        continue;
      }
      if (file.isDirectory() && recursive) {
        doListFileAttributes(config, file.getPath(), accumulator, recursive, matcher);
      }
      if (matcher.test(file)) {
        accumulator.put(file.getPath(), file);
      }
    }
  }

  private void doList(FileConnectorConfig config,
                      String path,
                      List<Result<InputStream, SftpFileAttributes>> accumulator,
                      boolean recursive,
                      Predicate<SftpFileAttributes> matcher) {

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
          doList(config, file.getPath(), accumulator, recursive, matcher);
        }
      } else {
        if (matcher.test(file)) {
          accumulator.add(fileSystem.read(config, file.getPath(), false));
        }
      }
    }
  }
}
