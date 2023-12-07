/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.operation;

import static org.mule.runtime.api.meta.model.display.PathModel.Location.EXTERNAL;
import static org.mule.runtime.api.meta.model.display.PathModel.Type.DIRECTORY;
import static org.mule.runtime.api.meta.model.display.PathModel.Type.FILE;
import static org.mule.runtime.core.api.util.StringUtils.isBlank;
import static org.mule.runtime.extension.api.annotation.param.MediaType.ANY;
import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED_TAB;

import org.mule.extension.sftp.api.FileAttributes;
import org.mule.extension.sftp.api.FileWriteMode;
import org.mule.extension.sftp.api.SftpFileAttributes;
import org.mule.extension.sftp.api.SftpFileMatcher;
import org.mule.extension.sftp.internal.connection.FileSystem;
import org.mule.extension.sftp.internal.connection.SftpFileSystemConnection;
import org.mule.extension.sftp.internal.error.provider.FileCopyErrorTypeProvider;
import org.mule.extension.sftp.internal.error.provider.FileDeleteErrorTypeProvider;
import org.mule.extension.sftp.internal.error.provider.FileListErrorTypeProvider;
import org.mule.extension.sftp.internal.error.provider.FileReadErrorTypeProvider;
import org.mule.extension.sftp.internal.error.provider.FileRenameErrorTypeProvider;
import org.mule.extension.sftp.internal.error.provider.FileWriteErrorTypeProvider;
import org.mule.extension.sftp.internal.exception.IllegalContentException;
import org.mule.extension.sftp.internal.exception.IllegalPathException;
import org.mule.extension.sftp.internal.extension.SftpConnector;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.ConfigOverride;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Path;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Ftp connector operations
 *
 * @since 1.0
 */
public final class SftpOperations extends BaseFileSystemOperations {

  /**
   * Lists all the files in the {@code directoryPath} which match the given {@code matcher}.
   * <p>
   * If the listing encounters a directory, the output list will include its contents depending on the value of the
   * {@code recursive} parameter.
   * <p>
   * If {@code recursive} is set to {@code true} but a found directory is rejected by the {@code matcher}, then there won't be any
   * recursion into such directory.
   *
   * @param config                   the config that is parameterizing this operation
   * @param fileSystem               a reference to the host {@link FileSystem}
   * @param directoryPath            the path to the directory to be listed
   * @param recursive                whether to include the contents of sub-directories. Defaults to false.
   * @param matcher                  a matcher used to filter the output list
   * @param timeBetweenSizeCheck     wait time between size checks to determine if a file is ready to be read.
   * @param timeBetweenSizeCheckUnit time unit to be used in the wait time between size checks.
   * @return a {@link List} of {@link Message messages} each one containing each file's path in the payload and metadata in the
   *         attributes
   * @throws IllegalArgumentException if {@code directoryPath} points to a file which doesn't exist or is not a directory
   */
  @Summary("List all the files from given directory")
  @MediaType(value = ANY, strict = false)
  @Throws(FileListErrorTypeProvider.class)
  public List<Result<String, SftpFileAttributes>> list(@Config SftpConnector config,
                                                       @Connection SftpFileSystemConnection fileSystem,
                                                       @DisplayName("Directory Path") @Path(type = DIRECTORY,
                                                           location = EXTERNAL) String directoryPath,
                                                       @Optional(defaultValue = "false") boolean recursive,
                                                       @Optional @DisplayName("File Matching Rules") @Summary("Matcher to filter the listed files") SftpFileMatcher matcher,
                                                       @ConfigOverride @Placement(tab = ADVANCED_TAB) Long timeBetweenSizeCheck,
                                                       @ConfigOverride @Placement(
                                                           tab = ADVANCED_TAB) TimeUnit timeBetweenSizeCheckUnit) {
    List result =
        doList(config, fileSystem, directoryPath, recursive, matcher,
               config.getTimeBetweenSizeCheckInMillis(timeBetweenSizeCheck, timeBetweenSizeCheckUnit).orElse(null));
    return (List<Result<String, SftpFileAttributes>>) result;
  }

