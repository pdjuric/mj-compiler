package rs.ac.bg.etf.pp1.method_declaration;

import rs.ac.bg.etf.pp1.ast.SyntaxNode;
import rs.ac.bg.etf.pp1.extended.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

import static rs.ac.bg.etf.pp1.extended.Helper.*;

public class GlobalScope extends Scope {

    public Method newMethod(String methodName, Struct returnType, SyntaxNode node) {
        Obj method;
        if (!isDeclaredInCurrentScope(methodName, node)) {
            method = Tab.insert(Obj.Meth, methodName, returnType);
        } else {
            method = new Obj(Obj.Meth, methodName, returnType);
        }
        return new Method(method);
    }

}
