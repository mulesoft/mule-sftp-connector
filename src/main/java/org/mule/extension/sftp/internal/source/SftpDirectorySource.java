/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.source;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.util.ExceptionUtils.extractConnectionException;
import static org.mule.runtime.core.api.util.IOUtils.closeQuietly;
import static org.mule.runtime.extension.api.annotation.param.MediaType.ANY;
import static org.mule.runtime.extension.api.runtime.source.PollContext.PollItemStatus.SOURCE_STOPPING;
import static org.mule.sdk.api.annotation.source.SourceClusterSupport.DEFAULT_PRIMARY_NODE_ONLY;

import static java.lang.String.format;

import org.apache.sshd.common.SshException;
import org.mule.extension.sftp.api.FileAttributes;
import org.mule.extension.sftp.api.SftpFileAttributes;
import org.mule.extension.sftp.api.SftpFileMatcher;
import org.mule.extension.sftp.api.matcher.NullFilePayloadPredicate;
import org.mule.extension.sftp.internal.exception.IllegalPathException;
import org.mule.extension.sftp.internal.extension.SftpConnector;
import org.mule.extension.sftp.internal.connection.SftpFileSystemConnection;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.execution.OnError;
import org.mule.runtime.extension.api.annotation.execution.OnSuccess;
import org.mule.runtime.extension.api.annotation.execution.OnTerminate;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.ConfigOverride;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.PollContext;
import org.mule.runtime.extension.api.runtime.source.PollContext.PollItemStatus;
import org.mule.runtime.extension.api.runtime.source.PollingSource;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;
import org.mule.sdk.api.annotation.source.ClusterSupport;

import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Polls a directory looking for files that have been created on it. One message will be generated for each file that is found.
 * <p>
 * The key part of this functionality is how to determine that a file is actually new. There're three strategies for that:
 * <ul>
 * <li>Set the <i>autoDelete</i> parameter to <i>true</i>: This will delete each processed file after it has been processed,
 * causing all files obtained in the next poll to be necessarily new</li>
 * <li>Set <i>moveToDirectory</i> parameter: This will move each processed file to a different directory after it has been
 * processed, achieving the same effect as <i>autoDelete</i> but without loosing the file</li>
 * <li></li>
 * <li>Use the <i>watermarkMode</i> parameter to only pick files that have been created/updated after the last poll was
 * executed.</li>
 * </ul>
 * <p>
 * A matcher can also be used for additional filtering of files.
 *
 * @since 1.1
 */
@MediaType(value = ANY, strict = false)
@DisplayName("On New or Updated File")
@Summary("Triggers when a new file is created in a directory")
@Alias("listener")
@ClusterSupport(DEFAULT_PRIMARY_NODE_ONLY)
// TODO: MULE-13940 - add mimeType here too
public class SftpDirectorySource extends PollingSource<InputStream, SftpFileAttributes> {

  private static final Logger LOGGER = LoggerFactory.getLogger(SftpDirectorySource.class);
  private static final String ATTRIBUTES_CONTEXT_VAR = "attributes";
  private static final String POST_PROCESSING_GROUP_NAME = "Post processing action";
  public static final String MATCHER = "Matcher";

  @Config
  private SftpConnector config;

  @Connection
  private ConnectionProvider<SftpFileSystemConnection> fileSystemProvider;

  /**
   * The directory on which polled files are contained
   */
  @Parameter
  @Optional
  private String directory;

  /**
   * Whether or not to also files contained in sub directories.
   */
  @Parameter
  @Optional(defaultValue = "true")
  @Summary("Whether or not to also catch files created on sub directories")
  private boolean recursive = true;

  /**
   * A matcher used to filter events on files which do not meet the matcher's criteria
   */
  @Parameter
  @Optional
  @Alias("matcher")
  @DisplayName(MATCHER)
  private SftpFileMatcher predicateBuilder;

  /**
   * Controls whether or not to do watermarking, and if so, if the watermark should consider the file's modification or creation
   * timestamps
   */
  @Parameter
  @Optional(defaultValue = "false")
  private boolean watermarkEnabled = false;

