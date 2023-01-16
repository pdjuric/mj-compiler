package rs.ac.bg.etf.pp1.extended;

import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

import static rs.ac.bg.etf.pp1.extended.Helper.*;

public class DumpSymbolTableVisitor extends rs.etf.pp1.symboltable.visitors.DumpSymbolTableVisitor {
    @Override
    public void visitObjNode(Obj objToVisit) {
        //output.append("[");
        if (objToVisit == null) {
            output.append("null");
            return;
        }
        switch (objToVisit.getKind()) {
            case Obj.Con:  output.append("Con "); break;
            case Obj.Var:  output.append("Var "); break;
            case Obj.Type: output.append("Type "); break;
            case Obj.Meth: output.append("Meth "); break;
            case Obj.Fld:  output.append("Fld "); break;
            case Obj.Prog: output.append("Prog "); break;
        }

        output.append(objToVisit.getName());
        output.append(": ");

        if (isThis(objToVisit) || isConstructor(objToVisit))
            output.append("");
        else if (objToVisit.getKind() != Obj.Type && objToVisit.getType().getKind() == Struct.Class) {
            if (objToVisit.getType() != Tab.noType && objToVisit.getType() != Tab.nullType)
                output.append("Class " + Tab.findClassName(objToVisit.getType()));
        }
        else
            objToVisit.getType().accept(this);

        output.append(", ");
        output.append(objToVisit.getAdr());
        output.append(", ");
        output.append(objToVisit.getLevel() + " ");

        if (objToVisit.getKind() == Obj.Prog || objToVisit.getKind() == Obj.Meth) {
            output.append("\n");
            nextIndentationLevel();
        }


        for (Obj o : objToVisit.getLocalSymbols()) {
            output.append(currentIndent.toString());
            o.accept(this);
            output.append("\n");
        }

        if (objToVisit.getKind() == Obj.Prog || objToVisit.getKind() == Obj.Meth)
            previousIndentationLevel();

    }

    public String toString(Obj obj){
        output = new StringBuilder();
        visitObjNode(obj);
        return output.toString();
    }

}
