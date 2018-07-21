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

import scouter.org.objectweb.asm.ModuleVisitor;
import scouter.org.objectweb.asm.Opcodes;

/**
 * A {@link ModuleVisitor} that prints the fields it visits with a {@link Printer}.
 *
 * @author Remi Forax
 */
public final class TraceModuleVisitor extends ModuleVisitor {

  /** The printer to convert the visited module into text. */
  public final Printer p;

  /**
   * Constructs a new {@link TraceModuleVisitor}.
   *
   * @param printer the printer to convert the visited module into text.
   */
  public TraceModuleVisitor(final Printer printer) {
    this(null, printer);
  }

  /**
   * Constructs a new {@link TraceModuleVisitor}.
   *
   * @param moduleVisitor the module visitor to which to delegate calls. May be <tt>null</tt>.
   * @param printer the printer to convert the visited module into text.
   */
  public TraceModuleVisitor(final ModuleVisitor moduleVisitor, final Printer printer) {
    super(Opcodes.ASM7_EXPERIMENTAL, moduleVisitor);
    this.p = printer;
  }

  @Override
  public void visitMainClass(final String mainClass) {
    p.visitMainClass(mainClass);
    super.visitMainClass(mainClass);
  }

  @Override
  public void visitPackage(final String packaze) {
    p.visitPackage(packaze);
    super.visitPackage(packaze);
  }

  @Override
  public void visitRequire(final String module, final int access, final String version) {
    p.visitRequire(module, access, version);
    super.visitRequire(module, access, version);
  }

  @Override
  public void visitExport(final String packaze, final int access, final String... modules) {
    p.visitExport(packaze, access, modules);
    super.visitExport(packaze, access, modules);
  }

  @Override
  public void visitOpen(final String packaze, final int access, final String... modules) {
    p.visitOpen(packaze, access, modules);
    super.visitOpen(packaze, access, modules);
  }

  @Override
  public void visitUse(final String use) {
    p.visitUse(use);
    super.visitUse(use);
  }

  @Override
  public void visitProvide(final String service, final String... providers) {
    p.visitProvide(service, providers);
    super.visitProvide(service, providers);
  }

  @Override
  public void visitEnd() {
    p.visitModuleEnd();
    super.visitEnd();
  }
}
