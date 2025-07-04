/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.operation;

import static org.mule.runtime.core.api.util.StringUtils.isBlank;
import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED_TAB;

import static java.lang.String.format;
import static java.nio.file.Paths.get;

import org.mule.extension.sftp.api.CustomWriteBufferSize;
import org.mule.extension.sftp.api.FileAttributes;
import org.mule.extension.sftp.api.FileWriteMode;
import org.mule.extension.sftp.api.WriteStrategy;
import org.mule.extension.sftp.api.matcher.FileMatcher;
import org.mule.extension.sftp.api.matcher.NullFilePayloadPredicate;
import org.mule.extension.sftp.internal.config.FileConnectorConfig;
import org.mule.extension.sftp.internal.connection.FileSystem;
import org.mule.extension.sftp.internal.exception.IllegalContentException;
import org.mule.extension.sftp.internal.exception.IllegalPathException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.io.InputStream;
import java.util.List;
import java.util.function.Predicate;

import org.apache.tika.Tika;

/**
 * Basic set of operations and templates for extensions which perform operations over a generic file system
 *
 * @since 1.0
 */
@SuppressWarnings("rawtypes")
public abstract class BaseFileSystemOperations {

  /**
   * Lists all the files in the {@code directoryPath} which match the given {@code matcher}.
   * <p>
   * If the listing encounters a directory, the output list will include its contents depending on the value of the
   * {@code recursive} parameter. If {@code recursive} is enabled, then all the files in that directory will be listed immediately
   * after their parent directory.
   * <p>
   *
   * @param config               the config that is parameterizing this operation
   * @param directoryPath        the path to the directory to be listed
   * @param recursive            whether to include the contents of sub-directories. Defaults to false.
   * @param matchWith            a matcher used to filter the output list
   * @param timeBetweenSizeCheck wait time between size checks to determine if a file is ready to be read in milliseconds.
   * @return a {@link List} of {@link Result} objects each one containing each file's content in the payload and metadata in the
   *         attributes
   * @throws IllegalArgumentException if {@code directoryPath} points to a file which doesn't exist or is not a directory
   */
  protected List<Result<String, org.mule.extension.sftp.api.FileAttributes>> doList(FileConnectorConfig config,
                                                                                    FileSystem fileSystem,
                                                                                    String directoryPath,
                                                                                    boolean recursive,
                                                                                    FileMatcher matchWith,
                                                                                    Long timeBetweenSizeCheck) {
    fileSystem.changeToBaseDir();
    return fileSystem.list(config, directoryPath, recursive, getPredicate(matchWith), timeBetweenSizeCheck);
  }

  /**
   * Obtains the content and metadata of a file at a given path. The operation itself returns a {@link Message} which payload is a
   * {@link InputStream} with the file's content, and the metadata is represent as a
   * {@link org.mule.extension.sftp.api.FileAttributes} object that's placed as the message {@link Message#getAttributes()
   * attributes}.
   * <p>
   * If the {@code lock} parameter is set to {@code true}, then a file system level lock will be placed on the file until the
   * input stream this operation returns is closed or fully consumed. Because the lock is actually provided by the host file
   * system, its behavior might change depending on the mounted drive and the operation system on which mule is running. Take that
   * into consideration before blindly relying on this lock.
   * <p>
   * This method also makes a best effort to determine the mime type of the file being read. A {@link Tika} instance will be used
   * to make an educated guess on the file's mime type. The user also has the chance to force the output ?encoding and mimeType
   * through the {@code outputEncoding} and {@code outputMimeType} optional parameters.
   *
   * @param config               the config that is parameterizing this operation
   * @param fileSystem           a reference to the host {@link FileSystem}
   * @param path                 the path to the file to be read
   * @param lock                 whether or not to lock the file. Defaults to false.
   * @param timeBetweenSizeCheck wait time between size checks to determine if a file is ready to be read in milliseconds.
   * @return the file's content and metadata on a {@link org.mule.extension.sftp.api.FileAttributes} instance
   * @throws IllegalArgumentException if the file at the given path doesn't exist
   */
  protected Result<InputStream, org.mule.extension.sftp.api.FileAttributes> doRead(@Config FileConnectorConfig config,
                                                                                   @Connection FileSystem fileSystem,
                                                                                   @DisplayName("File Path") String path,
                                                                                   @Optional(defaultValue = "false") @Placement(
                                                                                       tab = ADVANCED_TAB) boolean lock,
                                                                                   Long timeBetweenSizeCheck) {
    fileSystem.changeToBaseDir();
    return fileSystem.read(config, path, lock, timeBetweenSizeCheck);
  }

