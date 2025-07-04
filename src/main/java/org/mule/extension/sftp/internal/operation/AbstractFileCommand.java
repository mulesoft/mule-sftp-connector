/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.operation;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import static java.lang.String.format;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.extension.sftp.internal.exception.FileAccessDeniedException;
import org.mule.extension.sftp.internal.exception.FileAlreadyExistsException;
import org.mule.extension.sftp.internal.exception.IllegalPathException;
import org.mule.extension.sftp.internal.config.FileConnectorConfig;
import org.mule.extension.sftp.internal.connection.FileSystem;
import org.mule.runtime.api.exception.MuleRuntimeException;

import java.io.Serializable;
import java.util.concurrent.locks.Lock;
import java.util.function.Predicate;

import org.slf4j.Logger;

/**
 * Base class for implementations of the Command design pattern which performs operations on a file system
 *
 * @param <F> the generic type of the {@link FileSystem} on which the operation is performed
 * @param <I> generic type for a class that identifies a file or directory.
 * @since 1.3.0
 */
public abstract class AbstractFileCommand<F extends FileSystem, I extends Serializable> {

  private static final Logger LOGGER = getLogger(AbstractFileCommand.class);

  protected final F fileSystem;

  /**
   * Creates a new instance
   *
   * @param fileSystem the {@link FileSystem} on which the operation is performed
   */
  protected AbstractFileCommand(F fileSystem) {
    this.fileSystem = fileSystem;
  }

  /**
   * Returns true if the given {@code path} exists
   *
   * @param path the path to test
   * @return whether the {@code path} exists
   */
  protected abstract boolean exists(I path);

  /**
   * Method that, given a path, checks if its parent folder exists. If it exists, this method returns {@code void}. Otherwise,
   * this method allows to create such parent by setting the the createParentFolder parameter to true. If it is set to false and
   * the parent folder does not exist, this method throws {@link IllegalPathException}.
   *
   * @param path               the path to test
   * @param createParentFolder indicates whether to create the parent folder or not.
   *
   * @return {@code void} if the method completed correctly.
   * @throws {@link IllegalPathException} if the parent path does not exists and createParentFolder is set to false.
   */
  protected void assureParentFolderExists(I path, boolean createParentFolder) {
    if (exists(path)) {
      return;
    }

    I parentFolder = getParent(path);
    if (!exists(parentFolder)) {
      if (createParentFolder) {
        mkdirs(parentFolder);
      } else {
        throw new IllegalPathException(format("Cannot write to file '%s' because path to it doesn't exist. Consider setting the 'createParentDirectories' attribute to 'true'",
                                              pathToString(path)));
      }
    }
  }

  /**
   * Creates the directory pointed by {@code directoryPath} also creating any missing parent directories
   *
   * @param directoryPath the path to the directory you want to create
   */
  protected void mkdirs(I directoryPath) {
    Lock lock = fileSystem.createMuleLock(format("%s-mkdirs-%s", getClass().getName(), directoryPath));
    lock.lock();
    try {
      // verify no other thread beat us to it
      if (exists(directoryPath)) {
        return;
      }
      doMkDirs(directoryPath);
    } finally {
      lock.unlock();
    }

    LOGGER.debug("Directory '{}' created", directoryPath);
  }

  /**
   * Returns an absolute path for the given {@code filePath}
   *
   * @param filePath the relative path to a file or directory
   * @return an absolute path
   */
  protected I resolvePath(String filePath) {
    I path = getBasePath(fileSystem);
    if (filePath != null) {
      path = resolvePath(path, filePath);
    }
    return getAbsolutePath(path);
  }

  /**
   * @param fileName the name of a file
   * @return {@code true} if {@code fileName} equals to &quot;.&quot; or &quot;..&quot;
   */
  protected boolean isVirtualDirectory(String fileName) {
    return ".".equals(fileName) || "..".equals(fileName);
  }

  /**
   * Similar to {@link #resolvePath(String)} only that it throws a {@link IllegalArgumentException} if the given path doesn't
   * exist.
   * <p>
   * The existence of the obtained path is verified by delegating into {@link #exists(I)}
   *
   * @param filePath the path to a file or directory
   * @return an absolute path
   */
  protected I resolveExistingPath(String filePath) {
    I path = resolvePath(filePath);
    if (!exists(path)) {
      throw pathNotFoundException(path);
    }

    return path;
  }

  /**
   * Returns an {@link IllegalPathException} explaining that a {@link FileSystem#read(FileConnectorConfig, String, boolean)}
   * operation was attempted on a {@code path} pointing to a directory
   *
   * @param path the path on which a read was attempted
   * @return {@link IllegalPathException}
   */
  protected IllegalPathException cannotReadDirectoryException(I path) {
    return new IllegalPathException(format("Cannot read path '%s' since it's a directory", pathToString(path)));
  }

