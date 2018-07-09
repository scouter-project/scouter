// ASM: a very small and fast Java bytecode manipulation framework
// Copyright (c) 2000-2011 INRIA, France Telecom
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
// 3. Neither the name of the copyright holders nor the names of its
//    contributors may be used to endorse or promote products derived from
//    this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
// THE POSSIBILITY OF SUCH DAMAGE.

package scouter.org.objectweb.asm.commons;

import scouter.org.objectweb.asm.AnnotationVisitor;
import scouter.org.objectweb.asm.Attribute;
import scouter.org.objectweb.asm.ClassVisitor;
import scouter.org.objectweb.asm.FieldVisitor;
import scouter.org.objectweb.asm.MethodVisitor;
import scouter.org.objectweb.asm.ModuleVisitor;
import scouter.org.objectweb.asm.Opcodes;
import scouter.org.objectweb.asm.TypePath;

import java.util.List;

/**
 * A {@link ClassVisitor} that remaps types with a {@link Remapper}.
 *
 * @author Eugene Kuleshov
 */
public class ClassRemapper extends ClassVisitor {

  /** The remapper used to remap the types in the visited class. */
  protected final Remapper remapper;

  /** The internal name of the visited class. */
  protected String className;

  /**
   * Constructs a new {@link ClassRemapper}. <i>Subclasses must not use this constructor</i>.
   * Instead, they must use the {@link #ClassRemapper(int,ClassVisitor,Remapper)} version.
   *
   * @param classVisitor the class visitor this remapper must deleted to.
   * @param remapper the remapper to use to remap the types in the visited class.
   */
  public ClassRemapper(final ClassVisitor classVisitor, final Remapper remapper) {
    this(Opcodes.ASM6, classVisitor, remapper);
  }

  /**
   * Constructs a new {@link ClassRemapper}.
   *
   * @param api the ASM API version supported by this remapper. Must be one of {@link
   *     Opcodes#ASM4}, {@link Opcodes#ASM5}, {@link
   *     Opcodes#ASM6} or {@link Opcodes#ASM7_EXPERIMENTAL}.
   * @param classVisitor the class visitor this remapper must deleted to.
   * @param remapper the remapper to use to remap the types in the visited class.
   */
  protected ClassRemapper(final int api, final ClassVisitor classVisitor, final Remapper remapper) {
    super(api, classVisitor);
    this.remapper = remapper;
  }

  @Override
  public void visit(
      final int version,
      final int access,
      final String name,
      final String signature,
      final String superName,
      final String[] interfaces) {
    this.className = name;
    super.visit(
        version,
        access,
        remapper.mapType(name),
        remapper.mapSignature(signature, false),
        remapper.mapType(superName),
        interfaces == null ? null : remapper.mapTypes(interfaces));
  }

  @Override
  public ModuleVisitor visitModule(final String name, final int flags, final String version) {
    ModuleVisitor moduleVisitor = super.visitModule(remapper.mapModuleName(name), flags, version);
    return moduleVisitor == null ? null : createModuleRemapper(moduleVisitor);
  }

  @Override
  public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
    AnnotationVisitor annotationVisitor =
        super.visitAnnotation(remapper.mapDesc(descriptor), visible);
    return annotationVisitor == null ? null : createAnnotationRemapper(annotationVisitor);
  }

  @Override
  public AnnotationVisitor visitTypeAnnotation(
          final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    AnnotationVisitor annotationVisitor =
        super.visitTypeAnnotation(typeRef, typePath, remapper.mapDesc(descriptor), visible);
    return annotationVisitor == null ? null : createAnnotationRemapper(annotationVisitor);
  }

  @Override
  public void visitAttribute(final Attribute attribute) {
    if (attribute instanceof ModuleHashesAttribute) {
      ModuleHashesAttribute moduleHashesAttribute = (ModuleHashesAttribute) attribute;
      List<String> modules = moduleHashesAttribute.modules;
      for (int i = 0; i < modules.size(); ++i) {
        modules.set(i, remapper.mapModuleName(modules.get(i)));
      }
    }
    super.visitAttribute(attribute);
  }

  @Override
  public FieldVisitor visitField(
      final int access,
      final String name,
      final String descriptor,
      final String signature,
      final Object value) {
    FieldVisitor fieldVisitor =
        super.visitField(
            access,
            remapper.mapFieldName(className, name, descriptor),
            remapper.mapDesc(descriptor),
            remapper.mapSignature(signature, true),
            remapper.mapValue(value));
    return fieldVisitor == null ? null : createFieldRemapper(fieldVisitor);
  }

  @Override
  public MethodVisitor visitMethod(
      final int access,
      final String name,
      final String descriptor,
      final String signature,
      final String[] exceptions) {
    String remappedDescriptor = remapper.mapMethodDesc(descriptor);
    MethodVisitor methodVisitor =
        super.visitMethod(
            access,
            remapper.mapMethodName(className, name, descriptor),
            remappedDescriptor,
            remapper.mapSignature(signature, false),
            exceptions == null ? null : remapper.mapTypes(exceptions));
    return methodVisitor == null ? null : createMethodRemapper(methodVisitor);
  }

  @Override
  public void visitInnerClass(
      final String name, final String outerName, final String innerName, final int access) {
    super.visitInnerClass(
        remapper.mapType(name),
        outerName == null ? null : remapper.mapType(outerName),
        innerName,
        access);
  }

  @Override
  public void visitOuterClass(final String owner, final String name, final String descriptor) {
    super.visitOuterClass(
        remapper.mapType(owner),
        name == null ? null : remapper.mapMethodName(owner, name, descriptor),
        descriptor == null ? null : remapper.mapMethodDesc(descriptor));
  }

  @Override
  public void visitNestHostExperimental(final String nestHost) {
    super.visitNestHostExperimental(remapper.mapType(nestHost));
  }

  @Override
  public void visitNestMemberExperimental(final String nestMember) {
    super.visitNestMemberExperimental(remapper.mapType(nestMember));
  }

  /**
   * Constructs a new remapper for fields. The default implementation of this method returns a new
   * {@link FieldRemapper}.
   *
   * @param fieldVisitor the FieldVisitor the remapper must delegate to.
   * @return the newly created remapper.
   */
  protected FieldVisitor createFieldRemapper(final FieldVisitor fieldVisitor) {
    return new FieldRemapper(api, fieldVisitor, remapper);
  }

  /**
   * Constructs a new remapper for methods. The default implementation of this method returns a new
   * {@link MethodRemapper}.
   *
   * @param methodVisitor the MethodVisitor the remapper must delegate to.
   * @return the newly created remapper.
   */
  protected MethodVisitor createMethodRemapper(final MethodVisitor methodVisitor) {
    return new MethodRemapper(api, methodVisitor, remapper);
  }

  /**
   * Constructs a new remapper for annotations. The default implementation of this method returns a
   * new {@link AnnotationRemapper}.
   *
   * @param annotationVisitor the AnnotationVisitor the remapper must delegate to.
   * @return the newly created remapper.
   */
  protected AnnotationVisitor createAnnotationRemapper(final AnnotationVisitor annotationVisitor) {
    return new AnnotationRemapper(api, annotationVisitor, remapper);
  }

  /**
   * Constructs a new remapper for modules. The default implementation of this method returns a new
   * {@link ModuleRemapper}.
   *
   * @param moduleVisitor the ModuleVisitor the remapper must delegate to.
   * @return the newly created remapper.
   */
  protected ModuleVisitor createModuleRemapper(final ModuleVisitor moduleVisitor) {
    return new ModuleRemapper(api, moduleVisitor, remapper);
  }
}