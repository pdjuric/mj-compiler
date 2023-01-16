package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.*;
import rs.ac.bg.etf.pp1.error_reporting.Status;
import rs.ac.bg.etf.pp1.function_call.*;
import rs.ac.bg.etf.pp1.method_declaration.ClassScope;
import rs.ac.bg.etf.pp1.method_declaration.GlobalScope;
import rs.ac.bg.etf.pp1.method_declaration.Method;
import rs.ac.bg.etf.pp1.extended.Tab;
import rs.etf.pp1.symboltable.concepts.*;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import static rs.ac.bg.etf.pp1.error_reporting.Status.*;
import static rs.ac.bg.etf.pp1.extended.Helper.*;
import static rs.ac.bg.etf.pp1.method_declaration.Scope.currScope;

public class SemanticAnalyzer extends VisitorAdaptor {

    static {
        currScope = new GlobalScope();
    }

    //region Program

    @Override
    public void visit(Program program) {
        Tab.__temp0 = new Obj(Obj.Var, "__temp0", Tab.intType, 0, 0);
        Tab.__temp1 = new Obj(Obj.Var, "__temp1", Tab.intType, 1, 0);
        Tab.currentScope.addToLocals(Tab.__temp0);
        Tab.currentScope.addToLocals(Tab.__temp1);
        Tab.chainLocalSymbols(program.getProgName().obj);
        Tab.closeScope();
        if (!mainDefined) MainNotDefined().report(program);
    }

    @Override
    public void visit(ProgramName programName) {
        programName.obj = Tab.insert(Obj.Prog, programName.getProgramName(), Tab.noType, 0, 0);
        Tab.openScope();
    }

    //endregion

    //region Variable Declaration

    private Struct currVarType = null;

    @Override
    public void visit(VarType varType) {
        currVarType = varType.getType().struct;
    }

    @Override
    public void visit(LastVarIdentificator lastVarIdentificator) {
        currVarType = null;
    }

    private Obj addNewVar(String name, Struct type, int kind, SyntaxNode node) {
        Obj newObj;
        if (!nameEqualsThis(name, node) && !isDeclaredInCurrentScope(name, node)) {
            newObj = Tab.insert(kind, name, type);
            newObj.setAdr(Tab.currentScope.getnVars() - 1);
        } else {
            newObj = new Obj(kind, name, type);
            newObj.setAdr(-1);
        }

        newObj.setLevel(currClass == null ? 1 : 2);
        return newObj;
    }

    private Obj addNewGlobalVar(String name, Struct type, SyntaxNode node) {
        Obj obj = addNewVar(name, type, Obj.Var, node);
        obj.setLevel(0);
        return obj;
    }

    @Override
    public void visit(ObjectIdentificator objectIdentificator) {
        String name = objectIdentificator.getVarName();
        Struct type = currVarType;

        objectIdentificator.obj = addNewVar(name, type, Obj.Var, objectIdentificator);
    }

    @Override
    public void visit(ArrayIdentificator arrayIdentificator) {
        String name = arrayIdentificator.getVarName();
        Struct type = new Struct(Struct.Array, currVarType);

        arrayIdentificator.obj = addNewVar(name, type, Obj.Var, arrayIdentificator);
    }

    @Override
    public void visit(GlobalObjectIdentificator globalObjectIdentificator) {
        String name = globalObjectIdentificator.getVarName();
        Struct type = currVarType;

        globalObjectIdentificator.obj = addNewGlobalVar(name, type, globalObjectIdentificator);
    }

    @Override
    public void visit(GlobalArrayIdentificator globalArrayIdentificator) {
        String name = globalArrayIdentificator.getVarName();
        Struct type = new Struct(Struct.Array, currVarType);

        globalArrayIdentificator.obj = addNewGlobalVar(name, type, globalArrayIdentificator);
    }

    @Override
    public void visit(Type type) {
        String name = type.getTypeName();
        Obj typeNode = Tab.find(name);
        Status status = Ok();

        type.struct = Tab.noType;

        if (typeNode == Tab.noObj)
            status = UndeclaredType(name);

        else if (typeNode.getKind() != Obj.Type)
            status = KindMismatch(name, "type");

        else
            type.struct = typeNode.getType();

        status.report(type);
    }

    //endregion

    //region Constant Declaration

    private Struct currConstType = null;

    @Override
    public void visit(ConstType constType) {
        currConstType = constType.getType().struct;
    }

    @Override
    public void visit(LastConstIdent lastConstIdent) {
        currConstType = null;
    }