  /**
   * Wait time in milliseconds between size checks to determine if a file is ready to be read. This allows a file write to
   * complete before processing. You can disable this feature by omitting a value. When enabled, Mule performs two size checks
   * waiting the specified time between calls. If both checks return the same value, the file is ready to be read.
   */
  @Parameter
  @ConfigOverride
  @Summary("Wait time in milliseconds between size checks to determine if a file is ready to be read.")
  private Long timeBetweenSizeCheck;

  /**
   * A {@link TimeUnit} which qualifies the {@link #timeBetweenSizeCheck} attribute.
   */
  @Parameter
  @ConfigOverride
  @Summary("Time unit to be used in the wait time between size checks")
  private TimeUnit timeBetweenSizeCheckUnit;

  private URI directoryUri;
  private Predicate<SftpFileAttributes> fileAttributePredicate;

  private static final Map<String, SftpFileSystemConnection> OPEN_CONNECTIONS = new HashMap<>();
  private static final Map<SftpFileSystemConnection, Integer> FREQUENCY_OF_OPEN_CONNECTION = new HashMap<>();

  @Override
  protected void doStart() {
    refreshMatcher();
    directoryUri = resolveRootPath();
  }

  @OnSuccess
  public void onSuccess(@ParameterGroup(name = POST_PROCESSING_GROUP_NAME) PostActionGroup postAction,
                        SourceCallbackContext ctx) {
    ctx.<SftpFileAttributes>getVariable(ATTRIBUTES_CONTEXT_VAR).ifPresent(this::closeConnectionPostAction);
    postAction(postAction, ctx);
  }

  @OnError
  public void onError(@ParameterGroup(name = POST_PROCESSING_GROUP_NAME) PostActionGroup postAction,
                      SourceCallbackContext ctx) {
    ctx.<SftpFileAttributes>getVariable(ATTRIBUTES_CONTEXT_VAR).ifPresent(this::closeConnectionPostAction);
    if (postAction.isApplyPostActionWhenFailed()) {
      postAction(postAction, ctx);
    }
  }

  @OnTerminate
  public void onTerminate(SourceCallbackContext ctx) {
    // Does nothing
  }

  @Override
  public void poll(PollContext<InputStream, SftpFileAttributes> pollContext) {
    refreshMatcher();
    if (pollContext.isSourceStopping()) {
      return;
    }
    SftpFileSystemConnection fileSystem;
    try {
      fileSystem = openConnection();
    } catch (Exception e) {
      LOGGER.error(format("Could not obtain connection while trying to poll directory '%s'. %s", directoryUri.getPath(),
                          e.getMessage()),
                   e);
      return;
    }
    SftpFileAttributes attributes = null;
    boolean canDisconnect = true;
    try {
      Long timeBetweenSizeCheckInMillis =
          config.getTimeBetweenSizeCheckInMillis(timeBetweenSizeCheck, timeBetweenSizeCheckUnit).orElse(null);
      List<Result<String, SftpFileAttributes>> files =
          fileSystem.list(config, directoryUri.getPath(), recursive, fileAttributePredicate, timeBetweenSizeCheckInMillis);
      if (files.isEmpty()) {
        return;
      }
      canDisconnect = processFiles(files, pollContext, fileSystem, timeBetweenSizeCheckInMillis);
    } catch (IllegalPathException ex) {
      LOGGER.debug("The File with attributes {} was polled but not exist anymore", attributes);
    } catch (Exception e) {
      if (isChannelBeingClosed(e)) {
        try {
          fileSystem = cleanUpAndReconnectFilesystem(pollContext, fileSystem);
          // If reconnection succeeds and files are processed, canDisconnect may change
          Long timeBetweenSizeCheckInMillis =
              config.getTimeBetweenSizeCheckInMillis(timeBetweenSizeCheck, timeBetweenSizeCheckUnit).orElse(null);
          List<Result<String, SftpFileAttributes>> files =
              fileSystem.list(config, directoryUri.getPath(), recursive, fileAttributePredicate, timeBetweenSizeCheckInMillis);
          if (!files.isEmpty()) {
            canDisconnect = processFiles(files, pollContext, fileSystem, timeBetweenSizeCheckInMillis);
          }
        } catch (Exception reconnectError) {
          LOGGER.error(format("Failed to reconnect while polling directory '%s'. Will try again on the next poll.",
                              directoryUri.getPath()),
                       reconnectError.getMessage(), reconnectError);
          extractConnectionException(reconnectError).ifPresent(pollContext::onConnectionException);
        }
      } else {
        LOGGER.error(format("Found exception trying to poll directory '%s'. Will try again on the next poll. ",
                            directoryUri.getPath()),
                     e.getMessage(), e);
        extractConnectionException(e).ifPresent(pollContext::onConnectionException);
      }
    } finally {
      if (canDisconnect) {
        LOGGER.debug("Closing the connection since no file is in ACCEPTED state.");
        fileSystemProvider.disconnect(fileSystem);
      }
    }
  }

