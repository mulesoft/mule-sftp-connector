/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.source;

import static java.lang.String.format;
import static org.mule.runtime.api.meta.model.display.PathModel.Location.EXTERNAL;
import static org.mule.runtime.api.meta.model.display.PathModel.Type.DIRECTORY;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Path;

/**
 * Groups post processing action parameters
 *
 * @since 1.1
 */
public class PostActionGroup {

  /**
   * Whether each file should be deleted after processing or not
   */
  @Parameter
  @Optional(defaultValue = "false")
  private boolean autoDelete = false;

  /**
   * If provided, each processed file will be moved to a directory pointed by this path.
   */
  @Parameter
  @Optional
  @Path(type = DIRECTORY, location = EXTERNAL)
  private String moveToDirectory;

  /**
   * This parameter works in tandem with {@code moveToDirectory}. Use this parameter to enter the name under which the file should
   * be moved. Do not set this parameter if {@code moveToDirectory} hasn't been set as well.
   */
  @Parameter
  @Optional
  private String renameTo;

  /**
   * Whether any of the post actions ({@code autoDelete} and {@code moveToDirectory}) should also be applied in case the
   * file failed to be processed. If set to {@code false}, no failed files will be moved nor deleted.
   */
  @Parameter
  @Optional(defaultValue = "true")
  private boolean applyPostActionWhenFailed = true;


  public PostActionGroup() {}

  public PostActionGroup(boolean autoDelete, String moveToDirectory, String renameTo, boolean applyPostActionWhenFailed) {
    this.autoDelete = autoDelete;
    this.moveToDirectory = moveToDirectory;
    this.renameTo = renameTo;
    this.applyPostActionWhenFailed = applyPostActionWhenFailed;
  }

  public boolean isAutoDelete() {
    return autoDelete;
  }

  public String getMoveToDirectory() {
    return moveToDirectory;
  }

  public String getRenameTo() {
    return renameTo;
  }

  public boolean isApplyPostActionWhenFailed() {
    return applyPostActionWhenFailed;
  }

  public void validateSelf() throws IllegalArgumentException {
    if (autoDelete) {
      if (moveToDirectory != null) {
        throw new IllegalArgumentException(format("The autoDelete parameter was set to true, but the value '%s' was given to the "
            + "moveToDirectory parameter. These two are contradictory.", moveToDirectory));
      } else if (renameTo != null)
        throw new IllegalArgumentException(format("The autoDelete parameter was set to true, but the value '%s' was given to the "
            + "renameTo parameter. These two are contradictory.", renameTo));
    }
    if (moveToDirectory == null && renameTo != null) {
      throw new IllegalArgumentException(format("The value '%s' was given to the renameTo parameter, but the moveToDirectory parameter"
          + " was not set. renameTo is only used to change the name to the file when it is moved to " +
          "the moveToDirectory.", renameTo));
    }
  }
}