    private Obj addNewConst(String name, String typeName, Struct typeStruct, SyntaxNode node) {
        if (isDeclaredInCurrentScope(name, node))
            return new Obj(Obj.Con, name, typeStruct);

        if (currConstType != typeStruct)
            if (checkSimpleType(currConstType, "Constant type", node))
                ConstantInitializerTypeMismatch(name, typeName).report(node);

        return Tab.insert(Obj.Con, name, typeStruct);
    }

    @Override
    public void visit(NumConstIdent numConstIdent) {
        String name = numConstIdent.getConstName();
        Integer value = numConstIdent.getConstNumVal();

        numConstIdent.obj = addNewConst(name, "int", Tab.intType, numConstIdent);
        numConstIdent.obj.setAdr(value);
    }

    @Override
    public void visit(CharConstIdent charConstIdent) {
        String name = charConstIdent.getConstName();
        char value = charConstIdent.getConstNumVal();

        charConstIdent.obj = addNewConst(name, "char", Tab.charType, charConstIdent);
        charConstIdent.obj.setAdr(value);
    }

    @Override
    public void visit(BoolConstIdent boolConstIdent) {
        String name = boolConstIdent.getConstName();
        Boolean value = boolConstIdent.getConstNumVal();

        boolConstIdent.obj = addNewConst(name, "bool", Tab.boolType, boolConstIdent);
        boolConstIdent.obj.setAdr(value ? 1 : 0);
    }

    //endregion

    //region Class Declaration

    public static ClassScope currClass = null;

    @Override
    public void visit(ClassIdentificator classIdentificator) {
        String name = classIdentificator.getClassName();
        Obj classObj;
        // check whether the class is already defined
        Struct classType = new Struct(Struct.Class);
        if (!isDeclaredInCurrentScope(name, classIdentificator)) {
            classObj = Tab.insert(Obj.Type, name, classType, 0, 0);
        } else {
            classObj = new Obj(Obj.Type, name, classType, 0, 0);
        }

        classIdentificator.obj = classObj;
        currClass = new ClassScope(classObj);
    }

    @Override
    public void visit(ClassDeclaration classDeclaration) {
        currClass.endClassDeclaration();
        currClass = null;
    }

    @Override
    public void visit(SubclassDecl subclassDecl) {
        String name = subclassDecl.getType().getTypeName();
        Obj superclass = Tab.find(name);
        subclassDecl.struct = Tab.noType;

        if (superclass == Tab.noObj)
            UndeclaredType(name).report(subclassDecl);

        else if (superclass.getType().getKind() != Struct.Class)
            KindMismatch(name, "class").report(subclassDecl);

        else if (superclass == currClass.classObj) {
            RecursiveSubclass(name).report(subclassDecl);
        } else {
            subclassDecl.struct = superclass.getType();
            currClass.addSuperclass(superclass.getType());
        }
    }

    //endregion

    //region Constructor Declaration

    @Override
    public void visit(ConstructorDeclaration constructorDeclaration) {
        currMethod.endMethodDeclaration();
        currMethod = null;
    }

    @Override
    public void visit(ConstructorIdent constructorIdent) {
        String name = constructorIdent.getCtorIdent();
        String className = currClass.getClassName();
        if (!name.equals(className))
            InvalidConstructorName(name, className).report(constructorIdent);

        currMethod = currClass.newConstructor();
        constructorIdent.obj = currMethod.getObj();
    }

    //endregion

    //region Class Field Declaration

    @Override
    public void visit(LastClassFieldIdentificator lastClassFieldIdentificator) {
        currVarType = null;
    }

    private Obj addClassField(String name, Struct type, SyntaxNode node) {
        Obj newObj = addNewVar(name, type, Obj.Fld, node);
        newObj.setLevel(1);
        return newObj;
    }

    @Override
    public void visit(ClassFieldObjectIdentificator classFieldObjectIdentificator) {
        String name = classFieldObjectIdentificator.getVarName();
        Struct type = currVarType;

        classFieldObjectIdentificator.obj = addClassField(name, type, classFieldObjectIdentificator);
    }

    @Override
    public void visit(ClassFieldArrayIdentificator classFieldArrayIdentificator) {
        String name = classFieldArrayIdentificator.getVarName();
        Struct type = new Struct(Struct.Array, currVarType);

        classFieldArrayIdentificator.obj = addClassField(name, type, classFieldArrayIdentificator);
    }

    @Override
    public void visit(ClassFieldsEnd classFieldsEnd) {
        currClass.endFieldDeclaration();
    }