  /**
   * Obtains the content and metadata of a file at a given path. The operation itself returns a {@link Message} which payload is a
   * {@link InputStream} with the file's content, and the metadata is represent as a {@link SftpFileAttributes} object that's
   * placed as the message {@link Message#getAttributes() attributes}.
   * <p>
   * If the {@code lock} parameter is set to {@code true}, then a file system level lock will be placed on the file until the
   * input stream this operation returns is closed or fully consumed. Because the lock is actually provided by the host file
   * system, its behavior might change depending on the mounted drive and the operation system on which mule is running. Take that
   * into consideration before blindly relying on this lock.
   * <p>
   * This method also makes a best effort to determine the mime type of the file being read. The file's extension will be used to
   * make an educated guess on the file's mime type. The user also has the chance to force the output encoding and mimeType
   * through the {@code outputEncoding} and {@code outputMimeType} optional parameters.
   *
   * @param config                   the config that is parameterizing this operation
   * @param fileSystem               a reference to the host {@link FileSystem}
   * @param path                     the path to the file to be read
   * @param lock                     whether or not to lock the file. Defaults to false.
   * @param timeBetweenSizeCheck     wait time between size checks to determine if a file is ready to be read.
   * @param timeBetweenSizeCheckUnit time unit to be used in the wait time between size checks.
   * @return the file's content and metadata on a {@link FileAttributes} instance
   * @throws IllegalArgumentException if the file at the given path doesn't exist
   */
  @Summary("Obtains the content and metadata of a file at a given path")
  @Throws(FileReadErrorTypeProvider.class)
  @MediaType(value = ANY, strict = false)
  public Result<InputStream, SftpFileAttributes> read(@Config SftpConnector config,
                                                      @Connection SftpFileSystemConnection fileSystem,
                                                      @DisplayName("File Path") @Path(type = FILE,
                                                          location = EXTERNAL) String path,
                                                      @Optional(defaultValue = "false") @Placement(
                                                          tab = ADVANCED_TAB) boolean lock,
                                                      @ConfigOverride @Placement(tab = ADVANCED_TAB) Long timeBetweenSizeCheck,
                                                      @ConfigOverride @Placement(
                                                          tab = ADVANCED_TAB) TimeUnit timeBetweenSizeCheckUnit) {
    Result result =
        doRead(config, fileSystem, path, lock,
               config.getTimeBetweenSizeCheckInMillis(timeBetweenSizeCheck, timeBetweenSizeCheckUnit).orElse(null));
    return (Result<InputStream, SftpFileAttributes>) result;
  }

  /**
   * Writes the {@code content} into the file pointed by {@code path}.
   * <p>
   * If the directory on which the file is attempting to be written doesn't exist, then the operation will either throw
   * {@code SFTP:ILLEGAL_PATH} error or create such folder depending on the value of the {@code createParentDirectory}.
   * <p>
   * If the file itself already exists, then the behavior depends on the supplied {@code mode}.
   * <p>
   * This operation also supports locking support depending on the value of the {@code lock} argument, but following the same
   * rules and considerations as described in the read operation.
   *
   * @param fileSystem              a reference to the host {@link FileSystem}
   * @param path                    the path of the file to be written
   * @param content                 the content to be written into the file. Defaults to the current {@link Message} payload
   * @param createParentDirectories whether or not to attempt creating any parent directories which don't exists.
   * @param lock                    whether or not to lock the file. Defaults to false
   * @param mode                    a {@link FileWriteMode}. Defaults to {@code OVERWRITE}
   * @throws IllegalArgumentException if an illegal combination of arguments is supplied
   */
  @Summary("Writes the given \"Content\" in the file pointed by \"Path\"")
  @Throws(FileWriteErrorTypeProvider.class)
  public void write(@Connection SftpFileSystemConnection fileSystem,
                    @Path(type = DIRECTORY, location = EXTERNAL) String path,
                    @Content @Summary("Content to be written into the file") InputStream content,
                    @Optional(defaultValue = "true") boolean createParentDirectories,
                    @Optional(defaultValue = "false") boolean lock, @Optional(
                        defaultValue = "OVERWRITE") @Summary("How the file is going to be written") @DisplayName("Write Mode") FileWriteMode mode) {
    // TODO: Revert changes after removing changeToBaseDir() calls in File Commons (MULE-17483).
    if (content == null) {
      throw new IllegalContentException("Cannot write a null content");
    }

    if (isBlank(path)) {
      throw new IllegalPathException("path cannot be null nor blank");
    }

    fileSystem.write(path, content, mode, lock, createParentDirectories);
  }

