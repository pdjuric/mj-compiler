package rs.ac.bg.etf.pp1.function_call;

import rs.ac.bg.etf.pp1.error_reporting.Status;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

import static rs.ac.bg.etf.pp1.error_reporting.Status.*;

public class ErrFunctionCall extends FunctionCallHelper {

    public ErrFunctionCall() {}

    @Override
    public Status addArg(Struct arg) {
        return Ok();
    }

    @Override
    public Status argEnd() {
        return Ok();
    }

    @Override
    public Obj getMethod() {
        return null;
    }
}
