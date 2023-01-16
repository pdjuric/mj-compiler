package rs.ac.bg.etf.pp1.extended;

import rs.ac.bg.etf.pp1.ast.SyntaxNode;
import rs.ac.bg.etf.pp1.error_reporting.Status;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static rs.etf.pp1.mj.runtime.Code.*;
import static rs.etf.pp1.mj.runtime.Code.put;

public class Helper {

    /**
     * Checks whether provided name is already defined in the current scope.
     * @param name name to check for
     * @param node node to which to report error to
     * @return true if the name is already defined, false otherwise
     */
    public static boolean isDeclaredInCurrentScope(String name, SyntaxNode node) {
        boolean flag = Tab.currentScope().findSymbol(name) != null;
        if (flag) Status.AlreadyDeclaredIdent(name).report(node);
        return flag;
    }

    /**
     * Checks whether provided name is already defined in the current scope.
     * @param name name to check for
     * @param node node to which to report error to
     * @return true if the name is already defined, false otherwise
     */
    public static boolean nameEqualsThis(String name, SyntaxNode node) {
        boolean flag = "this".equals(name);
        if (flag) Status.ThisIdent().report(node);
        return flag;
    }

    /**
     * Checks whether the symbol can be assigned a value.
     * @param obj obj to check for
     * @param node node to which to report error to
     * @return true if obj is variable, class field, or array element
     */
    public static boolean isAssignableType(Obj obj, SyntaxNode node) {
        int kind = obj.getKind();
        boolean flag = kind == Obj.Var || kind == Obj.Fld || kind == Obj.Elem;
        if (!flag) Status.NonAssignableKind().report(node);
        return flag;
    }

    /**
     * Checks whether the type of the passed Struct is int, char or bool.
     * @param obj obj whose type to check
     * @param what how to address the designator when printing the error
     * @param node node to which to report error to
     */
    public static boolean checkSimpleType(Obj obj, String what, SyntaxNode node) {
        Struct type = obj.getType();
        return checkSimpleType(type, what, node);
    }

    /**
     * Checks whether the type is int, char or bool.
     * @param type type which needs to be checked
     * @param what how to address the designator when printing the error
     * @param node node to which to report error to
     * @return true | false
     */
    public static boolean checkSimpleType(Struct type, String what, SyntaxNode node) {
        boolean flag = type == Tab.intType || type == Tab.boolType || type == Tab.charType;
        if (!flag) Status.NotSimpleType(what).report(node);
        return flag;
    }

    /**
     * Checks if a value can be assigned to the designated type. Scenarios:
     * (1) same types
     * (2) arrays of the same elements
     * (3) value type is null, and variable type is Class or Array
     * (4) value type is a subclass of variable type
     */
    public static boolean isAssignableTo(Struct valueType, Struct varType) {
        return valueType.assignableTo(varType) || getInheritanceLevel(valueType, varType) != null;
    }

    public static Integer getInheritanceLevel(Struct subclass, Struct superclass) {
        if (subclass.getKind() != Struct.Class || superclass.getKind() != Struct.Class)
            return null;
        int cnt = 0;
        for (Struct currSubclass = subclass; currSubclass != Tab.noType; currSubclass = currSubclass.getElemType(), cnt++)
            if (currSubclass == superclass)
                return cnt;

        return null;
    }

    public static boolean isClassMethod(Obj methodObj) {
        Collection<Obj> localSymbols = methodObj.getLocalSymbols();
        return methodObj.getKind() == Obj.Meth &&
                !localSymbols.isEmpty() &&
                isThis(localSymbols.iterator().next()) &&
                !isConstructor(methodObj);
    }

    public static boolean isClassField(Obj fld) {
        return fld.getKind() == Obj.Fld && !isTVF(fld);
    }


    public static boolean isThis(Obj par) {
        return "this".equals(par.getName()) && par.getAdr() == 0 && par.getKind() == Obj.Var ;
    }

    public static boolean isTVF(Obj fld) {
        return "TVF".equals(fld.getName()) && fld.getType() == Tab.noType && fld.getLevel() == 1;
    }

    public static boolean isMain(Obj meth) {
        return "main".equals(meth.getName()) && meth.getType() == Tab.noType && meth.getLevel() == 0;

    }

    public static boolean isLocalVar(Obj var) {
        boolean isGlobalMethodLocal = var.getKind() == Obj.Var && var.getLevel() == 1;
        boolean isClassMethodLocal = var.getKind() == Obj.Var && var.getLevel() == 2;

        return isGlobalMethodLocal || isClassMethodLocal;
    }