  /**
   * Copies the file at the {@code sourcePath} into the {@code targetPath}.
   * <p>
   * If {@code targetPath} doesn't exist, and neither does its parent, then an attempt will be made to create depending on the
   * value of the {@code createParentFolder} argument. If such argument is {@false}, then a {@code SFTP:ILLEGAL_PATH} will be
   * thrown.
   * <p>
   * If the target file already exists, then it will be overwritten if the {@code overwrite} argument is {@code true}. Otherwise,
   * {@code SFTP:FILE_ALREADY_EXISTS} error will be thrown.
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
   * @param renameTo                copied file's new name. If not provided, original file name will be kept.
   * @throws IllegalArgumentException if an illegal combination of arguments is supplied
   */
  @Summary("Copies a file")
  @Throws(FileCopyErrorTypeProvider.class)
  public void copy(@Config SftpConnector config,
                   @Connection SftpFileSystemConnection fileSystem,
                   @Path(location = EXTERNAL) String sourcePath,
                   @Path(type = DIRECTORY, location = EXTERNAL) String targetPath,
                   @Optional(defaultValue = "true") boolean createParentDirectories,
                   @Optional(defaultValue = "false") boolean overwrite, @Optional String renameTo) {
    super.doCopy(config, fileSystem, sourcePath, targetPath, createParentDirectories, overwrite, renameTo);
  }

  /**
   * Moves the file at the {@code sourcePath} into the {@code targetPath}.
   * <p>
   * If {@code targetPath} doesn't exist, and neither does its parent, then an attempt will be made to create depending on the
   * value of the {@code createParentFolder} argument. If such argument is {@false}, then a {@code SFTP:ILLEGAL_PATH} will be
   * thrown.
   * <p>
   * If the target file already exists, then it will be overwritten if the {@code overwrite} argument is {@code true}. Otherwise,
   * {@code SFTP:FILE_ALREADY_EXISTS} error will be thrown.
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
   * @param renameTo                moved file's new name. If not provided, original file name will be kept.
   * @throws IllegalArgumentException if an illegal combination of arguments is supplied
   */
  @Summary("Moves a file")
  @Throws(FileCopyErrorTypeProvider.class)
  public void move(@Config SftpConnector config,
                   @Connection SftpFileSystemConnection fileSystem,
                   @Path(location = EXTERNAL) String sourcePath,
                   @Path(type = DIRECTORY, location = EXTERNAL) String targetPath,
                   @Optional(defaultValue = "true") boolean createParentDirectories,
                   @Optional(defaultValue = "false") boolean overwrite, @Optional String renameTo) {
    super.doMove(config, fileSystem, sourcePath, targetPath, createParentDirectories, overwrite, renameTo);
  }


  /**
   * Deletes the file pointed by {@code path}, provided that it's not locked
   *
   * @param fileSystem a reference to the host {@link FileSystem}
   * @param path       the path to the file to be deleted
   * @throws IllegalArgumentException if {@code filePath} doesn't exist or is locked
   */
  @Summary("Deletes a file")
  @Throws(FileDeleteErrorTypeProvider.class)
  public void delete(@Connection SftpFileSystemConnection fileSystem, @Path(location = EXTERNAL) String path) {
    super.doDelete(fileSystem, path);
  }

  /**
   * Renames the file pointed by {@code path} to the name provided on the {@code to} parameter
   * <p>
   * {@code to} argument should not contain any path separator. {@code SFTP:ILLEGAL_PATH} will be thrown if this precondition is
   * not honored.
   *
   * @param fileSystem a reference to the host {@link FileSystem}
   * @param path       the path to the file to be renamed
   * @param to         the file's new name
   * @param overwrite  whether or not overwrite the file if the target destination already exists.
   */
  @Summary("Renames a file")
  @Throws(FileRenameErrorTypeProvider.class)
  public void rename(@Connection SftpFileSystemConnection fileSystem, @Path(location = EXTERNAL) String path,
                     @DisplayName("New Name") String to, @Optional(defaultValue = "false") boolean overwrite) {
    super.doRename(fileSystem, path, to, overwrite);
  }

  /**
   * Creates a new directory on {@code directoryPath}
   *
   * @param fileSystem    a reference to the host {@link FileSystem}
   * @param directoryPath the new directory's name
   */
  @Summary("Creates a new directory")
  @Throws(FileRenameErrorTypeProvider.class)
  public void createDirectory(@Connection SftpFileSystemConnection fileSystem, @Path(location = EXTERNAL) String directoryPath) {
    super.doCreateDirectory(fileSystem, directoryPath);
  }
}
