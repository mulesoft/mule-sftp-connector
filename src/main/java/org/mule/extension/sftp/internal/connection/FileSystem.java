/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.connection;

import org.mule.extension.sftp.api.CustomWriteBufferSize;
import org.mule.extension.sftp.api.FileAttributes;
import org.mule.extension.sftp.api.FileWriteMode;
import org.mule.extension.sftp.api.WriteStrategy;
import org.mule.extension.sftp.internal.config.FileConnectorConfig;
import org.mule.extension.sftp.internal.lock.PathLock;
import org.mule.extension.sftp.internal.subset.SubsetList;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.message.OutputHandler;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.function.Predicate;

import org.apache.tika.Tika;

/**
 * Represents an abstract file system and the operations which can be performed on it.
 * <p>
 * This interface acts as a facade which allows performing common files operations regardless of those files being in a local
 * disk, an FTP server, a cloud storage service, etc.
 *
 * @since 1.0
 */
public interface FileSystem<A extends FileAttributes> {


  /**
   * Lists all the files in the {@code directoryPath} which match the given {@code matcher}.
   * <p>
   * If the listing encounters a directory, the output list will include its contents depending on the value of the
   * {@code recursive} argument. If {@code recursive} is enabled, then all the files in that directory will be listed immediately
   * after their parent directory.
   * <p>
   *
   * @param config               the config that is parameterizing this operation
   * @param directoryPath        the path to the directory to be listed
   * @param recursive            whether to include the contents of sub-directories
   * @param matcher              a {@link Predicate} of {@link FileAttributes} used to filter the output list
   * @param timeBetweenSizeCheck wait time between size checks to determine if a file is ready to be read in milliseconds.
   * @return a {@link List} of {@link Result} objects, each one containing each file's content in the payload and metadata in the
   *         attributes
   * @throws IllegalArgumentException if {@code directoryPath} points to a file which doesn't exist or is not a directory
   */
  List<Result<InputStream, A>> list(FileConnectorConfig config,
                                    String directoryPath,
                                    boolean recursive,
                                    Predicate<A> matcher,
                                    Long timeBetweenSizeCheck);

  /**
   * Lists all the files in the {@code directoryPath} which match the given {@code matcher}.
   * <p>
   * If the listing encounters a directory, the output list will include its contents depending on the value of the
   * {@code recursive} argument. If {@code recursive} is enabled, then all the files in that directory will be listed immediately
   * after their parent directory.
   * <p>
   *
   * @param config               the config that is parameterizing this operation
   * @param directoryPath        the path to the directory to be listed
   * @param recursive            whether to include the contents of sub-directories
   * @param matcher              a {@link Predicate} of {@link FileAttributes} used to filter the output list
   * @param timeBetweenSizeCheck wait time between size checks to determine if a file is ready to be read in milliseconds.
   * @param subsetList           parameter group that lets you obtain a subset of the results
   * @return a {@link List} of {@link Result} objects, each one containing each file's content in the payload and metadata in the
   *         attributes
   * @throws IllegalArgumentException if {@code directoryPath} points to a file which doesn't exist or is not a directory
   */
  default List<Result<InputStream, A>> list(FileConnectorConfig config,
                                            String directoryPath,
                                            boolean recursive,
                                            Predicate<A> matcher,
                                            Long timeBetweenSizeCheck, SubsetList subsetList) {
    return list(config, directoryPath, recursive, matcher, timeBetweenSizeCheck);
  }

