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

package scouter.org.objectweb.asm.commons;

import scouter.org.objectweb.asm.Attribute;
import scouter.org.objectweb.asm.ByteVector;
import scouter.org.objectweb.asm.ClassReader;
import scouter.org.objectweb.asm.ClassVisitor;
import scouter.org.objectweb.asm.ClassWriter;
import scouter.org.objectweb.asm.Label;

import java.util.ArrayList;
import java.util.List;

/**
 * ModuleHashes attribute.
 * This attribute is specific to the OpenJDK and may change in the future.
 * 
 * @author Remi Forax
 */
public final class ModuleHashesAttribute extends Attribute {
    public String algorithm;
    public List<String> modules;
    public List<byte[]> hashes;
    
    /**
     * Creates an attribute with a hashing algorithm, a list of module names,
     * and a list of the same length of hashes.
     * @param algorithm the hashing algorithm name.
     * @param modules a list of module name
     * @param hashes a list of hash, one for each module name.
     */
    public ModuleHashesAttribute(final String algorithm,
            final List<String> modules, final List<byte[]> hashes) {
        super("ModuleHashes");
        this.algorithm = algorithm;
        this.modules = modules;
        this.hashes = hashes;
    }
    
    /**
     * Creates an empty attribute that can be used as prototype
     * to be passed as argument of the method
     * {@link ClassReader#accept(ClassVisitor, Attribute[], int)}.
     */
    public ModuleHashesAttribute() {
        this(null, null, null);
    }
    
    @Override
    protected Attribute read(ClassReader cr, int off, int len, char[] buf,
            int codeOff, Label[] labels) {
        String hashAlgorithm = cr.readUTF8(off, buf); 
        
        int count = cr.readUnsignedShort(off + 2);
        ArrayList<String> modules = new ArrayList<String>(count);
        ArrayList<byte[]> hashes = new ArrayList<byte[]>(count);
        off += 4;
        
        for (int i = 0; i < count; i++) {
            String module = cr.readModule(off, buf);
            int hashLength = cr.readUnsignedShort(off + 2);
            off += 4;

            byte[] hash = new byte[hashLength];
            for (int j = 0; j < hashLength; j++) {
                hash[j] = (byte) (cr.readByte(off + j) & 0xff);
            }
            off += hashLength;

            modules.add(module);
            hashes.add(hash);
        }
        return new ModuleHashesAttribute(hashAlgorithm, modules, hashes);
    }
    
    @Override
    protected ByteVector write(ClassWriter cw, byte[] code, int len,
            int maxStack, int maxLocals) {
        ByteVector v = new ByteVector();
        int index = cw.newUTF8(algorithm);
        v.putShort(index);

        int count = (modules == null)? 0: modules.size();
        v.putShort(count);
        
        for(int i = 0; i < count; i++) {
            String module = modules.get(i);
            v.putShort(cw.newModule(module));
            
            byte[] hash = hashes.get(i);
            v.putShort(hash.length);
            for(byte b: hash) {
                v.putByte(b);
            }
        }
        return v;
    }
}