    //endregion

    //region Method Declaration

    Method currMethod = null;
    private boolean mainDefined = false;

    @Override
    public void visit(MethodDeclaration methodDeclaration) {
        currMethod.endMethodDeclaration();

        if (isMain(currMethod.getObj())) mainDefined = true;
        currMethod = null;
    }

    @Override
    public void visit(MethodTypeAndIdentifier methodTypeAndIdentifier) {
        String name = methodTypeAndIdentifier.getMethodIdent();
        Struct returnType = methodTypeAndIdentifier.getMethodType().struct;

        currMethod = currScope.newMethod(name, returnType, methodTypeAndIdentifier);
        methodTypeAndIdentifier.obj = currMethod.getObj();
    }

    @Override
    public void visit(NonVoidMethodType nonVoidMethodType) {
        nonVoidMethodType.struct = nonVoidMethodType.getType().struct;
    }

    @Override
    public void visit(VoidMethodType voidMethodType) {
        voidMethodType.struct = Tab.noType;
    }


    //endregion

    //region Formal Parameters

    @Override
    public void visit(FormalParameterList formalParameterList) {
        SyntaxNode parent;
        if (formalParameterList.getParent() instanceof ConstructorDeclaration)
            parent = ((ConstructorDeclaration) formalParameterList.getParent()).getConstructorIdent();
        else
            parent = ((MethodDeclaration) formalParameterList.getParent()).getMethodTypeIdent();
        currMethod.endPar().report(parent);
    }

    @Override
    public void visit(NoFormalParameters noFormalParameters) {
        currMethod.endPar().report(noFormalParameters);
    }

    @Override
    public void visit(FormParOk formParOk) {
        currMethod.addPar(formParOk.getVarIdent().obj).report(formParOk);
        currVarType = null;
    }

    //endregion

    //region Function Call & Actual Parameters
    private final Stack<FunctionCallHelper> methodCallStack = new Stack<>();
    private FunctionCallHelper currMethodCall = null;


    @Override
    public void visit(FunctionCall functionCall) {
        functionCall.struct = functionCall.getCallingFuncDesignator().struct;
    }

    @Override
    public void visit(CallingFuncDesignator callingFuncDesignator) {
        Obj func = callingFuncDesignator.getDesignator().obj;
        String funcIdent = func.getName();
        Struct funcType = func.getType();
        int designatorKind = func.getKind();

        methodCallStack.push(currMethodCall);

        if (designatorKind != Obj.Meth) {
            KindMismatch(funcIdent, "function").report(callingFuncDesignator);
            callingFuncDesignator.struct = Tab.noType;
            currMethodCall = new ErrFunctionCall();

        } else {
            callingFuncDesignator.struct = funcType;
            currMethodCall = new MethodCall(func);
            FunctionCall(func).report(callingFuncDesignator);
        }
    }

    @Override
    public void visit(ActPar actPar) {
        actPar.struct = actPar.getExpr().struct;
        currMethodCall.addArg(actPar.struct).report(actPar);
    }

    @Override
    public void visit(ActualParamList actualParamList) {
        currMethodCall.argEnd().report(actualParamList);
        actualParamList.obj = currMethodCall.getMethod();
        currMethodCall = methodCallStack.pop();
    }

    @Override
    public void visit(NoActualParam noActualParam) {
        currMethodCall.argEnd().report(noActualParam);
        noActualParam.obj = currMethodCall.getMethod();
        currMethodCall = methodCallStack.pop();
    }

    //endregion


    //region Condition, CondTerm & CondFact

    @Override
    public void visit(RelExpr relExpr) {
        Struct op1 = relExpr.getExpr().struct;
        Struct op2 = relExpr.getExpr1().struct;

        if (!op1.compatibleWith(op2)) {
            UncompatibleExprInRelop().report(relExpr);

        } else if (op1.isRefType() || op2.isRefType()) {
            Class<? extends Relop> relopClass = relExpr.getRelop().getClass();
            if (!relopClass.equals(Equal.class) && !relopClass.equals(NotEqual.class))
                RefTypeRelopError().report(relExpr);
        }
    }

    @Override
    public void visit(OnlyExpr onlyExpr) {
        if (onlyExpr.getExpr().struct != Tab.boolType)
            ConditionNotBoolean().report(onlyExpr);
    }

    //endregion

    //region Expression

