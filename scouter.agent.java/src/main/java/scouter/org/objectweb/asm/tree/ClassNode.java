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
package scouter.org.objectweb.asm.tree;

import java.util.ArrayList;
import java.util.List;

import scouter.org.objectweb.asm.AnnotationVisitor;
import scouter.org.objectweb.asm.Attribute;
import scouter.org.objectweb.asm.ClassVisitor;
import scouter.org.objectweb.asm.FieldVisitor;
import scouter.org.objectweb.asm.MethodVisitor;
import scouter.org.objectweb.asm.ModuleVisitor;
import scouter.org.objectweb.asm.Opcodes;
import scouter.org.objectweb.asm.TypePath;
import scouter.org.objectweb.asm.Type;

/**
 * A node that represents a class.
 *
 * @author Eric Bruneton
 */
public class ClassNode extends ClassVisitor {

  /**
   * The class version. The minor version is stored in the 16 most significant bits, and the major
   * version in the 16 least significant bits.
   */
  public int version;

  /**
   * The class's access flags (see {@link Opcodes}). This field also indicates if
   * the class is deprecated.
   */
  public int access;

  /** The internal name of this class (see {@link Type#getInternalName}). */
  public String name;

  /** The signature of this class. May be <tt>null</tt>. */
  public String signature;

  /**
   * The internal of name of the super class (see {@link Type#getInternalName}).
   * For interfaces, the super class is {@link Object}. May be <tt>null</tt>, but only for the
   * {@link Object} class.
   */
  public String superName;

  /**
   * The internal names of the interfaces directly implemented by this class (see {@link
   * Type#getInternalName}).
   */
  public List<String> interfaces;

  /** The name of the source file from which this class was compiled. May be <tt>null</tt>. */
  public String sourceFile;

  /**
   * The correspondence between source and compiled elements of this class. May be <tt>null</tt>.
   */
  public String sourceDebug;

  /** The module stored in this class. May be <tt>null</tt>. */
  public ModuleNode module;

  /** The internal name of the enclosing class of this class. May be <tt>null</tt>. */
  public String outerClass;

  /**
   * The name of the method that contains this class, or <tt>null</tt> if this class is not enclosed
   * in a method.
   */
  public String outerMethod;

  /**
   * The descriptor of the method that contains this class, or <tt>null</tt> if this class is not
   * enclosed in a method.
   */
  public String outerMethodDesc;

  /** The runtime visible annotations of this class. May be <tt>null</tt>. */
  public List<AnnotationNode> visibleAnnotations;

  /** The runtime invisible annotations of this class. May be <tt>null</tt>. */
  public List<AnnotationNode> invisibleAnnotations;

  /** The runtime visible type annotations of this class. May be <tt>null</tt>. */
  public List<TypeAnnotationNode> visibleTypeAnnotations;

  /** The runtime invisible type annotations of this class. May be <tt>null</tt>. */
  public List<TypeAnnotationNode> invisibleTypeAnnotations;

  /** The non standard attributes of this class. May be <tt>null</tt>. */
  public List<Attribute> attrs;

  /** The inner classes of this class. */
  public List<InnerClassNode> innerClasses;

  /**
   * <b>Experimental, use at your own risk. This field will be renamed when it becomes stable, this
   * will break existing code using it</b>. The internal name of the nest host class of this class.
   * May be <tt>null</tt>.
   */
  public String nestHostClassExperimental;

  /**
   * <b>Experimental, use at your own risk. This field will be renamed when it becomes stable, this
   * will break existing code using it</b>. The internal names of the nest members of this class.
   * May be <tt>null</tt>.
   */
  public List<String> nestMembersExperimental;

  /** The fields of this class. */
  public List<FieldNode> fields;

  /** The methods of this class. */
  public List<MethodNode> methods;

  /**
   * Constructs a new {@link ClassNode}. <i>Subclasses must not use this constructor</i>. Instead,
   * they must use the {@link #ClassNode(int)} version.
   *
   * @throws IllegalStateException If a subclass calls this constructor.
   */
  public ClassNode() {
    this(Opcodes.ASM6);
    if (getClass() != ClassNode.class) {
      throw new IllegalStateException();
    }
  }

  /**
   * Constructs a new {@link ClassNode}.
   *
   * @param api the ASM API version implemented by this visitor. Must be one of {@link
   *     Opcodes#ASM4}, {@link Opcodes#ASM5}, {@link Opcodes#ASM6} or {@link Opcodes#ASM7_EXPERIMENTAL}.
   */
  public ClassNode(final int api) {
    super(api);
    this.interfaces = new ArrayList<String>();
    this.innerClasses = new ArrayList<InnerClassNode>();
    this.fields = new ArrayList<FieldNode>();
    this.methods = new ArrayList<MethodNode>();
  }