  /**
   * Writes the {@code content} into the file pointed by {@code path}.
   * <p>
   * If the directory on which the file is attempting to be written doesn't exist, then the operation will either throw
   * {@link IllegalArgumentException} or create such folder depending on the value of the {@code createParentDirectory}.
   * <p>
   * If the file itself already exists, then the behavior depends on the supplied {@code mode}.
   * <p>
   * This operation also supports locking support depending on the value of the {@code lock} argument, but following the same
   * rules and considerations as described in the read operation.
   *
   * @param config                  the {@link FileConnectorConfig} on which the operation is being executed
   * @param fileSystem              a reference to the host {@link FileSystem}
   * @param path                    the path of the file to be written
   * @param content                 the content to be written into the file. Defaults to the current {@link Message} payload
   * @param createParentDirectories whether or not to attempt creating any parent directories which don't exists.
   * @param lock                    whether or not to lock the file. Defaults to false
   * @param mode                    a {@link org.mule.extension.sftp.api.FileWriteMode}. Defaults to {@code OVERWRITE}
   * @throws IllegalArgumentException   if an illegal combination of arguments is supplied
   */
  protected void doWrite(FileConnectorConfig config,
                         FileSystem fileSystem, String path, InputStream content,
                         boolean createParentDirectories, boolean lock, FileWriteMode mode) {
    if (content == null) {
      throw new IllegalContentException("Cannot write a null content");
    }

    validatePath(path, "path");
    fileSystem.changeToBaseDir();

    fileSystem.write(path, content, mode, lock, createParentDirectories, WriteStrategy.STANDARD,
                     CustomWriteBufferSize.BUFFER_SIZE_8KB);
  }

  /**
   * Copies the file at the {@code sourcePath} into the {@code targetPath}.
   * <p>
   * If {@code targetPath} doesn't exist, and neither does its parent, then an attempt will be made to create depending on the
   * value of the {@code createParentFolder} argument. If such argument is {@false}, then an {@link IllegalArgumentException} will
   * be thrown.
   * <p>
   * If the target file already exists, then it will be overwritten if the {@code overwrite} argument is {@code true}. Otherwise,
   * {@link IllegalArgumentException} will be thrown.
   * <p>
   * As for the {@code sourcePath}, it can either be a file or a directory. If it points to a directory, then it will be copied
   * recursively.
   *
   * @param config                  the config that is parameterizing this operation
   * @param fileSystem              a reference to the host {@link FileSystem}
   * @param sourcePath              the path to the file to be copied
   * @param targetPath              the target directory where the file is going to be copied
   * @param createParentDirectories whether or not to attempt creating any parent directories which don't exists.
   * @param overwrite               whether or not overwrite the file if the target destination already exists.
   * @param renameTo                the new file name, {@code null} if the file doesn't need to be renamed
   * @throws IllegalArgumentException if an illegal combination of arguments is supplied
   */
  protected void doCopy(FileConnectorConfig config, FileSystem fileSystem,
                        String sourcePath,
                        String targetPath, boolean createParentDirectories, boolean overwrite, String renameTo) {
    fileSystem.changeToBaseDir();
    validatePath(targetPath, "target path");
    validatePath(sourcePath, "source path");
    fileSystem.copy(config, sourcePath, targetPath, overwrite, createParentDirectories, renameTo);
  }

