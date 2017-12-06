/*
 *  Copyright 2015 the original author or authors.
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

/***
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2011 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package scouter.org.objectweb.asm.util;

import scouter.org.objectweb.asm.ModuleVisitor;
import scouter.org.objectweb.asm.Opcodes;

import java.util.HashSet;

/**
 * @author Remi Forax
 */
public final class CheckModuleAdapter extends ModuleVisitor {
    private boolean end;
    private final boolean isOpen;

    private final HashSet<String> requireNames = new HashSet<String>();
    private final HashSet<String> exportNames = new HashSet<String>();
    private final HashSet<String> openNames = new HashSet<String>();
    private final HashSet<String> useNames = new HashSet<String>();
    private final HashSet<String> provideNames = new HashSet<String>();
    
    public CheckModuleAdapter(final ModuleVisitor mv, final boolean isOpen) {
        super(Opcodes.ASM6, mv);
        this.isOpen = isOpen;
    }
    
    @Override
    public void visitRequire(String module, int access, String version) {
        checkEnd();
        if (module == null) {
            throw new IllegalArgumentException("require cannot be null");
        }
        checkDeclared("requires", requireNames, module);
        CheckClassAdapter.checkAccess(access, Opcodes.ACC_STATIC_PHASE
                + Opcodes.ACC_TRANSITIVE + Opcodes.ACC_SYNTHETIC + Opcodes.ACC_MANDATED);
        super.visitRequire(module, access, version);
    }
    
    @Override
    public void visitExport(String packaze, int access, String... modules) {
        checkEnd();
        if (packaze == null) {
            throw new IllegalArgumentException("packaze cannot be null");
        }
        CheckMethodAdapter.checkInternalName(packaze, "package name");
        checkDeclared("exports", exportNames, packaze);
        CheckClassAdapter.checkAccess(access, Opcodes.ACC_SYNTHETIC
                + Opcodes.ACC_MANDATED);
        if (modules != null) {
            for (int i = 0; i < modules.length; i++) {
                if (modules[i] == null) {
                    throw new IllegalArgumentException("module at index " + i + " cannot be null");
                }
            }
        }
        super.visitExport(packaze, access, modules);
    }
    
    @Override
    public void visitOpen(String packaze, int access, String... modules) {
        checkEnd();
        if (isOpen) {
            throw new IllegalArgumentException("an open module can not use open directive");
        }
        if (packaze == null) {
            throw new IllegalArgumentException("packaze cannot be null");
        }
        CheckMethodAdapter.checkInternalName(packaze, "package name");
        checkDeclared("opens", openNames, packaze);
        CheckClassAdapter.checkAccess(access, Opcodes.ACC_SYNTHETIC
                + Opcodes.ACC_MANDATED);
        if (modules != null) {
            for (int i = 0; i < modules.length; i++) {
                if (modules[i] == null) {
                    throw new IllegalArgumentException("module at index " + i + " cannot be null");
                }
            }
        }
        super.visitOpen(packaze, access, modules);
    }
    
    @Override
    public void visitUse(String service) {
        checkEnd();
        CheckMethodAdapter.checkInternalName(service, "service");
        checkDeclared("uses", useNames, service);
        super.visitUse(service);
    }
    
    @Override
    public void visitProvide(String service, String... providers) {
        checkEnd();
        CheckMethodAdapter.checkInternalName(service, "service");
        checkDeclared("provides", provideNames, service);
        if (providers == null || providers.length == 0) {
            throw new IllegalArgumentException("providers cannot be null or empty");
        }
        for (int i = 0; i < providers.length; i++) {
            CheckMethodAdapter.checkInternalName(providers[i], "provider");
        }
        super.visitProvide(service, providers);
    }
    
    @Override
    public void visitEnd() {
        checkEnd();
        end = true;
        super.visitEnd();
    }

    private void checkEnd() {
        if (end) {
            throw new IllegalStateException(
                    "Cannot call a visit method after visitEnd has been called");
        }
    }
    
    private static void checkDeclared(String directive, HashSet<String> names, String name) {
        if (!names.add(name)) {
            throw new IllegalArgumentException(directive + " " + name + " already declared");
        }
    }
}
