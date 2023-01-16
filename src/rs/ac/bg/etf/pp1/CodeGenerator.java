package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.*;
import rs.ac.bg.etf.pp1.extended.Helper;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;
import rs.ac.bg.etf.pp1.extended.Tab;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.*;


import static rs.ac.bg.etf.pp1.extended.Tab.findClassName;
import static rs.etf.pp1.mj.runtime.Code.*;
import static rs.ac.bg.etf.pp1.designator.DesignatorAction.*;
import static rs.ac.bg.etf.pp1.extended.Tab.getClassObjs;
import static rs.ac.bg.etf.pp1.extended.Helper.*;


interface Deferrable {
    void exec();
}

public class CodeGenerator extends VisitorAdaptor {

    enum RuntimeError {
        ArrayStatementDimension(1, "Source array has less elements than target array."),
        ReturnFromMethodMissing(2, "Method didn't return a value."),
        NewArrayDimensions(3, "Array has non-positive number of elements.");


        private final String msg;
        private final int code;
        RuntimeError(int code, String msg){
            this.code = code; this.msg = msg;
        }

        /**
         * Puts a trap instruction.
         */
        void put() {
           // putPrintString(msg);
            Code.put(trap);
            Code.put(code);
        }

        /**
         * Puts a trap instruction that will be executed if the condition is false.
         * @param relop case in which the error needs to be raised
         */
        void putIfFalse(int relop) {
            int patchAddr = putFalseUnknownJump(relop);
            this.put();
            fixup(patchAddr);
        }

    }



    private void generateOrdChr() {
        Obj ordObj = Tab.find("ord");
        Obj chrObj = Tab.find("chr");
        ordObj.setAdr(Code.pc);
        chrObj.setAdr(Code.pc);
        // todo moze i bez generisanja
        putEnter(1, 1);
        put(load_0);
        putExit();
    }

    private static int putPrintString(String s) {
        StringCharacterIterator iter = new StringCharacterIterator(s);
        int start = Code.pc;
        for (char c = iter.last(); c != CharacterIterator.DONE; c = iter.previous()) {
            loadConst(c);
            loadConst(1);
            put(bprint);
        }
        return Code.pc - start;
    }

    private int boolPrintAddr, boolReadAddr;

    private void generateBoolPrint(){
        boolPrintAddr = Code.pc;

        putEnter(1, 1);
        put(load_0);
        loadConst(0);
        int patchAdr = putFalseUnknownJump(Code.ne);
        putPrintString("true");
        putExit();
        fixup(patchAdr);
        putPrintString("false");
        putExit();
    }

    private final HashMap<String, List<Integer>> waitingForTVFAddr = new HashMap<>();

    private void addTVF(String className) {
        // todo: check whether TVF is already generated
        List<Integer> list = waitingForTVFAddr.getOrDefault(className, new LinkedList<>());
        list.add(Code.pc);
        waitingForTVFAddr.put(className, list);

        put4(0);
    }

    private void fixupTVF(String className, int TVFAddr) {
        if (waitingForTVFAddr.containsKey(className))
            for (Integer adr: waitingForTVFAddr.get(className)) {
                Code.pc = adr;
                put4(TVFAddr);
            }
    }

    /**
     * Generates TVF for all classes
     */
    private void generateTVFs() {
        // allocate two temp vars for estackFlip
        staticDataTop += 2;

        for (Obj clazz: getClassObjs()) {
            // get current class name
            String className = clazz.getName();
            System.out.print("Class: " +  className + " ");

            Obj tvf = getTVF(clazz);
            if (tvf == null) throw new RuntimeException("TVF not found");

            // set TVF address
            tvf.setAdr(staticDataTop);
            System.out.println("(TVF addr: " + tvf.getAdr() + "):");

            // fixup calls to members of the current class that were made before the TVF was generated
            int oldPC = Code.pc;
            fixupTVF(className, tvf.getAdr());
            Code.pc = oldPC;

            // generate TVF
            for (Obj method: getClassMethods(clazz)) {
                // set method name
                System.out.print("\t" + method.getName());
                for (char c: method.getName().toCharArray()){
                    loadConst(c);
                    storeStatic(staticDataTop++);
                }

                // set method name terminator
                loadConst(-1);
                storeStatic(staticDataTop++);

                // set method address
                int addr = findMethodAddr(method);
                System.out.println(addr);
                loadConst(addr);
                storeStatic(staticDataTop++);
            }

            // set class TVF terminator
            loadConst(-2);
            storeStatic(staticDataTop++);
        }
    }