  /**
   * Returns a {@link IllegalPathException} explaining that a
   * {@link FileSystem#list(FileConnectorConfig, String, boolean, Predicate)} operation was attempted on a {@code path} pointing
   * to a file.
   *
   * @param path the path on which a list was attempted
   * @return {@link IllegalPathException}
   */
  protected IllegalPathException cannotListFileException(I path) {
    return new IllegalPathException(format("Cannot list path '%s' because it's a file. Only directories can be listed",
                                           pathToString(path)));
  }

  /**
   * Returns a {@link IllegalPathException} explaining that a
   * {@link FileSystem#list(FileConnectorConfig, String, boolean, Predicate)} operation was attempted on a {@code path} pointing
   * to a file.
   *
   * @param path the on which a list was attempted
   * @return {@link RuntimeException}
   */
  protected IllegalPathException pathNotFoundException(I path) {
    return new IllegalPathException(format("Path '%s' doesn't exist", pathToString(path)));
  }

  /**
   * Returns a {@link IllegalPathException} explaining that an operation is trying to write to the given {@code path} but it
   * already exists and no overwrite instruction was provided.
   *
   * @param path the that the operation tried to modify
   * @return {@link IllegalPathException}
   */
  public FileAlreadyExistsException alreadyExistsException(I path) {
    return new FileAlreadyExistsException(format("'%s' already exists. Set the 'overwrite' parameter to 'true' to perform the operation anyway",
                                                 pathToString(path)));
  }

  /**
   * Returns an {@link IllegalPathException} explaining that an operation is trying to read to the given {@code path} but
   * user does not have permission to read
   *
   * @param path the path on which a read was attempted
   * @return {@link IllegalPathException}
   */
  protected FileAccessDeniedException cannotReadFileException(I path) {
    throw new FileAccessDeniedException(format("Cannot read file '%s' since user does not have read permission",
                                               pathToString(path)));
  }

  /**
   * Returns a properly formatted {@link MuleRuntimeException} for the given {@code message} and {@code cause}
   *
   * @param message the exception's message
   * @return a {@link RuntimeException}
   */
  public RuntimeException exception(String message) {
    return new MuleRuntimeException(createStaticMessage(message));
  }

  /**
   * Returns a properly formatted {@link MuleRuntimeException} for the given {@code message} and {@code cause}
   *
   * @param message the exception's message
   * @param cause   the exception's cause
   * @return {@link RuntimeException}
   */
  public RuntimeException exception(String message, Exception cause) {
    return new MuleRuntimeException(createStaticMessage(message), cause);
  }

  /**
   * Returns the <em>parent path</em>, or {@code null} if this path does not have a parent.
   *
   * <p>
   * The parent of this path object consists of this path's root component, if any, and each element in the path except for the
   * <em>farthest</em> from the root in the directory hierarchy. This method does not access the file system; the path or its
   * parent may not exist. Furthermore, this method does not eliminate special names such as "." and ".." that may be used in some
   * implementations. On UNIX for example, the parent of "{@code /a/b/c}" is "{@code /a/b}", and the parent of {@code "x/y/.}" is
   * "{@code x/y}".
   *
   * @return a path representing the path's parent
   */
  protected abstract I getParent(I path);

  /**
   * Returns a path to which all non absolute paths are relative to
   *
   * @param fileSystem the file system that we're connecting to
   * @return a not {@code null} base path for the filesystem
   */
  protected abstract I getBasePath(FileSystem fileSystem);

  /**
   * Resolve the given basePath against the filePath.
   *
   * <p>
   * If the {@code filePath} parameter is an absolute path then this method trivially returns {@code filePath}. If
   * {@code filePath} is an <i>empty path</i> then this method trivially returns basePath. Otherwise this method considers the
   * basePath to be a directory and resolves the given filePath against the basePath. In the simplest case, the given filePath
   * does not have a root component, in which case this method <em>joins</em> both paths and returns a resulting path that ends
   * with the given filePath. Where the given filePath has a root component then resolution is highly implementation dependent and
   * therefore unspecified.
   *
   * @param basePath the base path considered as a directory
   * @param filePath the path to resolve against the basePath
   *
   * @return the resulting path
   *
   */
  protected abstract I resolvePath(I basePath, String filePath);

  /**
   * Returns an object representing the absolute path for the given path.
   *
   * <p>
   * If the given path is already absolute then this method simply returns it. Otherwise, this method resolves the path in an
   * implementation dependent manner, typically by resolving the path against a file system default directory. Depending on the
   * implementation, this method may throw an I/O error if the file system is not accessible.
   *
   * @param path the given path
   *
   * @return the absolute path
   */
  protected abstract I getAbsolutePath(I path);

  /**
   * @return the path identified by {@code <I>} as a String.
   */
  protected abstract String pathToString(I path);

  /**
   * Creates the directory pointed by {@code directoryPath}.
   *
   * @param directoryPath the path to the directory you want to create
   */
  protected abstract void doMkDirs(I directoryPath);

}
