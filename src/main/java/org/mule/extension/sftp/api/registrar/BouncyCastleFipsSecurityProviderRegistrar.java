/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.api.registrar;

import org.apache.sshd.common.util.ExceptionUtils;
import org.apache.sshd.common.util.GenericUtils;
import org.apache.sshd.common.util.security.AbstractSecurityProviderRegistrar;
import org.apache.sshd.common.util.threads.ThreadUtils;

import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.Provider;
import java.security.Signature;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class BouncyCastleFipsSecurityProviderRegistrar extends AbstractSecurityProviderRegistrar {

  public static final String PROVIDER_CLASS = "org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider";
  public static final String MULE_SECURITY_MODEL = "mule.security.model";
  public static final String FIPS_140_2_MODEL = "fips140-2";
  private final AtomicReference<Boolean> supportHolder = new AtomicReference((Object) null);
  private final AtomicReference<String> allSupportHolder = new AtomicReference();

  public BouncyCastleFipsSecurityProviderRegistrar() {
    super("BCFIPS");
  }

  @Override
  public boolean isEnabled() {
    return FIPS_140_2_MODEL.equals(System.getProperty(MULE_SECURITY_MODEL));
  }

  public Provider getSecurityProvider() {
    try {
      return this.getOrCreateProvider(PROVIDER_CLASS);
    } catch (ReflectiveOperationException var3) {
      Throwable e = ExceptionUtils.peelException(var3);
      this.log.error("getSecurityProvider({}) failed ({}) to instantiate {}: {}",
                     new Object[] {this.getName(), e.getClass().getSimpleName(), PROVIDER_CLASS, e.getMessage()});
      if (e instanceof RuntimeException) {
        throw (RuntimeException) e;
      } else {
        throw new IllegalStateException(e);
      }
    }
  }

  public String getDefaultSecurityEntitySupportValue(Class<?> entityType) {
    String allValue = (String) this.allSupportHolder.get();
    if (GenericUtils.length(allValue) > 0) {
      return allValue;
    } else {
      String propName = this.getConfigurationPropertyName("supportAll");
      allValue = this.getStringProperty(propName, "all");
      if (GenericUtils.isEmpty(allValue)) {
        allValue = "none";
      }

      this.allSupportHolder.set(allValue);
      return allValue;
    }
  }

  public boolean isSecurityEntitySupported(Class<?> entityType, String name) {
    if (!this.isSupported()) {
      return false;
    } else {
      if (!KeyPairGenerator.class.isAssignableFrom(entityType) && !KeyFactory.class.isAssignableFrom(entityType)) {
        if (Signature.class.isAssignableFrom(entityType)
            && Objects.compare(name, "NONEwithEdDSA", String.CASE_INSENSITIVE_ORDER) == 0) {
          return false;
        }
      } else if (Objects.compare(name, "EdDSA", String.CASE_INSENSITIVE_ORDER) == 0) {
        return false;
      }

      return super.isSecurityEntitySupported(entityType, name);
    }
  }

  public boolean isSupported() {
    Boolean supported;
    synchronized (this.supportHolder) {
      supported = (Boolean) this.supportHolder.get();
      if (supported != null) {
        return supported;
      }

      Class<?> clazz = ThreadUtils.resolveDefaultClass(this.getClass(), PROVIDER_CLASS);
      supported = clazz != null;
      this.supportHolder.set(supported);
    }

    return supported;
  }
}