    @Override
    public void visit(AddTerm expr) {
        Struct expressionType = expr.getExpr().struct;
        Struct termType = expr.getTerm().struct;

        if (termType != Tab.intType) {
            TypeMismatch("Expression term", "int").report(expr);
            expr.getTerm().struct = Tab.intType;
        }

        if (expressionType != Tab.intType) {
            TypeMismatch("Expression term", "int").report(expr);
            expr.getExpr().struct = Tab.intType;
        }

        expr.struct = Tab.intType;
    }

    @Override
    public void visit(FirstAddTerm expr) {
        // does not have to be an int
        expr.struct = expr.getTerm().struct;
    }

    @Override
    public void visit(FirstNegAddTerm expr) {
        if (expr.getTerm().struct != Tab.intType)
            TypeMismatch("Negated expression term", "int").report(expr);
        expr.struct = Tab.intType;
    }

    //endregion

    //region Term

    @Override
    public void visit(MullFactor mullFactor) {
        if (mullFactor.getTerm() instanceof FirstMullFactor && mullFactor.getTerm().struct != Tab.intType)
            TypeMismatch("Term factor", "int").report(mullFactor);

        if (mullFactor.getFactor().struct != Tab.intType)
            TypeMismatch("Term factor", "int").report(mullFactor);


        mullFactor.struct = Tab.intType;
    }

    @Override
    public void visit(FirstMullFactor firstMullFactor) {
        firstMullFactor.struct = firstMullFactor.getFactor().struct;
    }

    //endregion

    //region Factor

    @Override
    public void visit(DesignatorFactor designatorFactor) {
        designatorFactor.struct = designatorFactor.getDesignator().obj.getType();
    }

    @Override
    public void visit(FunctionCallFactor functionCallFactor) {
        functionCallFactor.struct = functionCallFactor.getFuncCall().struct;
    }

    @Override
    public void visit(NumberFactor numberFactor) {
        numberFactor.struct = Tab.intType;
    }

    @Override
    public void visit(CharFactor charFactor) {
        charFactor.struct = Tab.charType;
    }

    @Override
    public void visit(BooleanFactor booleanFactor) {
        booleanFactor.struct = Tab.boolType;
    }

    @Override
    public void visit(NewObjectFactor newObjectFactor) {
        newObjectFactor.struct = newObjectFactor.getNewObjectType().struct;
        Obj obj = newObjectFactor.getActPars().obj;
        ConstructorCall(obj).report(newObjectFactor);
    }

    @Override
    public void visit(NewObjectType newObjectType) {
        String typeName = newObjectType.getType().getTypeName();
        Struct objType = newObjectType.getType().struct;
        int objKind = objType.getKind();

        methodCallStack.push(currMethodCall);
        if (objKind != Struct.Class) {
            KindMismatch(typeName, "class").report(newObjectType);
            newObjectType.struct = Tab.noType;
            currMethodCall = new ErrFunctionCall();

        } else {
            newObjectType.struct = objType;
            currMethodCall = new ConstructorCall(objType, typeName);
        }

    }

    @Override
    public void visit(NewArrayFactor newArrayFactor) {
        Struct elemType = newArrayFactor.getType().struct;
        Struct exprType = newArrayFactor.getExpr().struct;

        if (exprType != Tab.intType) {
            ArrayIndexType().report(newArrayFactor);
            newArrayFactor.struct = Tab.noType;
        } else
            newArrayFactor.struct = new Struct(Struct.Array, elemType);
    }

    @Override
    public void visit(GroupedExprFactor groupedExprFactor) {
        groupedExprFactor.struct = groupedExprFactor.getExpr().struct;
    }

    //endregion

    //region Designator

    @Override
    public void visit(MemberAccessDesignator memberAccessDesignator) {
        Obj classObj = memberAccessDesignator.getDesignator().obj;
        Struct classType = classObj.getType();
        String memberName = memberAccessDesignator.getMemberName();
        Collection<Obj> members;

        if (classType.getKind() != Struct.Class) {
            KindMismatch(classObj.getName(), "class").report(memberAccessDesignator);
            memberAccessDesignator.obj = Tab.noObj;
            return;
        }

        if ("this".equals(classObj.getName())) {
            members = Tab.currentScope.getOuter().getLocals().symbols();
        } else {
            members = classType.getMembers();
        }

        Obj member = members.stream()
                .filter(obj -> memberName.equals(obj.getName()))
                .findFirst()
                .orElse(Tab.noObj);

        if (member == Tab.noObj) {
            MemberDoesNotExist(memberName).report(memberAccessDesignator);
        } else {
            MemberAccess(classType, member).report(memberAccessDesignator);
        }

        memberAccessDesignator.obj = member;
    }

