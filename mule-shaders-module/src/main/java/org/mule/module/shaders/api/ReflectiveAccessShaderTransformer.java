/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.shaders.api;

import org.apache.maven.plugins.shade.relocation.Relocator;
import org.apache.maven.plugins.shade.resource.ResourceTransformer;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarOutputStream;

/**
 * The transformer configuration needed by the shader to enable reflective access for the specified class members.
 */
public class ReflectiveAccessShaderTransformer implements ResourceTransformer {

  private List<ClassMember> members;

  @Override
  public boolean canTransformResource(String resource) {
    return false;
  }

  @Override
  public boolean hasTransformedResource() {
    return false;
  }

  @Override
  public void processResource(String resource, InputStream is, List<Relocator> relocators) {}

  @Override
  public void modifyOutputStream(JarOutputStream os) {}

  public List<ClassMember> getMembers() {
    return members;
  }

  public void setMembers(List<ClassMember> members) {
    this.members = members;
  }

  public static class ClassMember {

    private String className;
    private String memberName;

    public boolean equals(String className, String memberName) {
      return Objects.equals(this.className, className) && Objects.equals(this.memberName, memberName);
    }

    public String getClassName() {
      return className;
    }

    public void setClassName(String className) {
      this.className = className;
    }

    public String getMemberName() {
      return memberName;
    }

    public void setMemberName(String memberName) {
      this.memberName = memberName;
    }
  }
}
