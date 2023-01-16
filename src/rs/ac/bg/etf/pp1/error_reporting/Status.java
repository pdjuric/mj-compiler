package rs.ac.bg.etf.pp1.error_reporting;

import rs.ac.bg.etf.pp1.ast.SyntaxNode;
import rs.ac.bg.etf.pp1.extended.DumpSymbolTableVisitor;
import rs.ac.bg.etf.pp1.extended.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

import java.util.HashSet;

import static rs.ac.bg.etf.pp1.extended.Helper.isClassMethod;

public class Status {
    public static Status Ok() {
        return new Status("", false);
    }

    public static Status UndeclaredType(String typeName) {
        return new Status("Undeclared type [" + typeName + "].");
    }

    public static Status UndeclaredIdent(String ident) {
        return new Status(ident + " is undeclared.");
    }

    public static Status AlreadyDeclaredIdent(String ident) {
        return new Status("Identifier " + ident + " already defined in this scope.");
    }

    public static Status TypeMismatch(String ident, String expectedType) {
        char c = expectedType.charAt(0);
        boolean flag = false;
        for (char v : vocals) flag |= c == v;
        return new Status(ident + " is not a" + (flag ? "n" : "") + " " + expectedType);
    }

    public static Status ConstantInitializerTypeMismatch(String constantIdent, String constantType) {
        return new Status("Constant " + constantIdent + " must be initialized with constant of type " + constantType);
    }

    public static Status OverrideExcessParameter(String parName, String funcName) {
        return new Status(String.format("Excess parameter %s in override function %s.", parName, funcName));
    }


    static char[] vocals = {'a', 'e', 'i', 'o', 'u', 'A', 'E', 'I', 'O', 'U'};

    public static Status KindMismatch(String ident, String expectedKind) {
        char c = expectedKind.charAt(0);
        boolean flag = false;
        for (char v : vocals) flag |= c == v;
        return new Status(ident + " is not a" + (flag ? "n" : "") + " " + expectedKind);
    }

    public static Status KindMismatch(String expectedKind) {
        return new Status(" expected.");
    }


    public static Status OperandTypeMismatch(String expectedType) {
        return new Status("Operand is not of type " + expectedType);

    }

    public static Status NonAssignableKind() {
        return new Status("Designator is not of assignable kind (variable, field or array element).");
    }

    public static Status AssignmentIncompatible() {
        return new Status("Operands are not compatible for assignment.");
    }

    public static Status IncrementTypeError() {
        return new Status("Incrementing can only be performed on int operand.");
    }

    public static Status DecrementTypeError() {
        return new Status("Decrementing can only be performed on int operand.");
    }

    public static Status BreakOutsideCondBlock() {
        return new Status("Break statement used outside while/foreach loop.");
    }

    public static Status ContinueOutsideCondBlock() {
        return new Status("Continue statement used outside while/foreach loop.");
    }

    public static Status ValueReturnedFromVoidMethod() {
        return new Status("Value returned from void method.");
    }

    public static Status ValueNotReturnedFromNonVoidMethod() {
        return new Status("Value returned from void method.");
    }

    public static Status ConstructorReturnsExpression() {
        return new Status("Constructor returns expression.");
    }

    public static Status ReturnWithIncompatibleValue() {
        return new Status("Value cannot be returned because its type does not match function return type.");
    }

    public static Status ThisIdent() {
        return new Status("\"this\" is not an allowed variable name.");

    }
    public static Status NotSimpleType(String what) {
        return new Status(what + " is not int, char or bool.");
    }

    public static Status ConditionNotBoolean() {
        return new Status("Condition type is not bool.");
    }

    public static Status ForEachOnNonArray() {
        return new Status("ForEach loop invoked on non-array variable.");
    }

    public static Status ForEachIdentKind(String ident) {
        return new Status("ForEach loop variable identifier " + ident + " does not correspond to local or global variable.");
    }

    public static Status ForEachIdentTypeMismatch(String ident) {
        return new Status("Type of ForEach loop variable " + ident + " does not match the type of array elements.");
    }

    public static Status ReadOnlyForEachIdent() {
        return new Status("Value of ForEach loop variable cannot be changed.");
    }

    public static Status MemberDoesNotExist(String member) {
        return new Status("The designated object does not have member " + member + ".");
    }

    public static Status OverrideReturnTypeMismatch(String methodName, String className) {
        return new Status("Override of function " + methodName + " in class " + className + " has incompatible return type.");
    }

    public static Status ArrayIndexType() {
        return new Status("Type of array index expression must be int.");
    }


    public static Status ReturnMissing(String methodIdent) {
        return new Status("Method " + methodIdent + " has no return statement.");
    }