  private boolean isChannelBeingClosed(Exception e) {
    return e.getCause() instanceof SshException && e.getCause().getMessage().contains("Channel is being closed");
  }

  @SuppressWarnings("java:S3655")
  private boolean processFiles(List<Result<String, SftpFileAttributes>> files,
                               PollContext<InputStream, SftpFileAttributes> pollContext,
                               SftpFileSystemConnection fileSystem,
                               Long timeBetweenSizeCheckInMillis) {
    SftpFileAttributes attributes = null;
    boolean canDisconnect = true;

    for (Result<String, SftpFileAttributes> file : files) {
      if (pollContext.isSourceStopping()) {
        return canDisconnect;
      }

      if (!hasAttributes(file)) {
        continue;
      }

      attributes = file.getAttributes().get();

      if (shouldSkipFile(attributes)) {
        continue;
      }

      Result<InputStream, SftpFileAttributes> result =
          fileSystem.read(config, attributes.getPath(), true, timeBetweenSizeCheckInMillis);
      PollItemStatus pollItemStatus = processFile(result, pollContext);

      if (canDisconnect && pollItemStatus == PollItemStatus.ACCEPTED) {
        LOGGER.debug("The file {} is in ACCEPTED state", attributes.getFileName());
        canDisconnect = false;
      }

      if (pollItemStatus == SOURCE_STOPPING) {
        break;
      }

      updateConnectionMaps(attributes.getPath(), fileSystem, pollItemStatus);
    }

    return canDisconnect;
  }