  /**
   * Obtains the content and metadata of a file at a given path.
   * <p>
   * Locking can be actually enabled through the {@code lock} argument, however, the extent of such lock will depend on the
   * implementation. What is guaranteed by passing {@code true} on the {@code lock} argument is that {@code this} instance will
   * not attempt to modify this file until the {@link InputStream} returned by {@link Result#getOutput()} this method returns is
   * closed or fully consumed. Some implementation might actually perform a file system level locking which goes beyond the extend
   * of {@code this} instance or even mule. For some other file systems that might be simply not possible and no extra assumptions
   * are to be taken.
   * <p>
   * This method also makes a best effort to determine the mime type of the file being read. A {@link Tika} instance will be used
   * to make an educated guess on the file's mime type
   *
   * @param config               the config that is parameterizing this operation
   * @param filePath             the path of the file you want to read
   * @param lock                 whether or not to lock the file
   * @param timeBetweenSizeCheck wait time between size checks to determine if a file is ready to be read in milliseconds.
   * @return An {@link Result} with an {@link InputStream} with the file's content as payload and a {@link FileAttributes} object
   *         as {@link Message#getAttributes()}
   * @throws IllegalArgumentException if the file at the given path doesn't exist
   */
  Result<InputStream, A> read(FileConnectorConfig config, String filePath, boolean lock,
                              Long timeBetweenSizeCheck);

  /**
   * Writes the {@code content} into the file pointed by {@code filePath}.
   * <p>
   * The {@code content} can be of any of the given types:
   * <ul>
   * <li>{@link String}</li>
   * <li>{@code String[]}</li>
   * <li>{@code byte}</li>
   * <li>{@code byte[]}</li>
   * <li>{@link OutputHandler}</li>
   * <li>{@link Iterable}</li>
   * <li>{@link Iterator}</li>
   * </ul>
   * <p>
   * {@code null} contents are not allowed and will result in an {@link IllegalArgumentException}.
   * <p>
   * If the directory on which the file is attempting to be written doesn't exist, then the operation will either throw
   * {@link IllegalArgumentException} or create such folder depending on the value of the {@code createParentDirectory}.
   * <p>
   * If the file itself already exists, then the behavior depends on the supplied {@code mode}.
   * <p>
   * This method also supports locking support depending on the value of the {@code lock} argument, but following the same rules
   * and considerations as described in the {@link #read(FileConnectorConfig, String, boolean, Long)} (FileConnectorConfig,
   * String, boolean)} method
   * This method is a custom write method call that takes the offset value manually.
   *
   * @param filePath                the path of the file to be written
   * @param content                 the content to be written into the file
   * @param mode                    a {@link FileWriteMode}
   * @param lock                    whether or not to lock the file
   * @param writeStrategy           a {@link WriteStrategy} defaults to STANDARD
   * @param bufferSizeForWriteStrategy  a {@link CustomWriteBufferSize}. Defaults to 8192
   * @param createParentDirectories whether or not to attempt creating any parent directories which don't exists.
   * @throws IllegalArgumentException   if an illegal combination of arguments is supplied
   */
  void write(String filePath, InputStream content, FileWriteMode mode, boolean lock, boolean createParentDirectories,
             WriteStrategy writeStrategy, CustomWriteBufferSize bufferSizeForWriteStrategy);


  /**
   * Copies the file at the {@code sourcePath} into the {@code targetPath}.
   * <p>
   * If {@code targetPath} doesn't exist, and neither does its parent, then an attempt will be made to create depending on the
   * value of the {@code createParentDirectory} argument. If such argument is {@false}, then an {@link IllegalArgumentException}
   * will be thrown.
   * <p>
   * It is also possible to use the {@code targetPath} to specify that the copied file should also be renamed. For example, if
   * {@code sourcePath} has the value <i>a/b/test.txt</i> and {@code targetPath} is assigned to <i>a/c/test.json</i>, then the
   * file will indeed be copied to the <i>a/c/</i> directory but renamed as <i>test.json</i>
   * <p>
   * If the target file already exists, then it will be overwritten if the {@code overwrite} argument is {@code true}. Otherwise,
   * {@link IllegalArgumentException} will be thrown
   * <p>
   * As for the {@code sourcePath}, it can either be a file or a directory. If it points to a directory, then it will be copied
   * recursively
   *
   * @param config                  the config that is parameterizing this operation
   * @param sourcePath              the path to the file to be copied
   * @param targetPath              the target directory
   * @param overwrite               whether or not overwrite the file if the target destination already exists.
   * @param createParentDirectories whether or not to attempt creating any parent directories which doesn't exist.
   * @param renameTo                the new file name, {@code null} if the file doesn't need to be renamed
   * @throws IllegalArgumentException if an illegal combination of arguments is supplied
   */
  void copy(FileConnectorConfig config, String sourcePath, String targetPath, boolean overwrite,
            boolean createParentDirectories,
            String renameTo);

