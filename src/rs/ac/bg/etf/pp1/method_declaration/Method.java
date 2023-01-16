package rs.ac.bg.etf.pp1.method_declaration;

import rs.ac.bg.etf.pp1.extended.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.ac.bg.etf.pp1.error_reporting.Status;

public class Method {

    protected Obj obj;

    protected void setObj(Obj obj) {
        this.obj = obj;
    }

    public Obj getObj() { return obj;}


    protected Method(Obj obj) {
        this.obj = obj;
        obj.setAdr(-1);
        Tab.openScope();
    }
    public Status endMethodDeclaration() {
        Tab.chainLocalSymbols(obj);
        Tab.closeScope();
        return Status.Ok();
    }

    private int formParCnt = 0;
    public Status addPar(Obj par) {
        formParCnt++;
        par.setFpPos(1);
        // par is already added to the symbol table, in VarIdent
        return Status.Ok();
    }
    public Status endPar() {
        obj.setLevel(formParCnt);
        return Status.Ok();
    }

}