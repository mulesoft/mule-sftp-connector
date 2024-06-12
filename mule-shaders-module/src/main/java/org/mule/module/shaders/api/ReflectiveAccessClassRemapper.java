/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.shaders.api;

import org.apache.maven.plugins.shade.resource.ResourceTransformer;
import org.mule.module.shaders.api.ReflectiveAccessShaderTransformer.ClassMember;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Changes the specified class members to be accessible via reflection at runtime.
 */
public class ReflectiveAccessClassRemapper extends ClassRemapper {

  private static final char PACKAGE_SEPARATOR = '.';
  private static final char PATH_SEPARATOR = '/';
  private static final char INNER_CLASS_SEPARATOR = '$';
  private final List<ClassMember> configuredMembers;

  public ReflectiveAccessClassRemapper(ClassVisitor classVisitor, Remapper remapper, List<ResourceTransformer> transformers) {
    super(classVisitor, remapper);
    this.configuredMembers = transformers.stream()
        .filter(ReflectiveAccessShaderTransformer.class::isInstance)
        .map(ReflectiveAccessShaderTransformer.class::cast)
        .map(ReflectiveAccessShaderTransformer::getMembers)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  @Override
  public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
    int effectiveAccess = isFieldConfigured(name) ? toAccessible(access) : access;
    return super.visitField(effectiveAccess, name, descriptor, signature, value);
  }

  private boolean isFieldConfigured(String fieldName) {
    return this.configuredMembers.stream()
        .anyMatch(member -> member.equals(toCanonicalName(this.className), fieldName));
  }

  private static int toAccessible(int access) {
    return (access & ~Opcodes.ACC_FINAL & ~Opcodes.ACC_PRIVATE & ~Opcodes.ACC_PROTECTED) | Opcodes.ACC_PUBLIC;
  }

  private static String toCanonicalName(String resourceName) {
    return Optional.ofNullable(resourceName)
        .map(name -> name.replace(PATH_SEPARATOR, PACKAGE_SEPARATOR))
        .map(name -> name.replace(INNER_CLASS_SEPARATOR, PACKAGE_SEPARATOR))
        .orElse(null);
  }
}