  /**
   * Moves the file at the {@code sourcePath} into the {@code targetPath}.
   * <p>
   * If {@code targetPath} doesn't exist, and neither does its parent, then an attempt will be made to create depending on the
   * value of the {@code createParentFolder} argument. If such argument is {@code false}, then an {@link IllegalArgumentException}
   * will be thrown.
   * <p>
   * If the target file already exists, then it will be overwritten if the {@code overwrite} argument is {@code true}. Otherwise,
   * {@link IllegalArgumentException} will be thrown.
   * <p>
   * As for the {@code sourcePath}, it can either be a file or a directory. If it points to a directory, then it will be moved
   * recursively.
   *
   * @param config                  the config that is parameterizing this operation
   * @param fileSystem              a reference to the host {@link FileSystem}
   * @param sourcePath              the path to the file to be copied
   * @param targetPath              the target directory
   * @param createParentDirectories whether or not to attempt creating any parent directories which don't exists.
   * @param overwrite               whether or not overwrite the file if the target destination already exists.
   * @param renameTo                the new file name, {@code null} if the file doesn't need to be renamed
   * @throws IllegalArgumentException if an illegal combination of arguments is supplied
   */
  protected void doMove(FileConnectorConfig config, FileSystem fileSystem, String sourcePath,
                        String targetPath, boolean createParentDirectories, boolean overwrite, String renameTo) {
    fileSystem.changeToBaseDir();
    validatePath(targetPath, "target path");
    validatePath(sourcePath, "source path");
    fileSystem.move(config, sourcePath, targetPath, overwrite, createParentDirectories, renameTo);
  }

  /**
   * Deletes the file pointed by {@code path}, provided that it's not locked
   *
   * @param fileSystem a reference to the host {@link FileSystem}
   * @param path       the path to the file to be deleted
   * @throws IllegalArgumentException if {@code filePath} doesn't exist or is locked
   */
  protected void doDelete(FileSystem fileSystem, @Optional String path) {
    fileSystem.changeToBaseDir();
    fileSystem.delete(path);
  }

  /**
   * Renames the file pointed by {@code path} to the name provided on the {@code to} parameter
   * <p>
   * {@code to} argument should not contain any path separator. {@link IllegalArgumentException} will be thrown if this
   * precondition is not honored.
   * 
   * @param fileSystem a reference to the host {@link FileSystem}
   * @param path       the path to the file to be renamed
   * @param to         the file's new name
   * @param overwrite  whether or not overwrite the file if the target destination already exists.
   */
  protected void doRename(@Connection FileSystem fileSystem, @Optional String path,
                          @DisplayName("New Name") String to, @Optional(defaultValue = "false") boolean overwrite) {
    if (get(to).getNameCount() != 1) {
      throw new IllegalPathException(
                                     format("'to' parameter of rename operation should not contain any file separator character but '%s' was received",
                                            to));
    }

    fileSystem.changeToBaseDir();
    fileSystem.rename(path, to, overwrite);
  }

  /**
   * Creates a new directory on {@code directoryPath}
   *
   * @param fileSystem    a reference to the host {@link FileSystem}
   * @param directoryPath the new directory's name
   */
  protected void doCreateDirectory(@Connection FileSystem fileSystem, String directoryPath) {
    validatePath(directoryPath, "directory path");
    fileSystem.changeToBaseDir();
    fileSystem.createDirectory(directoryPath);
  }

  private void validatePath(String path, String pathName) {
    if (isBlank(path)) {
      throw new IllegalPathException(format("%s cannot be null nor blank", pathName));
    }
  }

  private Predicate<FileAttributes> getPredicate(FileMatcher builder) {
    return builder != null ? builder.build() : new NullFilePayloadPredicate<FileAttributes>();
  }
}
