package rs.ac.bg.etf.pp1.method_declaration;


import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;
import rs.ac.bg.etf.pp1.error_reporting.Status;

import java.util.Iterator;

import static rs.ac.bg.etf.pp1.SemanticAnalyzer.currClass;
import static rs.ac.bg.etf.pp1.error_reporting.Status.*;
import static rs.ac.bg.etf.pp1.extended.Helper.getInheritanceLevel;

public class OverrideClassMethod extends ClassMethod {

    private final Obj inheritedMethod;
    private boolean errorOccurred = false;
    private final Iterator<Obj> parIt;
    private int leftParsCnt;

    public OverrideClassMethod(Obj method, Obj inheritedMethod, Struct classType) {
        super(method, classType);
        this.inheritedMethod = inheritedMethod;
        parIt = inheritedMethod.getLocalSymbols().iterator();
        // skip 'this'
        parIt.next();
        leftParsCnt = inheritedMethod.getLevel() - 1;
    }

    public Status endMethodDeclaration() {
        if (!errorOccurred) {
            currClass.inheritedMethods.remove(inheritedMethod);
            currClass.alreadyOverriddenMethods.add(obj);
            setObj(inheritedMethod);
        }

        return super.endMethodDeclaration();
    }

    public Status addPar(Obj par) {
        super.addPar(par);

        if (!parIt.hasNext() || leftParsCnt == 0) {
            errorOccurred = true;
            return OverrideExcessParameter(par.getName(), obj.getName());
        }
        leftParsCnt--;

        Obj oldPar = parIt.next();
        if (!par.getType().equals(oldPar.getType())) {
            errorOccurred = true;
            return OverrideParameterTypeMismatch(par.getName(), obj.getName());
        }

        return Ok();
    }

    public Status endPar(){
        super.endPar();

        if (parIt.hasNext() && leftParsCnt > 0 && !errorOccurred) {
            errorOccurred = true;
            return OverrideParametersMissing(obj.getName());
        }

        return Ok();
    }

}
