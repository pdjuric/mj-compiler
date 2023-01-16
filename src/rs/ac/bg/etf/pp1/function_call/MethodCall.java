package rs.ac.bg.etf.pp1.function_call;

import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;
import rs.ac.bg.etf.pp1.error_reporting.Status;

import java.util.Iterator;

import static rs.ac.bg.etf.pp1.error_reporting.Status.*;
import static rs.ac.bg.etf.pp1.extended.Helper.*;

public class MethodCall extends FunctionCallHelper {

    private Obj method;
    private Iterator<Obj> iter;
    private int parCnt;
    private int currParIdx = 0;
    private boolean errOccurred = false;

    public MethodCall(Obj method) {
        super();
        this.method = method;
        this.iter = method.getLocalSymbols().iterator();
        if (isClassMethod(method)){
            currParIdx++;
            iter.next();
        }
        this.parCnt = method.getLevel();

    }

    /**
     * Adds argument to the function call.
     * Checks if the argument is assignable to the parameter.
     * @return {@link Status#ArgumentTypeMismatch(int)} if the argument is not assignable to the parameter
     *       {@link Status#Ok()} otherwise
     */
    public Status addArg(Struct argType) {
        if (currParIdx++ < parCnt) {
            Struct parType = iter.next().getType();
            if (!isAssignableTo(argType, parType)){
                errOccurred = true;
                return ArgumentTypeMismatch(currParIdx - 1);
            }

        }
        return Ok();
    }

    /**
     * Checks if the number of arguments is correct.
     * @return {@link Status#TooManyFunctionArguments()} if there are more arguments than parameters
     *         {@link Status#FunctionArgumentsMissing(int)} if there are less arguments than parameters
     *         {@link Status#Ok()} otherwise
     */
    public Status argEnd() {
        if (currParIdx == parCnt || errOccurred) return Ok();
        else if (currParIdx > parCnt) return TooManyFunctionArguments();
        else return FunctionArgumentsMissing(parCnt - currParIdx);
    }

    public Obj getMethod() {
        return method;
    }

}