  // -----------------------------------------------------------------------------------------------
  // Implementation of the ClassVisitor abstract class
  // -----------------------------------------------------------------------------------------------

  @Override
  public void visit(
      final int version,
      final int access,
      final String name,
      final String signature,
      final String superName,
      final String[] interfaces) {
    this.version = version;
    this.access = access;
    this.name = name;
    this.signature = signature;
    this.superName = superName;
    this.interfaces = Util.asArrayList(interfaces);
  }

  @Override
  public void visitSource(final String file, final String debug) {
    sourceFile = file;
    sourceDebug = debug;
  }

  @Override
  public ModuleVisitor visitModule(final String name, final int access, final String version) {
    module = new ModuleNode(name, access, version);
    return module;
  }

  @Override
  public void visitNestHostExperimental(final String nestHost) {
    this.nestHostClassExperimental = nestHost;
  }

  @Override
  public void visitOuterClass(final String owner, final String name, final String descriptor) {
    outerClass = owner;
    outerMethod = name;
    outerMethodDesc = descriptor;
  }

  @Override
  public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
    AnnotationNode annotation = new AnnotationNode(descriptor);
    if (visible) {
      if (visibleAnnotations == null) {
        visibleAnnotations = new ArrayList<AnnotationNode>(1);
      }
      visibleAnnotations.add(annotation);
    } else {
      if (invisibleAnnotations == null) {
        invisibleAnnotations = new ArrayList<AnnotationNode>(1);
      }
      invisibleAnnotations.add(annotation);
    }
    return annotation;
  }

  @Override
  public AnnotationVisitor visitTypeAnnotation(
      final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    TypeAnnotationNode typeAnnotation = new TypeAnnotationNode(typeRef, typePath, descriptor);
    if (visible) {
      if (visibleTypeAnnotations == null) {
        visibleTypeAnnotations = new ArrayList<TypeAnnotationNode>(1);
      }
      visibleTypeAnnotations.add(typeAnnotation);
    } else {
      if (invisibleTypeAnnotations == null) {
        invisibleTypeAnnotations = new ArrayList<TypeAnnotationNode>(1);
      }
      invisibleTypeAnnotations.add(typeAnnotation);
    }
    return typeAnnotation;
  }

  @Override
  public void visitAttribute(final Attribute attribute) {
    if (attrs == null) {
      attrs = new ArrayList<Attribute>(1);
    }
    attrs.add(attribute);
  }

  @Override
  public void visitNestMemberExperimental(final String nestMember) {
    if (nestMembersExperimental == null) {
      nestMembersExperimental = new ArrayList<String>();
    }
    nestMembersExperimental.add(nestMember);
  }

  @Override
  public void visitInnerClass(
      final String name, final String outerName, final String innerName, final int access) {
    InnerClassNode innerClass = new InnerClassNode(name, outerName, innerName, access);
    innerClasses.add(innerClass);
  }

  @Override
  public FieldVisitor visitField(
      final int access,
      final String name,
      final String descriptor,
      final String signature,
      final Object value) {
    FieldNode field = new FieldNode(access, name, descriptor, signature, value);
    fields.add(field);
    return field;
  }

  @Override
  public MethodVisitor visitMethod(
      final int access,
      final String name,
      final String descriptor,
      final String signature,
      final String[] exceptions) {
    MethodNode method = new MethodNode(access, name, descriptor, signature, exceptions);
    methods.add(method);
    return method;
  }

  @Override
  public void visitEnd() {
    // Nothing to do.
  }

  // -----------------------------------------------------------------------------------------------
  // Accept method
  // -----------------------------------------------------------------------------------------------

  /**
   * Checks that this class node is compatible with the given ASM API version. This method checks
   * that this node, and all its children recursively, do not contain elements that were introduced
   * in more recent versions of the ASM API than the given version.
   *
   * @param api an ASM API version. Must be one of {@link Opcodes#ASM4}, {@link Opcodes#ASM5},
   *     {@link Opcodes#ASM6} or {@link Opcodes#ASM7_EXPERIMENTAL}.
   */
  public void check(final int api) {
    if (api < Opcodes.ASM7_EXPERIMENTAL
        && (nestHostClassExperimental != null || nestMembersExperimental != null)) {
      throw new UnsupportedClassVersionException();
    }
    if (api < Opcodes.ASM6 && module != null) {
      throw new UnsupportedClassVersionException();
    }
    if (api < Opcodes.ASM5) {
      if (visibleTypeAnnotations != null && !visibleTypeAnnotations.isEmpty()) {
        throw new UnsupportedClassVersionException();
      }
      if (invisibleTypeAnnotations != null && !invisibleTypeAnnotations.isEmpty()) {
        throw new UnsupportedClassVersionException();
      }
    }
    // Check the annotations.
    if (visibleAnnotations != null) {
      for (int i = visibleAnnotations.size() - 1; i >= 0; --i) {
        visibleAnnotations.get(i).check(api);
      }
    }
    if (invisibleAnnotations != null) {
      for (int i = invisibleAnnotations.size() - 1; i >= 0; --i) {
        invisibleAnnotations.get(i).check(api);
      }
    }
    if (visibleTypeAnnotations != null) {
      for (int i = visibleTypeAnnotations.size() - 1; i >= 0; --i) {
        visibleTypeAnnotations.get(i).check(api);
      }
    }
    if (invisibleTypeAnnotations != null) {
      for (int i = invisibleTypeAnnotations.size() - 1; i >= 0; --i) {
        invisibleTypeAnnotations.get(i).check(api);
      }
    }
    for (int i = fields.size() - 1; i >= 0; --i) {
      fields.get(i).check(api);
    }
    for (int i = methods.size() - 1; i >= 0; --i) {
      methods.get(i).check(api);
    }
  }

  /**
   * Makes the given class visitor visit this class.
   *
   * @param classVisitor a class visitor.
   */
  public void accept(final ClassVisitor classVisitor) {
    // Visit the header.
    String[] interfacesArray = new String[this.interfaces.size()];
    this.interfaces.toArray(interfacesArray);
    classVisitor.visit(version, access, name, signature, superName, interfacesArray);
    // Visit the source.
    if (sourceFile != null || sourceDebug != null) {
      classVisitor.visitSource(sourceFile, sourceDebug);
    }
    // Visit the module.
    if (module != null) {
      module.accept(classVisitor);
    }
    // Visit the nest host class.
    if (nestHostClassExperimental != null) {
      classVisitor.visitNestHostExperimental(nestHostClassExperimental);
    }
    // Visit the outer class.
    if (outerClass != null) {
      classVisitor.visitOuterClass(outerClass, outerMethod, outerMethodDesc);
    }
    // Visit the annotations.
    if (visibleAnnotations != null) {
      for (int i = 0, n = visibleAnnotations.size(); i < n; ++i) {
        AnnotationNode annotation = visibleAnnotations.get(i);
        annotation.accept(classVisitor.visitAnnotation(annotation.desc, true));
      }
    }
    if (invisibleAnnotations != null) {
      for (int i = 0, n = invisibleAnnotations.size(); i < n; ++i) {
        AnnotationNode annotation = invisibleAnnotations.get(i);
        annotation.accept(classVisitor.visitAnnotation(annotation.desc, false));
      }
    }
    if (visibleTypeAnnotations != null) {
      for (int i = 0, n = visibleTypeAnnotations.size(); i < n; ++i) {
        TypeAnnotationNode typeAnnotation = visibleTypeAnnotations.get(i);
        typeAnnotation.accept(
            classVisitor.visitTypeAnnotation(
                typeAnnotation.typeRef, typeAnnotation.typePath, typeAnnotation.desc, true));
      }
    }
    if (invisibleTypeAnnotations != null) {
      for (int i = 0, n = invisibleTypeAnnotations.size(); i < n; ++i) {
        TypeAnnotationNode typeAnnotation = invisibleTypeAnnotations.get(i);
        typeAnnotation.accept(
            classVisitor.visitTypeAnnotation(
                typeAnnotation.typeRef, typeAnnotation.typePath, typeAnnotation.desc, false));
      }
    }
    // Visit the non standard attributes.
    if (attrs != null) {
      for (int i = 0, n = attrs.size(); i < n; ++i) {
        classVisitor.visitAttribute(attrs.get(i));
      }
    }
    // Visit the nest members.
    if (nestMembersExperimental != null) {
      for (int i = 0, n = nestMembersExperimental.size(); i < n; ++i) {
        classVisitor.visitNestMemberExperimental(nestMembersExperimental.get(i));
      }
    }
    // Visit the inner classes.
    for (int i = 0, n = innerClasses.size(); i < n; ++i) {
      innerClasses.get(i).accept(classVisitor);
    }
    // Visit the fields.
    for (int i = 0, n = fields.size(); i < n; ++i) {
      fields.get(i).accept(classVisitor);
    }
    // Visit the methods.
    for (int i = 0, n = methods.size(); i < n; ++i) {
      methods.get(i).accept(classVisitor);
    }
    classVisitor.visitEnd();
  }
}
