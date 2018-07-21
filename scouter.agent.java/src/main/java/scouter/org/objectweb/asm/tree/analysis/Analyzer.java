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
package scouter.org.objectweb.asm.tree.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import scouter.org.objectweb.asm.Opcodes;
import scouter.org.objectweb.asm.Type;
import scouter.org.objectweb.asm.tree.AbstractInsnNode;
import scouter.org.objectweb.asm.tree.IincInsnNode;
import scouter.org.objectweb.asm.tree.InsnList;
import scouter.org.objectweb.asm.tree.JumpInsnNode;
import scouter.org.objectweb.asm.tree.LabelNode;
import scouter.org.objectweb.asm.tree.LookupSwitchInsnNode;
import scouter.org.objectweb.asm.tree.MethodNode;
import scouter.org.objectweb.asm.tree.TableSwitchInsnNode;
import scouter.org.objectweb.asm.tree.TryCatchBlockNode;
import scouter.org.objectweb.asm.tree.VarInsnNode;

/**
 * A semantic bytecode analyzer. <i>This class does not fully check that JSR and RET instructions
 * are valid.</i>
 *
 * @param <V> type of the Value used for the analysis.
 * @author Eric Bruneton
 */
public class Analyzer<V extends Value> implements Opcodes {

  /** The interpreter to use to symbolically interpret the bytecode instructions. */
  private final Interpreter<V> interpreter;

  /** The instructions of the currently analyzed method. */
  private InsnList insnList;

  /** The size of {@link #insnList}. */
  private int insnListSize;

  /** The exception handlers of the currently analyzed method (one list per instruction index). */
  private List<TryCatchBlockNode>[] handlers;

  /** The execution stack frames of the currently analyzed method (one per instruction index). */
  private Frame<V>[] frames;

  /** The subroutines of the currently analyzed method (one per instruction index). */
  private Subroutine[] subroutines;

  /** The instructions that remain to process (one boolean per instruction index). */
  private boolean[] inInstructionsToProcess;

  /** The indices of the instructions that remain to process in the currently analyzed method. */
  private int[] instructionsToProcess;

  /** The number of instructions that remain to process in the currently analyzed method. */
  private int numInstructionsToProcess;

  /**
   * Constructs a new {@link Analyzer}.
   *
   * @param interpreter the interpreter to use to symbolically interpret the bytecode instructions.
   */
  public Analyzer(final Interpreter<V> interpreter) {
    this.interpreter = interpreter;
  }