    public static Status ArrayAssignmentIncompatible(Integer idx) {
        return new Status("Array assignment operand at index " + idx + " is not compatible for assignment with array.");
    }

    public static Status TooManyFunctionArguments() {
        return new Status("No matching function to call - too many arguments.");
    }

    public static Status ArgumentTypeMismatch(int idx) {
        return new Status("No matching function to call - argument on index " + idx + ".");
    }

    public static Status FunctionArgumentsMissing(int cnt) {
        return new Status("No matching function to call - " + cnt + " argument" + (cnt > 1 ? "s" : "") + " missing.");
    }

    public static Status InvalidConstructorName(String constructorName, String className) {
        return new Status("Constructor " + constructorName + " declared in class " + className + " (will proceed like it's the correct name).");

    }

    public static Status RecursiveSubclass(String className) {
        return new Status("A class cannot be its own superclass [" + className + "].");
    }

    public static Status DuplicateConstructor(String className) {
        return new Status("Constructor with the same formal parameters has already been declared ( class " + className + " ).");
    }

    public static Status OverrideParameterTypeMismatch(String parameterName, String methodName) {
        return new Status("OverrideParameterTypeMismatch " + parameterName + " " + methodName);
    }

    public static Status OverrideParametersMissing(String methodName) {
        return new Status("OverrideParametersMissing " + " " + methodName);
    }

    public static Status UncompatibleExprInRelop() {
        return new Status("UncompatibleExprInRelop");
    }

    public static Status RefTypeRelopError() {
        return new Status("RefTypeRelopError");
    }

    public static Status NoMatchingConstructor(String className) {
        return new Status("No matching constructor of class " + className + " for provided arguments.");
    }

    public static Status AmbiguousConstructorCall(String className) {
        return new Status("Ambiguous constructor call for class " + className + ".");
    }

    public static Status AlreadyOverriddenMethod(String methodName, String className) {
        return new Status("Method " + methodName + " in class " + className + " already overridden.");
    }

    public static Status MainNotDefined() {
        return new Status("Function void main() is not defined.");
    }





    private static DumpSymbolTableVisitor v = new DumpSymbolTableVisitor();

    public static Status GlobalVarAccess(Obj obj) {
        return new Status("Access to global variable " + obj.getName() + " [" + v.toString(obj) + "].", false);
    }

    public static Status ConstAccess(Obj obj) {
        return new Status("Access to constant " + obj.getName() + " [" + v.toString(obj) + "].", false);
    }

    public static Status LocalVarAccess(Obj obj) {
        return new Status("Access to local variable " + obj.getName() + " [" + v.toString(obj) + "].", false);
    }

    public static Status FormalParAccess(Obj obj) {
        return new Status("Access to formal parameter " + obj.getName() + " [" + v.toString(obj) + "].", false);
    }

    public static Status ConstructorCall(Obj obj) {
        String name = "[unknown class]";
        String classRepr = "[]";
        if (obj != null && obj.getType() != null) {
            name = Tab.findClassName(obj.getType());
            classRepr = v.toString(obj);
        }
        return new Status("Call to constructor of class " + name + " [" + classRepr + "].", false);
    }

    public static Status FunctionCall(Obj obj) {
        return new Status("Call to " + (isClassMethod(obj) ? "method" : "global function") + " " + obj.getName() + " [" + v.toString(obj) + "].", false);
    }

    public static Status FunctionCall(String name) {
        return new Status("Call to global function " + name + ".", false);
    }

    public static Status MemberAccess(Struct classType, Obj member) {
        return new Status("Access to member " + member.getName() + (classType != null ? " of class " + Tab.findClassName(classType) : "") + " [" + v.toString(member) + "].", false);
    }

    public static Status ArrayElementAccess(String arrayName) {
        return new Status("Access to element of array " + arrayName + ".", false);
    }

    public static HashSet<Status> unreportedErrs = new HashSet<>();

    private static synchronized void addStatusToSet(Status s) {
        unreportedErrs.add(s);
    }

    private static synchronized void removeStatusFromSet(Status s) {
        unreportedErrs.remove(s);
    }
    private final String message;
    private final boolean isError;

    Status(String message) {
        this(message, true);
    }

    Status(String message, boolean isError) {
        this.message = message;
        this.isError = isError;
        if (isError) addStatusToSet(this);
    }

    public boolean isError() {
        return isError;
    }

    public String toString() {
        return this.message;
    }

    public void report(SyntaxNode node) {
        if (isError) removeStatusFromSet(this);
        if (message.equals("")) return;
        Reporter.reporter.submit(this, node);
    }

}
