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

import java.util.HashSet;

import scouter.org.objectweb.asm.ModuleVisitor;
import scouter.org.objectweb.asm.Opcodes;
import scouter.org.objectweb.asm.ClassVisitor;

/**
 * A {@link ModuleVisitor} that checks that its methods are properly used.
 *
 * @author Remi Forax
 */
public class CheckModuleAdapter extends ModuleVisitor {
  /** Whether the visited module is open. */
  private final boolean isOpen;

  /** The fully qualified names of the dependencies of the visited module. */
  private final HashSet<String> requiredModules = new HashSet<String>();

  /** The internal names of the packages exported by the visited module. */
  private final HashSet<String> exportedPackages = new HashSet<String>();

  /** The internal names of the packages opened by the visited module. */
  private final HashSet<String> openedPackages = new HashSet<String>();

  /** The internal names of the services used by the visited module. */
  private final HashSet<String> usedServices = new HashSet<String>();

  /** The internal names of the services provided by the visited module. */
  private final HashSet<String> providedServices = new HashSet<String>();

  /** The class version number. */
  int classVersion;

  /** Whether the {@link #visitEnd} method has been called. */
  private boolean visitEndCalled;

  /**
   * Constructs a new {@link CheckModuleAdapter}. <i>Subclasses must not use this constructor</i>.
   * Instead, they must use the {@link #CheckModuleAdapter(int, ModuleVisitor, boolean)} version.
   *
   * @param moduleVisitor the module visitor to which this adapter must delegate calls.
   * @param isOpen whether the visited module is open. Open modules have their {@link
   *     Opcodes#ACC_OPEN} access flag set in {@link ClassVisitor#visitModule}.
   * @throws IllegalStateException If a subclass calls this constructor.
   */
  public CheckModuleAdapter(final ModuleVisitor moduleVisitor, final boolean isOpen) {
    this(Opcodes.ASM6, moduleVisitor, isOpen);
    if (getClass() != CheckModuleAdapter.class) {
      throw new IllegalStateException();
    }
  }

  /**
   * Constructs a new {@link CheckModuleAdapter}.
   *
   * @param api the ASM API version implemented by this visitor. Must be one of {@link
   *     Opcodes#ASM4}, {@link Opcodes#ASM5}, {@link Opcodes#ASM6} or {@link Opcodes#ASM7_EXPERIMENTAL}.
   * @param moduleVisitor the module visitor to which this adapter must delegate calls.
   * @param isOpen whether the visited module is open. Open modules have their {@link
   *     Opcodes#ACC_OPEN} access flag set in {@link ClassVisitor#visitModule}.
   */
  protected CheckModuleAdapter(
      final int api, final ModuleVisitor moduleVisitor, final boolean isOpen) {
    super(api, moduleVisitor);
    this.isOpen = isOpen;
  }

  @Override
  public void visitMainClass(final String mainClass) {
    // Modules can only appear in V9 or more classes.
    CheckMethodAdapter.checkInternalName(Opcodes.V9, mainClass, "module main class");
    super.visitMainClass(mainClass);
  }

  @Override
  public void visitPackage(final String packaze) {
    CheckMethodAdapter.checkInternalName(Opcodes.V9, packaze, "module package");
    super.visitPackage(packaze);
  }

  @Override
  public void visitRequire(final String module, final int access, final String version) {
    checkVisitEndNotCalled();
    CheckClassAdapter.checkFullyQualifiedName(Opcodes.V9, module, "required module");
    checkNameNotAlreadyDeclared(module, requiredModules, "Modules requires");
    CheckClassAdapter.checkAccess(
        access,
        Opcodes.ACC_STATIC_PHASE
            | Opcodes.ACC_TRANSITIVE
            | Opcodes.ACC_SYNTHETIC
            | Opcodes.ACC_MANDATED);
    if (classVersion >= Opcodes.V10
        && module.equals("java.base")
        && (access & (Opcodes.ACC_STATIC_PHASE | Opcodes.ACC_TRANSITIVE)) != 0) {
      throw new IllegalArgumentException(
          "Invalid access flags: "
              + access
              + " java.base can not be declared ACC_TRANSITIVE or ACC_STATIC_PHASE");
    }
    super.visitRequire(module, access, version);
  }

  @Override
  public void visitExport(final String packaze, final int access, final String... modules) {
    checkVisitEndNotCalled();
    CheckMethodAdapter.checkInternalName(Opcodes.V9, packaze, "package name");
    checkNameNotAlreadyDeclared(packaze, exportedPackages, "Module exports");
    CheckClassAdapter.checkAccess(access, Opcodes.ACC_SYNTHETIC | Opcodes.ACC_MANDATED);
    if (modules != null) {
      for (String module : modules) {
        CheckClassAdapter.checkFullyQualifiedName(Opcodes.V9, module, "module export to");
      }
    }
    super.visitExport(packaze, access, modules);
  }

  @Override
  public void visitOpen(final String packaze, final int access, final String... modules) {
    checkVisitEndNotCalled();
    if (isOpen) {
      throw new UnsupportedOperationException("An open module can not use open directive");
    }
    CheckMethodAdapter.checkInternalName(Opcodes.V9, packaze, "package name");
    checkNameNotAlreadyDeclared(packaze, openedPackages, "Module opens");
    CheckClassAdapter.checkAccess(access, Opcodes.ACC_SYNTHETIC | Opcodes.ACC_MANDATED);
    if (modules != null) {
      for (String module : modules) {
        CheckClassAdapter.checkFullyQualifiedName(Opcodes.V9, module, "module open to");
      }
    }
    super.visitOpen(packaze, access, modules);
  }

  @Override
  public void visitUse(final String service) {
    checkVisitEndNotCalled();
    CheckMethodAdapter.checkInternalName(Opcodes.V9, service, "service");
    checkNameNotAlreadyDeclared(service, usedServices, "Module uses");
    super.visitUse(service);
  }

  @Override
  public void visitProvide(final String service, final String... providers) {
    checkVisitEndNotCalled();
    CheckMethodAdapter.checkInternalName(Opcodes.V9, service, "service");
    checkNameNotAlreadyDeclared(service, providedServices, "Module provides");
    if (providers == null || providers.length == 0) {
      throw new IllegalArgumentException("Providers cannot be null or empty");
    }
    for (String provider : providers) {
      CheckMethodAdapter.checkInternalName(Opcodes.V9, provider, "provider");
    }
    super.visitProvide(service, providers);
  }

  @Override
  public void visitEnd() {
    checkVisitEndNotCalled();
    visitEndCalled = true;
    super.visitEnd();
  }

  private static void checkNameNotAlreadyDeclared(
      final String name, final HashSet<String> declaredNames, final String message) {
    if (!declaredNames.add(name)) {
      throw new IllegalArgumentException(message + " " + name + " already declared");
    }
  }

  private void checkVisitEndNotCalled() {
    if (visitEndCalled) {
      throw new IllegalStateException("Cannot call a visit method after visitEnd has been called");
    }
  }
}