    // size of static data segment in bytes
    public int staticDataTop = 0;

    //region Program

    @Override
    public void visit(Program program) {
        dataSize = staticDataTop;
    }

    //endregion

    //region Variable Declaration

    @Override
    public void visit(GlobalObjectIdentificator globalObjectIdentificator) {
        staticDataTop++;
    }

    @Override
    public void visit(GlobalArrayIdentificator globalArrayIdentificator) {
        staticDataTop++;
    }

    //endregion

    //region Constructor Declaration

    /**
     * True if the current method is a constructor.
     * Used to determine whether to load the object address on the stack when returning.
     */
    private boolean inConstructorDefinition = false;

    @Override
    public void visit(ConstructorDeclaration constructorDeclaration) {
        inConstructorDefinition = false;
        if (!returned()) {
            put(load_0);
            putExit();
        }
    }

    @Override
    public void visit(ConstructorIdent constructorIdent) {
        Obj ctor = constructorIdent.obj;
        int fieldCnt = ctor.getType().getNumberOfFields();
        int parCnt = ctor.getLevel();
        int varCnt = ctor.getLocalSymbols().size();
        String className = findClassName(ctor.getType());

        ctor.setAdr(Code.pc);
        inConstructorDefinition = true;

        putEnter(parCnt, varCnt);

        // create object
        put(new_);
        put2(fieldCnt * 4);
        // stack: [objAddr]

        put(dup);
        // stack: [objAddr, objAddr]

        // store the address of the object in the first local variable (this)
        put(store_0);
        // stack: [objAddr]

        // store the address of the TVF in the first field of the object
        put(const_);
        addTVF(className);
        putField(0);
    }

    //endregion

    //region Method Declaration

    @Override
    public void visit(MethodDeclaration methodDeclaration) {
        // return statement is not needed in constructor and void methods
        Obj methodObj = methodDeclaration.getMethodTypeIdent().obj;
        if (!returned()) {
            if (isConstructor(methodObj)) {
                // constructor returns pointer to the created object
                put(load_0);
                putExit();
            } else if (returnsVoid(methodObj))
                putExit();
            else
                RuntimeError.ReturnFromMethodMissing.put();
        }
    }

    @Override
    public void visit(MethodTypeAndIdentifier methodTypeAndIdentifier) {
        Obj method = methodTypeAndIdentifier.obj;
        int parCnt = method.getLevel();
        int varCnt = method.getLocalSymbols().size();

        if (isMain(method)) {
            // predefined functions are generated before start of the main method
            generateOrdChr();
            generateBoolPrint();
            mainPc = Code.pc;
            generateTVFs();
        }

        method.setAdr(Code.pc);
        putEnter(parCnt, varCnt);
    }

    //endregion


    //region Function Call & Actual Parameters

    Stack<Boolean> callingClassMethod = new Stack<>();

    @Override
    public void visit(FunctionCall functionCall) {
        // estack: [this, args, this]
        // estack: [args]
        deferred.pop().exec();
    }

    @Override
    public void visit(ActPar actPar) {
        if (callingClassMethod.peek()) {
            // estack: [this, arg0, ..., this, argCurr]
            estackFlip();
            // estack: [this, arg0, ..., argCurr, this]
        }
    }

    //endregion


    //region Condition, CondTerm & CondFact
    Stack<Queue<Integer>> thenStack = new Stack<>();
    Queue<Integer> thenQueue;

