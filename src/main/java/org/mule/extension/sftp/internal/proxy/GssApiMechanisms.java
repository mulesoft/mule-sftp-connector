/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.proxy;

import org.mule.runtime.api.exception.MuleRuntimeException;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;

/**
 * Global repository of GSS-API mechanisms that we can use.
 */
public class GssApiMechanisms {

  private GssApiMechanisms() {
    // No instantiation
  }

  /**
   * Prefix to use with {@link GSSName#NT_HOSTBASED_SERVICE}.
   */
  public static final String GSSAPI_HOST_PREFIX = "host@";

  /**
   * The {@link Oid} of Kerberos 5.
   */
  public static final Oid KERBEROS_5 = createOid("1.2.840.113554.1.2.2");

  /**
   * SGNEGO is not to be used with ssh.
   */
  public static final Oid SPNEGO = createOid("1.3.6.1.5.5.2");

  /**
   * Protects {@link #supportedMechanisms}.
   */
  private static final Object LOCK = new Object();

  /**
   * The {@link AtomicBoolean} is set to {@code true} when the mechanism could be initialized successfully at least once.
   */
  private static Map<Oid, Boolean> supportedMechanisms;

  /**
   * Retrieves an immutable collection of the supported mechanisms.
   *
   * @return the supported mechanisms
   */

  public static Collection<Oid> getSupportedMechanisms() {
    synchronized (LOCK) {
      if (supportedMechanisms == null) {
        GSSManager manager = GSSManager.getInstance();
        Oid[] mechs = manager.getMechs();
        Map<Oid, Boolean> mechanisms = new LinkedHashMap<>();
        if (mechs != null) {
          for (Oid oid : mechs) {
            mechanisms.put(oid, Boolean.FALSE);
          }
        }
        supportedMechanisms = mechanisms;
      }
      return Collections.unmodifiableSet(supportedMechanisms.keySet());
    }
  }

  /**
   * Report that this mechanism was used successfully.
   *
   * @param mechanism that worked
   */
  public static void worked(Oid mechanism) {
    synchronized (LOCK) {
      supportedMechanisms.put(mechanism, Boolean.TRUE);
    }
  }

  /**
   * Mark the mechanisms as failed.
   *
   * @param mechanism to mark
   */
  public static void failed(Oid mechanism) {
    synchronized (LOCK) {
      Boolean worked = supportedMechanisms.get(mechanism);
      if (worked != null && !worked.booleanValue()) {
        // If it never worked, remove it
        supportedMechanisms.remove(mechanism);
      }
    }
  }

  /**
   * Resolves an {@link InetSocketAddress}.
   *
   * @param remote to resolve
   * @return the resolved {@link InetAddress}, or {@code null} if unresolved.
   */
  public static InetAddress resolve(InetSocketAddress remote) {
    InetAddress address = remote.getAddress();
    if (address == null) {
      try {
        address = InetAddress.getByName(remote.getHostString());
      } catch (UnknownHostException e) {
        throw new MuleRuntimeException(e);
      }
    }
    return address;
  }

  /**
   * Determines a canonical host name for use use with GSS-API.
   *
   * @param remote to get the host name from
   * @return the canonical host name, if it can be determined, otherwise the {@link InetSocketAddress#getHostString() unprocessed
   *         host name}.
   */

  public static String getCanonicalName(InetSocketAddress remote) {
    InetAddress address = resolve(remote);
    if (address == null) {
      return remote.getHostString();
    }
    return address.getCanonicalHostName();
  }

  /**
   * Creates a {@link GSSContext} for the given mechanism to authenticate with the host given by {@code fqdn}.
   *
   * @param mechanism {@link Oid} of the mechanism to use
   * @param fqdn      fully qualified domain name of the host to authenticate with
   * @return the context, if the mechanism is available and the context could be created, or {@code null} otherwise
   */
  public static GSSContext createContext(Oid mechanism,
                                         String fqdn) {
    GSSContext context = null;
    try {
      GSSManager manager = GSSManager.getInstance();
      context = manager.createContext(
                                      manager.createName(
                                                         GssApiMechanisms.GSSAPI_HOST_PREFIX + fqdn,
                                                         GSSName.NT_HOSTBASED_SERVICE),
                                      mechanism, null, GSSContext.DEFAULT_LIFETIME);
    } catch (GSSException e) {
      closeContextSilently(context);
      failed(mechanism);
      return null;
    }
    worked(mechanism);
    return context;
  }

  /**
   * Closes (disposes of) a {@link GSSContext} ignoring any {@link GSSException}s.
   *
   * @param context to dispose
   */
  public static void closeContextSilently(GSSContext context) {
    if (context != null) {
      try {
        context.dispose();
      } catch (GSSException e) {
        // Ignore
      }
    }
  }

  private static Oid createOid(String rep) {
    try {
      return new Oid(rep);
    } catch (GSSException e) {
      // Does not occur
      return null;
    }
  }

}
