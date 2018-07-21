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

import scouter.org.objectweb.asm.MethodVisitor;
import scouter.org.objectweb.asm.Opcodes;
import scouter.org.objectweb.asm.tree.MethodNode;
import scouter.org.objectweb.asm.tree.TryCatchBlockNode;

import java.util.Collections;
import java.util.Comparator;

/**
 * A {@link MethodVisitor} adapter to sort the exception handlers. The handlers are sorted in a
 * method innermost-to-outermost. This allows the programmer to add handlers without worrying about
 * ordering them correctly with respect to existing, in-code handlers.
 *
 * <p>Behavior is only defined for properly-nested handlers. If any "try" blocks overlap (something
 * that isn't possible in Java code) then this may not do what you want. In fact, this adapter just
 * sorts by the length of the "try" block, taking advantage of the fact that a given try block must
 * be larger than any block it contains).
 *
 * @author Adrian Sampson
 */
public class TryCatchBlockSorter extends MethodNode {

  public TryCatchBlockSorter(
      final MethodVisitor methodVisitor,
      final int access,
      final String name,
      final String descriptor,
      final String signature,
      final String[] exceptions) {
    this(Opcodes.ASM6, methodVisitor, access, name, descriptor, signature, exceptions);
    if (getClass() != TryCatchBlockSorter.class) {
      throw new IllegalStateException();
    }
  }

  protected TryCatchBlockSorter(
      final int api,
      final MethodVisitor methodVisitor,
      final int access,
      final String name,
      final String descriptor,
      final String signature,
      final String[] exceptions) {
    super(api, access, name, descriptor, signature, exceptions);
    this.mv = methodVisitor;
  }

  @Override
  public void visitEnd() {
    // Sort the TryCatchBlockNode elements by the length of their "try" block.
    Collections.sort(
        tryCatchBlocks,
        new Comparator<TryCatchBlockNode>() {

          public int compare(
              final TryCatchBlockNode tryCatchBlockNode1,
              final TryCatchBlockNode tryCatchBlockNode2) {
            return blockLength(tryCatchBlockNode1) - blockLength(tryCatchBlockNode2);
          }

          private int blockLength(final TryCatchBlockNode tryCatchBlockNode) {
            int startIndex = instructions.indexOf(tryCatchBlockNode.start);
            int endIndex = instructions.indexOf(tryCatchBlockNode.end);
            return endIndex - startIndex;
          }
        });
    // Update the 'target' of each try catch block annotation.
    for (int i = 0; i < tryCatchBlocks.size(); ++i) {
      tryCatchBlocks.get(i).updateIndex(i);
    }
    if (mv != null) {
      accept(mv);
    }
  }
}