  private boolean hasAttributes(Result<String, SftpFileAttributes> file) {
    if (!file.getAttributes().isPresent()) {
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn("Skipping file because attributes are not present. " +
            "Please check your server for errors or try enabling MDTM.");
      }
      return false;
    }
    return true;
  }

  private boolean shouldSkipFile(SftpFileAttributes attributes) {
    if (attributes.isDirectory()) {
      return true;
    }

    if (!fileAttributePredicate.test(attributes)) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Skipping file '{}' because the matcher rejected it", attributes.getPath());
      }
      return true;
    }

    return false;
  }

  private void refreshMatcher() {
    fileAttributePredicate = predicateBuilder != null ? predicateBuilder.build() : new NullFilePayloadPredicate<>();
  }

  private SftpFileSystemConnection openConnection()
      throws ConnectionException {
    SftpFileSystemConnection fileSystem = fileSystemProvider.connect();
    fileSystem.changeToBaseDir();
    return fileSystem;
  }


  private PollItemStatus processFile(Result<InputStream, SftpFileAttributes> file,
                                     PollContext<InputStream, SftpFileAttributes> pollContext) {
    SftpFileAttributes attributes = file.getAttributes().get();
    String fullPath = attributes.getPath();
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Processing file {}", attributes);
    }
    PollItemStatus status = pollContext.accept(item -> {
      final SourceCallbackContext ctx = item.getSourceCallbackContext();
      try {
        ctx.addVariable(ATTRIBUTES_CONTEXT_VAR, attributes);
        item.setResult(file).setId(attributes.getPath());

        if (watermarkEnabled) {
          item.setWatermark(attributes.getTimestamp());
        }
      } catch (Exception e) {
        I18nMessage message =
            createStaticMessage(format("Found file '%s' but found exception trying to dispatch it for processing", fullPath));
        throw new MuleRuntimeException(message, e);
      }
    });

    LOGGER.debug("The status of file {} is {}", file.getAttributes().get().getFileName(), status);
    return status;
  }

  @Override
  public void onRejectedItem(Result<InputStream, SftpFileAttributes> result, SourceCallbackContext callbackContext) {
    closeQuietly(result.getOutput());
  }

  private void postAction(PostActionGroup postAction, SourceCallbackContext ctx) {
    ctx.<SftpFileAttributes>getVariable(ATTRIBUTES_CONTEXT_VAR).ifPresent(attrs -> {
      LOGGER.debug("PostAction getting called for file {}", attrs.getPath());
      SftpFileSystemConnection fileSystem = null;
      try {
        fileSystem = fileSystemProvider.connect();
        fileSystem.changeToBaseDir();
        postAction.apply(fileSystem, attrs, config);
      } catch (ConnectionException e) {
        LOGGER
            .error(String
                .format("An error occurred while retrieving a connection to apply the post processing action to the file %s , it was neither moved nor deleted.",
                        attrs.getPath()));
      } finally {
        if (fileSystem != null) {
          LOGGER.debug("Post action is invoked and closing the connection for file {}", attrs.getFileName());
          fileSystemProvider.disconnect(fileSystem);
        }
      }
    });
  }

  @Override
  protected void doStop() {
    // Does Nothing
  }

  private URI resolveRootPath() {
    SftpFileSystemConnection fileSystem = null;
    try {
      fileSystem = fileSystemProvider.connect();
      fileSystem.changeToBaseDir();
      URI rootPath = new OnNewFileCommand(fileSystem).resolveRootPath(directory);
      fileSystemProvider.disconnect(fileSystem);
      return rootPath;
    } catch (Exception e) {
      I18nMessage message = createStaticMessage(
                                                format("Could not resolve path to directory '%s'. %s",
                                                       directory, e.getMessage()));
      throw new MuleRuntimeException(message, e);
    }
  }

  private void closeConnectionPostAction(FileAttributes attributes) {
    SftpFileSystemConnection connection = OPEN_CONNECTIONS.get(attributes.getPath());
    if (connection != null) {
      int frequency = FREQUENCY_OF_OPEN_CONNECTION.getOrDefault(connection, 0);
      LOGGER.debug("The frequency of the connection {} is {}", connection, frequency);
      if (frequency > 1) {
        FREQUENCY_OF_OPEN_CONNECTION.put(connection, frequency - 1);
      } else {
        fileSystemProvider.disconnect(connection);
        FREQUENCY_OF_OPEN_CONNECTION.remove(connection);
      }
      OPEN_CONNECTIONS.remove(attributes.getPath());
    }
  }

  private void updateConnectionMaps(String filepath, SftpFileSystemConnection fileSystem, PollItemStatus pollItemStatus) {
    if (pollItemStatus == PollItemStatus.ACCEPTED) {
      FREQUENCY_OF_OPEN_CONNECTION.put(fileSystem, FREQUENCY_OF_OPEN_CONNECTION.getOrDefault(fileSystem, 0) + 1);
      OPEN_CONNECTIONS.put(filepath, fileSystem);
    }
  }

  private SftpFileSystemConnection cleanUpAndReconnectFilesystem(
                                                                 PollContext<InputStream, SftpFileAttributes> pollContext,
                                                                 SftpFileSystemConnection fileSystem)
      throws ConnectionException {
    LOGGER.warn("SFTP channel is closed. Attempting to reconnect and retry...");
    // Disconnect and cleanup
    fileSystem.disconnect();
    // Get a new connection and return it
    return openConnection();
  }
}
