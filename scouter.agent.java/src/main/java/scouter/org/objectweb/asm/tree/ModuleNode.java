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
package scouter.org.objectweb.asm.tree;

import scouter.org.objectweb.asm.ClassVisitor;
import scouter.org.objectweb.asm.ModuleVisitor;
import scouter.org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.List;

/**
 * A node that represents a module declaration.
 * 
 * @author Remi Forax
 */
public class ModuleNode extends ModuleVisitor {
    /**
     * Module name
     */
    public String name;
    
    /**
     * Module access flags, among {@code ACC_OPEN}, {@code ACC_SYNTHETIC}
     *            and {@code ACC_MANDATED}.
     */
    public int access;
    
    /**
     * Version of the module.
     * May be <tt>null</tt>.
     */
    public String version;
    
    /**
     * Name of the main class in internal form
     * May be <tt>null</tt>.
     */
    public String mainClass;
    
    /**
     * A list of packages that are declared by the current module.
     * May be <tt>null</tt>.
     */
    public List<String> packages;
    
    /**
     * A list of modules can are required by the current module.
     * May be <tt>null</tt>.
     */
    public List<ModuleRequireNode> requires;
    
    /**
     * A list of packages that are exported by the current module.
     * May be <tt>null</tt>.
     */
    public List<ModuleExportNode> exports;
    
    /**
     * A list of packages that are opened by the current module.
     * May be <tt>null</tt>.
     */
    public List<ModuleOpenNode> opens;
    
    /**
     * A list of classes in their internal forms that are used
     * as a service by the current module. May be <tt>null</tt>.
     */
    public List<String> uses;
   
    /**
     * A list of services along with their implementations provided
     * by the current module. May be <tt>null</tt>.
     */
    public List<ModuleProvideNode> provides;

    public ModuleNode(final String name, final int access,
            final String version) {
        super(Opcodes.ASM6);
        this.name = name;
        this.access = access;
        this.version = version;
    }
    
    public ModuleNode(final int api,
      final String name,
      final int access,
      final String version,
      final List<ModuleRequireNode> requires,
      final List<ModuleExportNode> exports,
      final List<ModuleOpenNode> opens,
      final List<String> uses,
      final List<ModuleProvideNode> provides) {
        super(api);
        this.name = name;
        this.access = access;
        this.version = version;
        this.requires = requires;
        this.exports = exports;
        this.opens = opens;
        this.uses = uses;
        this.provides = provides;
        if (getClass() != ModuleNode.class) {
            throw new IllegalStateException();
        }
    }
    
    @Override
    public void visitMainClass(String mainClass) {
        this.mainClass = mainClass;
    }
    
    @Override
    public void visitPackage(String packaze) {
        if (packages == null) {
            packages = new ArrayList<String>(5);
        }
        packages.add(packaze);
    }
    
    @Override
    public void visitRequire(String module, int access, String version) {
        if (requires == null) {
            requires = new ArrayList<ModuleRequireNode>(5);
        }
        requires.add(new ModuleRequireNode(module, access, version));
    }
    
    @Override
    public void visitExport(String packaze, int access, String... modules) {
        if (exports == null) {
            exports = new ArrayList<ModuleExportNode>(5);
        }
        List<String> moduleList = null;
        if (modules != null) {
            moduleList = new ArrayList<String>(modules.length);
            for (int i = 0; i < modules.length; i++) {
                moduleList.add(modules[i]);
            }
        }
        exports.add(new ModuleExportNode(packaze, access, moduleList));
    }
    
    @Override
    public void visitOpen(String packaze, int access, String... modules) {
        if (opens == null) {
            opens = new ArrayList<ModuleOpenNode>(5);
        }
        List<String> moduleList = null;
        if (modules != null) {
            moduleList = new ArrayList<String>(modules.length);
            for (int i = 0; i < modules.length; i++) {
                moduleList.add(modules[i]);
            }
        }
        opens.add(new ModuleOpenNode(packaze, access, moduleList));
    }
    
    @Override
    public void visitUse(String service) {
        if (uses == null) {
            uses = new ArrayList<String>(5);
        }
        uses.add(service);
    }
    
    @Override
    public void visitProvide(String service, String... providers) {
        if (provides == null) {
            provides = new ArrayList<ModuleProvideNode>(5);
        }
        ArrayList<String> providerList =
                new ArrayList<String>(providers.length);
        for (int i = 0; i < providers.length; i++) {
                providerList.add(providers[i]);
        }
        provides.add(new ModuleProvideNode(service, providerList));
    }
    
    @Override
    public void visitEnd() {
    }
    
    public void accept(final ClassVisitor cv) {
        ModuleVisitor mv = cv.visitModule(name, access, version);
        if (mv == null) {
            return;
        }
        if (mainClass != null) {
            mv.visitMainClass(mainClass);
        }
        if (packages != null) {
            for (int i = 0; i < packages.size(); i++) {
                mv.visitPackage(packages.get(i));
            }
        }
        
        if (requires != null) {
            for (int i = 0; i < requires.size(); i++) {
                requires.get(i).accept(mv);
            }
        }
        if (exports != null) {
            for (int i = 0; i < exports.size(); i++) {
                exports.get(i).accept(mv);
            }
        }
        if (opens != null) {
            for (int i = 0; i < opens.size(); i++) {
                opens.get(i).accept(mv);
            }
        }
        if (uses != null) {
            for (int i = 0; i < uses.size(); i++) {
                mv.visitUse(uses.get(i));
            }
        }
        if (provides != null) {
            for (int i = 0; i < provides.size(); i++) {
                provides.get(i).accept(mv);
            }
        }
    }
}
