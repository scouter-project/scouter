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
package scouter.org.objectweb.asm.util;

import scouter.org.objectweb.asm.AnnotationVisitor;
import scouter.org.objectweb.asm.Attribute;
import scouter.org.objectweb.asm.FieldVisitor;
import scouter.org.objectweb.asm.Opcodes;
import scouter.org.objectweb.asm.TypePath;

/**
 * A {@link FieldVisitor} that prints the fields it visits with a {@link Printer}.
 *
 * @author Eric Bruneton
 */
public final class TraceFieldVisitor extends FieldVisitor {

  /** The printer to convert the visited field into text. */
  public final Printer p;

  /**
   * Constructs a new {@link TraceFieldVisitor}.
   *
   * @param printer the printer to convert the visited field into text.
   */
  public TraceFieldVisitor(final Printer printer) {
    this(null, printer);
  }

  /**
   * Constructs a new {@link TraceFieldVisitor}.
   *
   * @param fieldVisitor the field visitor to which to delegate calls. May be <tt>null</tt>.
   * @param printer the printer to convert the visited field into text.
   */
  public TraceFieldVisitor(final FieldVisitor fieldVisitor, final Printer printer) {
    super(Opcodes.ASM7_EXPERIMENTAL, fieldVisitor);
    this.p = printer;
  }

  @Override
  public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
    Printer annotationPrinter = p.visitFieldAnnotation(descriptor, visible);
    return new TraceAnnotationVisitor(
        super.visitAnnotation(descriptor, visible), annotationPrinter);
  }

  @Override
  public AnnotationVisitor visitTypeAnnotation(
      final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    Printer annotationPrinter = p.visitFieldTypeAnnotation(typeRef, typePath, descriptor, visible);
    return new TraceAnnotationVisitor(
        super.visitTypeAnnotation(typeRef, typePath, descriptor, visible), annotationPrinter);
  }

  @Override
  public void visitAttribute(final Attribute attribute) {
    p.visitFieldAttribute(attribute);
    super.visitAttribute(attribute);
  }

  @Override
  public void visitEnd() {
    p.visitFieldEnd();
    super.visitEnd();
  }
}
