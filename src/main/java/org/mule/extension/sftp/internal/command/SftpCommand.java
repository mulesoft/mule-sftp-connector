/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.command;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.mule.extension.file.common.api.util.UriUtils.createUri;
import static org.mule.extension.file.common.api.util.UriUtils.isAbsolute;
import static org.mule.extension.sftp.internal.SftpUtils.normalizePath;

import org.mule.extension.file.common.api.FileAttributes;
import org.mule.extension.file.common.api.FileConnectorConfig;
import org.mule.extension.file.common.api.FileSystem;
import org.mule.extension.file.common.api.command.UriBasedFileCommand;
import org.mule.extension.file.common.api.exceptions.FileAlreadyExistsException;
import org.mule.extension.sftp.api.SftpFileAttributes;
import org.mule.extension.sftp.internal.SftpCopyDelegate;
import org.mule.extension.sftp.internal.connection.SftpClient;
import org.mule.extension.sftp.internal.connection.SftpFileSystem;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Stack;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for {@link UriBasedFileCommand} implementations that target a SFTP server
 *
 * @since 1.0
 */
public abstract class SftpCommand extends UriBasedFileCommand<SftpFileSystem> {

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
    URI uri = resolvePathIntoUri(normalizePath(filePath));
    SftpFileAttributes attributes;
    SftpFileAttributes attributes2;
    try {
      attributes2 = client.getAttributes(path);
      attributes = client.getAttributes(uri);
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

  @Override
  protected boolean exists(URI uri) {
    return getBaseUri(fileSystem).equals(uri) || ROOT.equals(uri.getPath()) || getFile(normalizePath(uri.getPath())) != null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected Path getBasePath(FileSystem fileSystem) {
    return Paths.get(getCurrentWorkingDirectory());
  }

  @Override
  protected URI getBaseUri(FileSystem fileSystem) {
    return URI.create(getCurrentWorkingDirectory());
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

    URI sourceUri = resolveExistingPathIntoUri(filePath);
    URI targetUri = createUri(sourceUri.resolve("..").getPath(), newName);

    if (exists(targetUri)) {
      if (!overwrite) {
        throw new FileAlreadyExistsException(format("'%s' cannot be renamed because '%s' already exists", sourceUri, targetUri));
      }

      try {
        //fileSystem.delete(target.toString());
        fileSystem.delete(targetUri.getPath());
      } catch (Exception e) {
        throw exception(format("Exception was found deleting '%s' as part of renaming '%s'", targetUri, sourceUri), e);
      }
    }

    try {
      doRename(sourceUri.getPath(), targetUri.getPath());
      LOGGER.debug("{} renamed to {}", filePath, newName);
    } catch (Exception e) {
      throw exception(format("Exception was found renaming '%s' to '%s'", sourceUri, newName), e);
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
    final URI uri = createUri(fileSystem.getBasePath(), directoryPath);
    FileAttributes targetFile2 = getFile(directoryPath);
    FileAttributes targetFile = getFile(directoryPath);

    if (targetFile != null) {
      throw new FileAlreadyExistsException(format("Directory '%s' already exists", uri.getPath()));
    }

    mkdirs(uri);
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
    URI targetUri = resolvePathIntoUri(target);
    FileAttributes targetFile2 = getFile(targetPath.toString());
    FileAttributes targetFile = getFile(targetUri.getPath());
    String targetFileName2 = isBlank(renameTo) ? Paths.get(source).getFileName().toString() : renameTo;
    String targetFileName = isBlank(renameTo) ? FilenameUtils.getName(source) : renameTo;

    if (targetFile != null) {
      if (targetFile.isDirectory()) {
        if (sourceFile.isDirectory() && sourceFile.getName().equals(targetFile.getName()) && !overwrite) {
          throw alreadyExistsException(targetUri);
        } else {
          Path sourcePath = resolvePath(targetFileName);
          URI sourceUri = resolvePathIntoUri(targetFileName);
          if (sourcePath.isAbsolute() && isAbsolute(sourceUri)) {
            targetPath = targetPath.resolve(sourcePath.getName(sourcePath.getNameCount() - 1));
            targetUri = createUri(targetUri.getPath(), FilenameUtils.getName(source));
          } else {
            targetPath = targetPath.resolve(targetFileName);
            targetUri = createUri(targetUri.getPath(), targetFileName);
          }
        }
      } else if (!overwrite) {
        throw alreadyExistsException(targetUri);
      }
    } else {
      if (createParentDirectory) {
        mkdirs(targetUri);
        targetPath = targetPath.resolve(targetFileName);
        targetUri = createUri(targetUri.getPath(), targetFileName);
      } else {
        throw pathNotFoundException(targetPath.toAbsolutePath());
      }
    }

    final String cwd = getCurrentWorkingDirectory();
    delegate.doCopy(config, sourceFile, targetUri, overwrite);
    LOGGER.debug("Copied '{}' to '{}'", sourceFile, targetUri);
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

  @Override
  protected void doMkDirs(URI directoryUri) {
    Stack<URI> fragments = new Stack<>();
    String[] subPaths = directoryUri.getPath().split("/");
    URI subUri = directoryUri;
    for (int i = subPaths.length - 1; i > 0; i--) {
      if (exists(subUri)) {
        break;
      }
      fragments.push(subUri);
      subUri = directoryUri.resolve("..");
    }

    while (!fragments.isEmpty()) {
      URI fragment = fragments.pop();
      client.mkdir(fragment.getPath());
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