    @Override
    public void visit(IndexingDesignator indexingDesignator) {
        Obj array = indexingDesignator.getDesignator().obj;
        Struct arrayType = array.getType();
        int arrayKind = arrayType.getKind();
        Struct arrayElementType = arrayType.getElemType();
        Struct indexType = indexingDesignator.getExpr().struct;

        indexingDesignator.obj = Tab.noObj;

        if (arrayKind != Struct.Array) {
            KindMismatch(array.getName(), "array").report(indexingDesignator);
        } else if (indexType != Tab.intType) {
            TypeMismatch("Array index", "int");
        } else {
            indexingDesignator.obj = new Obj(Obj.Elem, "elem", arrayElementType);
            ArrayElementAccess(array.getName()).report(indexingDesignator);
        }
    }

    @Override
    public void visit(IdentifierDesignator identifierDesignator) {
        String name = identifierDesignator.getName();
        Obj obj = Tab.find(name);
        identifierDesignator.obj = obj;

        if (obj == Tab.noObj){
            UndeclaredIdent(name).report(identifierDesignator);
            return;
        }

        if (isGlobalVar(obj)) GlobalVarAccess(obj).report(identifierDesignator);
        else if (isConstant(obj)) ConstAccess(obj).report(identifierDesignator);
        else if (isClassField(obj)) MemberAccess(null, obj).report(identifierDesignator);
        else if (isLocalVar(obj)) {
            if (obj.getFpPos() == 0) LocalVarAccess(obj).report(identifierDesignator);
            else FormalParAccess(obj).report(identifierDesignator);
        }

    }

    //endregion

    //region Statement

    private int whileBlockLevel = 0;
    private boolean inWhileBlock() { return whileBlockLevel > 0; }

    private final Stack<Obj> forEachIdent = new Stack<>();

    private final Stack<Obj> forEachArray = new Stack<>();

    private boolean isForEachIdentUsage(Obj obj){
        for (Obj o: forEachIdent) if (o == obj) return true;
        return false;
    }

    private boolean inForEachBlock() {
        return !forEachIdent.empty();
    }

    @Override
    public void visit(WhileStmt whileStmt) {
        whileBlockLevel--;
    }

    @Override
    public void visit(WhileBodyStart whileBodyStart) {
        whileBlockLevel++;
    }

    @Override
    public void visit(BreakStmt breakStmt) {
        if (!inWhileBlock() && !inForEachBlock())
            BreakOutsideCondBlock().report(breakStmt);
    }

    @Override
    public void visit(ContinueStmt continueStmt) {
        if (!inWhileBlock() && !inForEachBlock())
            ContinueOutsideCondBlock().report(continueStmt);
    }

    @Override
    public void visit(ReturnStmt returnStmt) {
        Obj methodObj = currMethod.getObj();

        if (!returnsVoid(methodObj) && !isConstructor(methodObj))
            ValueReturnedFromVoidMethod().report(returnStmt);
    }

    @Override
    public void visit(ReturnExprStmt returnExprStmt) {
        Obj methodObj = currMethod.getObj();
        Struct currMethodType = currMethod.getObj().getType();
        Struct exprType = returnExprStmt.getExpr().struct;

        if (returnsVoid(methodObj))
            ValueNotReturnedFromNonVoidMethod().report(returnExprStmt);

        else if (isConstructor(methodObj))
            ConstructorReturnsExpression().report(returnExprStmt);

        else if (!isAssignableTo(currMethodType, exprType))
            ReturnWithIncompatibleValue().report(returnExprStmt);
    }

    @Override
    public void visit(ReadStmt readStmt) {
        Obj designator = readStmt.getDesignator().obj;

        if (!isAssignableType(designator, readStmt) || !checkSimpleType(designator, "Designator", readStmt))
            return;

        if (inForEachBlock() && isForEachIdentUsage(designator))
            ReadOnlyForEachIdent().report(readStmt);

        FunctionCall("read").report(readStmt);
    }

    @Override
    public void visit(PrintStmt printStmt) {
        Struct expr = printStmt.getExpr().struct;
        checkSimpleType(expr, "Expr", printStmt);
        FunctionCall("print").report(printStmt);
    }


    @Override
    public void visit(ForEachStmt forEachStmt) {
        forEachIdent.pop();
        forEachArray.pop();
    }