    Stack<Queue<Integer>> elseFiStack = new Stack<>();
    Queue<Integer> elseFiQueue;

    Stack<Queue<Integer>> orElseFiStack = new Stack<>();
    Queue<Integer> orElseFiQueue;

    private void openJumpScope() {
        thenStack.push(thenQueue);
        elseFiStack.push(elseFiQueue);
        orElseFiStack.push(orElseFiQueue);
        thenQueue = new LinkedList<>();
        elseFiQueue = new LinkedList<>();
        orElseFiQueue = new LinkedList<>();
    }

    private void closeJumpScope() {
        thenQueue = thenStack.pop();
        elseFiQueue = elseFiStack.pop();
        orElseFiQueue = orElseFiStack.pop();
    }

    @Override
    public void visit(FirstAndCondition firstAndCondition) {
        int patchAdr = putFalseUnknownJump(currentRelop);
        orElseFiQueue.add(patchAdr);
    }

    @Override
    public void visit(AddAndCondition addAndCondition) {
        int patchAdr = putFalseUnknownJump(currentRelop);
        orElseFiQueue.add(patchAdr);
    }

    @Override
    public void visit(LastAndCondition lastAndCondition) {
        int patchAdr = putFalseUnknownJump(currentRelop);
        orElseFiQueue.add(patchAdr);
    }

    @Override
    public void visit(FirstOrCondition firstOrCondition) {
        boolean multipleAnds = firstOrCondition.getCondTerm() instanceof LastAndCondition;
        int patchAdr;

        if (multipleAnds) patchAdr = putUnknownJump();
        else patchAdr = putFalseUnknownJump(Code.inverse[currentRelop]);

        thenQueue.add(patchAdr);
        fixupAll(orElseFiQueue);
    }

    @Override
    public void visit(AddOrCondition addOrCondition) {
        boolean multipleAnds = addOrCondition.getCondTerm() instanceof LastAndCondition;
        int patchAdr;

        if (multipleAnds) patchAdr = putUnknownJump();
        else patchAdr = putFalseUnknownJump(Code.inverse[currentRelop]);

        thenQueue.add(patchAdr);
        fixupAll(orElseFiQueue);
    }

    @Override
    public void visit(LastOrCondition lastOrCondition) {
        boolean multipleAnds = lastOrCondition.getCondTerm() instanceof LastAndCondition;

        if (!multipleAnds) {
            int patchAdr = putFalseUnknownJump(currentRelop);
            elseFiQueue.add(patchAdr);
        }
    }

    @Override
    public void visit(SingleOrCondition singleOrCondition) {
        boolean multipleAnds = singleOrCondition.getCondTerm() instanceof LastAndCondition;

        if (!multipleAnds) {
            int patchAdr = putFalseUnknownJump(currentRelop);
            elseFiQueue.add(patchAdr);
        }
    }

    @Override
    public void visit(OkBoolCondition okBoolCondition) {
        for (Integer adr: thenQueue) {
            // if the jump is to the next instruction, remove it
            if (Code.pc - 2 == adr) Code.pc -= 3;
            else fixup(adr);
        }
        thenQueue.clear();
    }

    @Override
    public void visit(OnlyExpr onlyExpr) {
        loadConst(0);
        currentRelop = Code.ne;
    }

    //endregion

    //region Expression

    @Override
    public void visit(AddTerm expr) {
        deferred.pop().exec(); // executes operation
    }

    @Override
    public void visit(FirstNegAddTerm expr) {
        // estack: [ term ]
        // estack: [ -term ]
        loadConst(-1);
        put(Code.mul);
    }

    //endregion

    //region Term

    @Override
    public void visit(MullFactor mullFactor) {
        deferred.pop().exec(); // executes operation
    }

    //endregion

    //region Factor

    @Override
    public void visit(NumberFactor numberFactor) {
        // todo odakle dohvatiti vrednost
        loadConst(numberFactor.getValue());
    }

    @Override
    public void visit(CharFactor charFactor) {
        // todo width ?
        loadConst(charFactor.getValue());
    }

