/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.extension.sftp.internal.stream.AbstractNonFinalizableFileInputStream;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;

import java.io.IOException;
import java.io.InputStream;

public class StreamCloserTestMessageProcessor implements Processor {

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    try {
      assertThat(((AbstractNonFinalizableFileInputStream) event.getMessage().getPayload().getValue()).isLocked(), is(true));
      ((InputStream) event.getMessage().getPayload().getValue()).close();
    } catch (IOException e) {
      throw new MuleRuntimeException(e);
    }
    return event;
  }
}
