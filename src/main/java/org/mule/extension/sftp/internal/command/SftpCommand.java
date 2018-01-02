/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.command;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.mule.extension.sftp.internal.SftpUtils.normalizePath;
import org.mule.extension.file.common.api.FileAttributes;
import org.mule.extension.file.common.api.FileConnectorConfig;
import org.mule.extension.file.common.api.FileSystem;
import org.mule.extension.file.common.api.command.FileCommand;
import org.mule.extension.file.common.api.exceptions.FileAlreadyExistsException;
import org.mule.extension.sftp.api.SftpFileAttributes;
import org.mule.extension.sftp.internal.SftpCopyDelegate;
import org.mule.extension.sftp.internal.connection.SftpClient;
import org.mule.extension.sftp.internal.connection.SftpFileSystem;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for {@link FileCommand} implementations that target a SFTP server
 *
 * @since 1.0
 */
public abstract class SftpCommand extends FileCommand<SftpFileSystem> {

  private static final Logger LOGGER = LoggerFactory.getLogger(SftpCommand.class);
  protected static final String ROOT = "/";

  protected final SftpClient client;

  public SftpCommand(SftpFileSystem fileSystem) {
    this(fileSystem, fileSystem.getClient());
  }

  /**
   * Creates a new instance
   *
   * @param fileSystem a {@link SftpFileSystem} used as the connection object
   * @param client a {@link SftpClient}
   */
  public SftpCommand(SftpFileSystem fileSystem, SftpClient client) {
    super(fileSystem);
    this.client = client;
  }

  /**
   * Similar to {@link #getFile(String)} but throwing an {@link IllegalArgumentException} if the
   * {@code filePath} doesn't exist
   *
   * @param filePath the path to the file you want
   * @return a {@link SftpFileAttributes}
   * @throws IllegalArgumentException if the {@code filePath} doesn't exist
   */
  protected SftpFileAttributes getExistingFile(String filePath) {
    return getFile(filePath, true);
  }

  /**
   * Obtains a {@link SftpFileAttributes} for the given {@code filePath}
   *
   * @param filePath the path to the file you want
   * @return a {@link SftpFileAttributes} or {@code null} if it doesn't exist
   */
  public SftpFileAttributes getFile(String filePath) {
    return getFile(filePath, false);
  }