    @Override
    public void visit(BooleanFactor booleanFactor) {
        // todo ?
        loadConst(booleanFactor.getValue() ? 1 : 0);
    }

    @Override
    public void visit(NewObjectFactor newObjectFactor) {
        // estack: [ args ]
        int adr = newObjectFactor.getActPars().obj.getAdr();

        callingClassMethod.pop();
        putCall(adr);
    }

    @Override
    public void visit(NewObjectType newObjectType) {
        // estack: []
        loadConst(0);
        callingClassMethod.push(false);
    }

    @Override
    public void visit(NewArrayFactor newArrayFactor) {
        // estack: [ n ]
        Struct elementType = newArrayFactor.getType().struct;

        put(dup);
        loadConst(0);
        RuntimeError.NewArrayDimensions.putIfFalse(Code.le);
        put(newarray);
        put(elementType == Tab.boolType ? 0 : 1);
    }

    //endregion

    //region Designator
    
    // some operations need to be deferred until the needed value is on the expression stack
    Stack<Deferrable> deferred = new Stack<>();

    @Override
    public void visit(MemberAccessDesignator memberAccessDesignator) {
        int idx = memberAccessDesignator.obj.getAdr();
        String name = memberAccessDesignator.obj.getName();

        switch (getAction(memberAccessDesignator)) {
            case GET:
                // estack: [objAddr]
                getField(idx);
                break;

            case DEFERRED_SET:
                deferred.push(() -> {
                    // estack: [objAddr, val]
                    putField(idx);
                });
                break;

            case CALL:
                // estack: [objAddr]
                
                // duplicate 'this' for the virtual method call
                put(dup);
                
                callingClassMethod.push(true);

                deferred.push(() -> {
                    // estack: [objAddr, ..., args, objAddr]
                    callingClassMethod.pop();

                    getField(0);
                    put(invokevirtual);
                    for (char c: name.toCharArray())
                        put4(c);
                    put4(-1);
                });

        }

    }

    @Override
    public void visit(IndexingDesignator indexingDesignator) {
        Obj obj = indexingDesignator.obj;

        switch (getAction(indexingDesignator)) {
            case GET:
                // estack: [arrAddr, idx]
                loadArray(obj);
                break;
            case DEFERRED_SET:
                deferred.push(() -> {
                    // estack: [arrAddr, idx, val]
                    storeArray(obj);
                });
                break;
            case CALL:
                throw new RuntimeException();
        }

    }


    @Override
    public void visit(IdentifierDesignator identifierDesignator) {
        Obj obj = identifierDesignator.obj;
        boolean isLenFunction = obj == Tab.lenObj;
        int adr = obj.getAdr();
        int kind = obj.getKind();

        if (kind == Obj.Fld || isClassMethod(obj)) {
            // implicit 'this', transform to MemberAccessDesignator
            put(load_0);
            MemberAccessDesignator newNode = new MemberAccessDesignator(null, null);
            newNode.obj = obj;
            newNode.setParent(identifierDesignator.getParent());
            visit(newNode);
            return;
        }

        switch (getAction(identifierDesignator)) {
            case GET:
                // estack: []
                load(obj);
                break;
            case DEFERRED_SET:
                deferred.push(() -> {
                    // estack: [val]
                    store(obj);
                });
                break;
            case CALL:
                callingClassMethod.push(false);
                deferred.push(() -> {
                    // estack: [args]
                    callingClassMethod.pop();

                    if (isLenFunction) put(arraylength);
                    else putCall(adr);
                });

        }
    }

    //endregion

    //region Statement

    Stack<List<Integer>> unresolvedBreaks = new Stack<>();
    Stack<List<Integer>> unresolvedContinues = new Stack<>();

    @Override
    public void visit(IfCondStart ifCondStart) {
        openJumpScope();
    }

    @Override
    public void visit(IfStmt ifStmt) {
        fixupAll(elseFiQueue);
        fixupAll(orElseFiQueue);
        closeJumpScope();
    }

