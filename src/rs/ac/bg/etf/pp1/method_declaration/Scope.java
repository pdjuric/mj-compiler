package rs.ac.bg.etf.pp1.method_declaration;

import rs.ac.bg.etf.pp1.ast.SyntaxNode;
import rs.etf.pp1.symboltable.concepts.Struct;

public abstract class Scope {

    public static Scope currScope;
    public abstract Method newMethod(String methodName, Struct returnType, SyntaxNode node);

}
