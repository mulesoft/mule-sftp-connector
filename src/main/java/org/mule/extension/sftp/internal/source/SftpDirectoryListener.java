/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.source;

import static java.lang.String.format;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.extension.file.common.api.FileDisplayConstants.MATCHER;
import static org.mule.runtime.core.api.util.IOUtils.closeQuietly;
import static org.mule.runtime.core.api.util.ExceptionUtils.extractConnectionException;
import static org.mule.runtime.extension.api.annotation.param.MediaType.ANY;
import static org.mule.runtime.extension.api.runtime.source.PollContext.PollItemStatus.SOURCE_STOPPING;

import org.mule.extension.file.common.api.matcher.NullFilePayloadPredicate;
import org.mule.extension.sftp.api.SftpFileAttributes;
import org.mule.extension.sftp.api.SftpFileMatcher;
import org.mule.extension.sftp.internal.SftpConnector;
import org.mule.extension.sftp.internal.connection.SftpFileSystem;
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

import java.io.InputStream;
import java.net.URI;
import java.util.List;
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
// TODO: MULE-13940 - add mimeType here too
public class SftpDirectoryListener extends PollingSource<InputStream, SftpFileAttributes> {

  private static final Logger LOGGER = LoggerFactory.getLogger(SftpDirectoryListener.class);
  private static final String ATTRIBUTES_CONTEXT_VAR = "attributes";
  private static final String POST_PROCESSING_GROUP_NAME = "Post processing action";

  @Config
  private SftpConnector config;

  @Connection
  private ConnectionProvider<SftpFileSystem> fileSystemProvider;

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
  private Predicate<SftpFileAttributes> matcher;

  @Override
  protected void doStart() {
    refreshMatcher();
    directoryUri = resolveRootPath();
  }

  @OnSuccess
  public void onSuccess(@ParameterGroup(name = POST_PROCESSING_GROUP_NAME) PostActionGroup postAction,
                        SourceCallbackContext ctx) {
    postAction(postAction, ctx);
  }

  @OnError
  public void onError(@ParameterGroup(name = POST_PROCESSING_GROUP_NAME) PostActionGroup postAction,
                      SourceCallbackContext ctx) {
    if (postAction.isApplyPostActionWhenFailed()) {
      postAction(postAction, ctx);
    }
  }

  @OnTerminate
  public void onTerminate(SourceCallbackContext ctx) {}

  @Override
  public void poll(PollContext<InputStream, SftpFileAttributes> pollContext) {
    refreshMatcher();
    if (pollContext.isSourceStopping()) {
      return;
    }

    SftpFileSystem fileSystem;
    try {
      fileSystem = openConnection(pollContext);
    } catch (Exception e) {
      LOGGER.error(format("Could not obtain connection while trying to poll directory '%s'. %s", directoryUri.getPath(),
                          e.getMessage()));
      return;
    }

    try {
      Long timeBetweenSizeCheckInMillis =
          config.getTimeBetweenSizeCheckInMillis(timeBetweenSizeCheck, timeBetweenSizeCheckUnit).orElse(null);
      List<Result<InputStream, SftpFileAttributes>> files =
          fileSystem.list(config, directoryUri.getPath(), recursive, matcher, timeBetweenSizeCheckInMillis);
      if (files.isEmpty()) {
        return;
      }

      for (Result<InputStream, SftpFileAttributes> file : files) {
        if (pollContext.isSourceStopping()) {
          return;
        }

        SftpFileAttributes attributes = file.getAttributes().get();

        if (attributes.isDirectory()) {
          continue;
        }

        if (!matcher.test(attributes)) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Skipping file '{}' because the matcher rejected it", attributes.getPath());
          }
          continue;
        }

        if (!processFile(file, pollContext)) {
          break;
        }
      }
    } catch (Exception e) {
      LOGGER.error(format("Found exception trying to poll directory '%s'. Will try again on the next poll. ",
                          directoryUri.getPath(), e.getMessage()),
                   e);
      extractConnectionException(e).ifPresent((connectionException) -> pollContext.onConnectionException(connectionException));
    } finally {
      fileSystemProvider.disconnect(fileSystem);
    }
  }

  private void refreshMatcher() {
    matcher = predicateBuilder != null ? predicateBuilder.build() : new NullFilePayloadPredicate<>();
  }

  private SftpFileSystem openConnection(PollContext pollContext) throws Exception {

    SftpFileSystem fileSystem = fileSystemProvider.connect();
    try {
      fileSystem.changeToBaseDir();
    } catch (Exception e) {
      LOGGER.debug("Exception while trying to open connection. Cause: {} . Message: {}", e.getCause(), e.getMessage());
      if (extractConnectionException(e).isPresent()) {
        extractConnectionException(e).ifPresent((connectionException) -> pollContext.onConnectionException(connectionException));
      } else {
        fileSystemProvider.disconnect(fileSystem);
      }
      throw e;
    }
    return fileSystem;
  }

  private boolean processFile(Result<InputStream, SftpFileAttributes> file,
                              PollContext<InputStream, SftpFileAttributes> pollContext) {
    SftpFileAttributes attributes = file.getAttributes().get();
    String fullPath = attributes.getPath();
    PollItemStatus status = pollContext.accept(item -> {
      final SourceCallbackContext ctx = item.getSourceCallbackContext();
      Result result = null;

      try {
        ctx.addVariable(ATTRIBUTES_CONTEXT_VAR, attributes);
        item.setResult(file).setId(attributes.getPath());

        if (watermarkEnabled) {
          item.setWatermark(attributes.getTimestamp());
        }
      } catch (Throwable t) {
        LOGGER.error(format("Found file '%s' but found exception trying to dispatch it for processing. %s",
                            fullPath, t.getMessage()),
                     t);

        if (result != null) {
          onRejectedItem(result, ctx);
        }

        throw new MuleRuntimeException(t);
      }
    });

    return status != SOURCE_STOPPING;
  }

  @Override
  public void onRejectedItem(Result<InputStream, SftpFileAttributes> result, SourceCallbackContext callbackContext) {
    closeQuietly(result.getOutput());
  }

  private void postAction(PostActionGroup postAction, SourceCallbackContext ctx) {
    ctx.<SftpFileAttributes>getVariable(ATTRIBUTES_CONTEXT_VAR).ifPresent(attrs -> {
      SftpFileSystem fileSystem = null;
      try {
        fileSystem = fileSystemProvider.connect();
        fileSystem.changeToBaseDir();
        postAction.apply(fileSystem, attrs, config);
      } catch (ConnectionException e) {
        LOGGER
            .error("An error occurred while retrieving a connection to apply the post processing action to the file %s , it was neither moved nor deleted.",
                   attrs.getPath());
      } finally {
        if (fileSystem != null) {
          fileSystemProvider.disconnect(fileSystem);
        }
      }
    });
  }

  @Override
  protected void doStop() {

  }

  private URI resolveRootPath() {
    SftpFileSystem fileSystem = null;
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
      LOGGER.error(message.getMessage(), e);
      throw new MuleRuntimeException(message, e);
    }
  }
}
