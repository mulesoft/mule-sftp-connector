/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.operation;

import static org.mule.extension.sftp.internal.util.UriUtils.createUri;
import static org.mule.extension.sftp.internal.util.UriUtils.trimLastFragment;

import org.mule.extension.sftp.internal.connection.ExternalFileSystem;

import java.net.URI;

/**
 * Extension of {@link AbstractFileCommand} for local file systems which use {@link URI} to identify and manage files and
 * directories.
 *
 * @param <F> the generic type of the {@link ExternalFileSystem} on which the operation is performed
 * @since 1.3.0
 */
public abstract class ExternalFileCommand<F extends ExternalFileSystem> extends AbstractFileCommand<F, URI> {

  /**
   * Creates a new instance
   *
   * @param externalFileSystem the {@link ExternalFileSystem} on which the operation is performed
   */
  protected ExternalFileCommand(F externalFileSystem) {
    super(externalFileSystem);
  }

  /**
   * {@inheritDoc}
   */
  protected String pathToString(URI uri) {
    return uri.getPath();
  }

  /**
   * {@inheritDoc}
   *
   * @return an absolute {@link URI}
   */
  protected URI getAbsolutePath(URI uri) {
    return uri;
  }

  /**
   * {@inheritDoc}
   *
   * @return the parent {@link URI}
   */
  protected URI getParent(URI uri) {
    return trimLastFragment(uri);
  }

  /**
   * {@inheritDoc}
   *
   * @return the resolved {@link URI}
   */
  protected URI resolvePath(URI baseUri, String filePath) {
    return createUri(baseUri.getPath(), filePath);
  }

}
