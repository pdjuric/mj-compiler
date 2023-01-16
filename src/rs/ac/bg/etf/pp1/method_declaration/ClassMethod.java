package rs.ac.bg.etf.pp1.method_declaration;

import rs.ac.bg.etf.pp1.extended.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

public class ClassMethod extends Method {

    /**
     * Creates a new method with the given name and return type.
     * @param obj the method object
     * @param classType the type of the class that contains this method
     */
    protected ClassMethod(Obj obj, Struct classType) {
        super(obj);
        Obj thisObj = Tab.insert(Obj.Var, "this", classType);
        super.addPar(thisObj);
        thisObj.setLevel(2);
    }
}