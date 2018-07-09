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

/**
 * A node that represents a service and its implementation provided by the current module.
 *
 * @author Remi Forax
 */
public class ModuleProvideNode {

  /** The internal name of the service. */
  public String service;

  /** The internal names of the implementations of the service (there is at least one provider). */
  public List<String> providers;

  /**
   * Constructs a new {@link ModuleProvideNode}.
   *
   * @param service the internal name of the service.
   * @param providers the internal names of the implementations of the service (there is at least
   *     one provider).
   */
  public ModuleProvideNode(final String service, final List<String> providers) {
    this.service = service;
    this.providers = providers;
  }

  /**
   * Makes the given module visitor visit this require declaration.
   *
   * @param moduleVisitor a module visitor.
   */
  public void accept(final ModuleVisitor moduleVisitor) {
    moduleVisitor.visitProvide(service, providers.toArray(new String[providers.size()]));
  }
}