  /**
   * Moves the file at the {@code sourcePath} into the {@code targetPath}.
   * <p>
   * If {@code targetPath} doesn't exist, and neither does its parent, then an attempt will be made to create depending on the
   * value of the {@code createParentDirectory} argument. If such argument is {@false}, then an {@link IllegalArgumentException}
   * will be thrown.
   * <p>
   * It is also possible to use the {@code targetPath} to specify that the moved file should also be renamed. For example, if
   * {@code sourcePath} has the value <i>a/b/test.txt</i> and {@code targetPath} is assigned to <i>a/c/test.json</i>, then the
   * file will indeed be moved to the <i>a/c/</i> directory but renamed as <i>test.json</i>
   * <p>
   * If the target file already exists, then it will be overwritten if the {@code overwrite} argument is {@code true}. Otherwise,
   * {@link IllegalArgumentException} will be thrown
   * <p>
   * As for the {@code sourcePath}, it can either be a file or a directory. If it points to a directory, then it will be moved
   * recursively
   *
   * @param config                  the config that is parameterizing this operation
   * @param sourcePath              the path to the file to be copied
   * @param targetPath              the target directory
   * @param overwrite               whether or not overwrite the file if the target destination already exists.
   * @param createParentDirectories whether or not to attempt creating any parent directories which don't exists.
   * @param renameTo                the new file name, {@code null} if the file doesn't need to be renamed
   * @throws IllegalArgumentException if an illegal combination of arguments is supplied
   */
  void move(FileConnectorConfig config, String sourcePath, String targetPath, boolean overwrite, boolean createParentDirectories,
            String renameTo);

  /**
   * Deletes the file pointed by {@code filePath}, provided that it's not locked
   *
   * @param filePath the path to the file to be deleted
   * @throws IllegalArgumentException if {@code filePath} doesn't exist or is locked
   */
  void delete(String filePath);

  /**
   * Renames the file pointed by {@code filePath} to the provided {@code newName}
   *
   * @param filePath  the path to the file to be renamed
   * @param newName   the file's new name
   * @param overwrite whether or not overwrite the file if the target destination already exists.
   */
  void rename(String filePath, String newName, boolean overwrite);

  /**
   * Creates a new directory
   *
   * @param directoryPath the new directory's path
   */
  void createDirectory(String directoryPath);

  /**
   * Acquires and returns lock over the given {@code path}.
   * <p>
   * Depending on the underlying filesystem, the extent of the lock will depend on the implementation. If a lock can not be
   * acquired, then an {@link IllegalStateException} is thrown.
   * <p>
   * Whoever request the lock <b>MUST</b> release it as soon as possible.
   *
   * @param path the path to the file you want to lock
   * @return an acquired {@link PathLock}
   * @throws IllegalArgumentException if a lock could not be acquired
   */
  PathLock lock(Path path);

  Lock createMuleLock(String id);

  /**
   * Creates a new {@link DataType} to be associated with a {@link Message} which payload is a {@link InputStream} and the
   * attributes an instance of {@link FileAttributes}
   * <p>
   * It will try to update the {@link DataType#getMediaType()} with a best guess derived from the given {@code attributes}. If no
   * best-guess is possible, then the {@code originalDataType}'s mimeType is honoured.
   * <p>
   * As for the {@link MediaType#getCharset()}, the {@code dataType} one is respected
   *
   * @param attributes the {@link FileAttributes} of the file being processed
   * @return a {@link DataType} the resulting {@link DataType}.
   */
  MediaType getFileMessageMediaType(FileAttributes attributes);

  /**
   * Verify that the given {@code path} is not locked
   *
   * @param path the path to test
   * @throws IllegalStateException if the {@code path} is indeed locked
   */
  void verifyNotLocked(Path path);

  /**
   * Changes the current working directory to the user base
   */
  void changeToBaseDir();

  String getBasePath();
}