  /**
   * Analyzes the given method.
   *
   * @param owner the internal name of the class to which 'method' belongs.
   * @param method the method to be analyzed.
   * @return the symbolic state of the execution stack frame at each bytecode instruction of the
   *     method. The size of the returned array is equal to the number of instructions (and labels)
   *     of the method. A given frame is <tt>null</tt> if and only if the corresponding instruction
   *     cannot be reached (dead code).
   * @throws AnalyzerException if a problem occurs during the analysis.
   */
  @SuppressWarnings("unchecked")
  public Frame<V>[] analyze(final String owner, final MethodNode method) throws AnalyzerException {
    if ((method.access & (ACC_ABSTRACT | ACC_NATIVE)) != 0) {
      frames = (Frame<V>[]) new Frame<?>[0];
      return frames;
    }
    insnList = method.instructions;
    insnListSize = insnList.size();
    handlers = (List<TryCatchBlockNode>[]) new List<?>[insnListSize];
    frames = (Frame<V>[]) new Frame<?>[insnListSize];
    subroutines = new Subroutine[insnListSize];
    inInstructionsToProcess = new boolean[insnListSize];
    instructionsToProcess = new int[insnListSize];
    numInstructionsToProcess = 0;

    // For each exception handler, and each instruction within its range, record in 'handlers' the
    // fact that execution can flow from this instruction to the exception handler.
    for (int i = 0; i < method.tryCatchBlocks.size(); ++i) {
      TryCatchBlockNode tryCatchBlock = method.tryCatchBlocks.get(i);
      int startIndex = insnList.indexOf(tryCatchBlock.start);
      int endIndex = insnList.indexOf(tryCatchBlock.end);
      for (int j = startIndex; j < endIndex; ++j) {
        List<TryCatchBlockNode> insnHandlers = handlers[j];
        if (insnHandlers == null) {
          insnHandlers = new ArrayList<TryCatchBlockNode>();
          handlers[j] = insnHandlers;
        }
        insnHandlers.add(tryCatchBlock);
      }
    }

    // For each instruction, compute the subroutine to which it belongs.
    // Follow the main 'subroutine', and collect the jsr instructions to nested subroutines.
    Subroutine main = new Subroutine(null, method.maxLocals, null);
    List<AbstractInsnNode> jsrInsns = new ArrayList<AbstractInsnNode>();
    findSubroutine(0, main, jsrInsns);
    // Follow the nested subroutines, and collect their own nested subroutines, until all
    // subroutines are found.
    Map<LabelNode, Subroutine> jsrSubroutines = new HashMap<LabelNode, Subroutine>();
    while (!jsrInsns.isEmpty()) {
      JumpInsnNode jsrInsn = (JumpInsnNode) jsrInsns.remove(0);
      Subroutine subroutine = jsrSubroutines.get(jsrInsn.label);
      if (subroutine == null) {
        subroutine = new Subroutine(jsrInsn.label, method.maxLocals, jsrInsn);
        jsrSubroutines.put(jsrInsn.label, subroutine);
        findSubroutine(insnList.indexOf(jsrInsn.label), subroutine, jsrInsns);
      } else {
        subroutine.callers.add(jsrInsn);
      }
    }
    // Clear the main 'subroutine', which is not a real subroutine (and was used only as an
    // intermediate step above to find the real ones).
    for (int i = 0; i < insnListSize; ++i) {
      if (subroutines[i] != null && subroutines[i].start == null) {
        subroutines[i] = null;
      }
    }

    // Initializes the data structures for the control flow analysis.
    Frame<V> currentFrame = computeInitialFrame(owner, method);
    merge(0, currentFrame, null);
    init(owner, method);

    // Control flow analysis.
    while (numInstructionsToProcess > 0) {
      // Get and remove one instruction from the list of instructions to process.
      int insnIndex = instructionsToProcess[--numInstructionsToProcess];
      Frame<V> oldFrame = frames[insnIndex];
      Subroutine subroutine = subroutines[insnIndex];
      inInstructionsToProcess[insnIndex] = false;

      // Simulate the execution of this instruction.
      AbstractInsnNode insnNode = null;
      try {
        insnNode = method.instructions.get(insnIndex);
        int insnOpcode = insnNode.getOpcode();
        int insnType = insnNode.getType();

        if (insnType == AbstractInsnNode.LABEL
            || insnType == AbstractInsnNode.LINE
            || insnType == AbstractInsnNode.FRAME) {
          merge(insnIndex + 1, oldFrame, subroutine);
          newControlFlowEdge(insnIndex, insnIndex + 1);
        } else {
          currentFrame.init(oldFrame).execute(insnNode, interpreter);
          subroutine = subroutine == null ? null : new Subroutine(subroutine);

          if (insnNode instanceof JumpInsnNode) {
            JumpInsnNode jumpInsn = (JumpInsnNode) insnNode;
            if (insnOpcode != GOTO && insnOpcode != JSR) {
              merge(insnIndex + 1, currentFrame, subroutine);
              newControlFlowEdge(insnIndex, insnIndex + 1);
            }
            int jumpInsnIndex = insnList.indexOf(jumpInsn.label);
            if (insnOpcode == JSR) {
              merge(
                  jumpInsnIndex,
                  currentFrame,
                  new Subroutine(jumpInsn.label, method.maxLocals, jumpInsn));
            } else {
              merge(jumpInsnIndex, currentFrame, subroutine);
            }
            newControlFlowEdge(insnIndex, jumpInsnIndex);
          } else if (insnNode instanceof LookupSwitchInsnNode) {
            LookupSwitchInsnNode lookupSwitchInsn = (LookupSwitchInsnNode) insnNode;
            int targetInsnIndex = insnList.indexOf(lookupSwitchInsn.dflt);
            merge(targetInsnIndex, currentFrame, subroutine);
            newControlFlowEdge(insnIndex, targetInsnIndex);
            for (int i = 0; i < lookupSwitchInsn.labels.size(); ++i) {
              targetInsnIndex = insnList.indexOf(lookupSwitchInsn.labels.get(i));
              merge(targetInsnIndex, currentFrame, subroutine);
              newControlFlowEdge(insnIndex, targetInsnIndex);
            }
          } else if (insnNode instanceof TableSwitchInsnNode) {
            TableSwitchInsnNode tableSwitchInsn = (TableSwitchInsnNode) insnNode;
            int targetInsnIndex = insnList.indexOf(tableSwitchInsn.dflt);
            merge(targetInsnIndex, currentFrame, subroutine);
            newControlFlowEdge(insnIndex, targetInsnIndex);
            for (int i = 0; i < tableSwitchInsn.labels.size(); ++i) {
              targetInsnIndex = insnList.indexOf(tableSwitchInsn.labels.get(i));
              merge(targetInsnIndex, currentFrame, subroutine);
              newControlFlowEdge(insnIndex, targetInsnIndex);
            }
          } else if (insnOpcode == RET) {
            if (subroutine == null) {
              throw new AnalyzerException(insnNode, "RET instruction outside of a sub routine");
            }
            for (int i = 0; i < subroutine.callers.size(); ++i) {
              JumpInsnNode caller = subroutine.callers.get(i);
              int jsrInsnIndex = insnList.indexOf(caller);
              if (frames[jsrInsnIndex] != null) {
                merge(
                    jsrInsnIndex + 1,
                    frames[jsrInsnIndex],
                    currentFrame,
                    subroutines[jsrInsnIndex],
                    subroutine.localsUsed);
                newControlFlowEdge(insnIndex, jsrInsnIndex + 1);
              }
            }
          } else if (insnOpcode != ATHROW && (insnOpcode < IRETURN || insnOpcode > RETURN)) {
            if (subroutine != null) {
              if (insnNode instanceof VarInsnNode) {
                int var = ((VarInsnNode) insnNode).var;
                subroutine.localsUsed[var] = true;
                if (insnOpcode == LLOAD
                    || insnOpcode == DLOAD
                    || insnOpcode == LSTORE
                    || insnOpcode == DSTORE) {
                  subroutine.localsUsed[var + 1] = true;
                }
              } else if (insnNode instanceof IincInsnNode) {
                int var = ((IincInsnNode) insnNode).var;
                subroutine.localsUsed[var] = true;
              }
            }
            merge(insnIndex + 1, currentFrame, subroutine);
            newControlFlowEdge(insnIndex, insnIndex + 1);
          }
        }

        List<TryCatchBlockNode> insnHandlers = handlers[insnIndex];
        if (insnHandlers != null) {
          for (int i = 0; i < insnHandlers.size(); ++i) {
            TryCatchBlockNode tryCatchBlock = insnHandlers.get(i);
            Type catchType;
            if (tryCatchBlock.type == null) {
              catchType = Type.getObjectType("java/lang/Throwable");
            } else {
              catchType = Type.getObjectType(tryCatchBlock.type);
            }
            if (newControlFlowExceptionEdge(insnIndex, tryCatchBlock)) {
              Frame<V> handler = newFrame(oldFrame);
              handler.clearStack();
              handler.push(interpreter.newValue(catchType));
              merge(insnList.indexOf(tryCatchBlock.handler), handler, subroutine);
            }
          }
        }
      } catch (AnalyzerException e) {
        throw new AnalyzerException(
            e.node, "Error at instruction " + insnIndex + ": " + e.getMessage(), e);
      } catch (Exception e) {
        throw new AnalyzerException(
            insnNode, "Error at instruction " + insnIndex + ": " + e.getMessage(), e);
      }
    }

    return frames;
  }

