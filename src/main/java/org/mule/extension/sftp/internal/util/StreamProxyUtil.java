/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatchers;
import org.mule.extension.sftp.internal.stream.LazyStreamSupplier;
import org.mule.runtime.api.exception.MuleRuntimeException;

import java.io.InputStream;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

/**
 * Utility class for creating proxy instances of {@link InputStream} using Byte Buddy.
 * The proxies lazily initialize and delegate method calls to real {@link InputStream} instances.
 *
 * <p>This utility is particularly useful when there's a need to delay the initialization of an {@link InputStream}
 * until it's actually used, for example, in scenarios where the initialization is expensive or requires external resources.</p>
 */
public class StreamProxyUtil {

  /**
   * Creates a proxy instance of {@link InputStream} that lazily initializes and delegates method calls
   * to a real {@link InputStream} instance provided by the specified {@link LazyStreamSupplier}.
   * The actual {@link InputStream} instance is retrieved from the supplier only when a method
   * on the proxy is first invoked.
   *
   * <p>The proxying mechanism is achieved by subclassing the {@link InputStream} class and intercepting all method calls.
   * These calls are redirected to the actual {@link InputStream} instance. If any method on the real {@link InputStream}
   * throws an exception, it is unwrapped and rethrown, ensuring the caller receives the original exception.</p>
   *
   * @param streamFactory The supplier that provides the real {@link InputStream} instance.
   * @return A proxy instance of {@link InputStream} that delegates to the actual instance.
   * @throws MuleRuntimeException If there's an error creating the proxy or if instantiation of the proxy class fails.
   */
  public static InputStream getInputStreamFromStreamFactory(LazyStreamSupplier streamFactory) {
    try {
      return new ByteBuddy()
          .subclass(InputStream.class)
          .method(ElementMatchers.any())
          .intercept(InvocationHandlerAdapter.of(new InvocationHandler() {

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
              try {
                return method.invoke(streamFactory.get(), args);
              } catch (InvocationTargetException e) {
                // This captures and throws the actual exception that might be thrown by the invoked method.
                throw e.getTargetException();
              }
            }
          }))
          .make()
          .load(InputStream.class.getClassLoader())
          .getLoaded()
          .newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      throw new MuleRuntimeException(createStaticMessage("Could not create instance of " + InputStream.class), e);
    }
  }
}
