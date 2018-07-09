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

import java.util.List;

import scouter.org.objectweb.asm.ModuleVisitor;
import scouter.org.objectweb.asm.Opcodes;

/**
 * A node that represents an exported package with its name and the module that can access to it.
 *
 * @author Remi Forax
 */
public class ModuleExportNode {

  /** The internal name of the exported package. */
  public String packaze;

  /**
   * The access flags (see {@link Opcodes}). Valid values are {@code
   * ACC_SYNTHETIC} and {@code ACC_MANDATED}.
   */
  public int access;

  /**
   * The list of modules that can access this exported package, specified with fully qualified names
   * (using dots). May be <tt>null</tt>.
   */
  public List<String> modules;

  /**
   * Constructs a new {@link ModuleExportNode}.
   *
   * @param packaze the internal name of the exported package.
   * @param access the package access flags, one or more of {@code ACC_SYNTHETIC} and {@code
   *     ACC_MANDATED}.
   * @param modules a list of modules that can access this exported package, specified with fully
   *     qualified names (using dots).
   */
  public ModuleExportNode(final String packaze, final int access, final List<String> modules) {
    this.packaze = packaze;
    this.access = access;
    this.modules = modules;
  }

  /**
   * Makes the given module visitor visit this export declaration.
   *
   * @param moduleVisitor a module visitor.
   */
  public void accept(final ModuleVisitor moduleVisitor) {
    moduleVisitor.visitExport(
        packaze, access, modules == null ? null : modules.toArray(new String[modules.size()]));
  }
}
