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
import java.util.Map;

import scouter.org.objectweb.asm.Label;
import scouter.org.objectweb.asm.MethodVisitor;
import scouter.org.objectweb.asm.Opcodes;

/**
 * A node that represents a LOOKUPSWITCH instruction.
 *
 * @author Eric Bruneton
 */
public class LookupSwitchInsnNode extends AbstractInsnNode {

  /** Beginning of the default handler block. */
  public LabelNode dflt;

  /** The values of the keys. */
  public List<Integer> keys;

  /** Beginnings of the handler blocks. */
  public List<LabelNode> labels;

  /**
   * Constructs a new {@link LookupSwitchInsnNode}.
   *
   * @param dflt beginning of the default handler block.
   * @param keys the values of the keys.
   * @param labels beginnings of the handler blocks. <tt>labels[i]</tt> is the beginning of the
   *     handler block for the <tt>keys[i]</tt> key.
   */
  public LookupSwitchInsnNode(final LabelNode dflt, final int[] keys, final LabelNode[] labels) {
    super(Opcodes.LOOKUPSWITCH);
    this.dflt = dflt;
    this.keys = Util.asArrayList(keys);
    this.labels = Util.asArrayList(labels);
  }

  @Override
  public int getType() {
    return LOOKUPSWITCH_INSN;
  }

  @Override
  public void accept(final MethodVisitor methodVisitor) {
    int[] keysArray = new int[this.keys.size()];
    for (int i = 0, n = keysArray.length; i < n; ++i) {
      keysArray[i] = this.keys.get(i).intValue();
    }
    Label[] labelsArray = new Label[this.labels.size()];
    for (int i = 0, n = labelsArray.length; i < n; ++i) {
      labelsArray[i] = this.labels.get(i).getLabel();
    }
    methodVisitor.visitLookupSwitchInsn(dflt.getLabel(), keysArray, labelsArray);
    acceptAnnotations(methodVisitor);
  }

  @Override
  public AbstractInsnNode clone(final Map<LabelNode, LabelNode> clonedLabels) {
    LookupSwitchInsnNode clone =
        new LookupSwitchInsnNode(clone(dflt, clonedLabels), null, clone(labels, clonedLabels));
    clone.keys.addAll(keys);
    return clone.cloneAnnotations(this);
  }
}
