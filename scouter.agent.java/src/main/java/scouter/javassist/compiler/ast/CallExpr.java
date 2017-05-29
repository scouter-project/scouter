/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999- Shigeru Chiba. All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License.  Alternatively, the contents of this file may be used under
 * the terms of the GNU Lesser General Public License Version 2.1 or later,
 * or the Apache License Version 2.0.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 */

package scouter.javassist.compiler.ast;

import scouter.javassist.compiler.CompileError;
import scouter.javassist.compiler.MemberResolver;
import scouter.javassist.compiler.TokenId;
import scouter.javassist.compiler.ast.ASTList;
import scouter.javassist.compiler.ast.ASTree;
import scouter.javassist.compiler.ast.Expr;
import scouter.javassist.compiler.ast.Visitor;

/**
 * Method call expression.
 */
public class CallExpr extends Expr {
    private MemberResolver.Method method;  // cached result of lookupMethod()

    private CallExpr(ASTree _head, ASTList _tail) {
        super(TokenId.CALL, _head, _tail);
        method = null;
    }

    public void setMethod(MemberResolver.Method m) {
        method = m;
    }

    public MemberResolver.Method getMethod() {
        return method;
    }

    public static CallExpr makeCall(ASTree target, ASTree args) {
        return new CallExpr(target, new ASTList(args));
    }

    public void accept(Visitor v) throws CompileError { v.atCallExpr(this); }
}