  /**
   * Follows the control flow graph of the currently analyzed method, starting at the given
   * instruction index, and stores a copy of the given subroutine in {@link #subroutines} for each
   * encountered instruction. Jumps to nested subroutines are <i>not</i> followed: instead, the
   * corresponding instructions are put in the given list.
   *
   * @param insnIndex an instruction index.
   * @param subroutine a subroutine.
   * @param jsrInsns where the jsr instructions for nested subroutines must be put.
   * @throws AnalyzerException if the control flow graph can fall off the end of the code.
   */
  private void findSubroutine(
      final int insnIndex, final Subroutine subroutine, final List<AbstractInsnNode> jsrInsns)
      throws AnalyzerException {
    int currentInsnIndex = insnIndex;
    while (true) {
      if (currentInsnIndex < 0 || currentInsnIndex >= insnListSize) {
        throw new AnalyzerException(null, "Execution can fall off the end of the code");
      }
      if (subroutines[currentInsnIndex] != null) {
        return;
      }
      subroutines[currentInsnIndex] = new Subroutine(subroutine);
      AbstractInsnNode currentInsn = insnList.get(currentInsnIndex);

      // Call findSubroutine recursively on the normal successors of currentInsn.
      if (currentInsn instanceof JumpInsnNode) {
        if (currentInsn.getOpcode() == JSR) {
          // Do not follow a jsr, it leads to another subroutine!
          jsrInsns.add(currentInsn);
        } else {
          JumpInsnNode jumpInsn = (JumpInsnNode) currentInsn;
          findSubroutine(insnList.indexOf(jumpInsn.label), subroutine, jsrInsns);
        }
      } else if (currentInsn instanceof TableSwitchInsnNode) {
        TableSwitchInsnNode tableSwitchInsn = (TableSwitchInsnNode) currentInsn;
        findSubroutine(insnList.indexOf(tableSwitchInsn.dflt), subroutine, jsrInsns);
        for (int i = tableSwitchInsn.labels.size() - 1; i >= 0; --i) {
          LabelNode l = tableSwitchInsn.labels.get(i);
          findSubroutine(insnList.indexOf(l), subroutine, jsrInsns);
        }
      } else if (currentInsn instanceof LookupSwitchInsnNode) {
        LookupSwitchInsnNode lookupSwitchInsn = (LookupSwitchInsnNode) currentInsn;
        findSubroutine(insnList.indexOf(lookupSwitchInsn.dflt), subroutine, jsrInsns);
        for (int i = lookupSwitchInsn.labels.size() - 1; i >= 0; --i) {
          LabelNode l = lookupSwitchInsn.labels.get(i);
          findSubroutine(insnList.indexOf(l), subroutine, jsrInsns);
        }
      }

      // Call findSubroutine recursively on the exception handler successors of currentInsn.
      List<TryCatchBlockNode> insnHandlers = handlers[currentInsnIndex];
      if (insnHandlers != null) {
        for (int i = 0; i < insnHandlers.size(); ++i) {
          TryCatchBlockNode tryCatchBlock = insnHandlers.get(i);
          findSubroutine(insnList.indexOf(tryCatchBlock.handler), subroutine, jsrInsns);
        }
      }

      // If currentInsn does not fall through to the next instruction, return.
      switch (currentInsn.getOpcode()) {
        case GOTO:
        case RET:
        case TABLESWITCH:
        case LOOKUPSWITCH:
        case IRETURN:
        case LRETURN:
        case FRETURN:
        case DRETURN:
        case ARETURN:
        case RETURN:
        case ATHROW:
          return;
        default:
          break;
      }
      currentInsnIndex++;
    }
  }

