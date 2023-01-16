package rs.ac.bg.etf.pp1.function_call;

import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;
import rs.ac.bg.etf.pp1.error_reporting.Status;

public abstract class FunctionCallHelper {

    public abstract Status addArg(Struct arg);
    public abstract Status argEnd();

    public abstract Obj getMethod();

}
