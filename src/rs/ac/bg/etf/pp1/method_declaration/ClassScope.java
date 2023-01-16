package rs.ac.bg.etf.pp1.method_declaration;

import rs.ac.bg.etf.pp1.ast.SyntaxNode;
import rs.ac.bg.etf.pp1.error_reporting.Status;
import rs.ac.bg.etf.pp1.extended.Tab;
import rs.ac.bg.etf.pp1.extended.Helper;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;
import rs.etf.pp1.symboltable.structure.HashTableDataStructure;

import java.util.LinkedList;

import static rs.ac.bg.etf.pp1.error_reporting.Status.*;
import static rs.ac.bg.etf.pp1.extended.Helper.*;


public class ClassScope extends Scope {

    static {
        currScope = new GlobalScope();
    }
    private final String className;
    private final Struct classType;
    private Struct superclassType;
    private int ctorCnt = 0;
    final LinkedList<Obj> inheritedMethods = new LinkedList<>();
    final LinkedList<Obj> alreadyOverriddenMethods = new LinkedList<>();
    private final LinkedList<Obj> declaredConstructors = new LinkedList<>();
    public Obj classObj;


    public ClassScope(Obj classObj) {
        currScope = this;
        this.classObj = classObj;
        this.classType = classObj.getType();
        this.className = classObj.getName();

        // the class does not inherit from another class (yet)
        classType.setElementType(Tab.noType);

        Tab.openScope();

        // insert TVF
        Tab.insert(Obj.Fld, "TVF", Tab.noType, 0, 1);
    }

    /**
     * Adds a superclass to the current class.
     * This method should be called only once.
     * It adds all the fields from the superclass to the current scope.
     * @param s the superclass
     */
    public void addSuperclass(Struct s) {
        superclassType = s;
        classType.setElementType(superclassType);

        // add all the fields from superclass to the current scope
        superclassType.getMembers().stream()
                .filter(Helper::isClassField)
                .forEach(Tab::insertCopy);
    }

    /**
     * Clones the Obj and its local symbols.
     * @param obj Obj to be cloned
     * @param newType if not null, the obj's type will be set to this type (used for changing the type of 'this')
     */
    private static Obj clone(Obj obj, Struct newType) {
        Obj newObj = new Obj(obj.getKind(), obj.getName(), newType == null ? obj.getType() : newType, obj.getAdr(), obj.getLevel());
        newObj.setFpPos(obj.getFpPos());

        HashTableDataStructure newLocals = new HashTableDataStructure();
        for (Obj o: obj.getLocalSymbols()) newLocals.insertKey(clone(o, null));
        newObj.setLocals(newLocals);
        return newObj;
    }

    /**
     * Adds a new inherited method to the current class.
     */
    private void newInheritedMethod(Obj method) {
        Obj o = Tab.insert(method.getKind(), method.getName(), method.getType());
        o.setFpPos(method.getFpPos());
        o.setLevel(method.getLevel());
        o.setAdr(method.getAdr());

        HashTableDataStructure newLocals = new HashTableDataStructure();
        for (Obj local: method.getLocalSymbols()) {
            newLocals.insertKey(clone(local, isThis(o) ? classType : null));
        }

        o.setLocals(newLocals);
        inheritedMethods.add(o);
    }

    public String getClassName() {
        return className;
    }

    /**
     * Ends the field declaration.
     * Should be called after all the fields are declared.
     * Adds all the inherited methods to the current scope.
     */
    public void endFieldDeclaration() {
        if (superclassType == null) return;
        superclassType.getMembers().stream()
                .filter(Helper::isClassMethod)
                .forEach(this::newInheritedMethod);
    }

    /**
     * Adds a new constructor to the current class.
     * Should be called after {@link #endFieldDeclaration()}. (unchecked)
     */
    public Method newConstructor() {
        Obj ctor = new Obj(Obj.Meth, "__ctor_" + ctorCnt++, classType);
        return new ClassConstructor(ctor, declaredConstructors);
    }

    /**
     * Adds a new method to the current class.
     * Should be called after {@link #endFieldDeclaration()} and {@link #newConstructor()}. (unchecked)
     * @param methodName the name of the method
     * @param returnType the return type of the method
     * @param node node for error reporting
     */
    public Method newMethod(String methodName, Struct returnType, SyntaxNode node) {
        Obj method;

        boolean alreadyOverridden = alreadyOverriddenMethods.stream()
                .anyMatch(m -> m.getName().equals(methodName));

        if (alreadyOverridden) {
            AlreadyOverriddenMethod(methodName, Tab.findClassName(classType)).report(node);
            method = new Obj(Obj.Meth, methodName, returnType);
            return new ClassMethod(method, classType);
        }

        Obj inheritedMethod = inheritedMethods.stream()
                .filter(m -> m.getName().equals(methodName))
                .findFirst()
                .orElse(null);

        // design: check return type
        if (inheritedMethod != null) {
            method = new Obj(Obj.Meth, methodName, returnType);
            if (!returnType.equals(inheritedMethod.getType()) && getInheritanceLevel(returnType, inheritedMethod.getType()) == null) {
                Status.OverrideReturnTypeMismatch(methodName, className).report(node);
            }
            return new OverrideClassMethod(method, inheritedMethod, classType);
        } else {
            method = Tab.insert(Obj.Meth, methodName, returnType);
            return new ClassMethod(method, classType);
        }
    }

    /**
     * Ends a class declaration.
     * Closes the current scope and opens a new Global scope.
     * Should be the last called method.
     */
    public void endClassDeclaration() {
        Tab.chainLocalSymbols(classType);
        Tab.closeScope();
        currScope = new GlobalScope();
    }

}