    @Override
    public void visit(IfElseStmt ifElseStmt) {
        fixupAll(elseFiQueue);
        fixupAll(orElseFiQueue);
        closeJumpScope();
    }

    @Override
    public void visit(WhileCondStart whileCondStart) {
        openJumpScope();
        loopStart.push(Code.pc);
        unresolvedBreaks.push(new LinkedList<>());
        unresolvedContinues.push(new LinkedList<>());
    }

    @Override
    public void visit(WhileStmt whileStmt) {
        int whileStart = loopStart.pop();

        // add jmp to the start of the loop
        putJump(whileStart);

        // resolve continue jumps
        fixupAll(unresolvedContinues.pop(), whileStart);

        // resolve break jumps
        fixupAll(unresolvedBreaks.pop());
        fixupAll(elseFiQueue);
        fixupAll(orElseFiQueue);
        
        closeJumpScope();
    }

    @Override
    public void visit(ElseStart elseStart) {
        // at the end of then block, put jmp to fi
        int patchAddr = putUnknownJump();

        fixupAll(elseFiQueue);
        fixupAll(orElseFiQueue);

        elseFiQueue.add(patchAddr);
    }

    @Override
    public void visit(BreakStmt breakStmt) {
        int patchAddr = putUnknownJump();
        unresolvedBreaks.peek().add(patchAddr);
    }

    @Override
    public void visit(ContinueStmt continueStmt) {
        int patchAddr = putUnknownJump();
        unresolvedContinues.peek().add(patchAddr);
    }

    @Override
    public void visit(ReturnStmt returnStmt) {
        Helper.putMultiple(pop, 2 * forEachLoopEnd.size());

        if (inConstructorDefinition) put(load_0);
        putExit();
    }

    @Override
    public void visit(ReturnExprStmt returnExprStmt) {
        // in foreach loop, every block keeps 2 stack locations
        for (int i = 0; i < forEachLoopEnd.size() * 2; i++) {
            estackFlip();
            put(pop);
        }

        putExit();
    }

    @Override
    public void visit(ReadStmt readStmt) {
        // estack: [objAddr]  ||  [arrAddr]  ||  []
        Obj designator = readStmt.getDesignator().obj;

        putRead(designator);
        deferred.pop().exec();
    }


    @Override
    public void visit(PrintStmt printStmt) {
        // estack: [val]
        Struct designatorType = printStmt.getExpr().struct;

        if (designatorType == Tab.charType) put(bprint);
        else if (designatorType == Tab.intType) put(print);
        else putCall(boolPrintAddr);
    }

    @Override
    public void visit(PrintStatementWidthSpecified printStatementWidthSpecified) {
        Code.loadConst(printStatementWidthSpecified.getN1());
    }

    @Override
    public void visit(PrintStatementNoWidth printStatementNoWidth) {
        Code.loadConst(5);
    }

    Stack<Integer> loopStart = new Stack<>();
    Stack<Integer> forEachLoopEnd = new Stack<>();

    @Override
    public void visit(ForEachStmt forEachStmt) {
            fixupAll(unresolvedContinues.pop());
        put(const_1);
        put(add);
        putJump(loopStart.pop());
            fixup(forEachLoopEnd.pop());
        putMultiple(pop, 3);
            fixupAll(unresolvedBreaks.pop());
        putMultiple(pop, 2);
    }

    @Override
    public void visit(ForEachStmtDesignator forEachStmtDesignator) {
        // estack: [arrAdr]
        Struct array = forEachStmtDesignator.obj.getType();
        int patchAddr;

        loadConst(0);
            loopStart.push(Code.pc);
        putMultiple(dup2, 3);
        put(pop);
        put(arraylength);
        patchAddr = putFalseUnknownJump(Code.lt);
            forEachLoopEnd.push(patchAddr);
        put(pop);
        loadArray(array);
    }

    @Override
    public void visit(ForEachStmtIdent forEachStmtIdent) {
        // estack: [arrAdr, currIdx, currVal ]
        Obj forEachIteratingVar = forEachStmtIdent.obj;

        store(forEachIteratingVar);
        unresolvedBreaks.push(new LinkedList<>());
        unresolvedContinues.push(new LinkedList<>());
    }