    @Override
    public void visit(ForEachStmtDesignator forEachStmtDesignator) {
        Obj designator = forEachStmtDesignator.getDesignator().obj;
        int designatorKind = designator.getType().getKind();

        if (designatorKind != Struct.Array) {
            ForEachOnNonArray().report(forEachStmtDesignator);
            forEachStmtDesignator.obj = new Obj(Obj.Var, "", new Struct(Struct.Array, Tab.noType));
        } else {
            forEachStmtDesignator.obj = designator;
        }

        forEachArray.push(forEachStmtDesignator.obj);
    }

    @Override
    public void visit(ForEachStmtIdent forEachStmtIdent) {
        Struct designatorElementType = forEachArray.peek().getType().getElemType();
        String identName = forEachStmtIdent.getName();

        Obj ident = Tab.find(identName);
        forEachStmtIdent.obj = Tab.noObj;

        if (ident == Tab.noObj){
            UndeclaredIdent(identName).report(forEachStmtIdent);
            forEachStmtIdent.obj = new Obj(Obj.Var, identName, designatorElementType);
        } else if (ident.getKind() != Obj.Var) {
            ForEachIdentKind(identName).report(forEachStmtIdent);
            // todo: 'kind' value for successful parsing
        }
        else if (designatorElementType != Tab.noType && !isAssignableTo(designatorElementType, ident.getType())){
            ForEachIdentTypeMismatch(identName).report(forEachStmtIdent);
            // todo: 'type' value for successful parsing
        }
        else forEachStmtIdent.obj = ident;

        forEachIdent.push(forEachStmtIdent.obj);
    }

    //endregion

    //region DesignatorStatement

    @Override
    public void visit(Assignment assignment) {
        Obj designator = assignment.getDesignator().obj;
        Struct designatorType = designator.getType();
        Struct exprType = assignment.getExpr().struct;

        if (!isAssignableType(designator, assignment))
            return;

        if (exprType != Tab.noType && !isAssignableTo(exprType, designatorType))
            AssignmentIncompatible().report(assignment);

        if (inForEachBlock() && isForEachIdentUsage(designator))
            ReadOnlyForEachIdent().report(assignment);
    }

    @Override
    public void visit(Increment increment) {
        Obj designator = increment.getDesignator().obj;
        Struct designatorType = designator.getType();

        if (!isAssignableType(designator, increment))
            return;

        if (designatorType != Tab.intType) {
            IncrementTypeError().report(increment);
            return;
        }

        if (inForEachBlock() && isForEachIdentUsage(designator))
            ReadOnlyForEachIdent().report(increment);
    }

    @Override
    public void visit(Decrement decrement) {
        Obj designator = decrement.getDesignator().obj;
        Struct designatorType = designator.getType();

        if (!isAssignableType(designator, decrement))
            return;

        if (designatorType != Tab.intType) {
            DecrementTypeError().report(decrement);

        } else if (inForEachBlock() && isForEachIdentUsage(designator))
            ReadOnlyForEachIdent().report(decrement);
    }

    private final List<Struct> arrayAssignmentElementTypes = new LinkedList<>();

    @Override
    public void visit(ArrayAssignmentStatement arrayAssignmentStatement) {
        Obj array = arrayAssignmentStatement.getDesignator().obj;
        Struct arrayType = array.getType();
        Struct arrayElemType = arrayType.getElemType();

        if (arrayType.getKind() != Struct.Array) {
            KindMismatch("array").report(arrayAssignmentStatement);

        } else {
            for (int idx = 0; idx < arrayAssignmentElementTypes.size(); idx++) {
                Struct curr = arrayAssignmentElementTypes.get(idx);
                if (curr != null && !isAssignableTo(arrayElemType, curr))
                    ArrayAssignmentIncompatible(idx).report(arrayAssignmentStatement);
            }
        }

        arrayAssignmentElementTypes.clear();
    }

    @Override
    public void visit(NotNullDesignator notNullDesignator) {
        Obj designator = notNullDesignator.getDesignator().obj;
        Struct designatorType = designator.getType();

        if (!isAssignableType(designator, notNullDesignator))
            designatorType = null;
        else if (inForEachBlock() && isForEachIdentUsage(designator))
            ReadOnlyForEachIdent().report(notNullDesignator);

        notNullDesignator.obj = designator;
        designator.setFpPos(arrayAssignmentElementTypes.size());

        arrayAssignmentElementTypes.add(designatorType);
    }

    @Override
    public void visit(NullDesignator nullDesignator) {
        arrayAssignmentElementTypes.add(null);
    }

    //endregion

}