    public static boolean isGlobalVar(Obj var) {
        return var.getKind() == Obj.Var && var.getLevel() == 0;
    }


    public static boolean isConstant(Obj var) {
        return var.getKind() == Obj.Con;
    }



    public static Obj getTVF(Obj clss) {
        return clss.getType().getMembers().stream().filter(Helper::isTVF).findFirst().orElse(null);
    }

    public static List<Obj> getClassMethods(Obj clazz) {
        final List<Obj> methods = new LinkedList<>();
        clazz.getType().getMembers().stream()
                .filter(Helper::isClassMethod)
                .forEach(methods::add);
        return methods;
    }

    public static boolean isConstructor(Obj method) {
        String name = method.getName();
        return Obj.Meth == method.getKind() && name != null && name.startsWith("__ctor_");
    }

    public static boolean returnsVoid(Obj method) {
        return method.getType() == Tab.noType;
    }



    //region Code Generation
    public static void loadArray(Obj obj) {
        Code.put((obj.getType() == Tab.charType) ? Code.baload : Code.aload);
    }

    public static void storeStatic(int n) {
        Code.put(putstatic);
        Code.put2(n);
    }


    public static void loadArray(Struct array) {
        Code.put((array.getElemType() == Tab.charType) ? Code.baload : Code.aload);
    }

    public static void storeArray(Obj obj) {
        Code.put((obj.getType() == Tab.charType) ? Code.bastore : Code.astore);
    }

    public static void storeArray(Struct array) {
        Code.put((array.getElemType() == Tab.charType) ? Code.bastore : Code.astore);
    }

    public static void putRead(Obj obj) {
        Code.put((obj.getType() == Tab.charType) ? Code.bread : Code.read);

    }

    public static void estackFlip() {
        Code.store(Tab.__temp0);
        Code.store(Tab.__temp1);
        Code.load(Tab.__temp0);
        Code.load(Tab.__temp1);
    }

    public static void getField(int num){
        Code.put(Code.getfield);
        Code.put2(num);
    }

    public static void putField(int num){
        Code.put(Code.putfield);
        Code.put2(num);
    }

    public static void putMultiple(int... num){
        for (int i: num) Code.put(i);
    }

    /**
     * Checks if the last instruction was a return.
     */
    public static boolean returned() {
        return Code.buf[Code.pc - 1] == Code.return_;
    }

    public static void fixup(int sourceAdr, int dstAdr){
        int oldPc = Code.pc;
        Code.pc = dstAdr;
        Code.fixup(sourceAdr);
        Code.pc = oldPc;
    }

    public static int putUnknownJump(){
        Code.putJump(0);
        return Code.pc - 2;
    }

    public static int putFalseUnknownJump(int relop){
        Code.putFalseJump(relop, 0);
        return Code.pc - 2;
    }

    public static void putCall(int addr){
        Code.put(call);
        Code.put2(addr - Code.pc + 1);
    }

    public static void putMultiple(int op, int cnt){
        for (int i = 0; i < cnt; i++)
            Code.put(op);
    }

    public static void fixupAll(Collection<Integer> srcAddrs) {
        for (int adr: srcAddrs) Code.fixup(adr);
        srcAddrs.clear();
    }

    public static void fixupAll(Collection<Integer> srcAddrs, int dstAddrs) {
        for (int adr: srcAddrs) fixup(adr, dstAddrs);
        srcAddrs.clear();
    }

    public static void putEnter(int nPars, int nVars) {
        put(enter);
        put(nPars);
        put(nVars);
    }

    public static void putExit() {
        put(exit);
        put(return_);
    }



    public static final int const_0 = Code.const_n;
    public static final int store_0 = Code.store_n;

    public static final int load_0 = Code.load_n;


    public static int findMethodAddr(Obj method) {
        Obj startMethod = method;
        String funcName = method.getName();

        while (true) {
            if (method.getAdr() != -1) {
                startMethod.setAdr(method.getAdr());
                return method.getAdr();
            }

            Obj firstArg = method.getLocalSymbols().stream().iterator().next();
            if (!isThis(firstArg))
                throw new RuntimeException("what the fuck");
            method = firstArg.getType().getMembers().stream().filter(m -> m.getKind() == Obj.Meth && funcName.equals(m.getName())).findFirst().orElse(null);
        }

    }
    //endregion

}