  /**
   * Computes the initial execution stack frame of the given method.
   *
   * @param owner the internal name of the class to which 'method' belongs.
   * @param method the method to be analyzed.
   * @return the initial execution stack frame of the 'method'.
   */
  private Frame<V> computeInitialFrame(final String owner, final MethodNode method) {
    Frame<V> frame = newFrame(method.maxLocals, method.maxStack);
    int currentLocal = 0;
    if ((method.access & ACC_STATIC) == 0) {
      Type ownerType = Type.getObjectType(owner);
      frame.setLocal(currentLocal++, interpreter.newValue(ownerType));
    }
    Type[] argumentTypes = Type.getArgumentTypes(method.desc);
    for (int i = 0; i < argumentTypes.length; ++i) {
      frame.setLocal(currentLocal++, interpreter.newValue(argumentTypes[i]));
      if (argumentTypes[i].getSize() == 2) {
        frame.setLocal(currentLocal++, interpreter.newValue(null));
      }
    }
    while (currentLocal < method.maxLocals) {
      frame.setLocal(currentLocal++, interpreter.newValue(null));
    }
    frame.setReturn(interpreter.newValue(Type.getReturnType(method.desc)));
    return frame;
  }

  /**
   * Returns the symbolic execution stack frame for each instruction of the last analyzed method.
   *
   * @return the symbolic state of the execution stack frame at each bytecode instruction of the
   *     method. The size of the returned array is equal to the number of instructions (and labels)
   *     of the method. A given frame is <tt>null</tt> if the corresponding instruction cannot be
   *     reached, or if an error occurred during the analysis of the method.
   */
  public Frame<V>[] getFrames() {
    return frames;
  }