    //endregion

    //region DesignatorStatement

    @Override
    public void visit(Assignment assignment) {
        deferred.pop().exec();
    }

    @Override
    public void visit(FunctionCallStatement functionCallStatement) {
        if (functionCallStatement.getFuncCall().struct != Tab.noType)
            put(pop);
    }

    @Override
    public void visit(Increment increment) {
        incDec(increment.getDesignator(), 1);
    }

    @Override
    public void visit(Decrement decrement) {
        incDec(decrement.getDesignator(), -1);
    }

    private void incDec(Designator d, int diff){

        if (d instanceof IdentifierDesignator && isLocalVar(d.obj)) {
            put(inc);
            put(d.obj.getAdr());
            put(diff);
            deferred.pop();
            return;
        }

        // get the value
        if (d instanceof IdentifierDesignator) {
            //estack: []
            load(d.obj);

        } else if (d instanceof IndexingDesignator) {
            // estack: [arrAddr, idx]
            put(dup2);
            loadArray(d.obj);

        } else {
            // estack: [objAddr]
            put(dup);
            getField(d.obj.getAdr());
        }

        // increment/decrement
        loadConst(diff);
        put(add);

        // store the value back int the designator
        deferred.pop().exec();
    }

    private static final boolean SKIP = true;
    private static final boolean DO_NOT_SKIP = false;

    Stack<Boolean> arrayAssignmentStack = new Stack<>();

    @Override
    public void visit(ArrayAssignmentStatement arrayAssignmentStatement) {
        Struct arrayStruct = arrayAssignmentStatement.getDesignator().obj.getType();

        // todo: instead of using __temp0 to store the array address, it can be duped everytime it is needed,
        //  and then popped after the assignment is done
        put(dup);
        store(Tab.__temp0);
        
        // check array bounds
        put(arraylength);
        loadConst(arrayAssignmentStack.size());
        RuntimeError.ArrayStatementDimension.putIfFalse(Code.lt);

        while (!arrayAssignmentStack.isEmpty()) {
            boolean action = arrayAssignmentStack.pop();
            if (action == SKIP) continue;

            load(Tab.__temp0);
            loadConst(arrayAssignmentStack.size());
            loadArray(arrayStruct);
            deferred.pop().exec();
        }

    }

    @Override
    public void visit(NullDesignator nullDesignator) {
        arrayAssignmentStack.push(SKIP);
    }

    @Override
    public void visit(NotNullDesignator notNullDesignator) {
        arrayAssignmentStack.push(DO_NOT_SKIP);
    }

    //endregion

    //region Relop

    private int currentRelop;

    @Override
    public void visit(Equal equal) {
        currentRelop = Code.eq;
    }

    @Override
    public void visit(NotEqual notEqual) {
        currentRelop = Code.ne;
    }

    @Override
    public void visit(Greater greater) {
        currentRelop = Code.gt;
    }

    @Override
    public void visit(GreaterOrEqual greaterOrEqual) {
        currentRelop = Code.ge;
    }

    @Override
    public void visit(Less less) {
        currentRelop = Code.lt;
    }

    @Override
    public void visit(LessOrEqual lessOrEqual) {
        currentRelop = Code.le;
    }

    //endregion

    //region Addop, Mullop

    @Override
    public void visit(Addition addition) {
        deferred.push(() -> Code.put(Code.add));
    }

    @Override
    public void visit(Subtraction subtraction) {
        deferred.push(() -> Code.put(Code.sub));
    }

    @Override
    public void visit(Multiplication multiplication) {
        deferred.push(() -> Code.put(Code.mul));
    }

    @Override
    public void visit(Division division) {
        deferred.push(() -> Code.put(Code.div));
    }

    @Override
    public void visit(Modulo modulo) {
        deferred.push(() -> Code.put(Code.rem));
    }

    //endregion
}