  protected SftpFileAttributes getFile(String filePath, boolean requireExistence) {
    Path path = resolvePath(normalizePath(filePath));
    SftpFileAttributes attributes;
    try {
      attributes = client.getAttributes(path);
    } catch (Exception e) {
      throw exception("Found exception trying to obtain path " + path, e);
    }

    if (attributes != null) {
      return attributes;
    } else {
      if (requireExistence) {
        throw pathNotFoundException(path);
      } else {
        return null;
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean exists(Path path) {
    return getBasePath(fileSystem).equals(path) || ROOT.equals(path.toString()) || getFile(normalizePath(path)) != null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected Path getBasePath(FileSystem fileSystem) {
    return Paths.get(getCurrentWorkingDirectory());
  }

  /**
   * Changes the current working directory to the given {@code path}
   *
   * @param path the {@link Path} to which you wish to move
   * @throws IllegalArgumentException if the CWD could not be changed
   */
  protected void changeWorkingDirectory(Path path) {
    changeWorkingDirectory(normalizePath(path.toString()));
  }

  /**
   * Changes the current working directory to the given {@code path}
   *
   * @param path the path to which you wish to move
   * @throws IllegalArgumentException if the CWD could not be changed
   */
  protected void changeWorkingDirectory(String path) {
    if (!tryChangeWorkingDirectory(path)) {
      throw new IllegalArgumentException(format("Could not change working directory to '%s'. Path doesn't exist or is not a directory",
                                                path.toString()));
    }
    LOGGER.debug("working directory changed to {}", path);
  }

  /**
   * Returns a {@link Path} relative to the {@code basePath} and the given {@code filePath}
   *
   * @param filePath the path to a file or directory
   * @return a relative {@link Path}
   */
  @Override
  protected Path resolvePath(String filePath) {
    Path path = getBasePath(fileSystem);
    if (filePath != null) {
      path = path.resolve(filePath);
    }

    return path;
  }

  /**
   * Attempts to change the current working directory. If it was not possible (for example, because it doesn't exist), it returns
   * {@code false}
   *
   * @param path the path to which you wish to move
   * @return {@code true} if the CWD was changed. {@code false} otherwise
   */
  protected boolean tryChangeWorkingDirectory(String path) {
    try {
      client.changeWorkingDirectory(normalizePath(path));
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Template method that renames the file at {@code filePath} to {@code newName}.
   * <p>
   * This method performs path resolution and validation and eventually delegates into {@link #doRename(String, String)}, in which
   * the actual renaming implementation is.
   *
   * @param filePath the path of the file to be renamed
   * @param newName the new name
   * @param overwrite whether to overwrite the target file if it already exists
   */
  protected void rename(String filePath, String newName, boolean overwrite) {
    Path source = resolveExistingPath(filePath);
    Path target = source.getParent().resolve(newName);

    if (exists(target)) {
      if (!overwrite) {
        throw new FileAlreadyExistsException(format("'%s' cannot be renamed because '%s' already exists", source, target));
      }

      try {
        fileSystem.delete(target.toString());
      } catch (Exception e) {
        throw exception(format("Exception was found deleting '%s' as part of renaming '%s'", target, source), e);
      }
    }

    try {
      doRename(source.toString(), target.toString());
      LOGGER.debug("{} renamed to {}", filePath, newName);
    } catch (Exception e) {
      throw exception(format("Exception was found renaming '%s' to '%s'", source, newName), e);
    }
  }

  /**
   * Template method which works in tandem with {@link #rename(String, String, boolean)}.
   * <p>
   * Implementations are to perform the actual renaming logic here
   *
   * @param filePath the path of the file to be renamed
   * @param newName the new name
   * @throws Exception if anything goes wrong
   */
  protected void doRename(String filePath, String newName) throws Exception {
    client.rename(normalizePath(filePath), newName);
  }


  protected void createDirectory(String directoryPath) {
    final Path path = Paths.get(fileSystem.getBasePath()).resolve(directoryPath);
    FileAttributes targetFile = getFile(directoryPath);

    if (targetFile != null) {
      throw new FileAlreadyExistsException(format("Directory '%s' already exists", path.toAbsolutePath()));
    }

    mkdirs(path);
  }

  /**
   * Performs the base logic and delegates into
   * {@link SftpCopyDelegate#doCopy(FileConnectorConfig, FileAttributes, Path, boolean)} to perform the actual
   * copying logic
   *  @param config the config that is parameterizing this operation
   * @param source the path to be copied
   * @param target the path to the target destination
   * @param overwrite whether to overwrite existing target paths
   * @param createParentDirectory whether to create the target's parent directory if it doesn't exist
   */
  protected final void copy(FileConnectorConfig config, String source, String target, boolean overwrite,
                            boolean createParentDirectory, String renameTo, SftpCopyDelegate delegate) {
    FileAttributes sourceFile = getExistingFile(source);
    Path targetPath = resolvePath(target);
    FileAttributes targetFile = getFile(targetPath.toString());
    String targetFileName = isBlank(renameTo) ? Paths.get(source).getFileName().toString() : renameTo;

    if (targetFile != null) {
      if (targetFile.isDirectory()) {
        if (sourceFile.isDirectory() && sourceFile.getName().equals(targetFile.getName()) && !overwrite) {
          throw alreadyExistsException(targetPath);
        } else {
          Path sourcePath = resolvePath(targetFileName);
          if (sourcePath.isAbsolute()) {
            targetPath = targetPath.resolve(sourcePath.getName(sourcePath.getNameCount() - 1));
          } else {
            targetPath = targetPath.resolve(targetFileName);
          }
        }
      } else if (!overwrite) {
        throw alreadyExistsException(targetPath);
      }
    } else {
      if (createParentDirectory) {
        mkdirs(targetPath);
        targetPath = targetPath.resolve(targetFileName);
      } else {
        throw pathNotFoundException(targetPath.toAbsolutePath());
      }
    }

    final String cwd = getCurrentWorkingDirectory();
    delegate.doCopy(config, sourceFile, targetPath, overwrite);
    LOGGER.debug("Copied '{}' to '{}'", sourceFile, targetPath);
    changeWorkingDirectory(cwd);
  }

  /**
   * Creates the directory pointed by {@code directoryPath} also creating any missing parent directories
   *
   * @param directoryPath the {@link Path} to the directory you want to create
   */
  @Override
  protected void doMkDirs(Path directoryPath) {
    Stack<Path> fragments = new Stack<>();
    for (int i = directoryPath.getNameCount(); i > 0; i--) {
      Path subPath = Paths.get("/").resolve(directoryPath.subpath(0, i));
      if (exists(subPath)) {
        break;
      }
      fragments.push(subPath);
    }

    while (!fragments.isEmpty()) {
      Path fragment = fragments.pop();
      client.mkdir(fragment.toString());
    }
  }

  /**
   * @return the path of the current working directory
   */
  protected String getCurrentWorkingDirectory() {
    try {
      return normalizePath(client.getWorkingDirectory());
    } catch (Exception e) {
      throw exception("Failed to determine current working directory");
    }
  }
}