  /**
   * Returns the exception handlers for the given instruction.
   *
   * @param insnIndex the index of an instruction of the last analyzed method.
   * @return a list of {@link TryCatchBlockNode} objects.
   */
  public List<TryCatchBlockNode> getHandlers(final int insnIndex) {
    return handlers[insnIndex];
  }

  /**
   * Initializes this analyzer. This method is called just before the execution of control flow
   * analysis loop in #analyze. The default implementation of this method does nothing.
   *
   * @param owner the internal name of the class to which the method belongs.
   * @param method the method to be analyzed.
   * @throws AnalyzerException if a problem occurs.
   */
  protected void init(final String owner, final MethodNode method) throws AnalyzerException {
    // Nothing to do.
  }

  /**
   * Constructs a new frame with the given size.
   *
   * @param nLocals the maximum number of local variables of the frame.
   * @param nStack the maximum stack size of the frame.
   * @return the created frame.
   */
  protected Frame<V> newFrame(final int nLocals, final int nStack) {
    return new Frame<V>(nLocals, nStack);
  }

  /**
   * Constructs a copy of the given frame.
   *
   * @param frame a frame.
   * @return the created frame.
   */
  protected Frame<V> newFrame(final Frame<? extends V> frame) {
    return new Frame<V>(frame);
  }

  /**
   * Creates a control flow graph edge. The default implementation of this method does nothing. It
   * can be overridden in order to construct the control flow graph of a method (this method is
   * called by the {@link #analyze} method during its visit of the method's code).
   *
   * @param insnIndex an instruction index.
   * @param successorIndex index of a successor instruction.
   */
  protected void newControlFlowEdge(final int insnIndex, final int successorIndex) {
    // Nothing to do.
  }

  /**
   * Creates a control flow graph edge corresponding to an exception handler. The default
   * implementation of this method does nothing. It can be overridden in order to construct the
   * control flow graph of a method (this method is called by the {@link #analyze} method during its
   * visit of the method's code).
   *
   * @param insnIndex an instruction index.
   * @param successorIndex index of a successor instruction.
   * @return true if this edge must be considered in the data flow analysis performed by this
   *     analyzer, or false otherwise. The default implementation of this method always returns
   *     true.
   */
  protected boolean newControlFlowExceptionEdge(final int insnIndex, final int successorIndex) {
    return true;
  }

  /**
   * Creates a control flow graph edge corresponding to an exception handler. The default
   * implementation of this method delegates to {@link #newControlFlowExceptionEdge(int, int)}. It
   * can be overridden in order to construct the control flow graph of a method (this method is
   * called by the {@link #analyze} method during its visit of the method's code).
   *
   * @param insnIndex an instruction index.
   * @param tryCatchBlock TryCatchBlockNode corresponding to this edge.
   * @return true if this edge must be considered in the data flow analysis performed by this
   *     analyzer, or false otherwise. The default implementation of this method delegates to {@link
   *     #newControlFlowExceptionEdge(int, int)}.
   */
  protected boolean newControlFlowExceptionEdge(
      final int insnIndex, final TryCatchBlockNode tryCatchBlock) {
    return newControlFlowExceptionEdge(insnIndex, insnList.indexOf(tryCatchBlock.handler));
  }

  // -----------------------------------------------------------------------------------------------

  /**
   * Merges the given frame and subroutine into the frame and subroutines at the given instruction
   * index. If the frame or the subroutine at the given instruction index changes as a result of
   * this merge, the instruction index is added to the list of instructions to process (if it is not
   * already the case).
   *
   * @param insnIndex an instruction index.
   * @param frame a frame. This frame is left unchanged by this method.
   * @param subroutine a subroutine. This subroutine is left unchanged by this method.
   * @throws AnalyzerException if the frames have incompatible sizes.
   */
  private void merge(final int insnIndex, final Frame<V> frame, final Subroutine subroutine)
      throws AnalyzerException {
    boolean changed;
    Frame<V> oldFrame = frames[insnIndex];
    if (oldFrame == null) {
      frames[insnIndex] = newFrame(frame);
      changed = true;
    } else {
      changed = oldFrame.merge(frame, interpreter);
    }
    Subroutine oldSubroutine = subroutines[insnIndex];
    if (oldSubroutine == null) {
      if (subroutine != null) {
        subroutines[insnIndex] = new Subroutine(subroutine);
        changed = true;
      }
    } else {
      if (subroutine != null) {
        changed |= oldSubroutine.merge(subroutine);
      }
    }
    if (changed && !inInstructionsToProcess[insnIndex]) {
      inInstructionsToProcess[insnIndex] = true;
      instructionsToProcess[numInstructionsToProcess++] = insnIndex;
    }
  }

  /**
   * Merges the given frame and subroutine into the frame and subroutines at the given instruction
   * index (case of a RET instruction). If the frame or the subroutine at the given instruction
   * index changes as a result of this merge, the instruction index is added to the list of
   * instructions to process (if it is not already the case).
   *
   * @param insnIndex the index of an instruction immediately following a jsr instruction.
   * @param frameBeforeJsr the execution stack frame before the jsr instruction. This frame is
   *     merged into 'frameAfterRet'.
   * @param frameAfterRet the execution stack frame after a ret instruction of the subroutine. This
   *     frame is merged into the frame at 'insnIndex' (after it has itself been merge with
   *     'frameBeforeJsr').
   * @param subroutineBeforeJsr if the jsr is itself part of a subroutine (case of nested
   *     subroutine), the subroutine it belongs to.
   * @param localsUsed the local variables read or written in the subroutine.
   * @throws AnalyzerException if the frames have incompatible sizes.
   */
  private void merge(
      final int insnIndex,
      final Frame<V> frameBeforeJsr,
      final Frame<V> frameAfterRet,
      final Subroutine subroutineBeforeJsr,
      final boolean[] localsUsed)
      throws AnalyzerException {
    frameAfterRet.merge(frameBeforeJsr, localsUsed);

    boolean changed;
    Frame<V> oldFrame = frames[insnIndex];
    if (oldFrame == null) {
      frames[insnIndex] = newFrame(frameAfterRet);
      changed = true;
    } else {
      changed = oldFrame.merge(frameAfterRet, interpreter);
    }
    Subroutine oldSubroutine = subroutines[insnIndex];
    if (oldSubroutine != null && subroutineBeforeJsr != null) {
      changed |= oldSubroutine.merge(subroutineBeforeJsr);
    }
    if (changed && !inInstructionsToProcess[insnIndex]) {
      inInstructionsToProcess[insnIndex] = true;
      instructionsToProcess[numInstructionsToProcess++] = insnIndex;
    }
  }
}
